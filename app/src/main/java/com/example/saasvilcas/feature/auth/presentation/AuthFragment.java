package com.example.saasvilcas.feature.auth.presentation;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.saasvilcas.R;
import com.example.saasvilcas.app.FrontendNavigator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.saasvilcas.shared.ui.CodeInputView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

/** Authentication UI; requests and session state belong to AuthViewModel. */
public class AuthFragment extends Fragment {
    public static final String LOGIN = "login";
    public static final String RECOVERY = "recovery";
    public static final String VERIFICATION = "verification";

    public static AuthFragment create(String screen, String email) {
        AuthFragment fragment = new AuthFragment();
        Bundle arguments = new Bundle();
        arguments.putString("screen", screen);
        arguments.putString("email", email);
        fragment.setArguments(arguments);
        return fragment;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        String screen = requireArguments().getString("screen", LOGIN);
        int layout = LOGIN.equals(screen) ? R.layout.activity_main
                : RECOVERY.equals(screen) ? R.layout.fragment_recovery : R.layout.fragment_verification;
        return inflater.inflate(layout, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // The Activity draws one continuous background behind the system bars.
        view.setBackground(null);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view,(v,insets)-> {
            androidx.core.graphics.Insets safe=insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                | androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                | androidx.core.view.WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(view);
        String screen = requireArguments().getString("screen", LOGIN);
        com.example.saasvilcas.VilcasApplication app=(com.example.saasvilcas.VilcasApplication)requireActivity().getApplication();
        View border=findBorder(view);
        if(border!=null) {
            animatedBorder=new com.example.saasvilcas.shared.ui.AnimatedAuthBorder(getResources().getDisplayMetrics().density);
            border.setBackground(animatedBorder);
        }
        model=new androidx.lifecycle.ViewModelProvider(requireActivity(),AuthViewModel.factory(app.auth,app.sessions)).get(AuthViewModel.class);
        navigating=false;
        if(VERIFICATION.equals(screen) && !model.hasChallenge() && app.sessions.get()==null) {
            navigator().showLogin(); return;
        }
        if (LOGIN.equals(screen)) bindLogin(view);
        else if (RECOVERY.equals(screen)) bindRecovery(view);
        else bindVerification(view);
    }
    private AuthViewModel model;
    private com.example.saasvilcas.shared.ui.AnimatedAuthBorder animatedBorder;
    private View findBorder(View view) { return view.findViewById(R.id.borderView); }
    private boolean navigating;
    private FrontendNavigator navigator() { return (FrontendNavigator) requireActivity(); }
    private void bindLogin(View view) {
        EditText email = view.findViewById(R.id.etEmail);
        EditText password = view.findViewById(R.id.etPassword);
        password.setSaveEnabled(false);
        android.content.SharedPreferences preferences=requireContext().getSharedPreferences("login_preferences",android.content.Context.MODE_PRIVATE);
        android.widget.CheckBox remember=view.findViewById(R.id.checkBox);
        remember.setChecked(preferences.getBoolean("remember_email",false));
        if(remember.isChecked() && email.length()==0) email.setText(preferences.getString("email",""));
        remember.setOnCheckedChangeListener((button,checked)-> {
            android.content.SharedPreferences.Editor edit=preferences.edit().putBoolean("remember_email",checked);
            if(checked) edit.putString("email",email.getText().toString().trim()); else edit.remove("email");
            edit.apply();
        });
        ((android.widget.ImageView)view.findViewById(R.id.ivTogglePassword)).setImageResource(password.getTransformationMethod()!=null?R.drawable.ic_eye_off:R.drawable.ic_eye);
        view.findViewById(R.id.textView6).setOnClickListener(ignored ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.contact_sales_title)
                        .setMessage(R.string.contact_sales_pending)
                        .setPositiveButton(android.R.string.ok, null).show());
        view.findViewById(R.id.tvForgotPassword).setOnClickListener(ignored ->
                navigator().showRecovery(email.getText().toString().trim()));
        view.findViewById(R.id.ivTogglePassword).setOnClickListener(toggle -> {
            boolean hidden = password.getTransformationMethod() != null;
            password.setTransformationMethod(hidden ? null : PasswordTransformationMethod.getInstance());
            password.setSelection(password.length());
            ((android.widget.ImageView)toggle).setImageResource(hidden?R.drawable.ic_eye:R.drawable.ic_eye_off);
            toggle.setContentDescription(getString(hidden ? R.string.hide_password : R.string.show_password));
        });
        view.findViewById(R.id.button).setOnClickListener(ignored -> {
            String address = email.getText().toString().trim();
            if (!isValidEmail(address)) { email.setError(getString(R.string.invalid_email)); return; }
            if (password.length() == 0) { password.setError(getString(R.string.required_password)); return; }
            if(remember.isChecked()) preferences.edit().putString("email",address).apply();
            model.login(address,password.getText().toString());
        });
        model.state().observe(getViewLifecycleOwner(),state->{
            renderStatus(view,state);
            view.findViewById(R.id.button).setEnabled(!state.busy());
            email.setEnabled(!state.busy()); password.setEnabled(!state.busy());
            view.findViewById(R.id.tvForgotPassword).setEnabled(!state.busy());
            if(state.stage==AuthViewModel.Stage.CHALLENGE && model.hasChallenge() && !navigating) {
                navigating=true; password.setText(""); navigator().showVerification(model.email());
            }
        });
    }
    private void renderStatus(View view,AuthViewModel.State state) {
        view.findViewById(R.id.authStatus).setVisibility(state.message.isEmpty()?View.GONE:View.VISIBLE);
        view.findViewById(R.id.authLoading).setVisibility(View.GONE);
        ((com.example.saasvilcas.MainActivity)requireActivity()).setAuthLoading(state.busy(),state.message);
        TextView message=view.findViewById(R.id.authMessage);
        message.setText(state.message); message.setTextColor(state.busy()?0xff7AAA95:0xffB3261E);
    }
    private void bindRecovery(View view) {
        com.example.saasvilcas.VilcasApplication app=(com.example.saasvilcas.VilcasApplication)requireActivity().getApplication();
        RecoveryViewModel recovery=new androidx.lifecycle.ViewModelProvider(this,RecoveryViewModel.factory(app.auth)).get(RecoveryViewModel.class);
        EditText email = view.findViewById(R.id.etEmail);
        email.setText(requireArguments().getString("email", ""));
        email.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        TextView submit = view.findViewById(R.id.button);
        submit.setTextSize(13);submit.setPadding(Math.round(24*getResources().getDisplayMetrics().density),0,Math.round(24*getResources().getDisplayMetrics().density),0);
        submit.setText(R.string.send_recovery_link);
        submit.setOnClickListener(ignored -> {
            if (!isValidEmail(email.getText().toString().trim())) {
                email.setError(getString(R.string.invalid_email));
                return;
            }
            recovery.send(email.getText().toString());
        });
        recovery.state.observe(getViewLifecycleOwner(),state->{
            email.setEnabled(!state.busy);
            submit.setEnabled(!state.busy);submit.setText(state.busy?"Enviando…":state.sent?"Reenviar enlace":getString(R.string.send_recovery_link));
            submit.setBackgroundResource(state.sent?R.drawable.recovery_resend_button:R.drawable.bg_button);
            submit.setTextColor(state.sent?0xff4A6A60:0xffFFFFFF);
            submit.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,state.sent?0:R.drawable.ic_arrow_forward,0);
            TextView error=view.findViewById(R.id.recoveryError);error.setText(state.error);error.setVisibility(state.error.isEmpty()?View.GONE:View.VISIBLE);
            if(state.sent) {
                ((TextView)view.findViewById(R.id.recoveryHeading)).setText("¡Revisa tu correo!");
                ((android.widget.ImageView)view.findViewById(R.id.recoveryIcon)).setImageResource(R.drawable.recovery_check);
                ((TextView)view.findViewById(R.id.recoveryInstructions)).setText("Si el correo está registrado, recibirás un enlace para recuperar tu contraseña. Revisa también la carpeta de spam.");
                email.setText(state.email);view.findViewById(R.id.recoveryEmailField).setVisibility(View.GONE);
            }
        });
        view.findViewById(R.id.recoveryBack).setOnClickListener(ignored -> navigator().showLogin());
    }

