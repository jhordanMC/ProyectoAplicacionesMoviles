package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.annotation.*;
import java.util.Locale;
import com.example.saasvilcas.R;

/** One compact management layout for categories, suppliers and warehouses. */
public final class InventoryManagementModal extends InventoryModalFragment {
    @Override protected boolean fullScreen() { return false; }
    @Override protected int maxWidthDp() { return 560; }
    @Override protected int surfaceBackground() { return R.drawable.inventory_bg_management_modal; }
    @Override protected int formTitleId() { return R.id.inventoryModalTitle; }

    private InventoryViewModel model;
    private int kind;
    private String previous;
    private EditText name,address,description,search;
    private View content;
    public static InventoryManagementModal create(int kind) {
        InventoryManagementModal modal=new InventoryManagementModal(); Bundle args=new Bundle(); args.putInt("kind",kind); modal.setArguments(args); return modal;
    }
    @Override public void onCreate(Bundle state) { super.onCreate(state); if(state!=null) previous=state.getString("previous"); }
    @Override protected int contentLayout() { return R.layout.inventory_modal_management; }
    @Override protected void bindContent(View content) {
        this.content=content; kind=requireArguments().getInt("kind");
        model=InventoryViewModel.obtain(requireParentFragment().requireParentFragment());
        int[] titles={R.string.inventory_categories,R.string.inventory_suppliers,R.string.inventory_warehouses};
        ((TextView)content.findViewById(R.id.inventoryModalTitle)).setText(titles[kind]);
        name=content.findViewById(R.id.inventoryManagementName);
        address=content.findViewById(R.id.inventoryManagementAddress);
        description=content.findViewById(R.id.inventoryManagementDescription);
        search=content.findViewById(R.id.inventoryManagementSearch);
        String[] hints={"Nombre de la categoría...","Nombre o razón social...","Nombre (Ej: Depósito Central)"};
        name.setHint(hints[kind]);
        address.setVisibility(kind==2?View.VISIBLE:View.GONE);
        description.setVisibility(kind==2?View.VISIBLE:View.GONE);
        String[] headings={"CATEGORÍAS EXISTENTES","PROVEEDORES EXISTENTES","ALMACENES EXISTENTES"};
        ((TextView)content.findViewById(R.id.inventoryManagementListTitle)).setText(headings[kind]);
        if(previous!=null) loadEditor(previous);
        updateActions();
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { renderRows(); }
            public void afterTextChanged(Editable s) { }
        });
        content.findViewById(R.id.inventoryFormSave).setOnClickListener(v->{
            try {
                if(kind==2) model.saveWarehouse(previous,InventoryForm.value(name),InventoryForm.value(address),InventoryForm.value(description),operationKey);
                else model.saveReference(kind,previous,InventoryForm.value(name),operationKey);
            } catch(IllegalArgumentException error) { InventoryForm.error(content,error.getMessage()); }
        });
        content.findViewById(R.id.inventoryFormCancel).setOnClickListener(v->clearEditor());
        renderRows();
    }
    private void loadEditor(String value) {
        previous=value; name.setText(value);
        if(kind==2) { String[] details=model.warehouseDetails(value); address.setText(details[0]); description.setText(details[1]); }
        updateActions();
    }
    private void clearEditor() {
        previous=null; name.setText(""); address.setText(""); description.setText(""); updateActions(); renderRows();
    }
    @Override protected void onOperationSuccess() {
        clearEditor(); content.findViewById(R.id.inventoryFormError).setVisibility(View.GONE);
        operationKey=java.util.UUID.randomUUID().toString();
    }
    private void updateActions() {
        String[] newLabels={"NUEVA CATEGORÍA","NUEVO PROVEEDOR","NUEVO ALMACÉN"};
        String[] editLabels={"EDITAR CATEGORÍA","EDITAR PROVEEDOR","EDITAR ALMACÉN"};
        ((TextView)content.findViewById(R.id.inventoryManagementFormTitle)).setText(previous==null?newLabels[kind]:editLabels[kind]);
        ((TextView)content.findViewById(R.id.inventoryFormSave)).setText(previous!=null?"Guardar cambios":kind==2?"+ Crear almacén":"+ Crear");
        content.findViewById(R.id.inventoryFormCancel).setVisibility(previous==null?View.GONE:View.VISIBLE);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        super.onViewCreated(view,state);
        model.revision.observe(getViewLifecycleOwner(),revision->renderRows());
    }
    private void renderRows() {
        LinearLayout rows=content.findViewById(R.id.inventoryManagementRows); rows.removeAllViews();
        search.setVisibility(model.references(kind).size()>4?View.VISIBLE:View.GONE);
        String term=model.references(kind).size()>4?InventoryForm.value(search).toLowerCase(Locale.ROOT):"";
        com.google.android.material.chip.ChipGroup suggestions=content.findViewById(R.id.inventoryCategorySuggestions);
        suggestions.removeAllViews(); suggestions.setVisibility(kind==0&&previous==null?View.VISIBLE:View.GONE);
        if(kind==0&&previous==null) {
            for(String value:getResources().getStringArray(R.array.inventory_category_suggestions)) {
                boolean exists=false;
                for(String existing:model.references(0)) if(existing.equalsIgnoreCase(value)) exists=true;
                if(exists) continue;
                com.google.android.material.chip.Chip chip=new com.google.android.material.chip.Chip(requireContext());
                chip.setText(value); chip.setTextSize(11); chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(0xffFFFFFF));
                chip.setChipStrokeWidth(1); chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(0xffD5E3E2));
                chip.setOnClickListener(v->name.setText(value)); suggestions.addView(chip);
            }
        }
        for(String value:model.references(kind)) {
            if(!value.toLowerCase(Locale.ROOT).contains(term)) continue;
            View row=getLayoutInflater().inflate(R.layout.inventory_item_reference,rows,false);
            row.setSelected(value.equals(previous));
            ((TextView)row.findViewById(R.id.inventoryReferenceName)).setText(value);
            TextView detail=row.findViewById(R.id.inventoryReferenceDetail);
            String subtitle=kind==2?model.warehouseDetails(value)[0]:"";
            detail.setText(subtitle); detail.setVisibility(subtitle.isEmpty()?View.GONE:View.VISIBLE);
            row.findViewById(R.id.inventoryReferenceEdit).setOnClickListener(v->{ loadEditor(value); renderRows(); name.requestFocus(); });
            row.findViewById(R.id.inventoryReferenceDelete).setOnClickListener(v->{
                if(getParentFragmentManager().findFragmentByTag("inventory-confirm")==null)
                    InventoryDeleteModal.reference(kind,value).show(getParentFragmentManager(),"inventory-confirm");
            });
            rows.addView(row);
        }
        content.findViewById(R.id.inventoryManagementEmpty).setVisibility(rows.getChildCount()==0?View.VISIBLE:View.GONE);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putString("previous",previous); }
    @Override public void onDestroyView() { content=null; name=null; address=null; description=null; search=null; super.onDestroyView(); }
}


