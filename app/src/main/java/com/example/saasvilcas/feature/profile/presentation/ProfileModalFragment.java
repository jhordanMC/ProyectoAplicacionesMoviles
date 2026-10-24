package com.example.saasvilcas.feature.profile.presentation;

import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.text.InputFilter;
import android.view.View;
import android.widget.*;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.R;
import com.example.saasvilcas.VilcasApplication;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.feature.profile.domain.UserProfile;
import com.example.saasvilcas.shared.ui.*;

/** The original profile menu and its subviews stay inside one branded modal. */
public final class ProfileModalFragment extends BrandedModalFragment {
    public static final String REQUEST_LOGOUT="profile_request_logout";
    private ProfileViewModel model;
    private LinearLayout host;
    private TextView error;
    private String page="menu",passwordStep="form";
    private long deadline;
    private android.os.CountDownTimer timer;
    private final androidx.activity.result.ActivityResultLauncher<String> photo=registerForActivityResult(
        new androidx.activity.result.contract.ActivityResultContracts.GetContent(),uri->{ if(uri!=null) model.avatar(uri.toString()); });
    public static ProfileModalFragment create(boolean owner) {
        ProfileModalFragment modal=new ProfileModalFragment(); Bundle args=new Bundle(); args.putBoolean("owner",owner); modal.setArguments(args); return modal;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null) { page=state.getString("page","menu"); passwordStep=state.getString("passwordStep","form"); deadline=state.getLong("deadline"); }
    }
    @Override protected int contentLayout() { return R.layout.profile_component_body; }
    @Override protected int surfaceBackground() { return R.drawable.modal_bg_white; }
    @Override protected void bindContent(View content) {
        host=(LinearLayout)content;
        VilcasApplication app=(VilcasApplication)requireActivity().getApplication();
        model=new ViewModelProvider(requireParentFragment(),ProfileViewModel.factory(app.newProfileRepository(),app.auth,app.sessions)).get(ProfileViewModel.class);
        render(); model.start();
    }
    @Override public void onViewCreated(View view,Bundle state) {
        super.onViewCreated(view,state); requireDialog().setCanceledOnTouchOutside(false);
        model.profile.observe(getViewLifecycleOwner(),profile->{ if(page.equals("menu")||page.equals("info")) render(); });
        model.loadError.observe(getViewLifecycleOwner(),message->{ if(error!=null && !message.isEmpty()) error.setText(message); });
        model.operation.observe(getViewLifecycleOwner(),stateValue->{
            ProfileUi.busy(host,stateValue.busy);
            if(error!=null) {error.setText(stateValue.busy?"Procesando…":stateValue.error);error.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),stateValue.busy?R.color.vl_text_deep:R.color.vl_negative));}
            if(!stateValue.success) return;
            switch(stateValue.kind) {
                case "password-request": passwordStep="code"; deadline=SystemClock.elapsedRealtime()+stateValue.seconds*1000L; render(); break;
                case "password-confirm": passwordStep="success"; render(); break;
                case "rate": page="rated"; render(); break;
                case "account-login": page="account-code"; deadline=SystemClock.elapsedRealtime()+stateValue.seconds*1000L; render(); break;
                case "activate": break; // The company shell rebuilds even if this modal was closed.
                case "avatar": if(page.equals("menu")) render(); break;
            }
            model.resetOperation();
        });
    }
    private void navigate(String next) { model.resetOperation(); page=next; render(); }
    private void render() {
        if(timer!=null) {timer.cancel();timer=null;}
        host.removeAllViews(); host.setPadding(ProfileUi.dp(host,24),0,ProfileUi.dp(host,24),ProfileUi.dp(host,20)); error=null;
        if(page.equals("menu")) { menu(); finishRender(); return; }
        if(!page.equals("accounts")) ProfileUi.button(host,"← Volver",false,()->navigate("menu"));
        switch(page) {
            case "info": info(); break;
            case "rating": rating(); break;
            case "rated": ProfileUi.text(host,"¡Gracias por tu opinión!",true); break;
            case "about": ProfileUi.text(host,"Acerca de VILCAS",true); ProfileUi.text(host,"Versión "+com.example.saasvilcas.BuildConfig.VERSION_NAME,false); ProfileUi.text(host,"Lo que vendiste, lo que gastaste, lo que ganaste. Todo en VILCAS.",false); break;
            case "password": password(); break;
            case "accounts": accounts(); break;
            case "account-login": loginAccount(); break;
            case "account-code": code(false); break;
        }
        error=ProfileUi.error(host); finishRender();
    }
    private void finishRender() {
        ProfileViewModel.Operation operation=model.operation.getValue();
        ProfileUi.busy(host,operation!=null&&operation.busy);
        ViewEntranceAnimator.play(host);
        host.post(()->{if(isAdded()&&getView()!=null) refreshModalSize();});
    }
    private void menu() {
        host.setPadding(0,0,0,0);
        View menu=getLayoutInflater().inflate(R.layout.profile_modal_content,host,false); host.addView(menu);
        AuthSession session=((VilcasApplication)requireActivity().getApplication()).sessions.get();
        UserProfile profile=model.profile.getValue();
        ((TextView)menu.findViewById(R.id.profileName)).setText(profile==null?(session==null?"":session.name):profile.name);
        ((TextView)menu.findViewById(R.id.profileRole)).setText(profile==null?getString(session!=null&&session.owner?R.string.owner_role:session!=null&&session.inventory&&!session.seller?R.string.inventory_role:R.string.seller_role):android.text.TextUtils.join(" · ",profile.roles));
        TextView initials=menu.findViewById(R.id.profileInitials); initials.setText(session==null?"?":session.initials());
        initials.setOnClickListener(v->photo.launch("image/*"));
        menu.findViewById(R.id.profilePhotoButton).setOnClickListener(v->photo.launch("image/*"));
        ImageView avatar=menu.findViewById(R.id.profileAvatar);
        avatar.setOnClickListener(v->photo.launch("image/*"));
        if(profile!=null&&!profile.avatar.isEmpty()) { avatar.setVisibility(View.VISIBLE); ProductImageLoader.load(avatar,profile.avatar); }
        LinearLayout options=menu.findViewById(R.id.profileOptions);
        int[] titles={R.string.profile_personal,R.string.profile_switch,R.string.profile_rating,R.string.profile_about,R.string.profile_password};
        int[] icons={R.drawable.modal_ic_user,R.drawable.modal_ic_switch,R.drawable.modal_ic_star,R.drawable.modal_ic_info,R.drawable.ic_lock};
        String[] pages={"info","accounts","rating","about","password"};
        for(int i=0;i<titles.length;i++) {
            final String next=pages[i]; View row=getLayoutInflater().inflate(R.layout.profile_menu_option,options,false);
            ((ImageView)row.findViewById(R.id.modalOptionIcon)).setImageResource(icons[i]);
            ((TextView)row.findViewById(R.id.modalOptionLabel)).setText(titles[i]);
            row.setOnClickListener(v->navigate(next)); options.addView(row);
        }
        error=ProfileUi.error(options);
        if(!model.loadError.getValue().isEmpty()) { error.setText(model.loadError.getValue()); ProfileUi.button(options,"Reintentar",false,()->model.reloadProfile()); }
        menu.findViewById(R.id.profileClose).setOnClickListener(v->dismiss());
        menu.findViewById(R.id.profileLogout).setOnClickListener(v->{dismiss(); getParentFragmentManager().setFragmentResult(REQUEST_LOGOUT,new Bundle());});
    }
    private void info() {
        ProfileUi.text(host,"Información personal",true); UserProfile p=model.profile.getValue();
        if(p==null) { ProfileUi.text(host,"Cargando tu perfil…",false); ProfileUi.button(host,"Reintentar",false,()->model.reloadProfile()); return; }
        for(String[] item:new String[][]{{"Nombre completo",p.name},{"Correo electrónico",p.email},{"Teléfono",p.phone},{"DNI",p.dni},{"Empresa",p.company}}) {
            View row=getLayoutInflater().inflate(R.layout.profile_info_row,host,false);
            ((TextView)row.findViewById(R.id.profileInfoLabel)).setText(item[0].toUpperCase(java.util.Locale.getDefault()));
            ((TextView)row.findViewById(R.id.profileInfoValue)).setText(item[1].isEmpty()?"No registrado":item[1]);
            host.addView(row);
        }
    }
    private void rating() {
        ProfileUi.text(host,"Califícanos",true); ProfileUi.text(host,"Cuéntanos qué tal tu experiencia con VILCAS.",false);
        RatingBar stars=new RatingBar(requireContext()); stars.setNumStars(5); stars.setStepSize(1); stars.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffDA9E0B));
        host.addView(stars,new LinearLayout.LayoutParams(-2,-2)); stars.setTag("profile-rating");
        AppCompatEditText comment=ProfileUi.field(host,"Comentario (opcional)",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        comment.setSingleLine(false); comment.setMinLines(3); comment.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)}); comment.getLayoutParams().height=ProfileUi.dp(host,100);
        androidx.appcompat.widget.AppCompatButton send=ProfileUi.button(host,"Enviar",true,()->model.rate((int)stars.getRating(),comment.getText().toString()));
        send.setEnabled(false); stars.setOnRatingBarChangeListener((bar,rating,user)->send.setEnabled(rating>=1));
    }
    private void password() {
        ProfileUi.text(host,"Cambio de contraseña",true);
        if(passwordStep.equals("success")) { ProfileUi.text(host,"Tu contraseña se actualizó correctamente.",false); return; }
        if(passwordStep.equals("code")) { code(true); return; }
        ProfileUi.text(host,"Ingresa tu contraseña actual y la nueva. Te enviaremos un código a tu correo para confirmar el cambio.",false);
        int type=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD;
        EditText current=ProfileUi.field(host,"Contraseña actual",type),next=ProfileUi.field(host,"Nueva contraseña",type),confirm=ProfileUi.field(host,"Confirmar contraseña",type);
        ProfileUi.button(host,"Enviar código de verificación",true,()-> {
            if(current.length()==0||next.length()<8||!next.getText().toString().equals(confirm.getText().toString())) { error.setText("Completa la contraseña actual y una nueva de al menos 8 caracteres. Las contraseñas nuevas deben coincidir."); return; }
            model.requestPassword(current.getText().toString(),next.getText().toString());
        });
    }
    private void code(boolean password) {
        ProfileUi.text(host,"Ingresa el código de 6 dígitos enviado a tu correo.",false);
        CodeInputView code=new CodeInputView(requireContext(),null); host.addView(code,new LinearLayout.LayoutParams(-1,ProfileUi.dp(host,56)));
        TextView remaining=ProfileUi.text(host,"",false);
        androidx.appcompat.widget.AppCompatButton verify=ProfileUi.button(host,password?"Verificar y cambiar":"Verificar y agregar",true,()-> {
            if(SystemClock.elapsedRealtime()>=deadline) {error.setText("El código venció. Solicita uno nuevo.");return;}
            if(password) model.confirmPassword(code.getCode()); else model.verifyAccount(code.getCode());
        });
        verify.setEnabled(false); code.setListener(value->{boolean ready=value.length()==6 && SystemClock.elapsedRealtime()<deadline; verify.setEnabled(ready); if(ready) verify.performClick();});
        timer=new android.os.CountDownTimer(Math.max(1,deadline-SystemClock.elapsedRealtime()),1000) {
            public void onTick(long ms) { remaining.setText("Vence en "+((ms+999)/1000)+"s"); }
            public void onFinish() { remaining.setText("El código venció."); code.setEnabled(false); verify.setEnabled(false); ProfileUi.button(host,"Solicitar otro código",false,()->{ if(password) {passwordStep="form";render();} else navigate("account-login"); }); }
        }.start();
    }
    private void accounts() {
        LinearLayout header=new LinearLayout(requireContext());header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(0,ProfileUi.dp(host,18),0,ProfileUi.dp(host,14));host.addView(header);
        androidx.appcompat.widget.AppCompatButton back=new androidx.appcompat.widget.AppCompatButton(requireContext());
        back.setText("←");back.setTextSize(18);back.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),R.color.vl_text_deep));back.setPadding(0,0,0,0);back.setMinWidth(0);back.setMinHeight(0);
        back.setBackgroundResource(R.drawable.profile_account_avatar);back.setContentDescription("Volver");back.setId(R.id.accountBack);
        header.addView(back,new LinearLayout.LayoutParams(ProfileUi.dp(host,32),ProfileUi.dp(host,32)));back.setOnClickListener(v->navigate("menu"));
        TextView title=ProfileUi.text(header,"Cambiar de cuenta",true);title.setTextSize(15);
        LinearLayout.LayoutParams titleParams=new LinearLayout.LayoutParams(0,-2,1);titleParams.leftMargin=ProfileUi.dp(host,10);title.setLayoutParams(titleParams);
        TextView description=ProfileUi.text(host,"Cuentas guardadas en este dispositivo — cambia sin volver a iniciar sesión.",false);
        description.setTextSize(12.5f);description.setGravity(android.view.Gravity.CENTER);description.setLineSpacing(0,1.5f);
        AuthSession active=((VilcasApplication)requireActivity().getApplication()).sessions.get();
        for(AuthSession account:model.accounts()) {
            boolean selected=active!=null&&account.userId.equals(active.userId);
            View row=getLayoutInflater().inflate(R.layout.profile_account_row,host,false);
            ((TextView)row.findViewById(R.id.accountInitials)).setText(account.initials());
            ((TextView)row.findViewById(R.id.accountName)).setText(account.name);
            ((TextView)row.findViewById(R.id.accountCompany)).setText(account.companyName);
            row.findViewById(R.id.accountActive).setVisibility(selected?View.VISIBLE:View.GONE);
            row.findViewById(R.id.accountForget).setVisibility(selected?View.GONE:View.VISIBLE);
            row.setOnClickListener(v->{if(!selected) model.activate(account);});
            row.findViewById(R.id.accountForget).setOnClickListener(v->{model.forget(account.userId);render();});
            host.addView(row);
        }
        View add=getLayoutInflater().inflate(R.layout.profile_menu_option,host,false);
        ((ImageView)add.findViewById(R.id.modalOptionIcon)).setImageResource(R.drawable.modal_ic_user);
        ((TextView)add.findViewById(R.id.modalOptionLabel)).setText("Agregar otra cuenta");
        add.setOnClickListener(v->navigate("account-login")); host.addView(add);
    }
    private void loginAccount() {
        ProfileUi.text(host,"Agregar otra cuenta",true);
        EditText email=ProfileUi.field(host,"Correo electrónico",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password=ProfileUi.field(host,"Contraseña",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ProfileUi.button(host,"Continuar",true,()->{
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.getText().toString().trim()).matches()||password.length()==0) {error.setText("Ingresa un correo válido y tu contraseña.");return;}
            model.loginAccount(email.getText().toString().trim(),password.getText().toString());
        });
    }
    @Override public void onSaveInstanceState(Bundle state) {super.onSaveInstanceState(state);state.putString("page",page);state.putString("passwordStep",passwordStep);state.putLong("deadline",deadline);}
    @Override public void onDestroyView() {if(timer!=null) timer.cancel();host=null;error=null;super.onDestroyView();}
}

