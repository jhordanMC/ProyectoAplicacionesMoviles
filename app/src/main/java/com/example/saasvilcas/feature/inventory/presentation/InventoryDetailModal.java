package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.inventory.domain.*;

public final class InventoryDetailModal extends InventoryModalFragment {
    static InventoryDetailModal create(String id) {
        InventoryDetailModal modal=new InventoryDetailModal(); Bundle args=new Bundle(); args.putString("id",id); modal.setArguments(args); return modal;
    }
    @Override protected int contentLayout() { return R.layout.inventory_modal_detail; }
    @Override protected void bindContent(View content) {
        InventoryProduct p=InventoryViewModel.obtain(requireParentFragment().requireParentFragment()).product(requireArguments().getString("id"));
        ((TextView)content.findViewById(R.id.inventoryDetailName)).setText(p.name);
        ImageView photo=content.findViewById(R.id.inventoryDetailPhoto);
        if(p.photo.isEmpty()) photo.setVisibility(View.GONE); else com.example.saasvilcas.shared.ui.ProductImageLoader.load(photo,p.photo);
        ((TextView)content.findViewById(R.id.inventoryDetailInfo)).setText(
            "Categoría: "+(p.category.isEmpty()?"Sin categoría":p.category)+"\nSexo: "+(p.gender.isEmpty()?"Sin definir":p.gender)+
            "\nProveedor: "+(p.supplier.isEmpty()?"Sin proveedor":p.supplier)+"\nCosto: "+InventoryForm.money(p.cost)+
            "\nPrecio: "+InventoryForm.money(p.price)+"\nMargen: "+InventoryForm.money(p.price-p.cost)+"\nStock total: "+p.stock);
        LinearLayout rows=content.findViewById(R.id.inventoryDetailRows);
        for(InventoryVariant variant:p.variants) {
            View row=getLayoutInflater().inflate(R.layout.inventory_item_variant,rows,false);
            ((TextView)row.findViewById(R.id.inventoryVariantSummary)).setText(getString(R.string.inventory_variant_summary,
                variant.size.isEmpty()?"Sin talla":variant.size,variant.type,variant.location,variant.stock,
                variant.sku.isEmpty()?"—":variant.sku,variant.barcode.isEmpty()?"—":variant.barcode));
            row.findViewById(R.id.inventoryVariantEdit).setVisibility(View.GONE);
            row.findViewById(R.id.inventoryVariantDelete).setVisibility(View.GONE); rows.addView(row);
        }
        content.findViewById(R.id.inventoryDetailClose).setOnClickListener(v->dismiss());
    }
}