    private CountDownTimer countdown;
    private long expiresAt;



    private void bindVerification(View view) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { navigator().showLogin(); }
        });
        expiresAt=model.deadline();
        CodeInputView code = view.findViewById(R.id.verificationCode);
        TextView time = view.findViewById(R.id.codeCountdown);
        View back = view.findViewById(R.id.codeExpiredBack);
        LinearProgressIndicator progress = view.findViewById(R.id.codeProgress);
        back.setOnClickListener(ignored -> navigator().showLogin());
        code.setListener(value -> {
            progress.setProgressCompat(value.length(),true);
            if(value.length()==6 && SystemClock.elapsedRealtime()<expiresAt) model.verify(value);
        });
        model.state().observe(getViewLifecycleOwner(),state->{
            renderStatus(view,state);
            code.setEnabled(!state.busy() && !state.restartRequired && SystemClock.elapsedRealtime()<expiresAt);
            if(!state.busy() && !state.message.isEmpty() && !state.restartRequired) code.clear();
            if(state.restartRequired) back.setVisibility(View.VISIBLE);
            if(state.stage==AuthViewModel.Stage.AUTHENTICATED && !navigating) {
                com.example.saasvilcas.feature.auth.domain.AuthSession session=
                    ((com.example.saasvilcas.VilcasApplication)requireActivity().getApplication()).sessions.get();
                if(session!=null) { navigating=true; navigator().showCompany(session.owner); }
            }
        });
        updateCountdown(code, time, back);
        long remaining = expiresAt - SystemClock.elapsedRealtime();
        if (remaining > 0) countdown = new CountDownTimer(remaining, 1000) {
            @Override public void onTick(long millis) { updateCountdown(code, time, back); }
            @Override public void onFinish() { updateCountdown(code, time, back); }
        }.start();
    }

    private void updateCountdown(CodeInputView code, TextView time, View back) {
        long seconds = Math.max(0, (expiresAt - SystemClock.elapsedRealtime() + 999) / 1000);
        if (seconds == 0) {
            code.setEnabled(false);
            time.setText(R.string.code_expired);
            back.setVisibility(View.VISIBLE);
        } else {
            time.setText(getString(R.string.code_time_remaining, seconds / 60, seconds % 60));
        }
    }

    @Override public void onDestroyView() {
        if(animatedBorder!=null) { animatedBorder.stop(); animatedBorder=null; }
        if (countdown != null) { countdown.cancel(); countdown = null; }
        navigating = false;
        super.onDestroyView();
    }
    @Override public void onStart() { super.onStart(); if(animatedBorder!=null) animatedBorder.start(); }
    @Override public void onStop() { if(animatedBorder!=null) animatedBorder.stop(); super.onStop(); }
    private boolean isValidEmail(String email) { return Patterns.EMAIL_ADDRESS.matcher(email).matches(); }
}



