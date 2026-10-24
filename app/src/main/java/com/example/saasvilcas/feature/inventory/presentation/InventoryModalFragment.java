package com.example.saasvilcas.feature.inventory.presentation;

import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

/** Inventory keeps the common modal structure with the white surface from Angular. */
public abstract class InventoryModalFragment extends BrandedModalFragment {
    @Override protected int maxWidthDp() { return showHeaderBrand()?720:380; }
    @Override protected int horizontalInsetDp() { return 20; }
    protected String operationKey;
    protected InventoryViewModel operationModel;
    @Override public void onCreate(android.os.Bundle state) {
        super.onCreate(state); operationKey=state==null?java.util.UUID.randomUUID().toString():state.getString("operationKey",java.util.UUID.randomUUID().toString());
    }
    @Override public void onSaveInstanceState(android.os.Bundle state) { super.onSaveInstanceState(state); state.putString("operationKey",operationKey); }
    @Override public void onViewCreated(android.view.View view,android.os.Bundle state) {
        super.onViewCreated(view,state);
        androidx.fragment.app.Fragment ancestor=getParentFragment();
        while(ancestor!=null && !(ancestor instanceof com.example.saasvilcas.feature.company.presentation.CompanyFragment)) ancestor=ancestor.getParentFragment();
        if(ancestor==null) return;
        operationModel=InventoryViewModel.obtain(ancestor);
        operationModel.state.observe(getViewLifecycleOwner(),result->{
            enableInputs(view,!result.busy&&!result.loading);
            android.view.View save=view.findViewById(R.id.inventoryFormSave);
            if(save!=null) save.setEnabled(!result.busy&&!result.loading&&!result.uncertain);
            android.view.View delete=view.findViewById(R.id.inventoryDeleteConfirm);
            if(delete!=null) delete.setEnabled(!result.busy&&!result.loading&&!result.uncertain);
            InventoryViewModel.State operation=operationKey.equals(result.key)?result:operationModel.outcome(operationKey);
            if(operation==null) return;
            if(operation.success) { com.example.saasvilcas.shared.ui.UiSounds.forEvent(operation,com.example.saasvilcas.shared.ui.UiSounds::success); onOperationSuccess(); }
            else if(!operation.message.isEmpty()) {
                if(!operation.busy) com.example.saasvilcas.shared.ui.UiSounds.forEvent(operation,com.example.saasvilcas.shared.ui.UiSounds::error);
                android.widget.TextView error=view.findViewById(R.id.inventoryFormError);
                if(error==null) error=view.findViewById(R.id.inventoryDeleteMessage);
                if(error!=null) { error.setText(operation.message); error.setVisibility(android.view.View.VISIBLE); }
            }
        });
    }
    protected void onOperationSuccess() { dismiss(); }
    private void enableInputs(android.view.View view,boolean enabled) {
        if(view.isClickable()||view instanceof android.widget.EditText||view instanceof android.widget.Spinner) view.setEnabled(enabled);
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) enableInputs(group.getChildAt(i),enabled);
        }
    }
    @Override protected void onCloseRequested() { if(operationModel==null||!operationModel.state.getValue().busy) dismiss(); }
    @Override protected int surfaceBackground() { return R.drawable.inventory_bg_modal; }
    @Override protected float widthFraction() { return .94f; }
    @Override protected float heightFraction() { return .88f; }
    @Override public android.view.View onCreateView(android.view.LayoutInflater inflater,
            android.view.ViewGroup container,android.os.Bundle state) {
        android.view.View view=super.onCreateView(inflater,container,state);
        applyBrandColor(view);
        formatBrand(view.findViewById(R.id.modalBrandHeader),false);
        formatBrand(view.findViewById(R.id.inventoryFooterBrand),true);
        android.view.View close=view.findViewById(R.id.modalCloseIcon);
        android.widget.FrameLayout.LayoutParams params=(android.widget.FrameLayout.LayoutParams)close.getLayoutParams();
        params.width=dp(34); params.height=dp(34); params.setMargins(dp(18),dp(18),dp(18),dp(18)); close.setLayoutParams(params);
        close.setPadding(dp(10),dp(10),dp(10),dp(10));
        return view;
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private void formatBrand(android.view.View view,boolean footer) {
        if(view==null) return;
        if(view instanceof android.widget.LinearLayout && (view.getId()==R.id.modalBrandHeader||view.getId()==R.id.inventoryFooterBrand))
            view.setPadding(0,dp(footer?10:30),0,dp(footer?0:6));
        if(view instanceof android.widget.ImageView) {
            android.view.ViewGroup.LayoutParams size=view.getLayoutParams(); size.width=dp(footer?40:60); size.height=dp(footer?35:52); view.setLayoutParams(size);
        }
        if(view instanceof android.widget.TextView) {
            android.widget.TextView text=(android.widget.TextView)view; text.setTextSize(footer?15:20); text.setLetterSpacing(.1f);
            text.setTextColor(text.getText().toString().equals("A")?0xffDA9E0B:androidx.core.content.ContextCompat.getColor(text.getContext(),R.color.vl_text_green));
        }
        if(view instanceof android.view.ViewGroup) for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++) formatBrand(((android.view.ViewGroup)view).getChildAt(i),footer);
    }
    private void applyBrandColor(android.view.View view) {
        if(view instanceof android.widget.TextView) {
            android.widget.TextView text=(android.widget.TextView)view;
            String value=text.getText().toString();
            if(text.getCurrentTextColor()==androidx.core.content.ContextCompat.getColor(requireContext(),R.color.company_ink))
                text.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),R.color.vl_text_ink2));
            if(value.equals("VILC")||value.equals("S")) text.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),R.color.vl_text_green));
        }
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) applyBrandColor(group.getChildAt(i));
        }
    }
}

