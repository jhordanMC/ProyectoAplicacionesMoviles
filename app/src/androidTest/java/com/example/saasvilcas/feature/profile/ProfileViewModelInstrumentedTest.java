package com.example.saasvilcas.feature.profile;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.ViewModelStore;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.profile.presentation.ProfileViewModel;
import com.example.saasvilcas.shared.network.ApiException;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProfileViewModelInstrumentedTest {
    private AuthSession session(String id){return new AuthSession("access-"+id,"refresh-"+id,id,"company","Cuenta "+id,id+"@example.com","Tienda",Collections.singletonList("Dueño de empresa"));}
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void await(ProfileViewModel model,String kind)throws Exception {
        long deadline=android.os.SystemClock.uptimeMillis()+5000;
        while(android.os.SystemClock.uptimeMillis()<deadline){boolean[] ready={false};main(()->{ProfileViewModel.Operation op=model.operation.getValue();ready[0]=op.kind.equals(kind)&&!op.busy;});if(ready[0])return;Thread.sleep(25);}fail("Operation timed out");
    }
    @Test public void wrongCurrentPasswordShowsErrorWithoutDiscardingValidSession()throws Exception {
        SessionStore sessions=new SessionStore();sessions.set(session("user"));
        ProfileFixtures.Repo repo=new ProfileFixtures.Repo(){@Override public int requestPassword(String token,String current,String next)throws ApiException{throw new ApiException("La contraseña actual es incorrecta",401);}};
        ProfileViewModel model=new ProfileViewModel(repo,null,sessions);ViewModelStore store=new ViewModelStore();
        try{main(()->{store.put("profile",model);model.requestPassword("wrong","new12345");});await(model,"password-request");
            main(()->{assertFalse(model.expired.getValue());assertNotNull(sessions.get());assertEquals("La contraseña actual es incorrecta",model.operation.getValue().error);});
        }finally{main(store::clear);}
    }
    @Test public void addingAccountUsesSeparateChallengeAndPreservesPreviousSession()throws Exception {
        SessionStore sessions=new SessionStore();AuthSession original=session("first"),second=session("second");sessions.set(original);
        AuthRepository auth=new AuthRepository(){public LoginChallenge login(String email,String password){return new LoginChallenge("separate-challenge",300);}public AuthSession verify(String token,String code){assertEquals("separate-challenge",token);assertEquals("001234",code);return second;}public void logout(String token){}};
        ProfileViewModel model=new ProfileViewModel(new ProfileFixtures.Repo(),auth,sessions);ViewModelStore store=new ViewModelStore();
        try{main(()->{store.put("profile",model);model.loginAccount("second@example.com","local-password");});await(model,"account-login");assertSame(original,sessions.get());
            main(()->model.verifyAccount("001234"));await(model,"activate");assertSame(second,sessions.get());assertEquals(2,sessions.accounts().size());
        }finally{main(store::clear);}
    }
}
