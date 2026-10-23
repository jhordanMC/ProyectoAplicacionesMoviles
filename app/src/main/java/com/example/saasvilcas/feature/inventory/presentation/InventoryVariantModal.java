package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.*;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.inventory.domain.InventoryVariant;

/** The second step edits a draft allocation; the product is saved only in its parent. */
public final class InventoryVariantModal extends InventoryModalFragment {
    @Override protected boolean fullScreen() { return false; }
    @Override protected int formTitleId() { return R.id.inventoryModalTitle; }
    @Override protected int[] fixedActionIds() { return new int[]{R.id.inventoryFormSave,R.id.inventoryFormCancel}; }

    static InventoryVariantModal create(int index) {
        InventoryVariantModal modal=new InventoryVariantModal(); Bundle args=new Bundle(); args.putInt("index",index); modal.setArguments(args); return modal;
    }
    @Override protected int contentLayout() { return R.layout.inventory_modal_form; }
    @Override protected void bindContent(View content) {
        InventoryProductModal parent=(InventoryProductModal)requireParentFragment();
        InventoryViewModel model=InventoryViewModel.obtain(parent.requireParentFragment().requireParentFragment());
        int index=requireArguments().getInt("index");
        List<InventoryVariant> draft=parent.draftVariants(); InventoryVariant current=index<0?null:draft.get(index);
        TextView title=content.findViewById(R.id.inventoryModalTitle);
        title.setText(index<0?"Agregar talla y stock":"Editar talla y stock"); title.setTextColor(0xff05833B); title.setTextSize(16);
        InventoryForm form=new InventoryForm(content);
        EditText size=form.text("Talla (opcional)",current==null?"":current.size,InputType.TYPE_CLASS_TEXT);
        size.setHint("Talla (Ej: 39) — vacío si no aplica");
        Spinner type=form.select("Tipo de sede",Arrays.asList("Tipo…","Local","Almacén"),current==null?"Tipo…":current.type);
        Spinner location=form.select("Sede / Almacén",Collections.singletonList("Elige un tipo primero"),"");
        EditText stock=form.text("Stock",current==null?"":String.valueOf(current.stock),InputType.TYPE_CLASS_NUMBER);
        stock.setHint("Stock (Ej: 10)");
        TextView notice=(TextView)getLayoutInflater().inflate(R.layout.inventory_form_label,null,false);
        notice.setText("El stock ingresado corresponde a la sede seleccionada."); notice.setTextColor(0xff79AC98); form.add(notice);
        EditText sku=form.text("SKU",current==null?"":current.sku,InputType.TYPE_CLASS_TEXT);
        sku.setHint("SKU (Ej: ZAP-39)");
        EditText barcode=form.text("Código de barras (EAN-13, opcional)",current==null?"":current.barcode,InputType.TYPE_CLASS_TEXT);
        final String[] boundType={null};
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapter,View item,int position,long id) {
                String value=InventoryForm.value(type);
                if(value.equals(boundType[0])) return;
                List<String> options=new ArrayList<>();
                options.add(position==0?"Elige un tipo primero":position==1?"Seleccionar local":"Seleccionar almacén");
                if(position==1) options.addAll(model.references(3));
                if(position==2) options.addAll(model.references(2));
                InventoryForm.bind(location,options,current!=null&&boundType[0]==null?current.location:"");
                location.setEnabled(position!=0); boundType[0]=value;
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        if(draft.size()>(current==null?0:1)) {
            TextView heading=(TextView)getLayoutInflater().inflate(R.layout.inventory_form_label,null,false);
            heading.setText("Otras tallas del producto"); heading.setTextColor(0xff05833B); form.add(heading);
            for(int i=0;i<draft.size();i++) if(i!=index) {
                InventoryVariant other=draft.get(i);
                TextView text=(TextView)getLayoutInflater().inflate(R.layout.inventory_form_label,null,false);
                text.setText((other.size.isEmpty()?"Sin talla":other.size)+" · "+other.stock+" uds\n"+other.location+
                    "\nSKU: "+other.sku+" · Cód. barras: "+other.barcode); text.setTextColor(0xff103D33); form.add(text);
            }
        }
        ((TextView)content.findViewById(R.id.inventoryFormSave)).setText("Guardar cambios");
        content.findViewById(R.id.inventoryFormCancel).setOnClickListener(v->dismiss());
        content.findViewById(R.id.inventoryFormSave).setOnClickListener(v->{
            try {
                if(type.getSelectedItemPosition()==0||location.getSelectedItemPosition()==0)
                    throw new IllegalArgumentException("Elige el local o almacén donde está este stock.");
                String selectedType=InventoryForm.value(type),selectedLocation=InventoryForm.value(location);
                boolean same=current!=null&&current.type.equals(selectedType)&&current.location.equals(selectedLocation);
                parent.saveVariant(index,new InventoryVariant(InventoryForm.value(size),selectedType,
                    selectedLocation,Integer.parseInt(InventoryForm.value(stock)),InventoryForm.value(sku),InventoryForm.value(barcode),same?current.locationId:"",same?current.version:null));
                dismiss();
            } catch(IllegalArgumentException error) { InventoryForm.error(content,error instanceof NumberFormatException?"Ingresa stock entero mayor o igual a 0.":error.getMessage()); }
        });
    }
    static Bundle encode(InventoryVariant variant) {
        Bundle b=new Bundle(); b.putString("size",variant.size); b.putString("type",variant.type); b.putString("location",variant.location);
        b.putInt("stock",variant.stock); b.putString("sku",variant.sku); b.putString("barcode",variant.barcode);
        b.putString("locationId",variant.locationId); if(variant.version!=null) b.putInt("version",variant.version); return b;
    }
    static InventoryVariant decode(Bundle b) {
        return new InventoryVariant(b.getString("size"),b.getString("type"),b.getString("location"),b.getInt("stock"),b.getString("sku"),b.getString("barcode"),b.getString("locationId",""),b.containsKey("version")?b.getInt("version"):null);
    }
}


