package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.ui.FilterFieldAdapter;
import java.math.BigDecimal;
import java.util.*;

/** Restorable form -> receipt(s) -> final confirmation, using the Angular validation rules. */
public final class CheckoutModalFragment extends SalesModalFragment {
    private final int[] methodIds={R.id.checkoutCash,R.id.checkoutYape,R.id.checkoutPlin,R.id.checkoutTransfer};
    private final ArrayList<CheckoutLine> divided=new ArrayList<>();
    private CheckoutLine single;
    private SalesViewModel model;
    private boolean split,advanceAfterPhoto;
    private long total;
    private Bundle restored;
    private View form;
    private Spinner register;
    private String[] methods;
    @Override public void onCreate(Bundle state) { super.onCreate(state); restored=state; }
    @Override protected int contentLayout() { return R.layout.sales_dialog_checkout; }
    @Override protected void bindContent(View content) {
        form=content;
        model=new ViewModelProvider(requireParentFragment().requireParentFragment()).get(SalesViewModel.class);
        total=model.total(); methods=getResources().getStringArray(R.array.checkout_methods);
        single=restored!=null && restored.containsKey("single")?decode(restored.getBundle("single")):new CheckoutLine(0,decimal(total),decimal(total));
        split=restored!=null && restored.getBoolean("split");
        advanceAfterPhoto=restored!=null && restored.getBoolean("advancePhoto");
        if(restored!=null) {
            ArrayList<Bundle> values=restored.getParcelableArrayList("divided");
            if(values!=null) for(Bundle value:values) divided.add(decode(value));
        }
        ((TextView)form.findViewById(R.id.checkoutTotal)).setText(money(total));
        register=form.findViewById(R.id.checkoutRegister);
        java.util.List<SalesSite> registers=model.registers();
        FilterFieldAdapter.bindItems(register,registers,null);
        String savedId=restored==null?"":restored.getString("registerId","");
        for(int i=0;i<registers.size();i++) if(registers.get(i).id.equals(savedId)) register.setSelection(i);
        EditText received=form.findViewById(R.id.checkoutReceived); received.setText(single.received);
        watch(received,value->{ single.received=value; refresh(); });
        for(int i=0;i<4;i++) {
            final int method=i;
            form.findViewById(methodIds[i]).setOnClickListener(v->{
                if(single.method!=method) single.receipt="";
                single.method=method;
                View button=form.findViewById(methodIds[method]);
                if(android.os.Build.VERSION.SDK_INT<26 || android.animation.ValueAnimator.areAnimatorsEnabled()) {
                    button.animate().scaleX(1.06f).scaleY(1.06f).setDuration(110).withEndAction(()->button.animate().scaleX(1).scaleY(1).setDuration(110).start()).start();
                }
                refresh();
            });
        }
        form.findViewById(R.id.checkoutSplitToggle).setOnClickListener(v->{
            split=!split;
            if(split) { divided.clear(); divided.add(new CheckoutLine(0,decimal(total),decimal(total))); }
            rebuildLines(); refresh();
        });
        form.findViewById(R.id.checkoutAddPayment).setOnClickListener(v->{
            Set<Integer> used=new HashSet<>(); long assigned=0;
            for(CheckoutLine line:divided) { used.add(line.method); try { assigned=Math.addExact(assigned,line.cents()); } catch(Exception ignored) { } }
            for(int method=0;method<4;method++) if(!used.contains(method)) {
                String remainder=decimal(Math.max(0,total-assigned)); divided.add(new CheckoutLine(method,remainder,remainder)); break;
            }
            rebuildLines(); refresh();
        });
        form.findViewById(R.id.checkoutReceipt).setOnClickListener(v->photo(-1,false));
        form.findViewById(R.id.checkoutCancel).setOnClickListener(v->dismiss());
        form.findViewById(R.id.checkoutContinue).setOnClickListener(v->advance());
        rebuildLines(); refresh();
    }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        getChildFragmentManager().setFragmentResultListener(ReceiptModalFragment.RESULT,getViewLifecycleOwner(),(key,result)->{
            int index=result.getInt("index");
            CheckoutLine line=index<0?single:index<divided.size()?divided.get(index):null;
            if(line!=null) line.receipt=result.getString("uri","");
            rebuildLines(); refresh();
            if(advanceAfterPhoto) { advanceAfterPhoto=false; view.post(this::advance); }
        });
    }
    private void rebuildLines() {
        LinearLayout container=form.findViewById(R.id.checkoutSplitFields); container.removeAllViews();
        for(int i=0;i<divided.size();i++) {
            final int index=i; CheckoutLine line=divided.get(i);
            View row=getLayoutInflater().inflate(R.layout.checkout_item_line,container,false);
            Spinner method=row.findViewById(R.id.paymentMethod);
            method.setSaveEnabled(false);
            FilterFieldAdapter.bind(method,Arrays.asList(methods),methods[line.method]);
            method.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> p,View v,int position,long id) {
                    if(line.method==position) return;
                    line.method=position; line.receipt=""; refreshRow(row,line); refresh();
                }
                public void onNothingSelected(AdapterView<?> p) { }
            });
            EditText amount=row.findViewById(R.id.paymentAmount),received=row.findViewById(R.id.paymentReceived);
            amount.setText(line.amount); received.setText(line.received);
            watch(amount,value->{
                line.amount=value;
                try { if(line.method==0 && CheckoutPayment.cents(line.received)<line.cents()) { line.received=decimal(line.cents()); received.setText(line.received); } } catch(Exception ignored) { }
                refreshRow(row,line); refresh();
            });
            watch(received,value->{ line.received=value; refreshRow(row,line); refresh(); });
            row.findViewById(R.id.paymentRemove).setVisibility(divided.size()>1?View.VISIBLE:View.GONE);
            row.findViewById(R.id.paymentRemove).setOnClickListener(v->{ divided.remove(index); rebuildLines(); refresh(); });
            row.findViewById(R.id.paymentReceipt).setOnClickListener(v->photo(index,false));
            refreshRow(row,line); container.addView(row);
        }
    }
    private void refreshRow(View row,CheckoutLine line) {
        row.findViewById(R.id.paymentCashFields).setVisibility(line.method==0?View.VISIBLE:View.GONE);
        TextView receipt=row.findViewById(R.id.paymentReceipt);
        receipt.setVisibility(line.method==0?View.GONE:View.VISIBLE);
        receipt.setText(line.receipt.isEmpty()?R.string.payment_attach:R.string.payment_attached);
        try { ((TextView)row.findViewById(R.id.paymentChange)).setText(getString(R.string.checkout_change,money(line.change()))); }
        catch(Exception error) { ((TextView)row.findViewById(R.id.paymentChange)).setText(R.string.checkout_invalid); }
    }
    public List<CheckoutLine> payments() { single.amount=decimal(total); return split?divided:Collections.singletonList(single); }
    public SalesViewModel model() { return model; }
    public String registerName() { return register.getSelectedItem()==null?"":register.getSelectedItem().toString(); }
    public String registerId() { return register.getSelectedItem() instanceof SalesSite?((SalesSite)register.getSelectedItem()).id:""; }
    public long total() { return total; }
    private void refresh() {
        form.findViewById(R.id.checkoutMethods).setVisibility(split?View.GONE:View.VISIBLE);
        form.findViewById(R.id.checkoutSplitFields).setVisibility(split?View.VISIBLE:View.GONE);
        form.findViewById(R.id.checkoutAddPayment).setVisibility(split?View.VISIBLE:View.GONE);
        form.findViewById(R.id.checkoutAddPayment).setEnabled(divided.size()<4);
        form.findViewById(R.id.checkoutAssigned).setVisibility(split?View.VISIBLE:View.GONE);
        ((TextView)form.findViewById(R.id.checkoutSplitToggle)).setText(split?R.string.checkout_single:R.string.checkout_split);
        for(int i=0;i<4;i++) form.findViewById(methodIds[i]).setSelected(i==single.method);
        form.findViewById(R.id.checkoutCashFields).setVisibility(!split && single.method==0?View.VISIBLE:View.GONE);
        TextView receipt=form.findViewById(R.id.checkoutReceipt);
        receipt.setVisibility(!split && single.method!=0?View.VISIBLE:View.GONE);
        receipt.setText(single.receipt.isEmpty()?R.string.payment_attach:R.string.payment_attached);
        try {
            long paid=CheckoutPayment.cents(single.received),difference=paid-total;
            TextView change=form.findViewById(R.id.checkoutChange);
            change.setText(difference==0?getString(R.string.checkout_exact):getString(difference>0?R.string.checkout_change:R.string.checkout_missing,money(Math.abs(difference))));
            change.setTextColor(difference<0?0xffEF4444:0xff22C55E);
        } catch(Exception error) { ((TextView)form.findViewById(R.id.checkoutChange)).setText(R.string.checkout_invalid); }
        try {
            long sum=0; for(CheckoutLine line:divided) sum=Math.addExact(sum,line.cents());
            ((TextView)form.findViewById(R.id.checkoutAssigned)).setText("Asignado: "+money(sum)+" de "+money(total)+"\n"+(sum==total?"✓ Cuadra exacto":sum<total?"Falta "+money(total-sum):"Sobra "+money(sum-total)));
        } catch(Exception error) { ((TextView)form.findViewById(R.id.checkoutAssigned)).setText(R.string.checkout_invalid); }
        String error=validation(false);
        TextView message=form.findViewById(R.id.checkoutError); message.setText(error); message.setVisibility(error.isEmpty()?View.GONE:View.VISIBLE);
        View next=form.findViewById(R.id.checkoutContinue); next.setEnabled(error.isEmpty()); next.setAlpha(error.isEmpty()?1f:.5f);
    }
    private String validation(boolean receipts) {
        try {
            if(registerId().isEmpty()) return "No tienes cajas de cobro (locales) asignadas. Asigna un local a tu cuenta para registrar ventas.";
            if(model.lines().isEmpty() || model.total()!=total) return "El carrito cambió. Cierra este formulario y vuelve a completar la venta.";
            CheckoutLine.validate(total,payments(),split,receipts); return "";
        } catch(IllegalArgumentException error) { return error.getMessage(); }
    }
    private void photo(int index,boolean advance) {
        if(getChildFragmentManager().findFragmentByTag("receipt")!=null) return;
        advanceAfterPhoto=advance;
        CheckoutLine line=index<0?single:divided.get(index);
        ReceiptModalFragment.create(index,line.receipt).show(getChildFragmentManager(),"receipt");
    }
    private void advance() {
        if(!validation(false).isEmpty()) { refresh(); return; }
        List<CheckoutLine> lines=payments();
        for(int i=0;i<lines.size();i++) if(lines.get(i).method!=0 && lines.get(i).receipt.isEmpty()) { photo(split?i:-1,true); return; }
        if(getChildFragmentManager().findFragmentByTag("checkout-confirmation")==null) {
            model.resetSubmission();
            new CheckoutConfirmationModal().show(getChildFragmentManager(),"checkout-confirmation");
        }
    }
    public void validateConfirmation() {
        String message=validation(true); if(!message.isEmpty()) throw new IllegalArgumentException(message);
    }
    private interface Changed { void changed(String value); }
    private void watch(EditText field,Changed changed) {
        field.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { changed.changed(s.toString()); }
            public void afterTextChanged(Editable s) { }
        });
    }
    static String decimal(long value) { return BigDecimal.valueOf(value,2).stripTrailingZeros().toPlainString(); }
    static String money(long value) { return String.format(Locale.US,"S/%.2f",BigDecimal.valueOf(value,2)); }
    private static Bundle encode(CheckoutLine line) { Bundle b=new Bundle(); b.putInt("method",line.method); b.putString("amount",line.amount); b.putString("received",line.received); b.putString("receipt",line.receipt); return b; }
    private static CheckoutLine decode(Bundle b) { CheckoutLine line=new CheckoutLine(b.getInt("method"),b.getString("amount","0"),b.getString("received","0")); line.receipt=b.getString("receipt",""); return line; }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("split",split); state.putBoolean("advancePhoto",advanceAfterPhoto);
        if(single!=null) state.putBundle("single",encode(single));
        ArrayList<Bundle> lines=new ArrayList<>(); for(CheckoutLine line:divided) lines.add(encode(line)); state.putParcelableArrayList("divided",lines);
        if(register!=null) state.putString("registerId",registerId());
    }
}
