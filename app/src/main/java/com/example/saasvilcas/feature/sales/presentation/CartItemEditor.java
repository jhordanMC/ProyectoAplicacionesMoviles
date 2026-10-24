package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.sales.domain.SalesProduct;
import com.example.saasvilcas.feature.sales.domain.CartLine;
import com.example.saasvilcas.shared.ui.BrandedModalFragment;

/** Restorable add/edit dialog using the same shell as the profile. */
public final class CartItemEditor extends SalesModalFragment {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setShowsDialog(true); }
    @Override protected int formTitleId() { return R.id.salesDialogTitle; }
    @Override protected int[] fixedActionIds() { return new int[]{R.id.salesDialogSave,R.id.salesDialogCancel}; }
    static final String UPDATED = "sales_cart_updated";
    static void show(Fragment owner, SalesProduct product, CartLine line) {
        if (owner.getChildFragmentManager().findFragmentByTag("cart-editor") != null) return;
        CartItemEditor modal = new CartItemEditor();
        Bundle args = new Bundle();
        args.putString("product", product.id);
        if (line != null) {
            args.putString("key", line.key()); args.putString("size", line.size);
            args.putString("site", line.site); args.putInt("quantity", line.quantity);
            args.putLong("discount", line.discountCents);
            args.putString("siteId",line.siteId); args.putString("variantId",line.variantId);
        }
        modal.setArguments(args);
        modal.show(owner.getChildFragmentManager(), "cart-editor");
    }
    @Override protected int contentLayout() { return R.layout.sales_dialog_cart_item; }
    @Override protected void bindContent(View form) {
        SalesViewModel model = new ViewModelProvider(requireParentFragment().requireParentFragment()).get(SalesViewModel.class);
        Bundle args = requireArguments();
        SalesProduct product = model.product(args.getString("product"));
        String key = args.getString("key");
        ((TextView) form.findViewById(R.id.salesDialogTitle)).setText(product.name);
        Spinner size = form.findViewById(R.id.salesDialogSize);
        Spinner site = form.findViewById(R.id.salesDialogSite);
        com.google.android.material.chip.ChipGroup chips=form.findViewById(R.id.salesDialogSizeChips);
        for(String option:product.sizes) {
            com.google.android.material.chip.Chip chip=(com.google.android.material.chip.Chip)getLayoutInflater().inflate(R.layout.sales_item_size,chips,false);
            chip.setId(View.generateViewId()); chip.setText(option); chip.setCheckable(true); chips.addView(chip);
            chip.setOnCheckedChangeListener((button,checked)->{
                if(checked) for(int i=0;i<size.getCount();i++) if(option.equals(size.getItemAtPosition(i).toString())) size.setSelection(i);
            });
        }
        size.setVisibility(View.GONE);
        EditText quantity = form.findViewById(R.id.salesDialogQuantity);
        SalesFieldAdapter.bind(requireContext(), size, product.sizes, args.getString("size", model.getSize()));
        SalesFieldAdapter.bind(requireContext(), site, product.sites, args.getString("site", model.getSite()));
        final java.util.ArrayList<com.example.saasvilcas.feature.sales.domain.SalesSite> origins=new java.util.ArrayList<>();
        final Runnable bindOrigins=()->{
            if(!product.realStock) return;
            String selectedId=site.getSelectedItem() instanceof com.example.saasvilcas.feature.sales.domain.SalesSite?
                    ((com.example.saasvilcas.feature.sales.domain.SalesSite)site.getSelectedItem()).id:args.getString("siteId",model.getSite());
            String selectedSize=size.getSelectedItem()==null?"":size.getSelectedItem().toString();
            origins.clear(); origins.add(new com.example.saasvilcas.feature.sales.domain.SalesSite("","Selecciona sede",""));
            java.util.HashSet<String> added=new java.util.HashSet<>();
            for(com.example.saasvilcas.feature.sales.domain.SalesVariant variant:product.variants) {
                if(variant.size.equals(selectedSize) && model.assigned(variant.siteId) && added.add(variant.siteId))
                    origins.add(new com.example.saasvilcas.feature.sales.domain.SalesSite(variant.siteId,variant.siteName,""));
            }
            com.example.saasvilcas.shared.ui.FilterFieldAdapter.bindItems(site,new java.util.ArrayList<>(origins),null);
            for(int i=0;i<origins.size();i++) if(origins.get(i).id.equals(selectedId)) site.setSelection(i);
        };
        if(product.realStock) {
            java.util.ArrayList<String> values=new java.util.ArrayList<>(); values.add("Selecciona talla"); values.addAll(product.sizes);
            String selected=args.getString("size",model.getSize());
            if(product.sizes.size()==1) selected=product.sizes.get(0);
            SalesFieldAdapter.bind(requireContext(),size,values,selected);
            boolean noSize=product.sizes.size()==1 && "Sin talla".equals(product.sizes.get(0));
            chips.setVisibility(noSize?View.GONE:View.VISIBLE);
            form.findViewById(R.id.salesDialogSizeLabel).setVisibility(noSize?View.GONE:View.VISIBLE);
            size.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id) {
                    bindOrigins.run();
                    for(int i=0;i<chips.getChildCount();i++) {
                        com.google.android.material.chip.Chip chip=(com.google.android.material.chip.Chip)chips.getChildAt(i);
                        chip.setChecked(chip.getText().toString().equals(size.getSelectedItem().toString()));
                    }
                }
                public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            });
            bindOrigins.run();
            com.example.saasvilcas.shared.ui.ProductImageLoader.load(form.findViewById(R.id.salesDialogImage),product.imageUrl);
        }
        form.findViewById(R.id.salesDialogNoPhoto).setVisibility(product.imageUrl.isEmpty()?View.VISIBLE:View.GONE);
        quantity.setText(String.valueOf(args.getInt("quantity", 1)));
        EditText discount = form.findViewById(R.id.salesDialogDiscount);
        discount.setText(BigDecimal.valueOf(args.getLong("discount", 0), 2).toPlainString());
        TextView total = form.findViewById(R.id.salesDialogTotal);
        Runnable refreshTotal = () -> {
            try {
                int count = Integer.parseInt(quantity.getText().toString().trim());
                long reduction = discountCents(discount);
                long gross = product.priceCents * count;
                if (count <= 0 || reduction > gross) throw new IllegalArgumentException();
                total.setText(String.format(Locale.US, "S/%.2f", BigDecimal.valueOf(gross-reduction,2)));
            } catch (IllegalArgumentException error) { total.setText("—"); }
        };
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { refreshTotal.run(); }
            public void afterTextChanged(Editable s) { }
        };
        quantity.addTextChangedListener(watcher); discount.addTextChangedListener(watcher);
        refreshTotal.run();
        TextView save = form.findViewById(R.id.salesDialogSave);
        save.setText(key == null ? R.string.sales_add : R.string.sales_update);
        form.findViewById(R.id.salesDialogCancel).setOnClickListener(v -> dismiss());
        save.setOnClickListener(v -> {
            try {
                int count = Integer.parseInt(quantity.getText().toString().trim());
                long reduction;
                try {
                    reduction = discountCents(discount);
                    if (reduction > product.priceCents * count) throw new IllegalArgumentException();
                } catch (IllegalArgumentException error) {
                    discount.setError(getString(R.string.sales_invalid_discount)); return;
                }
                if(product.realStock) {
                    if(size.getSelectedItem()==null || !(site.getSelectedItem() instanceof com.example.saasvilcas.feature.sales.domain.SalesSite))
                        throw new IllegalArgumentException("Selecciona una talla y sede.");
                    String siteId=((com.example.saasvilcas.feature.sales.domain.SalesSite)site.getSelectedItem()).id;
                    com.example.saasvilcas.feature.sales.domain.SalesVariant chosen=null;
                    for(com.example.saasvilcas.feature.sales.domain.SalesVariant variant:product.variants)
                        if(variant.size.equals(size.getSelectedItem().toString()) && variant.siteId.equals(siteId)) { chosen=variant; break; }
                    if(chosen==null) throw new IllegalArgumentException("Selecciona una talla y sede con stock.");
                    model.put(key,product,chosen,count,reduction);
                } else model.put(key, product, size.getSelectedItem().toString(), site.getSelectedItem().toString(), count, reduction);
                com.example.saasvilcas.shared.ui.UiSounds.add();
                getParentFragmentManager().setFragmentResult(UPDATED, new Bundle()); dismiss();
            } catch (IllegalArgumentException error) { com.example.saasvilcas.shared.ui.UiSounds.error(); ((TextView)form.findViewById(R.id.salesDialogError)).setText(error.getMessage()==null?getString(R.string.sales_invalid_quantity):error.getMessage()); }
        });
    }
    private long discountCents(EditText field) {
        try {
            String text = field.getText().toString().trim().replace(',', '.');
            long value = new BigDecimal(text.isEmpty()?"0":text).setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2).longValueExact();
            if (value < 0) throw new IllegalArgumentException();
            return value;
        } catch (ArithmeticException error) { throw new IllegalArgumentException(error); }
    }
}

