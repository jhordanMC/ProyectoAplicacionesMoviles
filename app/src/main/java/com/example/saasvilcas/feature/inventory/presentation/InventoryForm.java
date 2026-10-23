package com.example.saasvilcas.feature.inventory.presentation;

import android.view.*;
import android.widget.*;
import java.util.*;
import java.math.*;
import com.example.saasvilcas.R;

/** Builds labeled inputs from reusable XML resources with stable view IDs. */
final class InventoryForm {
    private final LinearLayout container;
    private int nextId=0x6000;
    InventoryForm(View content) { container=content.findViewById(R.id.inventoryFormFields); }
    private void label(String name) {
        TextView label=(TextView)LayoutInflater.from(container.getContext()).inflate(R.layout.inventory_form_label,container,false);
        label.setText(name.toUpperCase(Locale.ROOT)); label.setTextColor(0xff7AAA95);
        label.setTextSize(11.5f); label.setLetterSpacing(.04f); label.setTypeface(label.getTypeface(),android.graphics.Typeface.BOLD);
        label.setLabelFor(nextId); container.addView(label);
    }
    EditText text(String name,String value,int inputType) {
        label(name);
        EditText field=(EditText)LayoutInflater.from(container.getContext()).inflate(R.layout.inventory_form_text,container,false);
        field.setId(nextId++); field.setText(value); field.setInputType(inputType); field.setContentDescription(name);
        container.addView(field); return field;
    }
    Spinner select(String name,List<String> values,String selected) {
        label(name);
        Spinner field=(Spinner)LayoutInflater.from(container.getContext()).inflate(R.layout.inventory_form_spinner,container,false);
        field.setId(nextId++); field.setContentDescription(name); bind(field,values,selected);
        container.addView(field); return field;
    }
    View inflate(int layout) { return LayoutInflater.from(container.getContext()).inflate(layout,container,false); }
    void add(View view) { container.addView(view); }
    static void bind(Spinner spinner,List<String> values,String selected) {
        com.example.saasvilcas.shared.ui.FilterFieldAdapter.bind(spinner,values,selected);
    }
    static String value(Spinner field) { return field.getSelectedItem()==null?"":field.getSelectedItem().toString(); }
    static String value(EditText field) { return field.getText().toString().trim(); }
    static long cents(EditText field) {
        try { return new BigDecimal(value(field).replace(',','.')).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
        catch(RuntimeException error) { throw new IllegalArgumentException("Ingresa importes válidos, con máximo dos decimales."); }
    }
    static String money(long cents) { return String.format(Locale.US,"S/%.2f",BigDecimal.valueOf(cents,2)); }
    static void error(View content,String message) {
        TextView error=content.findViewById(R.id.inventoryFormError); error.setText(message); error.setVisibility(View.VISIBLE);
    }
}

