package com.example.saasvilcas.shared.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.saasvilcas.R;

public final class InfoModalFragment extends BrandedModalFragment {
    public static InfoModalFragment create(int title,String message) {
        InfoModalFragment modal=new InfoModalFragment();
        Bundle args=new Bundle(); args.putInt("title",title); args.putString("message",message);
        modal.setArguments(args); return modal;
    }
    @Override protected int contentLayout() { return R.layout.modal_info_content; }
    @Override protected void bindContent(View content) {
        ((TextView)content.findViewById(R.id.modalInfoTitle)).setText(requireArguments().getInt("title"));
        ((TextView)content.findViewById(R.id.modalInfoMessage)).setText(requireArguments().getString("message"));
        content.findViewById(R.id.modalInfoClose).setOnClickListener(v->dismiss());
    }
}
