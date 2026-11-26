package com.example.saasvilcas.shared.ui;

import android.graphics.*;
import android.widget.ImageView;
import android.util.LruCache;
import com.example.saasvilcas.BuildConfig;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;
import java.util.concurrent.*;

/** Public product media from the API origin. Never forwards session tokens to image URLs. */
public final class ProductImageLoader {
    private static final ExecutorService worker=Executors.newFixedThreadPool(2);
    private static final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private static volatile File diskCache;
    private ProductImageLoader() { }
    /** Keeps downloaded product photos so the saved catalog still shows them without connection (HU-18). */
    public static void init(android.content.Context context) { diskCache=new File(context.getCacheDir(),"fotos_productos"); }
    private static File diskFile(String value) {
        File folder=diskCache;
        return folder==null?null:new File(folder,Integer.toHexString(value.hashCode())+"_"+value.length()+".img");
    }
    private static void show(ImageView image,String value,byte[] content) {
        BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(content,0,content.length,options); options.inJustDecodeBounds=false; options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>512) options.inSampleSize*=2;
        Bitmap bitmap=BitmapFactory.decodeByteArray(content,0,content.length,options);
        if(bitmap==null) return;
        cache.put(value,bitmap);
        image.post(()->{ if(value.equals(image.getTag()) && image.isAttachedToWindow()) image.setImageBitmap(bitmap); });
    }
    private static void store(String value,byte[] content) {
        File file=diskFile(value);
        if(file==null || content.length>2*1024*1024) return;
        File folder=file.getParentFile();
        if(folder==null || (!folder.isDirectory() && !folder.mkdirs())) return;
        try(FileOutputStream output=new FileOutputStream(file)) { output.write(content); }
        catch(IOException ignored) { }
    }
    private static void showStored(ImageView image,String value) {
        File file=diskFile(value);
        if(file==null || !file.isFile()) return;
        try(InputStream stream=new FileInputStream(file); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int count;
            while((count=stream.read(buffer))!=-1) bytes.write(buffer,0,count);
            show(image,value,bytes.toByteArray());
        } catch(IOException | RuntimeException ignored) { }
    }
    public static void load(ImageView image,String value) {
        image.setImageDrawable(null); image.setTag(value);
        if(value==null || value.isEmpty()) return;
        Bitmap cached=cache.get(value);
        if(cached!=null) { image.setImageBitmap(cached); return; }
        worker.execute(()->{
            HttpsURLConnection connection=null;
            try {
                URL base=new URL(BuildConfig.API_BASE_URL+"/"),url=new URL(base,value);
                if(!"https".equals(url.getProtocol()) || !base.getHost().equalsIgnoreCase(url.getHost()) || url.getPort()!=base.getPort()) return;
                connection=(HttpsURLConnection)url.openConnection(); connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                if(connection.getResponseCode()!=200) { showStored(image,value); return; }
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                try(InputStream stream=connection.getInputStream()) {
                    byte[] buffer=new byte[4096]; int count;
                    while((count=stream.read(buffer))!=-1) { if(bytes.size()+count>8*1024*1024) return; bytes.write(buffer,0,count); }
                }
                byte[] content=bytes.toByteArray();
                store(value,content);
                show(image,value,content);
            } catch(IOException | RuntimeException error) { showStored(image,value); }
            finally { if(connection!=null) connection.disconnect(); }
        });
    }
}
