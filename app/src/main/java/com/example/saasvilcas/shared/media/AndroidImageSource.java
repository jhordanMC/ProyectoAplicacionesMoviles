package com.example.saasvilcas.shared.media;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.*;

/** Bounded decoding/compression. Called only by the repository's background worker. */
public final class AndroidImageSource implements ImageSource {
    private final Context context;
    public AndroidImageSource(Context context) { this.context=context.getApplicationContext(); }
    @Override public byte[] jpeg(String localUri) throws ImageException {
        Bitmap image=null,rotated=null;
        try {
            Uri uri=Uri.parse(localUri);
            if(!"content".equals(uri.getScheme())) throw new IOException("Invalid local URI");
            ByteArrayOutputStream raw=new ByteArrayOutputStream();
            try(InputStream input=context.getContentResolver().openInputStream(uri)) {
                if(input==null) throw new IOException("Missing image");
                byte[] buffer=new byte[8192]; int count;
                while((count=input.read(buffer))!=-1) {
                    if(raw.size()+count>16*1024*1024) throw new IOException("Image too large");
                    raw.write(buffer,0,count);
                }
            }
            byte[] bytes=raw.toByteArray();
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(options.outWidth<=0 || options.outHeight<=0) throw new IOException("Invalid image");
            options.inJustDecodeBounds=false; options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600) options.inSampleSize*=2;
            image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
            if(image==null) throw new IOException("Unsupported image");
            Matrix matrix=new Matrix();
            int orientation=new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.postScale(-1,1); break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.postScale(1,-1); break;
                case ExifInterface.ORIENTATION_TRANSPOSE: matrix.postScale(-1,1); matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_TRANSVERSE: matrix.postScale(-1,1); matrix.postRotate(270); break;
            }
            rotated=Bitmap.createBitmap(image,0,0,image.getWidth(),image.getHeight(),matrix,true);
            ByteArrayOutputStream jpeg=new ByteArrayOutputStream();
            if(!rotated.compress(Bitmap.CompressFormat.JPEG,85,jpeg)) throw new IOException("Compression failed");
            return jpeg.toByteArray();
        } catch(IOException | SecurityException | IllegalArgumentException error) {
            throw new ImageException("No se pudo leer la imagen. Selecciona otra foto de hasta 16 MB.");
        } finally {
            if(rotated!=null && rotated!=image) rotated.recycle();
            if(image!=null) image.recycle();
        }
    }
}

