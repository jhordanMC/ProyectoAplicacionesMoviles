package com.example.saasvilcas;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import com.example.saasvilcas.feature.auth.data.RemoteAuthRepository;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.sales.data.*;
import com.example.saasvilcas.shared.network.ApiException;
import com.example.saasvilcas.shared.network.ConnectivityMonitor;
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
        connectivity=new ConnectivityMonitor(this);
        offlineSales=new OfflineSalesRepository(remoteSales,new OfflineDatabase.Store(OfflineDatabase.open(this)),
                new AndroidReceiptFiles(this),uri->new AndroidReceiptSource(this).jpeg(uri),new SalesSession(),
                connectivity::isOnline,System::currentTimeMillis);
        offlineSales.setListener(salesQueue::postValue);
        sales=offlineSales;
        connectivity.online().observeForever(online->{ if(Boolean.TRUE.equals(online)) syncSales(); });
        syncSales();
    }
    public final SessionStore sessions=new SessionStore();
    public boolean restoreRequired;
    public final AuthRepository auth=new RemoteAuthRepository(new HttpsApiClient(BuildConfig.API_BASE_URL));
    private final RemoteSalesRepository remoteSales=new RemoteSalesRepository(new HttpsApiClient(BuildConfig.API_BASE_URL),
            ()->{ AuthSession session=sessions.get(); return session==null?null:session.accessToken; },
            uri->new AndroidReceiptSource(this).jpeg(uri));
    public ConnectivityMonitor connectivity;
    /** HU-18: sales queue and cached catalog for selling without connection. */
    public OfflineSalesRepository offlineSales;
    public com.example.saasvilcas.feature.sales.domain.SalesRepository sales;
    public final MutableLiveData<OfflineSalesRepository.Status> salesQueue=new MutableLiveData<>(OfflineSalesRepository.Status.EMPTY);
    private final ExecutorService logoutWorker=Executors.newSingleThreadExecutor();
    private final ExecutorService queueWorker=Executors.newSingleThreadExecutor();
    /** Sends pending sales now if there is network, otherwise as soon as it returns. */
    public void syncSales() {
        SalesSyncWorker.schedule(this);
        queueWorker.execute(offlineSales::publish);
    }
    public void retryPendingSale(String id) { queueWorker.execute(()->{ offlineSales.retry(id); SalesSyncWorker.schedule(this); }); }
    public void discardPendingSale(String id) { queueWorker.execute(()->offlineSales.discard(id)); }
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
        salesQueue.postValue(OfflineSalesRepository.Status.EMPTY);
    }

    /** The queue belongs to the signed-in user; tokens are renewed the same way as on startup. */
    private final class SalesSession implements OfflineSalesRepository.Session {
        @Override public String companyId() { AuthSession session=sessions.get(); return session==null?null:session.companyId; }
        @Override public String userId() { AuthSession session=sessions.get(); return session==null?null:session.userId; }
        @Override public boolean refresh() {
            AuthSession saved=sessions.get();
            if(saved==null) return false;
            try {
                AuthSession refreshed=newProfileRepository().refresh(saved,rotated->{
                    synchronized(sessions) { if(sessions.get()!=null && sessions.get().userId.equals(saved.userId)) sessions.set(rotated); }
                });
                synchronized(sessions) {
                    if(sessions.get()==null || !sessions.get().userId.equals(saved.userId)) return false;
                    sessions.set(refreshed); return true;
                }
            } catch(ApiException|RuntimeException error) { return false; }
        }
    }
}
