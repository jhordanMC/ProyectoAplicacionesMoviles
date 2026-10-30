package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.ui.ReceiptImageLoader;

/** Final customer review, followed by the actual FastAPI sale result. */
public final class CheckoutConfirmationModal extends SalesModalFragment {
    private View content;
    private String receiptRows="";
    private boolean successRendered;
    private CheckoutModalFragment checkout;
    private SalesViewModel model;
    @Override protected int contentLayout() { return R.layout.checkout_modal_confirmation; }
    @Override protected void bindContent(View view) {
        content=view; successRendered=false; checkout=(CheckoutModalFragment)requireParentFragment(); model=checkout.model();
        LinearLayout products=view.findViewById(R.id.confirmationProducts),payments=view.findViewById(R.id.confirmationPayments);
        if(receiptRows.isEmpty()) for(CartLine item:model.lines()) receiptRows+="<tr><td>"+android.text.TextUtils.htmlEncode(item.product.name)+"</td><td>"+item.quantity+"</td><td>"+CheckoutModalFragment.money(item.subtotalCents())+"</td></tr>";
        for(CartLine line:model.lines()) {
            View row=getLayoutInflater().inflate(R.layout.checkout_confirmation_product,products,false);
            ((TextView)row.findViewById(R.id.confirmationProductName)).setText(line.product.name);
            ((TextView)row.findViewById(R.id.confirmationProductDetail)).setText(line.quantity+" unidad(es) · "+CheckoutModalFragment.money(line.subtotalCents()));
            if(line.product.realStock) com.example.saasvilcas.shared.ui.ProductImageLoader.load(row.findViewById(R.id.confirmationProductImage),line.product.imageUrl);
            products.addView(row);
        }
        String[] names=getResources().getStringArray(R.array.checkout_methods);
        for(CheckoutLine line:checkout.payments()) {
            field(payments,getString(R.string.checkout_method),names[line.method]+" · "+CheckoutModalFragment.money(line.cents()));
            if(line.method==0) {
                field(payments,getString(R.string.checkout_received),CheckoutModalFragment.money(CheckoutPayment.cents(line.received)));
                if(line.change()>0) field(payments,"Vuelto a entregar",CheckoutModalFragment.money(line.change()));
            } else {
                field(payments,"Comprobante","");
                ImageView image=new ImageView(requireContext());
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(44),dp(44)); params.bottomMargin=dp(10);
                image.setLayoutParams(params); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setContentDescription("Comprobante de "+names[line.method]); payments.addView(image);
                ReceiptImageLoader.load(image,line.receipt);
                image.setOnClickListener(v->{ if(getChildFragmentManager().findFragmentByTag("receipt-preview")==null) ReceiptModalFragment.preview(line.receipt).show(getChildFragmentManager(),"receipt-preview"); });
            }
        }
        field(payments,"Local (Caja)",checkout.registerName());
        ((TextView)view.findViewById(R.id.confirmationTotal)).setText(CheckoutModalFragment.money(checkout.total()));
        view.findViewById(R.id.confirmationBack).setOnClickListener(v->dismiss());
        view.findViewById(R.id.confirmationSubmit).setOnClickListener(v->{
            SalesViewModel.Submission state=model.submission().getValue();
            if(state.stage==SalesViewModel.Submission.Stage.SUCCEEDED || state.stage==SalesViewModel.Submission.Stage.UNCERTAIN) { dismiss(); checkout.dismiss(); return; }
            try {
                checkout.validateConfirmation();
                model.submit(new SaleDraft(checkout.registerId(),model.lines(),checkout.payments()));
            } catch(IllegalArgumentException error) { ((TextView)view.findViewById(R.id.confirmationError)).setText(error.getMessage()); }
        });
    }
    @Override public void onViewCreated(@NonNull View view,Bundle state) {
        super.onViewCreated(view,state);
        model.submission().observe(getViewLifecycleOwner(),this::render);
    }
    private void render(SalesViewModel.Submission state) {
        if(state.stage==SalesViewModel.Submission.Stage.SUCCEEDED) { showSuccess(state.sale); return; }
        boolean busy=state.busy(),success=state.stage==SalesViewModel.Submission.Stage.SUCCEEDED;
        boolean uncertain=state.stage==SalesViewModel.Submission.Stage.UNCERTAIN;
        setCancelable(!busy);
        requireView().findViewById(R.id.modalCloseIcon).setEnabled(!busy);
        content.findViewById(R.id.confirmationBack).setEnabled(!busy);
        content.findViewById(R.id.confirmationSubmit).setEnabled(!busy);
        content.findViewById(R.id.confirmationSubmit).setAlpha(busy?.5f:1f);
        ((TextView)content.findViewById(R.id.confirmationSubmit)).setText(busy?R.string.sales_sending:success||uncertain?R.string.payment_close:R.string.payment_confirm);
        TextView error=content.findViewById(R.id.confirmationError);
        error.setText(success?"":state.message);
        if(!busy && !success && !state.message.isEmpty()) com.example.saasvilcas.shared.ui.UiSounds.forEvent(state,com.example.saasvilcas.shared.ui.UiSounds::error);
        error.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),busy?R.color.vl_text_green:R.color.vl_negative));
        content.findViewById(R.id.confirmationQuestion).setVisibility(success||uncertain?View.GONE:View.VISIBLE);
        content.findViewById(R.id.confirmationBack).setVisibility(success||uncertain?View.GONE:View.VISIBLE);
        if(success) {
            ((TextView)content.findViewById(R.id.confirmationTitle)).setText(R.string.sales_registered);
            ((TextView)content.findViewById(R.id.confirmationCaption)).setText(getString(R.string.sales_registered_message,CheckoutModalFragment.money(state.sale.totalCents),state.sale.id));
            ((TextView)content.findViewById(R.id.confirmationTotal)).setText(CheckoutModalFragment.money(state.sale.totalCents));
            content.findViewById(R.id.confirmationProducts).setVisibility(View.GONE);
            content.findViewById(R.id.confirmationPayments).setVisibility(View.GONE);
        }
    }
    @Override public void onCreate(Bundle state) { super.onCreate(state); if(state!=null) receiptRows=state.getString("receiptRows",""); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putString("receiptRows",receiptRows); }
    private void showSuccess(SaleResult sale) {
        if(successRendered) return; successRendered=true; setCancelable(true);
        com.example.saasvilcas.shared.ui.UiSounds.forEvent(sale,com.example.saasvilcas.shared.ui.UiSounds::success);
        if(checkout.getDialog()!=null&&checkout.getDialog().getWindow()!=null) {
            checkout.getDialog().getWindow().getDecorView().setVisibility(View.INVISIBLE);
            checkout.getDialog().getWindow().setDimAmount(0f);
        }
        requireView().findViewById(R.id.modalCloseIcon).setEnabled(true);
        android.view.ViewGroup body=(android.view.ViewGroup)content.getParent(); body.removeView(content);
        content=getLayoutInflater().inflate(R.layout.checkout_modal_success,body,false); body.addView(content);
        String amount=CheckoutModalFragment.money(sale.totalCents);
        if(sale.pending) {
            ((TextView)content.findViewById(R.id.successTitle)).setText(R.string.sales_saved_offline);
            ((TextView)content.findViewById(R.id.successMessage)).setText(android.text.Html.fromHtml("Venta por <b>"+amount
                    +"</b> guardada en el celular. Se enviará automáticamente al recuperar internet.",android.text.Html.FROM_HTML_MODE_LEGACY));
        } else ((TextView)content.findViewById(R.id.successMessage)).setText(android.text.Html.fromHtml("Venta por <b>"+amount+"</b> registrada correctamente.",android.text.Html.FROM_HTML_MODE_LEGACY));
        ((TextView)content.findViewById(R.id.successId)).setText("ID: "+sale.id);
        content.findViewById(R.id.successClose).setOnClickListener(v->onCloseRequested());
        content.findViewById(R.id.successTicket).setOnClickListener(v->printSale(sale,true));
        content.findViewById(R.id.successNormal).setOnClickListener(v->printSale(sale,false));
        com.example.saasvilcas.shared.ui.ViewEntranceAnimator.play(content);
        content.post(()->{ if(isAdded()&&getView()!=null) refreshModalSize(); });
    }
    private void printSale(SaleResult sale,boolean ticket) {
        android.webkit.WebView page=new android.webkit.WebView(requireContext());
        String html="<html><head><meta charset='UTF-8'><style>body{font-family:sans-serif;color:#0b2e27;padding:12px}h2{text-align:center;color:#05833b}table{width:100%;border-collapse:collapse}td,th{text-align:left;padding:8px 2px;border-bottom:1px solid #ddd}p{overflow-wrap:anywhere}</style></head><body><h2>VILCAS</h2><h3>"+(sale.pending?"Venta pendiente de sincronizar":"Venta registrada")+"</h3><p>ID: "+android.text.TextUtils.htmlEncode(sale.id)+"</p><table><tr><th>Producto</th><th>Cant.</th><th>Subtotal</th></tr>"+receiptRows+"</table><h3>Total: "+CheckoutModalFragment.money(sale.totalCents)+"</h3></body></html>";
        page.setWebViewClient(new android.webkit.WebViewClient() {
            boolean opened;
            @Override public void onPageFinished(android.webkit.WebView view,String url) {
                if(opened||!isAdded()) return; opened=true;
                android.print.PrintManager manager=(android.print.PrintManager)requireContext().getSystemService(android.content.Context.PRINT_SERVICE);
                android.print.PrintAttributes.MediaSize paper=ticket?new android.print.PrintAttributes.MediaSize("VILCAS80","Ticket 80 mm",3150,Math.max(5000,2000+receiptRows.length()*3)):android.print.PrintAttributes.MediaSize.ISO_A4;
                manager.print("VILCAS - "+sale.id,view.createPrintDocumentAdapter("Venta "+sale.id),new android.print.PrintAttributes.Builder().setMediaSize(paper).setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).build());
            }
        });
        page.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);
    }
    @Override protected void onCloseRequested() {
        if(model!=null && model.submitting()) return;
        SalesViewModel.Submission state=model==null?null:model.submission().getValue();
        dismiss();
        if(state!=null && (state.stage==SalesViewModel.Submission.Stage.SUCCEEDED || state.stage==SalesViewModel.Submission.Stage.UNCERTAIN)) checkout.dismiss();
    }
    @Override public void onCancel(@NonNull android.content.DialogInterface dialog) {
        super.onCancel(dialog);
        SalesViewModel.Submission state=model.submission().getValue();
        if(state.stage==SalesViewModel.Submission.Stage.SUCCEEDED || state.stage==SalesViewModel.Submission.Stage.UNCERTAIN) checkout.dismiss();
    }
    private void field(LinearLayout target,String label,String value) {
        View field=getLayoutInflater().inflate(R.layout.checkout_confirmation_field,target,false);
        ((TextView)field.findViewById(R.id.confirmationFieldLabel)).setText(label);
        TextView text=field.findViewById(R.id.confirmationFieldValue); text.setText(value);
        if(value.isEmpty()) text.setVisibility(View.GONE); target.addView(field);
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
