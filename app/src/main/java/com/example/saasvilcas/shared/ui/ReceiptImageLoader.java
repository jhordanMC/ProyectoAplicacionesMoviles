package com.example.saasvilcas.shared.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;
import android.content.Context;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Bounds decode + sampled preview off the UI thread, guarded against recycled views. */
public final class ReceiptImageLoader {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private ReceiptImageLoader() { }
    public static void load(ImageView view,String value) {
        view.setTag(value); view.setImageDrawable(null);
        if(value.isEmpty()) return;
        Context app=view.getContext().getApplicationContext();
        WORKER.execute(()-> {
            Bitmap bitmap=null;
            try {
                Uri uri=Uri.parse(value); BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
                try(InputStream input=app.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input,null,options); }
                if(options.outWidth>0 && options.outHeight>0) {
                    options.inSampleSize=1;
                    while(options.outWidth/options.inSampleSize>1024 || options.outHeight/options.inSampleSize>1024) options.inSampleSize*=2;
                    options.inJustDecodeBounds=false;
                    try(InputStream input=app.getContentResolver().openInputStream(uri)) { bitmap=BitmapFactory.decodeStream(input,null,options); }
                }
            } catch(Exception ignored) { }
            Bitmap result=bitmap;
            view.post(()-> { if(value.equals(view.getTag())) view.setImageBitmap(result); });
        });
    }
}
