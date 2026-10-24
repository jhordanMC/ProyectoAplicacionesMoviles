package com.example.saasvilcas;

import android.app.Application;
import com.example.saasvilcas.feature.auth.data.RemoteAuthRepository;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.shared.network.HttpsApiClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Composition root. Session snapshots are encrypted and excluded from device backups. */
public final class VilcasApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        sessions.attach(new com.example.saasvilcas.feature.auth.data.EncryptedSessionPersistence(this));
        restoreRequired=sessions.get()!=null;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(getSharedPreferences("appearance",0).getInt("night_mode",androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        com.example.saasvilcas.shared.ui.UiSounds.init(this);
    }
    public final SessionStore sessions=new SessionStore();
    public boolean restoreRequired;
    public final AuthRepository auth=new RemoteAuthRepository(new HttpsApiClient(BuildConfig.API_BASE_URL));
    public final com.example.saasvilcas.feature.sales.domain.SalesRepository sales=
            new com.example.saasvilcas.feature.sales.data.RemoteSalesRepository(new HttpsApiClient(BuildConfig.API_BASE_URL),
                    ()->{ AuthSession session=sessions.get(); return session==null?null:session.accessToken; },
                    uri->new com.example.saasvilcas.feature.sales.data.AndroidReceiptSource(this).jpeg(uri));
    private final ExecutorService logoutWorker=Executors.newSingleThreadExecutor();
    public com.example.saasvilcas.feature.profile.domain.ProfileRepository newProfileRepository() {
        return new com.example.saasvilcas.feature.profile.data.RemoteProfileRepository(new HttpsApiClient(BuildConfig.API_BASE_URL),
            uri->new com.example.saasvilcas.shared.media.AndroidImageSource(this).jpeg(uri));
    }
    public com.example.saasvilcas.feature.dashboard.domain.DashboardRepository newDashboardRepository() {
        return new com.example.saasvilcas.feature.dashboard.data.RemoteDashboardRepository(new HttpsApiClient(BuildConfig.API_BASE_URL),
            ()->{ AuthSession session=sessions.get(); return session==null?null:session.accessToken; });
    }
    public com.example.saasvilcas.feature.inventory.domain.InventoryRepository newInventoryRepository() {
        return new com.example.saasvilcas.feature.inventory.data.RemoteInventoryRepository(new HttpsApiClient(BuildConfig.API_BASE_URL),
            ()->{ AuthSession session=sessions.get(); return session==null?null:session.accessToken; },
            uri->new com.example.saasvilcas.shared.media.AndroidImageSource(this).jpeg(uri));
    }
    public void clearSession() {
        AuthSession old=sessions.clear();
        if(old!=null) logoutWorker.execute(()-> { try { auth.logout(old.refreshToken); } catch(AuthException ignored) { } });
    }
}
