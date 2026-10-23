package com.example.saasvilcas.feature.profile.presentation;

import androidx.lifecycle.*;
import com.example.saasvilcas.feature.profile.domain.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.shared.network.ApiException;
import java.util.*;
import java.util.concurrent.*;

/** Company-scoped data. Every request keeps the originating session to avoid cross-account updates. */
public final class ProfileViewModel extends ViewModel {
    public static final class Operation {
        public final String kind,error; public final boolean busy,success; public final int seconds;
        Operation(String kind,boolean busy,String error,boolean success,int seconds) { this.kind=kind; this.busy=busy; this.error=error; this.success=success; this.seconds=seconds; }
    }
    public final MutableLiveData<UserProfile> profile=new MutableLiveData<>();
    public final MutableLiveData<List<Announcement>> announcements=new MutableLiveData<>();
    public final MutableLiveData<String> loadError=new MutableLiveData<>("");
    public final MutableLiveData<String> announcementsError=new MutableLiveData<>("");
    public final MutableLiveData<Boolean> expired=new MutableLiveData<>(false);
    public final MutableLiveData<Operation> operation=new MutableLiveData<>(new Operation("",false,"",false,0));
    private final ProfileRepository repository; private final AuthRepository auth; private final SessionStore sessions;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean started,inFlight;
    private LoginChallenge challenge; private long accountDeadline;
    private AuthSession pendingActivation;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    public ProfileViewModel(ProfileRepository repository,AuthRepository auth,SessionStore sessions) { this.repository=repository; this.auth=auth; this.sessions=sessions; }
    public static ViewModelProvider.Factory factory(ProfileRepository repo,AuthRepository auth,SessionStore sessions) {
        return new ViewModelProvider.Factory() { @Override public <T extends ViewModel> T create(Class<T> type) { return type.cast(new ProfileViewModel(repo,auth,sessions)); } };
    }
    private boolean same(AuthSession origin) { AuthSession active=sessions.get(); return origin!=null && active!=null && origin.accessToken.equals(active.accessToken); }
    private void error(Exception failure,MutableLiveData<String> target,AuthSession origin) {
        main.post(()->{ if(!same(origin)) return; target.setValue(failure.getMessage()==null?"No se pudo completar la operación.":failure.getMessage());
            if(failure instanceof ApiException && ((ApiException)failure).status==401) expired.setValue(true); });
    }
    public void start() { if(started) return; started=true; reloadProfile(); reloadAnnouncements(); }
    public void reloadProfile() {
        AuthSession origin=sessions.get(); if(origin==null) return;
        loadError.setValue(""); worker.execute(()->{ try { UserProfile value=repository.profile(origin.accessToken);
            AuthSession updated=value.session(origin.accessToken,origin.refreshToken);
            main.post(()->{ if(same(origin)) { sessions.set(updated); profile.setValue(value); } });
        } catch(Exception e) { error(e,loadError,origin); } });
    }
    public void reloadAnnouncements() {
        AuthSession origin=sessions.get(); if(origin==null) return;
        announcementsError.setValue(""); worker.execute(()->{ try { List<Announcement> values=repository.announcements(origin.accessToken);
            main.post(()->{ if(same(origin)) announcements.setValue(values); });
        } catch(Exception e) { error(e,announcementsError,origin); } });
    }
    private interface Work { int run(AuthSession origin) throws Exception; }
    private void perform(String kind,Work task) {
        if(inFlight) return; AuthSession origin=sessions.get(); if(origin==null) return;
        inFlight=true; if(kind.equals("activate")) pendingActivation=null;
        operation.setValue(new Operation(kind,true,"",false,0));
        worker.execute(()->{
            try { int seconds=task.run(origin); main.post(()-> {
                inFlight=false; boolean valid=same(origin);
                if(kind.equals("activate")) {
                    AuthSession fresh=pendingActivation; pendingActivation=null;
                    if(!valid) {
                        if(fresh!=null) java.util.concurrent.ForkJoinPool.commonPool().execute(()->{try {auth.logout(fresh.refreshToken);}catch(Exception ignored){}});
                        return;
                    }
                    if(fresh!=null) sessions.set(fresh);
                }
                if(valid) operation.setValue(new Operation(kind,false,"",true,seconds));
            }); }
            catch(Exception e) {
                boolean sessionInvalid=false;
                if(e instanceof ApiException && ((ApiException)e).status==401 && !kind.equals("activate")) {
                    // Password mistakes and exhausted OTP challenges also return 401.
                    try { repository.profile(origin.accessToken); }
                    catch(ApiException check) { sessionInvalid=check.status==401; }
                }
                final boolean logout=sessionInvalid;
                main.post(()->{ inFlight=false; if(!same(origin)) return;
                operation.setValue(new Operation(kind,false,e.getMessage()==null?"No se pudo completar la operación.":e.getMessage(),false,0));
                if(logout) expired.setValue(true);
            }); }
        });
    }
    public void resetOperation() { if(!inFlight) operation.setValue(new Operation("",false,"",false,0)); }
    public void avatar(String uri) { perform("avatar",origin->{ UserProfile value=repository.avatar(origin.accessToken,uri);
        AuthSession updated=value.session(origin.accessToken,origin.refreshToken);
        main.post(()->{ if(same(origin)) { sessions.set(updated); profile.setValue(value); } }); return 0; }); }
    public void rate(int stars,String comment) { if(stars<1||stars>5||comment.length()>500) return; perform("rate",origin->{repository.rate(origin.accessToken,stars,comment); return 0;}); }
    public void requestPassword(String current,String next) { perform("password-request",origin->repository.requestPassword(origin.accessToken,current,next)); }
    public void confirmPassword(String code) { if(!code.matches("[0-9]{6}")) return; perform("password-confirm",origin->{repository.confirmPassword(origin.accessToken,code);return 0;}); }
    public void vote(String id,String option) { perform("vote",origin->{repository.vote(origin.accessToken,id,option);return 0;}); }
    public void markSeen(String id) { markSeen(java.util.Collections.singletonList(id)); }
    public void markSeen(List<String> ids) {
        if(ids.isEmpty()) return; AuthSession origin=sessions.get(); if(origin==null) return;
        worker.execute(()->{ try {
            for(String id:ids) repository.seen(origin.accessToken,id);
            List<Announcement> values=repository.announcements(origin.accessToken);
            main.post(()->{if(same(origin)) announcements.setValue(values);});
        } catch(Exception e) {error(e,announcementsError,origin);} });
    }
    public List<AuthSession> accounts() { return sessions.accounts(); }
    public void activate(AuthSession account) { perform("activate",origin->{ pendingActivation=repository.refresh(account); return 0; }); }
    public void forget(String id) { AuthSession account=sessions.forget(id); if(account!=null) worker.execute(()->{try {auth.logout(account.refreshToken);} catch(Exception ignored) { }}); }
    public void loginAccount(String email,String password) { perform("account-login",origin->{ challenge=auth.login(email,password); accountDeadline=android.os.SystemClock.elapsedRealtime()+challenge.expiresInSeconds*1000L; return challenge.expiresInSeconds; }); }
    public void verifyAccount(String code) { if(challenge==null||accountDeadline<android.os.SystemClock.elapsedRealtime()) return;
        perform("activate",origin->{pendingActivation=auth.verify(challenge.token,code); challenge=null;return 0;}); }
    @Override protected void onCleared() { worker.shutdownNow(); }
}
