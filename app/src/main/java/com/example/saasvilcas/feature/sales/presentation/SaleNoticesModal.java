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
import java.util.List;

/** HU-19: list behind the bell for the business owner; each row opens the detail of the sale. */
public final class SaleNoticesModal extends SalesModalFragment {
    public static final String OPEN_ANNOUNCEMENTS="sale_notices_open_announcements";
    private LinearLayout list;
    private View empty,markAll;
    @Override protected int contentLayout() { return R.layout.sales_modal_notices; }
    private VilcasApplication app() { return (VilcasApplication)requireActivity().getApplication(); }
    @Override protected void bindContent(View content) {
        list=content.findViewById(R.id.salesNoticeList); empty=content.findViewById(R.id.salesNoticeEmpty); markAll=content.findViewById(R.id.salesNoticeMarkAll);
        markAll.setOnClickListener(v->app().markSaleNoticesRead());
        content.findViewById(R.id.salesNoticeAnnouncements).setOnClickListener(v->{
            getParentFragmentManager().setFragmentResult(OPEN_ANNOUNCEMENTS,new Bundle());
            dismiss();
        });
    }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        app().saleNotices.observe(getViewLifecycleOwner(),this::render);
    }
    private void render(List<SaleNotice> notices) {
        list.removeAllViews();
        boolean unread=false;
        for(SaleNotice notice:notices) { unread|=!notice.read; list.addView(row(notice)); }
        empty.setVisibility(notices.isEmpty()?View.VISIBLE:View.GONE);
        markAll.setVisibility(unread?View.VISIBLE:View.GONE);
        refreshModalSize();
    }
    private View row(SaleNotice notice) {
        View row=getLayoutInflater().inflate(R.layout.sales_item_notice,list,false);
        ((TextView)row.findViewById(R.id.salesNoticeAmount)).setText(SaleNotifier.money(notice.totalCents));
        String place=notice.site.isEmpty()?notice.seller:notice.seller.isEmpty()?notice.site:notice.site+" · "+notice.seller;
        ((TextView)row.findViewById(R.id.salesNoticePlace)).setText(place);
        ((TextView)row.findViewById(R.id.salesNoticeDate)).setText(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(notice.createdAt)));
        row.findViewById(R.id.salesNoticeDot).setVisibility(notice.read?View.INVISIBLE:View.VISIBLE);
        row.setOnClickListener(v->{ if(getChildFragmentManager().findFragmentByTag("sale-notice")==null) SaleNoticeDetailModal.create(notice.id).show(getChildFragmentManager(),"sale-notice"); });
        return row;
    }
    @Override public void onDestroyView() { list=null; empty=null; markAll=null; super.onDestroyView(); }
}
