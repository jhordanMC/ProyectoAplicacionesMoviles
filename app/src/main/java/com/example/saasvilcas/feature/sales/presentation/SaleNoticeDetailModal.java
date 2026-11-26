package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.example.saasvilcas.R;
import com.example.saasvilcas.VilcasApplication;
import com.example.saasvilcas.feature.sales.data.SaleNotifier;
import com.example.saasvilcas.feature.sales.domain.SaleNotice;

/** HU-19: detail of the sale behind a notification. Opening it marks the notice as read. */
public final class SaleNoticeDetailModal extends SalesModalFragment {
    private LinearLayout fields;
    private TextView total;
    public static SaleNoticeDetailModal create(String id) {
        SaleNoticeDetailModal modal=new SaleNoticeDetailModal(); Bundle args=new Bundle(); args.putString("id",id); modal.setArguments(args); return modal;
    }
    private VilcasApplication app() { return (VilcasApplication)requireActivity().getApplication(); }
    @Override protected int contentLayout() { return R.layout.sales_modal_notice_detail; }
    @Override protected void bindContent(View content) { fields=content.findViewById(R.id.salesNoticeFields); total=content.findViewById(R.id.salesNoticeTotal); }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        String id=requireArguments().getString("id");
        app().saleNotices.observe(getViewLifecycleOwner(),notices->{
            for(SaleNotice notice:notices) if(notice.id.equals(id)) { render(notice); return; }
        });
        app().openSaleNotice(id);
    }
    private void render(SaleNotice notice) {
        fields.removeAllViews();
        total.setText(SaleNotifier.money(notice.totalCents));
        field(R.string.sales_notice_date,java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT).format(new java.util.Date(notice.createdAt)));
        field(R.string.sales_notice_site,notice.site);
        field(R.string.sales_notice_seller,notice.seller);
        field(R.string.sales_notice_products,notice.summary);
        field(R.string.sales_notice_payments,notice.payments);
        field(R.string.sales_notice_id,notice.id);
        refreshModalSize();
    }
    private void field(int label,String value) {
        if(value.isEmpty()) return;
        View field=getLayoutInflater().inflate(R.layout.checkout_confirmation_field,fields,false);
        ((TextView)field.findViewById(R.id.confirmationFieldLabel)).setText(label);
        ((TextView)field.findViewById(R.id.confirmationFieldValue)).setText(value);
        fields.addView(field);
    }
    @Override public void onDestroyView() { fields=null; total=null; super.onDestroyView(); }
}
