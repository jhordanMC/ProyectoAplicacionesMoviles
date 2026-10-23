package com.example.saasvilcas.feature.auth.presentation;

import androidx.lifecycle.*;
import com.example.saasvilcas.feature.auth.domain.*;
import java.util.concurrent.*;

public final class RecoveryViewModel extends ViewModel {
    public static final class State {
        public final boolean busy,sent;
        public final String email,error;
        State(boolean busy,boolean sent,String email,String error) {this.busy=busy;this.sent=sent;this.email=email;this.error=error;}
    }
    public final MutableLiveData<State> state=new MutableLiveData<>(new State(false,false,"",""));
    private final AuthRepository repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean cleared;
    public RecoveryViewModel(AuthRepository repository) {this.repository=repository;}
    public void send(String email) {
        State previous=state.getValue();if(previous.busy) return;
        String address=email.trim();state.setValue(new State(true,previous.sent,address,""));
        worker.execute(()->{
            String error="";try {repository.recover(address);}catch(AuthException failure) {error=failure.getMessage();}
            String message=error;
            main.post(()->{if(!cleared) state.setValue(new State(false,previous.sent||message.isEmpty(),address,message));});
        });
    }
    @Override protected void onCleared() {cleared=true;worker.shutdownNow();}
    public static ViewModelProvider.Factory factory(AuthRepository repository) {
        return new ViewModelProvider.Factory() {public <T extends ViewModel> T create(Class<T> type){return type.cast(new RecoveryViewModel(repository));}};
    }
}
