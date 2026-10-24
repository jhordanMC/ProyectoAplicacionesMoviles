package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

/** Checkout surface and typography shared by its form, receipt and confirmation steps. */
public abstract class SalesModalFragment extends BrandedModalFragment {
    @Override protected int surfaceBackground() { return R.drawable.modal_bg_white; }
    @Override protected float widthFraction() { return .92f; }
    @Override protected float heightFraction() { return .92f; }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        tintBrand(view.findViewById(R.id.modalBrandHeader));
        ((ImageButton)view.findViewById(R.id.modalCloseIcon)).setImageTintList(androidx.core.content.ContextCompat.getColorStateList(requireContext(),R.color.vl_text_deep));
    }
    private void tintBrand(View view) {
        if(view==null) return;
        if(view instanceof TextView && !"A".contentEquals(((TextView)view).getText())) ((TextView)view).setTextColor(androidx.core.content.ContextCompat.getColor(view.getContext(),R.color.vl_text_deep));
        if(view instanceof ViewGroup) { ViewGroup group=(ViewGroup)view; for(int i=0;i<group.getChildCount();i++) tintBrand(group.getChildAt(i)); }
    }
    @Override public void onStart() {
        super.onStart();
        if(getDialog()!=null && getDialog().getWindow()!=null)
            getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
