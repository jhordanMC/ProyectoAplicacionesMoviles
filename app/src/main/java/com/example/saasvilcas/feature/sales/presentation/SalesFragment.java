package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.sales.domain.SalesProduct;
import com.example.saasvilcas.feature.sales.domain.CartLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SalesFragment extends Fragment {
    private SalesViewModel model;
    private List<SalesProduct> renderedProducts;
    private int renderedColumns;
    private boolean cartExpanded;
    public SalesFragment() { super(R.layout.fragment_sales); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state) {
        com.example.saasvilcas.shared.ui.FilterPanelController.bind(this,view,"sales");
        renderedProducts=null;
        // Company scope retains the preview cart when navigating to another section.
        model=new ViewModelProvider(requireParentFragment(),SalesViewModel.factory(
                ((com.example.saasvilcas.VilcasApplication)requireActivity().getApplication()).sales)).get(SalesViewModel.class);
        view.findViewById(R.id.salesProducts).addOnLayoutChangeListener((grid,l,t,r,b,ol,ot,or,ob)-> {
            if(r-l!=or-ol) renderProducts(view);
        });
        cartExpanded=state!=null && state.getBoolean("cartExpanded");
        view.findViewById(R.id.salesCartToggle).setOnClickListener(button ->
                setCartExpanded(view,!cartExpanded,true));
        view.addOnLayoutChangeListener((root,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom)->{
            if(bottom-top!=oldBottom-oldTop) sizeCartBody(view);
        });
        getChildFragmentManager().setFragmentResultListener(CartItemEditor.UPDATED,
                getViewLifecycleOwner(), (key, result) -> {
                    renderCart(view); setCartExpanded(view,true,true);
                });
        EditText search=view.findViewById(R.id.filterSearch);
        search.setText(model.getSearch());
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text,int start,int count,int after) { }
            @Override public void onTextChanged(CharSequence text,int start,int before,int count) {
                model.search(text.toString()); renderProducts(view);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        bindFilter(view,R.id.salesSizeFilter,model.sizes(),model.getSize(),true);
        bindSiteFilter(view);
        bindGender(view);
        view.findViewById(R.id.salesClearFilters).setOnClickListener(button -> {
            model.search(""); model.size(""); model.site(""); model.gender("");
            search.setText("");
            ((Spinner)view.findViewById(R.id.salesSizeFilter)).setSelection(0);
            ((Spinner)view.findViewById(R.id.salesSiteFilter)).setSelection(0);
            ((ChipGroup)view.findViewById(R.id.salesGenderFilter)).clearCheck();
            renderProducts(view);
        });
        view.findViewById(R.id.salesCheckout).setOnClickListener(button->{
            if(model.lines().isEmpty()) return;
            if (getChildFragmentManager().isStateSaved()) return;
            if (getChildFragmentManager().findFragmentByTag("sales-summary") == null)
                new CheckoutModalFragment().showNow(getChildFragmentManager(), "sales-summary");
        });
        view.findViewById(R.id.salesRetry).setOnClickListener(v->model.reload());
        view.findViewById(R.id.salesLoadMore).setOnClickListener(v->model.loadMore());
        ((android.widget.ScrollView)view.findViewById(R.id.salesCatalogScroll)).setOnScrollChangeListener((v,x,y,oldX,oldY)->{
            android.widget.ScrollView scroll=(android.widget.ScrollView)v;
            if(y>oldY && scroll.getChildAt(0).getHeight()-scroll.getHeight()-y<dp(200)
                    && model.state().getValue().message.isEmpty()) model.loadMore();
        });
        model.state().observe(getViewLifecycleOwner(),status->{
            if(status.expired) { ((com.example.saasvilcas.app.FrontendNavigator)requireActivity()).showLogin(); return; }
            TextView message=view.findViewById(R.id.sales_demo_notice);
            message.setText(status.message); message.setVisibility(status.message.isEmpty()?View.GONE:View.VISIBLE);
            view.findViewById(R.id.salesRetry).setVisibility(status.message.isEmpty()?View.GONE:View.VISIBLE);
            view.findViewById(R.id.salesLoadMore).setVisibility(model.hasMore()?View.VISIBLE:View.GONE);
            view.findViewById(R.id.salesLoadMore).setEnabled(!status.loading);
            bindFilter(view,R.id.salesSizeFilter,model.sizes(),model.getSize(),true); bindSiteFilter(view);
            renderProducts(view); renderCart(view);
        });
        model.submission().observe(getViewLifecycleOwner(),status->renderCart(view));
        renderProducts(view); renderCart(view); setCartExpanded(view,cartExpanded,false); model.reload();
    }
    private void sizeCartBody(View view) {
        View body=view.findViewById(R.id.salesCartBody);
        android.view.ViewGroup.LayoutParams params=body.getLayoutParams();
        int height=Math.max(1,Math.round(view.getHeight()*.48f));
        if(params.height!=height) { params.height=height; body.setLayoutParams(params); }
    }
    private void setCartExpanded(View view,boolean expanded,boolean animate) {
        cartExpanded=expanded;
        if(animate && view.isLaidOut()) {
            android.transition.AutoTransition transition=new android.transition.AutoTransition();
            transition.setDuration(240);
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup)view,transition);
        }
        sizeCartBody(view);
        view.findViewById(R.id.salesCartBody).setVisibility(expanded?View.VISIBLE:View.GONE);
        View chevron=view.findViewById(R.id.salesCartChevron);
        chevron.animate().cancel();
        if(animate) chevron.animate().rotation(expanded?180f:0f).setDuration(240).start();
        else chevron.setRotation(expanded?180f:0f);
        view.findViewById(R.id.salesCartToggle).setContentDescription(getString(expanded?
                R.string.sales_collapse_cart:R.string.sales_expand_cart));
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("cartExpanded",cartExpanded);
    }
    private void bindFilter(View view,int id,List<String> options,String selected,boolean size) {
        Spinner spinner=view.findViewById(id);
        List<String> values=new ArrayList<>(); values.add(getString(R.string.sales_all)); values.addAll(options);
        SalesFieldAdapter.bind(requireContext(),spinner,values,selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent,View item,int position,long rowId) {
                String value=position==0?"":values.get(position);
                if(size) model.size(value); else model.site(value);
                renderProducts(view);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    private void bindSiteFilter(View view) {
        Spinner spinner=view.findViewById(R.id.salesSiteFilter);
        List<com.example.saasvilcas.feature.sales.domain.SalesSite> sites=new ArrayList<>(model.locations());
        List<String> labels=new ArrayList<>(); labels.add(getString(R.string.sales_all));
        int selected=0;
        for(int i=0;i<sites.size();i++) { labels.add(sites.get(i).name); if(sites.get(i).id.equals(model.getSite())) selected=i+1; }
        spinner.setOnItemSelectedListener(null);
        SalesFieldAdapter.bind(requireContext(),spinner,labels,labels.get(selected)); spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent,View item,int position,long id) { model.site(position==0?"":sites.get(position-1).id); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
    private void bindGender(View view) {
        ChipGroup group=view.findViewById(R.id.salesGenderFilter);
        String[] genders=getResources().getStringArray(R.array.sales_genders);
        for(String gender:genders) {
            Chip chip=(Chip)getLayoutInflater().inflate(R.layout.sales_item_gender,group,false);
            chip.setText(gender); chip.setId(View.generateViewId()); chip.setCheckable(true);
            chip.setSaveEnabled(false);
            group.addView(chip); chip.setChecked(gender.equals(model.getGender()));
            chip.setOnCheckedChangeListener((button,checked)->{
                if(checked) model.gender(gender);
                else if(model.getGender().equals(gender)) model.gender("");
                renderProducts(view);
            });
        }
    }
    private void renderProducts(View view) {
        GridLayout grid=view.findViewById(R.id.salesProducts);
        int columns=com.example.saasvilcas.shared.ui.ResponsiveGrid.columns(grid,12);
        List<SalesProduct> products=model.products();
        view.findViewById(R.id.salesNoResults).setVisibility(products.isEmpty() && !model.state().getValue().loading
                && model.state().getValue().message.isEmpty()?View.VISIBLE:View.GONE);
        if(columns==renderedColumns&&products.equals(renderedProducts)) return;
        renderedProducts=new ArrayList<>(products); renderedColumns=columns;
        grid.removeAllViews(); grid.setColumnCount(columns);
        for(int index=0;index<products.size();index++) {
            SalesProduct product=products.get(index);
            View card=getLayoutInflater().inflate(R.layout.sales_item_product,grid,false);
            label(card,R.id.salesProductName,product.name);
            label(card,R.id.salesProductPrice,money(product.priceCents));
            label(card,R.id.salesProductStock,getString(R.string.sales_stock,product.stock));
            card.findViewById(R.id.salesProductNoPhoto).setVisibility(product.imageUrl.isEmpty()?View.VISIBLE:View.GONE);
            com.example.saasvilcas.shared.ui.ProductImageLoader.load(card.findViewById(R.id.salesProductImage),product.imageUrl);
            card.setOnClickListener(button->CartItemEditor.show(this,product,null));
            GridLayout.LayoutParams params=new GridLayout.LayoutParams();
            params.width=0; params.height=GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec=GridLayout.spec(index%columns,1f); params.rowSpec=GridLayout.spec(index/columns);
            params.setMargins(index%columns==0?0:dp(6),0,index%columns==columns-1?0:dp(6),dp(12));
            card.setLayoutParams(params); grid.addView(card);
        }
        com.example.saasvilcas.shared.ui.ViewEntranceAnimator.play(grid);
    }
    private void renderCart(View view) {
        LinearLayout rows=view.findViewById(R.id.salesCartItems); rows.removeAllViews();
        List<CartLine> lines=model.lines();
        boolean empty=lines.isEmpty();
        view.findViewById(R.id.salesCartCount).setVisibility(empty?View.GONE:View.VISIBLE);
        view.findViewById(R.id.salesCartDivider).setVisibility(empty?View.GONE:View.VISIBLE);
        view.findViewById(R.id.salesCartTotals).setVisibility(empty?View.GONE:View.VISIBLE);
        view.findViewById(R.id.salesCartItems).setVisibility(empty?View.GONE:View.VISIBLE);
        View count=view.findViewById(R.id.salesCartCount); count.setSelected(true);
        label(view,R.id.salesCartCount,String.valueOf(lines.size()));
        view.findViewById(R.id.salesCartEmpty).setVisibility(lines.isEmpty()?View.VISIBLE:View.GONE);
        for(CartLine line:lines) {
            View row=getLayoutInflater().inflate(R.layout.sales_item_cart,rows,false);
            label(row,R.id.salesCartName,line.product.name);
            label(row,R.id.salesCartSite,getString(R.string.sales_item_details,line.size,line.site));
            label(row,R.id.salesCartQuantity,getString(R.string.sales_line_subtotal,line.quantity,money(line.product.priceCents)));
            label(row,R.id.salesCartSubtotal,money(line.subtotalCents()));
            row.findViewById(R.id.salesCartEdit).setOnClickListener(button->
                CartItemEditor.show(this,line.product,line));
            row.findViewById(R.id.salesCartRemove).setOnClickListener(button->{ model.remove(line.key()); renderCart(view); });
            row.findViewById(R.id.salesCartRemove).setEnabled(!model.submitting());
            row.findViewById(R.id.salesCartEdit).setEnabled(!model.submitting());
            if(line.product.realStock) com.example.saasvilcas.shared.ui.ProductImageLoader.load(row.findViewById(R.id.salesCartImage),line.product.imageUrl);
            rows.addView(row);
        }
        label(view,R.id.salesCartTotal,money(model.total()));
        label(view,R.id.salesCartCompactTotal,money(model.total()));
        label(view,R.id.salesCheckout,getString(R.string.sales_complete,money(model.total())));
        view.findViewById(R.id.salesCheckout).setEnabled(!lines.isEmpty() && !model.submitting());
        view.findViewById(R.id.salesCheckout).setAlpha(lines.isEmpty()?.5f:1f);
    }
    private void label(View root,int id,String value) { ((TextView)root.findViewById(id)).setText(value); }
    private String money(long cents) { return String.format(Locale.US,"S/%.2f",java.math.BigDecimal.valueOf(cents,2)); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}


