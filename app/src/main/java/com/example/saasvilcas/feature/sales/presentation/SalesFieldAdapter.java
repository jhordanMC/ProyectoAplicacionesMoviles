package com.example.saasvilcas.feature.sales.presentation;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import java.util.List;
import com.example.saasvilcas.R;

final class SalesFieldAdapter {
    private SalesFieldAdapter() { }
    static void bind(Context context,Spinner spinner,List<String> values,String selected) {
        com.example.saasvilcas.shared.ui.FilterFieldAdapter.bind(spinner,values,selected);
    }
}
