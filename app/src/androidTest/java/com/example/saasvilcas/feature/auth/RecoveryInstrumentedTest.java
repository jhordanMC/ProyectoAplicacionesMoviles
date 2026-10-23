package com.example.saasvilcas.feature.auth;

import android.view.View;
import android.widget.*;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.auth.presentation.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RecoveryInstrumentedTest {
    @Test public void failedRequestDoesNotClaimSuccessAndRetrySurvivesRotation() throws Exception {
        VilcasApplication app=(VilcasApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        AuthSession saved=app.sessions.get();
        boolean restoreRequired=app.restoreRequired;
        // Run this public login flow without activating or revoking any existing device account.
        app.sessions.set(null);app.restoreRequired=false;
        AtomicInteger requests=new AtomicInteger();
        AuthRepository fake=new AuthRepository() {
            public LoginChallenge login(String e,String p) {throw new UnsupportedOperationException();}
            public AuthSession verify(String t,String c) {throw new UnsupportedOperationException();}
            public void logout(String t) { }
            public void recover(String email) throws AuthException {
                assertEquals("test@example.com",email);
                if(requests.getAndIncrement()==0) throw new AuthException("Demasiados intentos. Intenta más tarde.");
            }
        };
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    public void onFragmentCreated(FragmentManager manager,Fragment fragment,android.os.Bundle state) {
                        if(fragment instanceof AuthFragment) new ViewModelProvider(fragment,RecoveryViewModel.factory(fake)).get(RecoveryViewModel.class);
                    }
                },false);
                activity.showRecovery("test@example.com");activity.getSupportFragmentManager().executePendingTransactions();
                assertNotNull("Recovery screen did not inflate",activity.findViewById(R.id.recoveryError));
                activity.findViewById(R.id.button).performClick();
            });
            settle();
            scenario.onActivity(activity->{
                assertNotNull("Current screen: "+activity.getSupportFragmentManager().findFragmentById(R.id.screenHost)+", requests="+requests.get(),activity.findViewById(R.id.recoveryError));
                assertEquals(View.VISIBLE,activity.findViewById(R.id.recoveryError).getVisibility());
                assertEquals(View.VISIBLE,activity.findViewById(R.id.recoveryEmailField).getVisibility());
                assertTrue(activity.findViewById(R.id.button).isEnabled());
            });
            scenario.recreate();settle();
            scenario.onActivity(activity->activity.findViewById(R.id.button).performClick());settle();
            scenario.onActivity(activity->{
                assertEquals("¡Revisa tu correo!",((TextView)activity.findViewById(R.id.recoveryHeading)).getText().toString());
                assertEquals(View.GONE,activity.findViewById(R.id.recoveryEmailField).getVisibility());
                assertEquals(View.GONE,activity.findViewById(R.id.recoveryError).getVisibility());
                assertEquals("Reenviar enlace",((TextView)activity.findViewById(R.id.button)).getText().toString());
            });
            assertEquals(2,requests.get());scenario.recreate();settle();
            scenario.onActivity(activity->{assertEquals(View.GONE,activity.findViewById(R.id.recoveryEmailField).getVisibility());activity.findViewById(R.id.button).performClick();});
            settle();assertEquals(3,requests.get());
        } finally {app.sessions.set(saved);app.restoreRequired=restoreRequired;}
    }
    private static void settle() {InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(450);}
}
