package com.example.saasvilcas.feature.profile.presentation;

import android.view.*;
import android.widget.*;
import androidx.appcompat.widget.*;
import com.example.saasvilcas.R;

final class ProfileUi {
    static void busy(View view,boolean busy) {
        if(view instanceof Button || view instanceof EditText || view instanceof RatingBar) {
            if(busy) { if(view.getTag(R.id.profileWasEnabled)==null) view.setTag(R.id.profileWasEnabled,view.isEnabled()); view.setEnabled(false); }
            else if(view.getTag(R.id.profileWasEnabled) instanceof Boolean) { view.setEnabled((Boolean)view.getTag(R.id.profileWasEnabled)); view.setTag(R.id.profileWasEnabled,null); }
        }
        if(view instanceof android.view.ViewGroup) for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++) busy(((android.view.ViewGroup)view).getChildAt(i),busy);
    }
    static int dp(View view,int size) { return Math.round(size*view.getResources().getDisplayMetrics().density); }
    static LinearLayout body(LayoutInflater inflater,android.view.ViewGroup parent) {
        LinearLayout body=(LinearLayout)inflater.inflate(R.layout.profile_component_body,parent,false); parent.addView(body); return body;
    }
    static TextView text(LinearLayout root,String value,boolean heading) {
        TextView text=new TextView(root.getContext()); text.setText(value); text.setTextSize(heading?16:13);
        text.setTextColor(androidx.core.content.ContextCompat.getColor(root.getContext(),heading?R.color.vl_text_ink:R.color.vl_text_slate));
        text.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(root.getContext(),R.font.inter),heading?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2); params.bottomMargin=dp(root,12); text.setLayoutParams(params); root.addView(text); return text;
    }
    static TextView label(LinearLayout root,String value) {
        TextView text=(TextView)LayoutInflater.from(root.getContext()).inflate(R.layout.profile_component_label,root,false); text.setText(value); root.addView(text); return text;
    }
    static AppCompatEditText field(LinearLayout root,String label,int inputType) {
        label(root,label); AppCompatEditText text=(AppCompatEditText)LayoutInflater.from(root.getContext()).inflate(R.layout.profile_component_field,root,false);
        text.setInputType(inputType); root.addView(text); return text;
    }
    static AppCompatButton button(LinearLayout root,String value,boolean primary,Runnable click) {
        AppCompatButton button=(AppCompatButton)LayoutInflater.from(root.getContext()).inflate(R.layout.profile_component_button,root,false);
        button.setText(value); if(primary) { button.setBackgroundResource(R.drawable.profile_bg_primary); button.setTextColor(0xffFFFFFF); }
        button.setOnClickListener(v->click.run()); root.addView(button); return button;
    }
    static TextView error(LinearLayout root) { TextView text=text(root,"",false); text.setTextColor(androidx.core.content.ContextCompat.getColor(root.getContext(),R.color.vl_negative)); text.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); return text; }
}
