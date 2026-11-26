package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.saasvilcas.R;
import com.example.saasvilcas.VilcasApplication;
import com.example.saasvilcas.feature.sales.domain.PendingSale;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

/** HU-18: sales the server rejected while syncing; the seller retries or discards each one. */
public final class PendingSalesModal extends SalesModalFragment {
    private LinearLayout list;
    @Override protected int contentLayout() { return R.layout.sales_modal_pending; }
    @Override protected void bindContent(View content) { list=content.findViewById(R.id.salesPendingList); }
    private VilcasApplication app() { return (VilcasApplication)requireActivity().getApplication(); }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        app().salesQueue.observe(getViewLifecycleOwner(),queue->{
            if(queue.attention.isEmpty()) { dismissAllowingStateLoss(); return; }
            list.removeAllViews();
            for(PendingSale sale:queue.attention) list.addView(row(sale));
            refreshModalSize();
        });
    }
    private View row(PendingSale sale) {
        View row=getLayoutInflater().inflate(R.layout.sales_item_pending,list,false);
        ((TextView)row.findViewById(R.id.salesPendingAmount)).setText(CheckoutModalFragment.money(sale.totalCents));
        ((TextView)row.findViewById(R.id.salesPendingDate)).setText(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(sale.createdAt)));
        ((TextView)row.findViewById(R.id.salesPendingSummary)).setText(sale.summary);
        ((TextView)row.findViewById(R.id.salesPendingReason)).setText(sale.message);
        row.findViewById(R.id.salesPendingRetry).setOnClickListener(v->app().retryPendingSale(sale.id));
        row.findViewById(R.id.salesPendingDiscard).setOnClickListener(v->{
            if(getChildFragmentManager().findFragmentByTag("sales-discard")==null)
                Discard.create(sale.id).show(getChildFragmentManager(),"sales-discard");
        });
        return row;
    }
    @Override public void onDestroyView() { list=null; super.onDestroyView(); }

    public static final class Discard extends BrandedModalFragment {
        static Discard create(String id) {
            Discard modal=new Discard(); Bundle args=new Bundle(); args.putString("id",id); modal.setArguments(args); return modal;
        }
        @Override protected int contentLayout() { return R.layout.sales_modal_discard; }
        @Override protected boolean showHeaderBrand() { return false; }
        @Override protected boolean showCloseIcon() { return false; }
        @Override protected void bindContent(View content) {
            content.findViewById(R.id.salesDiscardCancel).setOnClickListener(v->dismiss());
            content.findViewById(R.id.salesDiscardConfirm).setOnClickListener(v->{
                ((VilcasApplication)requireActivity().getApplication()).discardPendingSale(requireArguments().getString("id"));
                dismiss();
            });
        }
    }
}
