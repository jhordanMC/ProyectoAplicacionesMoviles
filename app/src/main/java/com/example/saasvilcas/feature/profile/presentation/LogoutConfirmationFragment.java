package com.example.saasvilcas.feature.profile.presentation;

import android.os.Bundle;
import android.view.View;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

public final class LogoutConfirmationFragment extends BrandedModalFragment {
    public static final String CONFIRMED="profile_logout_confirmed";
    @Override protected int contentLayout() { return R.layout.profile_modal_logout; }
    @Override protected boolean showHeaderBrand() { return false; }
    @Override protected void bindContent(View content) {
        content.findViewById(R.id.logoutCancel).setOnClickListener(v->dismiss());
        content.findViewById(R.id.logoutConfirm).setOnClickListener(v->{
            dismiss();
            getParentFragmentManager().setFragmentResult(CONFIRMED,new Bundle());
        });
    }
}
