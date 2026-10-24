package com.example.saasvilcas.feature.auth.data;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import com.example.saasvilcas.feature.auth.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

/** Device-bound AES-GCM snapshot, excluded from cloud backups and device transfers. */
public final class EncryptedSessionPersistence implements SessionStore.Persistence {
    private final AtomicFile file;
    private final String alias;
    public EncryptedSessionPersistence(Context context) { this(context,"vilcas_sessions"); }
    public EncryptedSessionPersistence(Context context,String name) {
        file=new AtomicFile(new File(context.getNoBackupFilesDir(),name+".bin"));
        alias=context.getPackageName()+"."+name;
    }
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if(store.containsAlias(alias)) return (javax.crypto.SecretKey)store.getKey(alias,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    @Override public synchronized SessionStore.Snapshot read() {
        if(!file.getBaseFile().exists()) return new SessionStore.Snapshot("",Collections.emptyList());
        try {
            byte[] data=file.readFully();
            if(data.length<29 || data[0]!=1) throw new IOException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(data,1,13)));
            JSONObject root=new JSONObject(new String(cipher.doFinal(Arrays.copyOfRange(data,13,data.length)),StandardCharsets.UTF_8));
            JSONArray rows=root.getJSONArray("accounts"); List<AuthSession> accounts=new ArrayList<>();
            for(int i=0;i<rows.length();i++) {
                JSONObject row=rows.getJSONObject(i);
                List<String> roles=new ArrayList<>(); JSONArray saved=row.optJSONArray("roles");
                if(saved!=null) for(int j=0;j<saved.length();j++) roles.add(saved.getString(j));
                // Snapshots written before multi-role support only stored the owner flag.
                else roles.add(row.getBoolean("owner")?AuthSession.OWNER_ROLE:AuthSession.SELLER_ROLE);
                accounts.add(new AuthSession(row.getString("access"),row.getString("refresh"),row.getString("id"),row.getString("company"),
                        row.getString("name"),row.getString("email"),row.getString("companyName"),roles));
            }
            return new SessionStore.Snapshot(root.optString("active",""),accounts);
        } catch(Exception invalid) {
            file.delete(); return new SessionStore.Snapshot("",Collections.emptyList());
        }
    }
    @Override public synchronized void write(SessionStore.Snapshot snapshot) {
        FileOutputStream output=null;
        try {
            JSONArray rows=new JSONArray();
            for(AuthSession session:snapshot.accounts) rows.put(new JSONObject().put("access",session.accessToken).put("refresh",session.refreshToken)
                    .put("id",session.userId).put("company",session.companyId).put("name",session.name).put("email",session.email)
                    .put("companyName",session.companyName).put("owner",session.owner).put("roles",new JSONArray(session.roles)));
            byte[] plain=new JSONObject().put("active",snapshot.activeId).put("accounts",rows).toString().getBytes(StandardCharsets.UTF_8);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key());
            byte[] encrypted=cipher.doFinal(plain);
            output=file.startWrite(); output.write(1); output.write(cipher.getIV()); output.write(encrypted); file.finishWrite(output);
        } catch(Exception failure) {
            if(output!=null) file.failWrite(output);
            // Do not resurrect an older login after a failed save or logout.
            file.delete();
        }
    }
}
