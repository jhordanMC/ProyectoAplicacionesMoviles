package com.example.saasvilcas.feature.inventory.presentation;

import android.os.Bundle;
import android.net.Uri;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.annotation.*;
import java.util.*;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.inventory.domain.InventoryProduct;

public final class InventoryFragment extends Fragment {
    private InventoryViewModel model;
    private List<InventoryProduct> renderedProducts;
    private int renderedColumns;
    private String requestedId,requestedKey,requestedAction;
    public InventoryFragment() { super(R.layout.fragment_inventory); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        com.example.saasvilcas.shared.ui.FilterPanelController.bind(this,view,"inventory");
        model=InventoryViewModel.obtain(requireParentFragment());
        renderedProducts=null;
        view.findViewById(R.id.inventoryProducts).addOnLayoutChangeListener((grid,l,t,r,b,ol,ot,or,ob)-> { if(r-l!=or-ol) render(view); });
        if(state!=null) { requestedId=state.getString("requestedId"); requestedKey=state.getString("requestedKey"); requestedAction=state.getString("requestedAction"); }
        int[] managers={R.id.inventoryCategories,R.id.inventorySuppliers,R.id.inventoryWarehouses};
        int[] managerTitles={R.string.inventory_categories,R.string.inventory_suppliers,R.string.inventory_warehouses};
        int[] managerIcons={R.drawable.inventory_ic_folder,R.drawable.modal_ic_user,R.drawable.sales_ic_site};
        String[] hints=getResources().getStringArray(R.array.inventory_management_hints);
        for(int i=0;i<managers.length;i++) {
            View row=view.findViewById(managers[i]);
            ((TextView)row.findViewById(R.id.inventoryManagementTitle)).setText(managerTitles[i]);
            ((TextView)row.findViewById(R.id.inventoryManagementSubtitle)).setText(hints[i]);
            ((ImageView)row.findViewById(R.id.inventoryManagementIcon)).setImageResource(managerIcons[i]);
            final int kind=i;
            view.findViewById(managers[i]).setOnClickListener(v->{
                if(getChildFragmentManager().findFragmentByTag("management")==null)
                    InventoryManagementModal.create(kind).show(getChildFragmentManager(),"management");
            });
        }
        view.findViewById(R.id.inventoryNew).setOnClickListener(v->edit(null));
        EditText search=view.findViewById(R.id.filterSearch); search.setText(model.search);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { model.search=s.toString(); model.filtersChanged(true); }
            public void afterTextChanged(Editable s) { }
        });
        model.revision.observe(getViewLifecycleOwner(),revision->{ bindFilters(view); render(view); });
        view.findViewById(R.id.inventoryMore).setOnClickListener(v->model.loadMore());
        model.state.observe(getViewLifecycleOwner(),result->{
            TextView status=view.findViewById(R.id.inventoryStatus); status.setText(result.message); status.setVisibility(result.message.isEmpty()?View.GONE:View.VISIBLE);
            view.findViewById(R.id.inventoryMore).setVisibility(model.hasMore()?View.VISIBLE:View.GONE);
            view.findViewById(R.id.inventoryMore).setEnabled(!result.loading&&!result.busy);
            view.findViewById(R.id.inventoryNew).setEnabled(!result.loading&&!result.busy);
            for(int manager:managers) view.findViewById(manager).setEnabled(!result.loading&&!result.busy);
            if(result.expired) { ((com.example.saasvilcas.VilcasApplication)requireActivity().getApplication()).sessions.clear(); ((com.example.saasvilcas.MainActivity)requireActivity()).showLogin(); return; }
            if(result.success&&result.key.equals(requestedKey)) {
                String id=requestedId,action=requestedAction; requestedKey=null; requestedId=null;
                if("edit".equals(action)) InventoryProductModal.create(id).show(getChildFragmentManager(),"product-editor");
                else InventoryDetailModal.create(id).show(getChildFragmentManager(),"product-detail");
            }
            render(view);
        });
        model.reload();
    }
    private void bindFilters(View view) {
        List<String> categories=new ArrayList<>(); categories.add("Todas"); categories.addAll(model.references(0));
        bindFilter(view,R.id.inventoryCategoryFilter,categories,model.category,true);
        List<String> genders=new ArrayList<>(); genders.add("Todos"); genders.addAll(Arrays.asList(getResources().getStringArray(R.array.inventory_genders)));
        bindFilter(view,R.id.inventoryGenderFilter,genders,model.gender,false);
    }
    private void bindFilter(View view,int id,List<String> values,String selected,boolean category) {
        Spinner spinner=view.findViewById(id); spinner.setOnItemSelectedListener(null);
        InventoryForm.bind(spinner,values,selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,View child,int position,long rowId) {
                String value=position==0?"":values.get(position);
                String old=category?model.category:model.gender;
                if(old.equals(value)) return;
                if(category) model.category=value; else model.gender=value;
                model.filtersChanged(false);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    private void edit(String id) {
        if(model.state.getValue().busy||model.state.getValue().loading) return;
        if(id!=null) { requestDetail(id,"edit"); return; }
        if(getChildFragmentManager().findFragmentByTag("product-editor")==null)
            InventoryProductModal.create(id).show(getChildFragmentManager(),"product-editor");
    }
    private void requestDetail(String id,String action) {
        if(model.state.getValue().busy||model.state.getValue().loading) return;
        requestedId=id; requestedAction=action; requestedKey=java.util.UUID.randomUUID().toString(); model.detail(id,requestedKey);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putString("requestedId",requestedId); state.putString("requestedKey",requestedKey); state.putString("requestedAction",requestedAction);
    }
    private void render(View view) {
        GridLayout grid=view.findViewById(R.id.inventoryProducts);
        int columns=com.example.saasvilcas.shared.ui.ResponsiveGrid.columns(grid,12);
        List<InventoryProduct> products=model.products();
        InventoryViewModel.State state=model.state.getValue();
        view.findViewById(R.id.inventoryEmpty).setVisibility(products.isEmpty()&&!state.loading&&!state.busy&&state.message.isEmpty()?View.VISIBLE:View.GONE);
        if(columns==renderedColumns&&products.equals(renderedProducts)) return;
        renderedProducts=new ArrayList<>(products); renderedColumns=columns;
        grid.removeAllViews(); grid.setColumnCount(columns);
        int index=0;
        for(InventoryProduct product:products) {
            View card=getLayoutInflater().inflate(R.layout.inventory_item_product,grid,false);
            card.findViewById(R.id.inventoryName).setOnClickListener(v->{
                if(getChildFragmentManager().findFragmentByTag("product-detail")==null)
                    requestDetail(product.id,"detail");
            });
            card.findViewById(R.id.inventoryImage).setOnClickListener(v->{
                if(getChildFragmentManager().findFragmentByTag("product-detail")==null)
                    requestDetail(product.id,"detail");
            });
            text(card,R.id.inventoryName,product.name);
            text(card,R.id.inventoryTags,product.category+" · "+product.gender);
            text(card,R.id.inventorySupplier,product.supplier);
            text(card,R.id.inventoryPrice,getString(R.string.inventory_price_margin,InventoryForm.money(product.price),InventoryForm.money(product.price-product.cost)));
            text(card,R.id.inventoryStock,getString(R.string.sales_stock,product.stock));
            if(!product.photo.isEmpty()) {
                try {
                    com.example.saasvilcas.shared.ui.ProductImageLoader.load(card.findViewById(R.id.inventoryImage),product.photo);
                    card.findViewById(R.id.inventoryPhotoMissing).setVisibility(View.GONE);
                } catch(SecurityException ignored) { }
            }
            card.findViewById(R.id.inventoryEdit).setOnClickListener(v->edit(product.id));
            card.findViewById(R.id.inventoryDuplicate).setOnClickListener(v->model.duplicate(product.id));
            card.findViewById(R.id.inventoryDelete).setOnClickListener(v->{
                if(getChildFragmentManager().findFragmentByTag("inventory-confirm")==null)
                    InventoryDeleteModal.product(product.id).show(getChildFragmentManager(),"inventory-confirm");
            });
            GridLayout.LayoutParams params=new GridLayout.LayoutParams();
            params.width=0; params.height=GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec=GridLayout.spec(index%columns,1f); params.rowSpec=GridLayout.spec(index/columns);
            int gap=Math.round(6*getResources().getDisplayMetrics().density);
            params.setMargins(index%columns==0?0:gap,0,index%columns==columns-1?0:gap,gap*2);
            grid.addView(card,params); index++;
        }
        com.example.saasvilcas.shared.ui.ViewEntranceAnimator.play(grid);
    }
    private void text(View view,int id,String value) { ((TextView)view.findViewById(id)).setText(value); }
}


