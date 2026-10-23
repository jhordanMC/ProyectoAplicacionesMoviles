package com.example.saasvilcas.feature.profile;
import androidx.fragment.app.*;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.profile.domain.*;
import com.example.saasvilcas.feature.profile.presentation.ProfileViewModel;
import java.util.*;

/** Keeps company UI fixtures entirely independent of live topbar endpoints. */
public final class ProfileFixtures {
    public static void install(MainActivity activity) {install(activity,new Repo());}
    public static void install(MainActivity activity,Repo repo) {
        activity.getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override public void onFragmentCreated(FragmentManager manager,Fragment fragment,android.os.Bundle state) {
                if(fragment instanceof CompanyFragment) {
                    VilcasApplication app=(VilcasApplication)activity.getApplication();
                    new ViewModelProvider(fragment,ProfileViewModel.factory(repo,app.auth,app.sessions)).get(ProfileViewModel.class);
                }
            }
        },false);
    }
    public static class Repo implements ProfileRepository {
        public volatile int ratings,passwordRequests,passwordConfirmations,votes,seen;
        public boolean voted,viewed;
        public UserProfile profile(String token) { return new UserProfile("user","company","Prueba Android","test@example.com","Tienda","999123456","12345678","",Collections.singletonList("Dueño de empresa")); }
        public UserProfile avatar(String token,String uri) { return profile(token); }
        public List<Announcement> announcements(String token) { return Collections.singletonList(new Announcement("ad","Encuesta de prueba","Elige una opción","","",voted?"yes":"",viewed,false,Arrays.asList(new Announcement.Option("yes","Sí",voted?1:null),new Announcement.Option("no","No",voted?0:null)))); }
        public void seen(String token,String id) {seen++;viewed=true;}
        public void vote(String token,String id,String option) {votes++;voted=true;}
        public void rate(String token,int stars,String comment) {ratings++;}
        public int requestPassword(String token,String current,String next) throws com.example.saasvilcas.shared.network.ApiException {passwordRequests++;return 300;}
        public void confirmPassword(String token,String code) {passwordConfirmations++;}
        public AuthSession refresh(AuthSession session) {return session;}
    }
}

