package com.example.saasvilcas.feature.sales.data;

import android.content.Context;
import java.io.*;

/** Receipt photos of offline sales live in the no-backup folder until they are uploaded. */
public final class AndroidReceiptFiles implements OfflineSalesRepository.ReceiptFiles {
    private final File folder;
    public AndroidReceiptFiles(Context context) { folder=new File(context.getNoBackupFilesDir(),"comprobantes_pendientes"); }
    @Override public String save(String saleId,int index,byte[] jpeg) throws IOException {
        if(!folder.isDirectory() && !folder.mkdirs()) throw new IOException("No se pudo crear la carpeta de comprobantes.");
        File file=new File(folder,saleId.replaceAll("[^A-Za-z0-9-]","")+"_"+index+".jpg");
        try(FileOutputStream output=new FileOutputStream(file)) { output.write(jpeg); }
        return file.getName();
    }
    @Override public byte[] read(String path) throws IOException {
        File file=new File(folder,new File(path).getName());
        try(InputStream input=new FileInputStream(file); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int count;
            while((count=input.read(buffer))!=-1) output.write(buffer,0,count);
            return output.toByteArray();
        }
    }
    @Override public void delete(String path) { //noinspection ResultOfMethodCallIgnored
        new File(folder,new File(path).getName()).delete(); }
}
