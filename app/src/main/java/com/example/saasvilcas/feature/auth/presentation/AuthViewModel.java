package com.example.saasvilcas.feature.auth.presentation;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.lifecycle.*;
import androidx.annotation.NonNull;
import com.example.saasvilcas.feature.auth.domain.*;
import java.util.concurrent.*;

/** Owns requests and the pending challenge across rotation; UI never performs network I/O. */
public final class AuthViewModel extends ViewModel {
    public enum Stage { IDLE, LOGIN_LOADING, CHALLENGE, VERIFY_LOADING, AUTHENTICATED }
    public static final class State {
        public final Stage stage;
        public final String message;
        public final boolean restartRequired;
        State(Stage stage,String message,boolean restartRequired) {
            this.stage=stage; this.message=message; this.restartRequired=restartRequired;
        }
        public boolean busy() { return stage==Stage.LOGIN_LOADING || stage==Stage.VERIFY_LOADING; }
    }
    private final AuthRepository repository;
    private final SessionStore sessions;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(Stage.IDLE,"",false));
    private Future<?> request;
    private int generation;
    private LoginChallenge challenge;
    private long deadline;
    private String email="";
    public AuthViewModel(AuthRepository repository,SessionStore sessions) { this.repository=repository; this.sessions=sessions; }
    public LiveData<State> state() { return state; }
    public long deadline() { return deadline; }
    public String email() { return email; }
    public boolean hasChallenge() { return challenge!=null; }
    public boolean busy() { return state.getValue().busy(); }
    public void reset() {
        generation++; if(request!=null) request.cancel(true);
        challenge=null; deadline=0; email="";
        state.setValue(new State(Stage.IDLE,"",false));
    }
    public void login(String address,String password) {
        if(busy()) return;
        email=address;
        int attempt=++generation;
        state.setValue(new State(Stage.LOGIN_LOADING,"Validando tu cuenta…",false));
        request=worker.submit(()-> {
            try {
                LoginChallenge result=repository.login(address,password);
                main.post(()-> {
                    if(attempt!=generation) return;
                    challenge=result; deadline=SystemClock.elapsedRealtime()+result.expiresInSeconds*1000L;
                    state.setValue(new State(Stage.CHALLENGE,"",false));
                });
            } catch(AuthException error) {
                main.post(()-> { if(attempt==generation) state.setValue(new State(Stage.IDLE,error.getMessage(),false)); });
            }
        });
    }
    public void verify(String code) {
        if(busy()) return;
        if(challenge==null || SystemClock.elapsedRealtime()>=deadline) {
            state.setValue(new State(Stage.CHALLENGE,"El código expiró. Vuelve a iniciar sesión.",true)); return;
        }
        if(!code.matches("[0-9]{6}")) return;
        String token=challenge.token;
        int attempt=++generation;
        state.setValue(new State(Stage.VERIFY_LOADING,"Verificando tu identidad…",false));
        request=worker.submit(()-> {
            try {
                AuthSession result=repository.verify(token,code);
                main.post(()-> {
                    if(attempt!=generation) {
                        if(!worker.isShutdown()) worker.execute(()-> { try { repository.logout(result.refreshToken); } catch(AuthException ignored) { } });
                        return;
                    }
                    sessions.set(result); challenge=null;
                    state.setValue(new State(Stage.AUTHENTICATED,"",false));
                });
            } catch(AuthException error) {
                main.post(()-> { if(attempt==generation) state.setValue(new State(Stage.CHALLENGE,error.getMessage(),error.restartRequired)); });
            }
        });
    }
    @Override protected void onCleared() { generation++; if(request!=null) request.cancel(true); worker.shutdownNow(); }
    public static ViewModelProvider.Factory factory(AuthRepository repository,SessionStore sessions) {
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                if(!type.isAssignableFrom(AuthViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
                return type.cast(new AuthViewModel(repository,sessions));
            }
        };
    }
}
