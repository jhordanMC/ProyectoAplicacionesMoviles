package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import java.util.*;
import java.math.BigDecimal;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;
import com.example.saasvilcas.feature.inventory.domain.InventoryProduct;
import com.example.saasvilcas.feature.inventory.domain.InventoryVariant;

public final class InventoryProductModal extends InventoryModalFragment {
    @Override protected boolean fullScreen() { return false; }
    @Override protected int formTitleId() { return R.id.inventoryModalTitle; }
    @Override protected int[] fixedActionIds() { return new int[]{R.id.inventoryFormSave,R.id.inventoryFormCancel}; }

    private String photo="";
    private boolean photoLoaded;
    private ImageView preview;
    private View photoControls;
    private String pendingPhoto;
    private final ArrayList<InventoryVariant> variants=new ArrayList<>();
    private boolean variantsLoaded;
    private View variantComponent;
    private View formContent;
    private EditText name,cost,price;
    private Spinner category,gender,supplier;
    private String originalFingerprint;
    private final ActivityResultLauncher<Uri> camera=registerForActivityResult(new ActivityResultContracts.TakePicture(),success->{
        if(success&&pendingPhoto!=null) { photo=pendingPhoto; if(preview!=null) preview.setImageURI(Uri.parse(photo)); refreshPhotoControls(); }
        pendingPhoto=null;
    });
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri==null) return;
        try { requireContext().getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch(SecurityException ignored) { }
        photo=uri.toString(); if(preview!=null) preview.setImageURI(uri); refreshPhotoControls();
    });
    public static InventoryProductModal create(String id) {
        InventoryProductModal modal=new InventoryProductModal(); Bundle args=new Bundle(); args.putString("id",id); modal.setArguments(args); return modal;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setCancelable(false);
        if(state!=null) {
            photo=state.getString("photo",""); pendingPhoto=state.getString("pendingPhoto"); photoLoaded=true;
            ArrayList<Bundle> saved=state.getParcelableArrayList("variants");
            if(saved!=null) for(Bundle bundle:saved) variants.add(InventoryVariantModal.decode(bundle));
            variantsLoaded=true; originalFingerprint=state.getString("originalFingerprint");
        }
    }
    @Override public android.app.Dialog onCreateDialog(Bundle state) {
        android.app.Dialog dialog=super.onCreateDialog(state);
        dialog.setOnKeyListener((d,key,event)-> {
            if(key!=android.view.KeyEvent.KEYCODE_BACK) return false;
            if(event.getAction()==android.view.KeyEvent.ACTION_UP) onCloseRequested();
            return true;
        });
        return dialog;
    }
    @Override protected int contentLayout() { return R.layout.inventory_modal_form; }
    @Override protected void bindContent(View content) {
        InventoryViewModel model=InventoryViewModel.obtain(requireParentFragment().requireParentFragment());
        String id=requireArguments().getString("id"); InventoryProduct p=id==null?null:model.product(id);
        formContent=content;
        if(!variantsLoaded) { if(p!=null) variants.addAll(p.variants); variantsLoaded=true; }
        if(!photoLoaded) { photo=p==null?"":p.photo; photoLoaded=true; }
        ((TextView)content.findViewById(R.id.inventoryModalTitle)).setText(p==null?"Nuevo producto":"Editar producto");
        ((TextView)content.findViewById(R.id.inventoryModalTitle)).setTextColor(android.graphics.Color.rgb(5,131,59));
        ((TextView)content.findViewById(R.id.inventoryModalTitle)).setTextSize(16);
        InventoryForm form=new InventoryForm(content);
        View photoComponent=form.inflate(R.layout.inventory_component_photo);
        photoControls=photoComponent;
        preview=photoComponent.findViewById(R.id.inventoryPhotoPreview);
        if(!photo.isEmpty()) {
            if(photo.startsWith("content:")) preview.setImageURI(Uri.parse(photo));
            else com.example.saasvilcas.shared.ui.ProductImageLoader.load(preview,photo);
        }
        photoComponent.findViewById(R.id.inventoryPhotoGallery).setOnClickListener(v->picker.launch(new String[]{"image/*"}));
        photoComponent.findViewById(R.id.inventoryPhotoRemove).setOnClickListener(v->{ photo=""; preview.setImageDrawable(null); refreshPhotoControls(); });
        photoComponent.findViewById(R.id.inventoryPhotoCamera).setOnClickListener(v->{
            try {
                java.io.File directory=new java.io.File(requireContext().getFilesDir(),"inventory_photos");
                if(!directory.exists()&&!directory.mkdirs()) throw new java.io.IOException();
                java.io.File file=java.io.File.createTempFile("product_",".jpg",directory);
                Uri uri=androidx.core.content.FileProvider.getUriForFile(requireContext(),requireContext().getPackageName()+".files",file);
                pendingPhoto=uri.toString(); camera.launch(uri);
            } catch(java.io.IOException|android.content.ActivityNotFoundException error) {
                InventoryForm.error(content,"No se pudo abrir la cámara. Puedes elegir una imagen de la galería.");
            }
        });
        form.add(photoComponent);
        refreshPhotoControls();
        String[] labels=getResources().getStringArray(R.array.inventory_product_fields);
        name=form.text(labels[0],p==null?"":p.name,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setHint("Ej: Zapatos Ejecutivos Confort");
        category=form.select(labels[1],optional("Sin categoría",model.references(0)),p==null||p.category.isEmpty()?"Sin categoría":p.category);
        gender=form.select(labels[2],optional("Sin definir",Arrays.asList(getResources().getStringArray(R.array.inventory_genders))),p==null||p.gender.isEmpty()?"Sin definir":p.gender);
        supplier=form.select("Proveedor (Opcional)",optional("Sin proveedor",model.references(1)),p==null||p.supplier.isEmpty()?"Sin proveedor":p.supplier);
        int moneyType=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL;
        cost=form.text(labels[5],p==null?"0":BigDecimal.valueOf(p.cost,2).toPlainString(),moneyType);
        price=form.text(labels[6],p==null?"0":BigDecimal.valueOf(p.price,2).toPlainString(),moneyType);
        TextView margin=(TextView)getLayoutInflater().inflate(R.layout.inventory_form_label,null,false);
        margin.setTextColor(android.graphics.Color.rgb(5,131,59)); form.add(margin);
        Runnable refreshMargin=()-> {
            try { margin.setText("Margen de ganancia: "+InventoryForm.money(InventoryForm.cents(price)-InventoryForm.cents(cost))+"  ⓘ"); }
            catch(IllegalArgumentException error) { margin.setText("Margen de ganancia: —  ⓘ"); }
        };
        android.text.TextWatcher watcher=new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { refreshMargin.run(); }
            public void afterTextChanged(android.text.Editable s) { }
        };
        cost.addTextChangedListener(watcher); price.addTextChangedListener(watcher); refreshMargin.run();
        margin.setOnClickListener(v->com.example.saasvilcas.shared.ui.InfoModalFragment.create(R.string.inventory_margin,
            "El margen de ganancia sin descontar IGV, gastos u otros costos.").show(getChildFragmentManager(),"margin"));
        variantComponent=form.inflate(R.layout.inventory_component_variants); form.add(variantComponent);
        variantComponent.findViewById(R.id.inventoryVariantAdd).setOnClickListener(v->editVariant(-1));
        renderVariants();
        if(originalFingerprint==null) originalFingerprint=fingerprint();
        ((TextView)content.findViewById(R.id.inventoryFormSave)).setText("Guardar producto");
        content.findViewById(R.id.inventoryFormCancel).setOnClickListener(v->onCloseRequested());
        content.findViewById(R.id.inventoryFormSave).setOnClickListener(v->{
            try {
                if(InventoryForm.value(name).isEmpty()) { name.setError("Ingresa el nombre del producto."); return; }
                model.save(new InventoryProduct(id==null?UUID.randomUUID().toString():id,InventoryForm.value(name),
                    optionalValue(category),optionalValue(gender),optionalValue(supplier),
                    InventoryForm.cents(price),InventoryForm.cents(cost),photo,variants),operationKey);
            } catch(IllegalArgumentException error) { InventoryForm.error(content,error instanceof NumberFormatException?"Ingresa un stock válido.":error.getMessage()); }
        });
    }
    private List<String> optional(String first,List<String> values) { List<String> result=new ArrayList<>(); result.add(first); result.addAll(values); return result; }
    private void refreshPhotoControls() {
        if(photoControls==null) return;
        boolean empty=photo.isEmpty();
        photoControls.setBackgroundResource(empty?R.drawable.inventory_bg_photo_empty:R.drawable.inventory_bg_photo_card);
        preview.setVisibility(empty?View.GONE:View.VISIBLE);
        photoControls.findViewById(R.id.inventoryPhotoEmptyTitle).setVisibility(empty?View.VISIBLE:View.GONE);
        TextView cameraButton=photoControls.findViewById(R.id.inventoryPhotoCamera),galleryButton=photoControls.findViewById(R.id.inventoryPhotoGallery);
        cameraButton.setText(empty?"Tomar foto":"Cámara"); galleryButton.setText(empty?"Subir archivo":"Galería");
        cameraButton.setBackgroundResource(empty?R.drawable.inventory_bg_primary:R.drawable.inventory_bg_cancel);
        cameraButton.setTextColor(empty?0xffFFFFFF:0xff4B5A63);
        cameraButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.inventory_ic_camera,0,0,0);
        galleryButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.inventory_ic_gallery,0,0,0);
        cameraButton.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(empty?0xffFFFFFF:0xff4B5A63));
        int spacing=Math.round(6*getResources().getDisplayMetrics().density);
        cameraButton.setCompoundDrawablePadding(spacing); galleryButton.setCompoundDrawablePadding(spacing);
        cameraButton.setTextSize(empty?11.5f:10.5f); galleryButton.setTextSize(empty?11.5f:10.5f);
        galleryButton.setBackgroundResource(empty?R.drawable.inventory_bg_secondary:R.drawable.inventory_bg_cancel);
    }
    private String optionalValue(Spinner field) { return field.getSelectedItemPosition()==0?"":InventoryForm.value(field); }
    List<InventoryVariant> draftVariants() { return new ArrayList<>(variants); }
    void editVariant(int index) {
        if(getChildFragmentManager().findFragmentByTag("variant-editor")==null)
            InventoryVariantModal.create(index).show(getChildFragmentManager(),"variant-editor");
    }
    void saveVariant(int index,InventoryVariant variant) {
        for(int i=0;i<variants.size();i++) if(i!=index&&variants.get(i).key().equals(variant.key()))
            throw new IllegalArgumentException("Ya existe esa talla en la misma sede. Edita la fila existente.");
        if(index<0) variants.add(variant); else variants.set(index,variant); renderVariants();
    }
    void removeVariant(int index) { variants.remove(index); renderVariants(); }
    private void renderVariants() {
        LinearLayout rows=variantComponent.findViewById(R.id.inventoryVariantRows); rows.removeAllViews();
        long stock=0; Set<String> locations=new HashSet<>();
        for(int i=0;i<variants.size();i++) {
            final int index=i; InventoryVariant variant=variants.get(i);
            View row=getLayoutInflater().inflate(R.layout.inventory_item_variant,rows,false);
            ((TextView)row.findViewById(R.id.inventoryVariantSummary)).setText(getString(R.string.inventory_variant_summary,
                variant.size.isEmpty()?"Sin talla":variant.size,variant.type,variant.location,variant.stock,
                variant.sku.isEmpty()?"—":variant.sku,variant.barcode.isEmpty()?"—":variant.barcode));
            row.findViewById(R.id.inventoryVariantEdit).setOnClickListener(v->editVariant(index));
            row.findViewById(R.id.inventoryVariantDelete).setOnClickListener(v->InventoryDraftConfirmModal.create(index).show(getChildFragmentManager(),"draft-confirm"));
            rows.addView(row); stock+=variant.stock; locations.add(variant.type+":"+variant.location);
        }
        variantComponent.findViewById(R.id.inventoryVariantEmpty).setVisibility(variants.isEmpty()?View.VISIBLE:View.GONE);
        TextView totals=variantComponent.findViewById(R.id.inventoryVariantTotals);
        totals.setText(getString(R.string.inventory_variant_totals,variants.size(),locations.size(),stock));
        totals.setVisibility(variants.isEmpty()?View.GONE:View.VISIBLE);
    }
    private String fingerprint() {
        StringBuilder value=new StringBuilder(); value.append(InventoryForm.value(name)).append('|').append(InventoryForm.value(cost))
            .append('|').append(InventoryForm.value(price)).append('|').append(InventoryForm.value(category)).append('|')
            .append(InventoryForm.value(gender)).append('|').append(InventoryForm.value(supplier)).append('|').append(photo);
        for(InventoryVariant v:variants) value.append('|').append(v.key()).append('|').append(v.stock).append('|').append(v.sku).append('|').append(v.barcode);
        return value.toString();
    }
    @Override protected void onCloseRequested() {
        if(operationModel!=null&&operationModel.state.getValue().busy) return;
        if(formContent==null||fingerprint().equals(originalFingerprint)) { dismiss(); return; }
        if(getChildFragmentManager().findFragmentByTag("draft-confirm")==null)
            InventoryDraftConfirmModal.create(-1).show(getChildFragmentManager(),"draft-confirm");
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putString("photo",photo); state.putString("pendingPhoto",pendingPhoto);
        state.putString("originalFingerprint",originalFingerprint);
        ArrayList<Bundle> saved=new ArrayList<>(); for(InventoryVariant variant:variants) saved.add(InventoryVariantModal.encode(variant));
        state.putParcelableArrayList("variants",saved);
    }
    @Override public void onDestroyView() { preview=null; photoControls=null; variantComponent=null; formContent=null; name=null; cost=null; price=null; category=null; gender=null; supplier=null; super.onDestroyView(); }
}


