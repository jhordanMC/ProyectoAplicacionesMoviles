package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

/** Shown when the saved session could not be renewed for lack of network; HU-18 lets the seller keep selling. */
public final class OfflineStartModal extends BrandedModalFragment {
    public static final String RESULT="offline_start_result",ACTION="action",SELL="sell",RETRY="retry",CLOSE="close";
    public static OfflineStartModal create(boolean canSell) {
        OfflineStartModal modal=new OfflineStartModal();
        Bundle args=new Bundle(); args.putBoolean("canSell",canSell); modal.setArguments(args);
        modal.setCancelable(false);
        return modal;
    }
    @Override protected int contentLayout() { return R.layout.sales_modal_offline_start; }
    @Override protected boolean showHeaderBrand() { return false; }
    @Override protected boolean showCloseIcon() { return false; }
    @Override protected void bindContent(View content) {
        boolean canSell=requireArguments().getBoolean("canSell");
        ((TextView)content.findViewById(R.id.salesStartMessage)).setText(canSell?R.string.sales_start_message_sell:R.string.sales_start_message);
        content.findViewById(R.id.salesStartOffline).setVisibility(canSell?View.VISIBLE:View.GONE);
        content.findViewById(R.id.salesStartOffline).setOnClickListener(v->finish(SELL));
        content.findViewById(R.id.salesStartRetry).setOnClickListener(v->finish(RETRY));
        content.findViewById(R.id.salesStartClose).setOnClickListener(v->finish(CLOSE));
    }
    private void finish(String action) {
        Bundle result=new Bundle(); result.putString(ACTION,action);
        getParentFragmentManager().setFragmentResult(RESULT,result);
        dismiss();
    }
}
