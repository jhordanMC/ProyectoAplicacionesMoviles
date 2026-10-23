package com.example.saasvilcas;

import android.os.Bundle;
import android.os.Build;
import android.graphics.Color;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.example.saasvilcas.app.FrontendNavigator;
import com.example.saasvilcas.feature.auth.presentation.AuthFragment;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity implements FrontendNavigator {

    private android.animation.ObjectAnimator logoRotation;
    private boolean brandLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        VilcasApplication app=(VilcasApplication)getApplication();
        super.onCreate(app.restoreRequired?null:savedInstanceState);
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(false);
        setContentView(R.layout.activity_host);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override public void onFragmentViewCreated(FragmentManager manager, Fragment fragment,
                    android.view.View view, Bundle state) {
                // Switch the backdrop in the same UI transaction as the incoming screen.
                if(manager==getSupportFragmentManager()) {
                    if(fragment instanceof AuthFragment) findViewById(R.id.windowBackdrop).setBackgroundResource(R.drawable.fondoaaas);
                    else if(fragment instanceof CompanyFragment) findViewById(R.id.windowBackdrop).setBackgroundColor(Color.rgb(0,77,64));
                    androidx.core.view.OneShotPreDrawListener.add(view,()->{
                        if(manager.findFragmentById(R.id.screenHost)!=fragment) return;
                        boolean busy=fragment instanceof AuthFragment && new androidx.lifecycle.ViewModelProvider(MainActivity.this).get(com.example.saasvilcas.feature.auth.presentation.AuthViewModel.class).busy();
                        if(!busy) setAuthLoading(false,"");
                    });
                }
                // The company shell and dialogs own their separate drawer/window transitions.
                if (!(fragment instanceof CompanyFragment) && !(fragment instanceof androidx.fragment.app.DialogFragment))
                    com.example.saasvilcas.shared.ui.ViewEntranceAnimator.play(view);
            }
        }, true);
        // Screens apply safe insets to their controls; the host always covers the full window.
        if(app.restoreRequired) restoreSession(app);
        else if (savedInstanceState == null ||
                (getSupportFragmentManager().findFragmentById(R.id.screenHost) instanceof CompanyFragment
                && ((VilcasApplication)getApplication()).sessions.get()==null)) showLogin();
    }

    private androidx.appcompat.app.AlertDialog restoreError;
    private void restoreSession(VilcasApplication app) {
        com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel model=new androidx.lifecycle.ViewModelProvider(this,
                com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel.factory(app.sessions,app.newProfileRepository()))
                .get(com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel.class);
        setAuthLoading(true,"Restaurando tu sesión…");
        model.result.observe(this,result->{
            if(result==com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel.Result.READY) {
                app.restoreRequired=false; showCompany(app.sessions.get().owner);
            } else if(result==com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel.Result.EXPIRED) {
                app.restoreRequired=false; showLogin();
            } else if(result==com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel.Result.RETRY && restoreError==null) {
                restoreError=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("No se pudo conectar")
                        .setMessage("Tu cuenta sigue guardada. Revisa tu conexión y vuelve a intentar.")
                        .setCancelable(false).setPositiveButton("Reintentar",(dialog,which)->{restoreError=null;model.start();})
                        .setNegativeButton("Cerrar",(dialog,which)->finish()).show();
            }
        });
        if(model.result.getValue()==null) model.start();
    }

    @Override public void showLogin() {
        VilcasApplication app=(VilcasApplication)getApplication();
        app.clearSession();
        new androidx.lifecycle.ViewModelProvider(this,
                com.example.saasvilcas.feature.auth.presentation.AuthViewModel.factory(app.auth,app.sessions))
                .get(com.example.saasvilcas.feature.auth.presentation.AuthViewModel.class).reset();
        clearHistory();
        show(AuthFragment.create(AuthFragment.LOGIN, ""), false);
    }
    @Override public void showRecovery(String email) {
        show(AuthFragment.create(AuthFragment.RECOVERY, email), true);
    }
    @Override public void showVerification(String email) {
        show(AuthFragment.create(AuthFragment.VERIFICATION, email), false);
    }
    @Override public void showCompany(boolean owner) {
        com.example.saasvilcas.feature.auth.domain.AuthSession session=((VilcasApplication)getApplication()).sessions.get();
        if(session==null) { showLogin(); return; }
        clearHistory();
        show(CompanyFragment.create(session.owner), false);
    }
    public void setAuthLoading(boolean loading,String message) {
        android.view.View overlay=findViewById(R.id.brandLoadingOverlay);
        brandLoading=loading; overlay.animate().cancel();
        if(loading) {
            ((android.widget.TextView)findViewById(R.id.brandLoadingMessage)).setText(message.isEmpty()?getString(R.string.brand_loading):message);
            overlay.setAlpha(1f); overlay.setVisibility(android.view.View.VISIBLE);
            if(logoRotation==null && (Build.VERSION.SDK_INT<26 || android.animation.ValueAnimator.areAnimatorsEnabled())) {
                logoRotation=android.animation.ObjectAnimator.ofFloat(findViewById(R.id.brandLoadingLogo),"rotation",0f,360f);
                logoRotation.setDuration(700); logoRotation.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                logoRotation.setInterpolator(new android.view.animation.LinearInterpolator()); logoRotation.start();
            }
        } else if(overlay.getVisibility()==android.view.View.VISIBLE) {
            overlay.animate().alpha(0f).setDuration(160).withEndAction(()->{
                if(brandLoading) return;
                overlay.setVisibility(android.view.View.GONE); overlay.setAlpha(1f);
                if(logoRotation!=null) { logoRotation.cancel(); logoRotation=null; }
                findViewById(R.id.brandLoadingLogo).setRotation(0f);
            }).start();
        }
    }
    @Override protected void onDestroy() {
        if(restoreError!=null) {restoreError.dismiss();restoreError=null;}
        if(logoRotation!=null) { logoRotation.cancel(); logoRotation=null; }
        super.onDestroy();
    }
    private void clearHistory() {
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    private void show(Fragment screen, boolean remember) {
        if(getSupportFragmentManager().findFragmentById(R.id.screenHost) instanceof AuthFragment)
            setAuthLoading(true,getString(R.string.brand_loading));
        android.view.View focus=getCurrentFocus();
        if(focus!=null) focus.clearFocus();
        new WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView()).hide(WindowInsetsCompat.Type.ime());
        androidx.fragment.app.FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction().setReorderingAllowed(true).replace(R.id.screenHost, screen);
        if (remember) transaction.addToBackStack(null);
        transaction.commit();
    }
}
