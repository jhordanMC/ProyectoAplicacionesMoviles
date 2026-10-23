package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.saasvilcas.R;

/** Confirmation for draft-only changes, without touching the catalog. */
public final class InventoryDraftConfirmModal extends InventoryModalFragment {
    static InventoryDraftConfirmModal create(int index) {
        InventoryDraftConfirmModal modal=new InventoryDraftConfirmModal(); Bundle args=new Bundle(); args.putInt("index",index); modal.setArguments(args); return modal;
    }
    @Override protected boolean showHeaderBrand() { return false; }
    @Override protected boolean showCloseIcon() { return false; }
    @Override protected int contentLayout() { return R.layout.inventory_modal_delete; }
    @Override protected void bindContent(View content) {
        int index=requireArguments().getInt("index");
        ((TextView)content.findViewById(R.id.inventoryDeleteHeading)).setText(index<0?"¿Salir sin guardar?":"¿Eliminar esta talla?");
        ((TextView)content.findViewById(R.id.inventoryDeleteMessage)).setText(index<0?
            "Se perderán los cambios que hiciste en este formulario.":"Se quitará esta fila del producto. El inventario cambia al guardar el producto.");
        ((TextView)content.findViewById(R.id.inventoryDeleteConfirm)).setText(index<0?"Sí, descartar cambios":"Sí, quiero eliminar");
        content.findViewById(R.id.inventoryDeleteCancel).setOnClickListener(v->dismiss());
        content.findViewById(R.id.inventoryDeleteConfirm).setOnClickListener(v->{
            InventoryProductModal parent=(InventoryProductModal)requireParentFragment();
            if(index<0) { dismiss(); parent.dismiss(); }
            else { parent.removeVariant(index); dismiss(); }
        });
    }
}

