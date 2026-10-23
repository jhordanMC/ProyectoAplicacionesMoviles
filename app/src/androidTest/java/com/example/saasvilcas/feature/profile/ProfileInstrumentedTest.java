package com.example.saasvilcas.feature.profile;

import android.view.*;
import android.widget.*;
import androidx.fragment.app.*;
import androidx.lifecycle.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.profile.presentation.*;
import com.example.saasvilcas.feature.dashboard.domain.*;
import com.example.saasvilcas.feature.dashboard.presentation.DashboardViewModel;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProfileInstrumentedTest {
    @Test public void topbarAndProfileUseRealFlowsWithFixtureRepository() throws Exception {
        VilcasApplication application=(VilcasApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        // A prior interrupted UI run must not restore fake credentials through live endpoints.
        if(application.sessions.get()!=null && application.sessions.get().accessToken.startsWith("fake")) {
            application.sessions.clear(); application.restoreRequired=false;
        }
        ProfileFixtures.Repo repo=new ProfileFixtures.Repo();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                ((VilcasApplication)activity.getApplication()).sessions.set(new AuthSession("fake-other","fake-other-refresh","other","company","Otra cuenta","other@example.com","Otra tienda",Collections.singletonList("Vendedor")));
                ((VilcasApplication)activity.getApplication()).sessions.set(new AuthSession("fake","fake-refresh","user","company","Prueba Android","test@example.com","Tienda",Collections.singletonList("Dueño de empresa")));
                ProfileFixtures.install(activity,repo);
                activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override public void onFragmentCreated(FragmentManager manager,Fragment fragment,android.os.Bundle state) {
                        if(fragment instanceof CompanyFragment) new ViewModelProvider(fragment,DashboardViewModel.factory((section,filter)->{
                            switch(section) {
                                case SUMMARY: return DashboardPanel.metrics(new DashboardPanel.Metrics(0,0,0,0,0,false,false,true));
                                case INVENTORY: return DashboardPanel.inventory(0);
                                case TREND: return DashboardPanel.trend(Collections.emptyList());
                                case CATEGORIES: case PAYMENTS: case EXPENSES: return DashboardPanel.entries(Collections.emptyList());
                                default: return DashboardPanel.products(Collections.emptyList());
                            }
                        })).get(DashboardViewModel.class);
                    }
                },false);
                activity.showCompany(true);activity.getSupportFragmentManager().executePendingTransactions();
            });
            await(scenario,a->a.findViewById(R.id.companyNotificationBadge).getVisibility()==View.VISIBLE);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.companyProfileButton)).perform(androidx.test.espresso.action.ViewActions.click());
            scenario.onActivity(a->{
                assertTrue(((DialogFragment)company(a).getChildFragmentManager().findFragmentByTag("profile")).requireDialog().isShowing());
                profile(a).findViewById(R.id.profileClose).performClick();
            });
            idle();
            scenario.onActivity(a->a.findViewById(R.id.companyAvatar).setVisibility(View.VISIBLE));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.companyProfileButton)).perform(androidx.test.espresso.action.ViewActions.click());
            idle();
            scenario.onActivity(a->assertTrue(((DialogFragment)company(a).getChildFragmentManager().findFragmentByTag("profile")).requireDialog().isShowing()));
            idle();capture("profile-menu.png");
            scenario.onActivity(a->{
                click(profile(a),a.getString(R.string.profile_switch));
                assertNotNull(text(profile(a),"Otra cuenta"));assertNotNull(text(profile(a),"Otra tienda"));assertNotNull(text(profile(a),"Activa"));
                assertNotNull(profile(a).findViewById(R.id.accountInitials));
            });
            idle();capture("profile-saved-accounts.png");
            scenario.onActivity(a->profile(a).findViewById(R.id.accountBack).performClick());
            scenario.onActivity(a->{click(profile(a),a.getString(R.string.profile_personal));assertNotNull(text(profile(a),"999123456"));assertNotNull(text(profile(a),"test@example.com"));});
            idle();capture("profile-info.png");
            scenario.onActivity(a->{click(profile(a),"← Volver");click(profile(a),a.getString(R.string.profile_rating)); ((RatingBar)profile(a).findViewWithTag("profile-rating")).setRating(5); click(profile(a),"Enviar");});
            await(scenario,a->text(profile(a),"¡Gracias por tu opinión!")!=null);assertEquals(1,repo.ratings);
            scenario.onActivity(a->{click(profile(a),"← Volver");click(profile(a),a.getString(R.string.profile_password));
                List<EditText> fields=new ArrayList<>();fields(profile(a),fields);assertEquals(3,fields.size());fields.get(0).setText("actual123");fields.get(1).setText("nueva1234");fields.get(2).setText("nueva1234");click(profile(a),"Enviar código de verificación");});
            await(scenario,a->findCode(profile(a))!=null);assertEquals(1,repo.passwordRequests);
            scenario.onActivity(a->{((EditText)findCode(profile(a)).getChildAt(0)).setText("001234");});
            await(scenario,a->text(profile(a),"Tu contraseña se actualizó correctamente.")!=null);assertEquals(1,repo.passwordConfirmations);
            scenario.onActivity(a->{((DialogFragment)company(a).getChildFragmentManager().findFragmentByTag("profile")).dismissNow();a.findViewById(R.id.companyNotifications).performClick();company(a).getChildFragmentManager().executePendingTransactions();});
            await(scenario,a->repo.seen>0&&a.findViewById(R.id.companyNotificationBadge).getVisibility()==View.GONE);
            scenario.onActivity(a->{click(announcements(a),"Encuesta de prueba");click(announcements(a),"Sí");click(announcements(a),"Confirmar voto");});
            await(scenario,a->text(announcements(a),"¡Gracias por responder la encuesta!")!=null);assertEquals(1,repo.votes);
            idle();capture("topbar-announcement.png");
            scenario.onActivity(a->{((DialogFragment)company(a).getChildFragmentManager().findFragmentByTag("announcements")).dismissNow();((VilcasApplication)a.getApplication()).sessions.clear();((VilcasApplication)a.getApplication()).sessions.forget("other");((VilcasApplication)a.getApplication()).sessions.forget("u");});
        }
    }
    interface Ready {boolean test(MainActivity activity);}
    private static void await(ActivityScenario<MainActivity> scenario,Ready ready) throws Exception {
        long deadline=android.os.SystemClock.uptimeMillis()+6000;
        while(android.os.SystemClock.uptimeMillis()<deadline) {boolean[] value={false};scenario.onActivity(a->value[0]=ready.test(a));if(value[0]) {idle();return;}Thread.sleep(25);}fail("Profile flow timed out");
    }
    static CompanyFragment company(MainActivity a) {return (CompanyFragment)a.getSupportFragmentManager().findFragmentById(R.id.screenHost);}
    static View profile(MainActivity a) {return company(a).getChildFragmentManager().findFragmentByTag("profile").requireView();}
    static View announcements(MainActivity a) {return company(a).getChildFragmentManager().findFragmentByTag("announcements").requireView();}
    static TextView text(View view,String value) {
        if(view instanceof TextView&&((TextView)view).getText().toString().equals(value)) return (TextView)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView result=text(((ViewGroup)view).getChildAt(i),value);if(result!=null)return result;}return null;
    }
    static void click(View view,String value) {TextView target=text(view,value);assertNotNull("Missing control: "+value,target);assertTrue("Disabled: "+value,target.isEnabled());View clickable=target;while(!clickable.hasOnClickListeners()&&clickable.getParent() instanceof View) clickable=(View)clickable.getParent();assertTrue(clickable.performClick());}
    static void fields(View root,List<EditText> values) {if(root instanceof EditText)values.add((EditText)root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)fields(((ViewGroup)root).getChildAt(i),values);}
    static com.example.saasvilcas.shared.ui.CodeInputView findCode(View root) {if(root instanceof com.example.saasvilcas.shared.ui.CodeInputView)return (com.example.saasvilcas.shared.ui.CodeInputView)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){com.example.saasvilcas.shared.ui.CodeInputView result=findCode(((ViewGroup)root).getChildAt(i));if(result!=null)return result;}return null;}
    static void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(350);}
    static void capture(String name)throws Exception {android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),name))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);}finally{image.recycle();}}
}



