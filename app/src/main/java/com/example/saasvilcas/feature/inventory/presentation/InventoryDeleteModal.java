package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

public final class InventoryDeleteModal extends InventoryModalFragment {
    @Override protected boolean showHeaderBrand() { return false; }
    @Override protected boolean showCloseIcon() { return false; }
    public static InventoryDeleteModal product(String id) { return create(-1,id); }
    public static InventoryDeleteModal reference(int kind,String name) { return create(kind,name); }
    private static InventoryDeleteModal create(int kind,String value) {
        InventoryDeleteModal modal=new InventoryDeleteModal(); Bundle args=new Bundle(); args.putInt("kind",kind); args.putString("value",value); modal.setArguments(args); return modal;
    }
    @Override protected int contentLayout() { return R.layout.inventory_modal_delete; }
    @Override protected void bindContent(View content) {
        InventoryViewModel model=InventoryViewModel.obtain(requireParentFragment().requireParentFragment());
        int kind=requireArguments().getInt("kind"); String value=requireArguments().getString("value");
        String name=value;
        if(kind<0) {
            try { name=model.product(value).name; }
            catch(IllegalArgumentException missing) { name="este producto"; } // A completed delete may precede view recreation.
        }
        ((TextView)content.findViewById(R.id.inventoryDeleteHeading)).setText(kind<0?
            getString(R.string.inventory_delete_title):getResources().getStringArray(R.array.inventory_delete_headings)[kind]);
        ((TextView)content.findViewById(R.id.inventoryDeleteMessage)).setText(
            kind<0?getString(R.string.inventory_delete_message,name):"Se eliminará “"+name+"”.");
        content.findViewById(R.id.inventoryDeleteCancel).setOnClickListener(v->dismiss());
        content.findViewById(R.id.inventoryDeleteConfirm).setOnClickListener(v->{
            try { if(kind<0) model.delete(value,operationKey); else model.deleteReference(kind,value,operationKey); }
            catch(IllegalArgumentException error) {
                TextView message=content.findViewById(R.id.inventoryDeleteMessage); message.setText(error.getMessage());
            }
        });
    }
}

