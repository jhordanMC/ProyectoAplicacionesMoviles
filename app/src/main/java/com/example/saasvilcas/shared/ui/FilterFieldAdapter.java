package com.example.saasvilcas.shared.ui;

import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.List;
import com.example.saasvilcas.R;

/** Shared dropdown appearance and selection for catalog filters and forms. */
public final class FilterFieldAdapter {
    private FilterFieldAdapter() { }
    public static void bind(Spinner spinner,List<String> values,String selected) {
        bindItems(spinner,values,selected);
    }
    public static <T> void bindItems(Spinner spinner,List<T> values,T selected) {
        spinner.setBackgroundResource(R.drawable.filter_bg_dropdown);
        int inset=Math.round(12*spinner.getResources().getDisplayMetrics().density);
        spinner.setPaddingRelative(inset,0,inset*3,0);
        ArrayAdapter<T> adapter=new ArrayAdapter<T>(spinner.getContext(),R.layout.filter_item_option,values) {
            @Override public android.view.View getDropDownView(int position,android.view.View recycled,android.view.ViewGroup parent) {
                android.view.View option=super.getDropDownView(position,recycled,parent);
                option.setActivated(position==spinner.getSelectedItemPosition()); return option;
            }
        };
        adapter.setDropDownViewResource(R.layout.filter_item_dropdown_option);
        spinner.setPopupBackgroundResource(R.drawable.filter_bg_popup);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0,values.indexOf(selected)));
        spinner.post(()-> { if(spinner.getWidth()>0) spinner.setDropDownWidth(spinner.getWidth()); });
    }
}
