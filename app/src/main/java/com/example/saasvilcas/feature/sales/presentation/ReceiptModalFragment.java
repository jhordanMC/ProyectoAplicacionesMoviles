package com.example.saasvilcas.feature.sales.presentation;

import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import com.example.saasvilcas.R;
import com.example.saasvilcas.shared.ui.ReceiptImageLoader;
import java.io.File;

public final class ReceiptModalFragment extends SalesModalFragment {
    public static final String RESULT="checkout-receipt";
    private String photo="",pending="";
    private View content;
    private final ActivityResultLauncher<String[]> picker=registerForActivityResult(new ActivityResultContracts.OpenDocument(),uri->{
        if(uri==null) return;
        String mime=requireContext().getContentResolver().getType(uri);
        if(mime==null || !mime.startsWith("image/")) { error("El archivo debe ser una imagen."); return; }
        try { requireContext().getContentResolver().takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch(SecurityException ignored) { }
        photo=uri.toString(); render();
    });
    private final ActivityResultLauncher<Uri> camera=registerForActivityResult(new ActivityResultContracts.TakePicture(),success->{
        if(success && !pending.isEmpty()) photo=pending;
        pending=""; render();
    });
    public static ReceiptModalFragment create(int index,String uri) {
        ReceiptModalFragment modal=new ReceiptModalFragment(); Bundle args=new Bundle();
        args.putInt("index",index); args.putString("uri",uri); modal.setArguments(args); return modal;
    }
    public static ReceiptModalFragment preview(String uri) {
        ReceiptModalFragment modal=create(-1,uri); modal.requireArguments().putBoolean("readOnly",true); return modal;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); photo=state==null?requireArguments().getString("uri",""):state.getString("photo","");
        pending=state==null?"":state.getString("pending","");
    }
    @Override protected boolean showHeaderBrand() { return false; }
    @Override protected int contentLayout() { return R.layout.checkout_modal_receipt; }
    @Override protected void bindContent(View view) {
        content=view;
        view.findViewById(R.id.receiptGallery).setOnClickListener(v->picker.launch(new String[]{"image/*"}));
        view.findViewById(R.id.receiptCamera).setOnClickListener(v->{
            try {
                File dir=new File(requireContext().getFilesDir(),"sales_receipts"); if(!dir.exists() && !dir.mkdirs()) throw new java.io.IOException();
                File file=File.createTempFile("receipt-",".jpg",dir);
                Uri uri=FileProvider.getUriForFile(requireContext(),requireContext().getPackageName()+".files",file);
                pending=uri.toString(); camera.launch(uri);
            } catch(Exception error) { error("No se pudo abrir la cámara. Puedes subir una imagen."); }
        });
        view.findViewById(R.id.receiptRemove).setOnClickListener(v->{ photo=""; render(); });
        view.findViewById(R.id.receiptCancel).setOnClickListener(v->dismiss());
        view.findViewById(R.id.receiptContinue).setOnClickListener(v->{
            if(photo.isEmpty()) return;
            Bundle result=new Bundle(); result.putInt("index",requireArguments().getInt("index")); result.putString("uri",photo);
            dismiss(); getParentFragmentManager().setFragmentResult(RESULT,result);
        });
        render();
    }
    private void render() {
        if(content==null) return;
        ReceiptImageLoader.load(content.findViewById(R.id.receiptImage),photo);
        content.findViewById(R.id.receiptPlaceholder).setVisibility(photo.isEmpty()?View.VISIBLE:View.GONE);
        content.findViewById(R.id.receiptRemove).setVisibility(photo.isEmpty()?View.GONE:View.VISIBLE);
        content.findViewById(R.id.receiptContinue).setEnabled(!photo.isEmpty());
        content.findViewById(R.id.receiptCamera).setVisibility(photo.isEmpty()?View.VISIBLE:View.GONE);
        content.findViewById(R.id.receiptGallery).setVisibility(photo.isEmpty()?View.VISIBLE:View.GONE);
        if(requireArguments().getBoolean("readOnly")) {
            content.findViewById(R.id.receiptCamera).setVisibility(View.GONE);
            content.findViewById(R.id.receiptGallery).setVisibility(View.GONE);
            content.findViewById(R.id.receiptRemove).setVisibility(View.GONE);
            content.findViewById(R.id.receiptCancel).setVisibility(View.GONE);
            ((TextView)content.findViewById(R.id.receiptContinue)).setText(R.string.payment_close);
            content.findViewById(R.id.receiptContinue).setOnClickListener(v->dismiss());
        }
        error("");
    }
    private void error(String message) { if(content!=null) ((TextView)content.findViewById(R.id.receiptError)).setText(message); }
    @Override public void onSaveInstanceState(@NonNull Bundle state) { super.onSaveInstanceState(state); state.putString("photo",photo); state.putString("pending",pending); }
    @Override public void onDestroyView() { content=null; super.onDestroyView(); }
}


