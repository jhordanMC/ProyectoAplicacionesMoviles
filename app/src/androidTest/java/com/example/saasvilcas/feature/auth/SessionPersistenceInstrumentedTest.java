package com.example.saasvilcas.feature.auth;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.ViewModelStore;
import com.example.saasvilcas.feature.auth.data.EncryptedSessionPersistence;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.auth.presentation.StartupSessionViewModel;
import com.example.saasvilcas.feature.profile.ProfileFixtures;
import com.example.saasvilcas.shared.network.ApiException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SessionPersistenceInstrumentedTest {
    private static AuthSession account(String id,String suffix) {
        return new AuthSession("access-secret-"+suffix,"refresh-secret-"+suffix,id,"company","Prueba","test@example.com","Tienda",Collections.singletonList("Vendedor"));
    }
    @Test public void freshStoreRestoresAccountsAndLogoutDoesNotResurrectActiveAccount() throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="sessions-test-"+UUID.randomUUID();
        EncryptedSessionPersistence persistence=new EncryptedSessionPersistence(context,name);
        SessionStore original=new SessionStore(); original.attach(persistence);
        original.set(account("first","one")); original.set(account("second","two"));
        File file=new File(context.getNoBackupFilesDir(),name+".bin");
        byte[] ciphertext=java.nio.file.Files.readAllBytes(file.toPath());
        String bytes=new String(ciphertext,StandardCharsets.ISO_8859_1);
        assertFalse(bytes.contains("access-secret")); assertFalse(bytes.contains("refresh-secret")); assertFalse(bytes.contains("test@example.com"));
        SessionStore restarted=new SessionStore(); restarted.attach(new EncryptedSessionPersistence(context,name));
        assertEquals("second",restarted.get().userId); assertEquals(2,restarted.accounts().size());
        restarted.clear();
        SessionStore afterLogout=new SessionStore(); afterLogout.attach(persistence);
        assertNull(afterLogout.get()); assertEquals(1,afterLogout.accounts().size());
        afterLogout.forget("first");
        SessionStore afterForget=new SessionStore(); afterForget.attach(persistence);
        assertTrue(afterForget.accounts().isEmpty());
        persistence.write(new SessionStore.Snapshot("",Collections.emptyList())); file.delete();
    }
    @Test public void tamperedSnapshotCannotRestoreTokens() throws Exception {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="sessions-test-"+UUID.randomUUID();
        EncryptedSessionPersistence persistence=new EncryptedSessionPersistence(context,name);
        SessionStore store=new SessionStore();store.attach(persistence);store.set(account("user","one"));
        File file=new File(context.getNoBackupFilesDir(),name+".bin");
        try(RandomAccessFile data=new RandomAccessFile(file,"rw")) {data.seek(data.length()-1);int value=data.read();data.seek(data.length()-1);data.write(value^1);}
        SessionStore restarted=new SessionStore();restarted.attach(persistence);
        assertNull(restarted.get());assertTrue(restarted.accounts().isEmpty());assertFalse(file.exists());
    }
    @Test public void networkFailureRetainsRotatedTokensAndRetryRestoresSession() throws Exception {
        SessionStore store=new SessionStore();store.set(account("user","old")); AtomicInteger calls=new AtomicInteger();
        ProfileFixtures.Repo repo=new ProfileFixtures.Repo() {
            @Override public AuthSession refresh(AuthSession saved,java.util.function.Consumer<AuthSession> rotated) throws ApiException {
                if(calls.getAndIncrement()==0) {rotated.accept(account("user","new"));throw new ApiException("Sin conexión");}
                assertEquals("refresh-secret-new",saved.refreshToken);return saved;
            }
        };
        StartupSessionViewModel model=new StartupSessionViewModel(store,repo);ViewModelStore scope=new ViewModelStore();scope.put("restore",model);
        try {
            main(model::start);await(model,StartupSessionViewModel.Result.RETRY);
            assertEquals("refresh-secret-new",store.get().refreshToken);
            main(model::start);await(model,StartupSessionViewModel.Result.READY);assertEquals(2,calls.get());
        } finally {main(scope::clear);}
    }
    @Test public void revokedRefreshRequestsLoginInsteadOfOpeningCompany() throws Exception {
        SessionStore store=new SessionStore();store.set(account("user","old"));
        StartupSessionViewModel model=new StartupSessionViewModel(store,new ProfileFixtures.Repo() {
            @Override public AuthSession refresh(AuthSession saved,java.util.function.Consumer<AuthSession> rotated) throws ApiException {throw new ApiException("Revocado",401);}
        });
        ViewModelStore scope=new ViewModelStore();scope.put("restore",model);
        try {main(model::start);await(model,StartupSessionViewModel.Result.EXPIRED);}finally {main(scope::clear);}
    }
    private static void main(Runnable task) {InstrumentationRegistry.getInstrumentation().runOnMainSync(task);}
    private static void await(StartupSessionViewModel model,StartupSessionViewModel.Result result) throws Exception {
        long deadline=android.os.SystemClock.elapsedRealtime()+5000;
        while(android.os.SystemClock.elapsedRealtime()<deadline) {
            if(model.result.getValue()==result) return;
            android.os.SystemClock.sleep(25);
        }
        assertEquals(result,model.result.getValue());
    }
}
