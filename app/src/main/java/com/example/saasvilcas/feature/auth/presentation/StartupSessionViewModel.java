package com.example.saasvilcas.feature.auth.presentation;

import androidx.lifecycle.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.profile.domain.ProfileRepository;
import com.example.saasvilcas.shared.network.ApiException;
import java.util.concurrent.*;

/** One restore request survives rotation. Transient network errors keep saved accounts intact. */
public final class StartupSessionViewModel extends ViewModel {
    public enum Result { LOADING, READY, EXPIRED, RETRY }
    public final MutableLiveData<Result> result=new MutableLiveData<>();
    private final SessionStore sessions;
    private final ProfileRepository repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean running;
    public StartupSessionViewModel(SessionStore sessions,ProfileRepository repository) { this.sessions=sessions;this.repository=repository; }
    public void start() {
        if(running || result.getValue()==Result.READY || result.getValue()==Result.EXPIRED) return;
        AuthSession saved=sessions.get();
        if(saved==null) {result.setValue(Result.EXPIRED);return;}
        running=true; result.setValue(Result.LOADING);
        worker.execute(()->{
            Result outcome;
            try {
                AuthSession refreshed=repository.refresh(saved,rotated->{
                    synchronized(sessions) { if(sessions.get()!=null && sessions.get().userId.equals(saved.userId)) sessions.set(rotated); }
                });
                synchronized(sessions) {
                    if(sessions.get()!=null && sessions.get().userId.equals(saved.userId)) {sessions.set(refreshed);outcome=Result.READY;}
                    else outcome=Result.EXPIRED;
                }
            } catch(ApiException error) {outcome=error.status==401||error.status==403?Result.EXPIRED:Result.RETRY;}
            catch(RuntimeException error) {outcome=Result.RETRY;}
            Result completed=outcome;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{running=false;result.setValue(completed);});
        });
    }
    @Override protected void onCleared() {worker.shutdownNow();}
    public static ViewModelProvider.Factory factory(SessionStore sessions,ProfileRepository repository) {
        return new ViewModelProvider.Factory() {
            @Override public <T extends ViewModel> T create(Class<T> type) {return type.cast(new StartupSessionViewModel(sessions,repository));}
        };
    }
}
