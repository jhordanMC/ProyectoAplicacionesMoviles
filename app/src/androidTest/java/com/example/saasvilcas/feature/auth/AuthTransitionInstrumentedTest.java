package com.example.saasvilcas.feature.auth;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.auth.presentation.*;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AuthTransitionInstrumentedTest {
    @Test public void rememberEmailAndPasswordToggleWorkWithoutStoringPassword() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                android.content.SharedPreferences prefs=activity.getSharedPreferences("login_preferences",0);
                prefs.edit().clear().commit();
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); activity.showLogin(); activity.getSupportFragmentManager().executePendingTransactions();
                EditText email=activity.findViewById(R.id.etEmail),password=activity.findViewById(R.id.etPassword);
                email.setText("remember@example.com"); password.setText("local-only");
                assertNotNull(password.getTransformationMethod());
                activity.findViewById(R.id.ivTogglePassword).performClick();
                assertNull(password.getTransformationMethod());
                activity.findViewById(R.id.ivTogglePassword).performClick();
                assertNotNull(password.getTransformationMethod());
                ((android.widget.CheckBox)activity.findViewById(R.id.checkBox)).setChecked(true);
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); activity.showLogin(); activity.getSupportFragmentManager().executePendingTransactions();
                assertEquals("remember@example.com",((EditText)activity.findViewById(R.id.etEmail)).getText().toString());
                assertEquals("",((EditText)activity.findViewById(R.id.etPassword)).getText().toString());
                assertTrue(((android.widget.CheckBox)activity.findViewById(R.id.checkBox)).isChecked());
                ((android.widget.CheckBox)activity.findViewById(R.id.checkBox)).setChecked(false);
                assertFalse(prefs.contains("email"));
                assertFalse(prefs.contains("password"));
                com.example.saasvilcas.shared.ui.CodeInputView otp=new com.example.saasvilcas.shared.ui.CodeInputView(activity,null);
                ((EditText)otp.getChildAt(0)).setText("12");
                assertEquals("12",otp.getCode());
                assertEquals(0xffFFFFFF,((EditText)otp.getChildAt(0)).getCurrentTextColor());
                otp.clear(); assertEquals("",otp.getCode());
                assertEquals(0xff004D40,((EditText)otp.getChildAt(0)).getCurrentTextColor());
                prefs.edit().clear().commit();
            });
        }
    }
    @Test public void loginAndVerificationNavigateWithoutChangingOutgoingBackdrop() throws Exception {
        AuthRepository fake=new AuthRepository() {
            public LoginChallenge login(String email,String password) { return new LoginChallenge("fake",300); }
            public AuthSession verify(String token,String code) { return new AuthSession("fake-access","fake-refresh","u","c","Prueba","test@example.com","Tienda",Collections.singletonList("Dueño de empresa")); }
            public void logout(String token) { }
        };
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                VilcasApplication app=(VilcasApplication)activity.getApplication();
                activity.getViewModelStore().put("androidx.lifecycle.ViewModelProvider.DefaultKey:"+AuthViewModel.class.getCanonicalName(),new AuthViewModel(fake,app.sessions));
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); activity.showLogin(); activity.getSupportFragmentManager().executePendingTransactions();
                View login=activity.getSupportFragmentManager().findFragmentById(R.id.screenHost).requireView();
                View border=login.findViewById(R.id.borderView),card=login.findViewById(R.id.cardView);
                assertEquals("Animate the full border and card together",1f,card.getAlpha(),0f);
                assertEquals(0f,border.getAlpha(),0f);
                ((EditText)login.findViewById(R.id.etEmail)).setText("test@example.com");
                ((EditText)login.findViewById(R.id.etPassword)).setText("test-password");
                login.findViewById(R.id.button).performClick();
                assertEquals(View.VISIBLE,activity.findViewById(R.id.brandLoadingOverlay).getVisibility());
            });
            awaitScreen(scenario,false);
            scenario.onActivity(activity->{
                com.example.saasvilcas.shared.ui.CodeInputView code=activity.findViewById(R.id.verificationCode);
                ((EditText)code.getChildAt(0)).setText("12");
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(1200);
            android.graphics.Bitmap capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"auth-liquid.png"))) {
                capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);
            } finally { capture.recycle(); }
            scenario.onActivity(activity->{
                AuthFragment fragment=(AuthFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                assertNotNull(fragment.requireView().findViewById(R.id.verificationCode));
                assertFalse(activity.findViewById(R.id.windowBackdrop).getBackground() instanceof ColorDrawable);
                activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override public void onFragmentCreated(androidx.fragment.app.FragmentManager manager,androidx.fragment.app.Fragment screen,android.os.Bundle state) {
                        if(screen instanceof CompanyFragment) {
                            com.example.saasvilcas.feature.dashboard.domain.DashboardRepository dashboard=(section,filter)->{
                                switch(section) {
                                    case SUMMARY: return com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.metrics(new com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.Metrics(0,0,0,0,0,false,false,true));
                                    case INVENTORY: return com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.inventory(0);
                                    case TREND: return com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.trend(Collections.emptyList());
                                    case CATEGORIES: case PAYMENTS: case EXPENSES: return com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.entries(Collections.emptyList());
                                    default: return com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.products(Collections.emptyList());
                                }
                            };
                            new ViewModelProvider(screen,com.example.saasvilcas.feature.dashboard.presentation.DashboardViewModel.factory(dashboard)).get(com.example.saasvilcas.feature.dashboard.presentation.DashboardViewModel.class);
                        }
                    }
                },false);
                new ViewModelProvider(activity).get(AuthViewModel.class).verify("123456");
                assertEquals(View.VISIBLE,activity.findViewById(R.id.brandLoadingOverlay).getVisibility());
                assertFalse("Keep the photo while the outgoing 2FA is visible",activity.findViewById(R.id.windowBackdrop).getBackground() instanceof ColorDrawable);
            });
            awaitScreen(scenario,true);
            scenario.onActivity(activity->{
                assertTrue(activity.findViewById(R.id.windowBackdrop).getBackground() instanceof ColorDrawable);
                assertEquals(1,((android.view.ViewGroup)activity.findViewById(R.id.screenHost)).getChildCount());
                assertTrue(activity.findViewById(R.id.companyMenu).isShown());
                ((VilcasApplication)activity.getApplication()).sessions.clear();
            });
        }
    }
    private static void awaitScreen(ActivityScenario<MainActivity> scenario,boolean company) throws Exception {
        long deadline=android.os.SystemClock.uptimeMillis()+5000;
        while(android.os.SystemClock.uptimeMillis()<deadline) {
            boolean[] ready={false};
            scenario.onActivity(activity->{
                androidx.fragment.app.Fragment screen=activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                ready[0]=company?screen instanceof CompanyFragment:screen instanceof AuthFragment&&screen.getView()!=null&&screen.getView().findViewById(R.id.verificationCode)!=null;
            });
            if(ready[0]) { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); return; }
            Thread.sleep(25);
        }
        fail("Authentication transition timed out");
    }
}


