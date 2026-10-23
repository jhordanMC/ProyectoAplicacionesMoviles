package com.example.saasvilcas.feature.company.presentation;

import android.os.Bundle;
import android.content.res.Configuration;
import android.view.View;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.saasvilcas.R;
import com.example.saasvilcas.app.FrontendNavigator;
import com.example.saasvilcas.feature.dashboard.presentation.DashboardFragment;
import com.example.saasvilcas.feature.sales.presentation.SalesFragment;
import com.example.saasvilcas.feature.inventory.presentation.InventoryFragment;
import com.example.saasvilcas.shared.ui.SidebarAnimator;
import com.example.saasvilcas.feature.profile.presentation.ProfileModalFragment;
import com.example.saasvilcas.feature.profile.presentation.LogoutConfirmationFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Company navigation only. Each feature owns its screen and presentation state. */
public class CompanyFragment extends Fragment {
    private static final int DASHBOARD=0, INVENTORY=1, SALES=2;
    private int selectedSection;
    private boolean owner;
    private SidebarAnimator sidebarAnimator;
    public CompanyFragment() { super(R.layout.fragment_company); }
    public static CompanyFragment create(boolean owner) {
        CompanyFragment fragment = new CompanyFragment();
        Bundle args = new Bundle(); args.putBoolean("owner",owner); fragment.setArguments(args);
        return fragment;
    }
    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        owner = requireArguments().getBoolean("owner");
        selectedSection = state == null ? (owner ? DASHBOARD : INVENTORY) : state.getInt("section");
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        com.example.saasvilcas.feature.auth.domain.AuthSession session=
                ((com.example.saasvilcas.VilcasApplication)requireActivity().getApplication()).sessions.get();
        if(session==null) {
            // Restoration may still be executing a fragment transaction.
            view.post(()-> { if(isAdded() && getView()==view) ((FrontendNavigator)requireActivity()).showLogin(); });
            return;
        }
        ((TextView)view.findViewById(R.id.companyProfile)).setText(session.initials());
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        requireActivity().findViewById(R.id.windowBackdrop).setBackgroundResource(R.color.company_canvas);
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(requireActivity().getWindow(),view);
        bars.setAppearanceLightStatusBars(!dark); bars.setAppearanceLightNavigationBars(!dark);
        View overlay = view.findViewById(R.id.companyDrawerOverlay);
        View drawer = view.findViewById(R.id.companyDrawer);
        View toolbar=view.findViewById(R.id.companyToolbar);
        int toolbarHeight=Math.round((getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?56:64)*getResources().getDisplayMetrics().density);
        int toolbarStart=toolbar.getPaddingStart(), toolbarEnd=toolbar.getPaddingEnd();
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view,(v,insets)-> {
            androidx.core.graphics.Insets safe=insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            int keyboard=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            toolbar.setPadding(toolbarStart+safe.left,safe.top,toolbarEnd+safe.right,0);
            toolbar.getLayoutParams().height=toolbarHeight+safe.top;
            toolbar.requestLayout();
            view.findViewById(R.id.companyPage).setPadding(0,0,0,0);
            view.findViewById(R.id.companyContent).setPadding(safe.left,0,safe.right,Math.max(safe.bottom,keyboard));
            drawer.setPadding(safe.left,safe.top,0,safe.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(view);
        sidebarAnimator = new SidebarAnimator(overlay, drawer,
                view.findViewById(R.id.companyDrawerScrim), view.findViewById(R.id.companyMenu),
                view.findViewById(R.id.companyCloseMenu));
        sidebarAnimator.setVisibilityListener(open->{
            bars.setAppearanceLightStatusBars(!open&&!dark);
            bars.setAppearanceLightNavigationBars(!open&&!dark);
        });
        view.findViewById(R.id.companyMenu).setOnClickListener(v -> sidebarAnimator.open());
        view.findViewById(R.id.companyCloseMenu).setOnClickListener(v -> sidebarAnimator.close());
        view.findViewById(R.id.companyDrawerScrim).setOnClickListener(v -> sidebarAnimator.close());
        view.post(() -> {
            if (getView()!=view) return;
            int width = Math.round(250 * getResources().getDisplayMetrics().density);
            drawer.getLayoutParams().width = Math.min(width+drawer.getPaddingLeft(), (int)(view.getWidth()*.85f));
            drawer.requestLayout();
        });
        view.findViewById(R.id.companyNavDashboard).setVisibility(owner ? View.VISIBLE : View.GONE);
        int[] ids = {R.id.companyNavDashboard,R.id.companyNavInventory,R.id.companyNavSales};
        for (int i=0;i<ids.length;i++) {
            final int section=i;
            view.findViewById(ids[i]).setOnClickListener(v -> { select(view,section); sidebarAnimator.close(); });
        }
        com.example.saasvilcas.VilcasApplication app=(com.example.saasvilcas.VilcasApplication)requireActivity().getApplication();
        com.example.saasvilcas.feature.profile.presentation.ProfileViewModel profile=new androidx.lifecycle.ViewModelProvider(this,
            com.example.saasvilcas.feature.profile.presentation.ProfileViewModel.factory(app.newProfileRepository(),app.auth,app.sessions))
            .get(com.example.saasvilcas.feature.profile.presentation.ProfileViewModel.class);
        profile.profile.observe(getViewLifecycleOwner(),value->{
            com.example.saasvilcas.feature.auth.domain.AuthSession current=app.sessions.get();
            if(current!=null) ((TextView)view.findViewById(R.id.companyProfile)).setText(current.initials());
            android.widget.ImageView avatar=view.findViewById(R.id.companyAvatar);
            avatar.setVisibility(value.avatar.isEmpty()?View.GONE:View.VISIBLE);
            if(!value.avatar.isEmpty()) com.example.saasvilcas.shared.ui.ProductImageLoader.load(avatar,value.avatar);
        });
        profile.announcements.observe(getViewLifecycleOwner(),values->{
            boolean unseen=false; for(com.example.saasvilcas.feature.profile.domain.Announcement value:values) unseen|=!value.seen;
            view.findViewById(R.id.companyNotificationBadge).setVisibility(unseen?View.VISIBLE:View.GONE);
            view.findViewById(R.id.companyNotifications).setContentDescription(unseen?"Anuncios nuevos":getString(R.string.company_notifications));
        });
        profile.expired.observe(getViewLifecycleOwner(),expired->{if(expired) ((FrontendNavigator)requireActivity()).showLogin();});
        profile.operation.observe(getViewLifecycleOwner(),operation->{
            if(operation.kind.equals("activate") && operation.success) {
                profile.resetOperation();
                ((FrontendNavigator)requireActivity()).showCompany(false);
            }
        });
        profile.start();
        view.findViewById(R.id.companyNotifications).setOnClickListener(v->{
            if(getChildFragmentManager().findFragmentByTag("announcements")==null)
                new com.example.saasvilcas.feature.profile.presentation.AnnouncementsModalFragment().showNow(getChildFragmentManager(),"announcements");
        });
        view.findViewById(R.id.companyTheme).setOnClickListener(v->{
            int mode=dark?AppCompatDelegate.MODE_NIGHT_NO:AppCompatDelegate.MODE_NIGHT_YES;
            requireContext().getSharedPreferences("appearance",0).edit().putInt("night_mode",mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });
        ((android.widget.ImageButton)view.findViewById(R.id.companyTheme)).setImageResource(dark?R.drawable.company_ic_sun:R.drawable.company_ic_moon);
        view.findViewById(R.id.companyTheme).setContentDescription(dark?"Activar tema claro":"Activar tema oscuro");
        view.findViewById(R.id.companyProfileButton).setOnClickListener(v -> showProfile());
        getChildFragmentManager().setFragmentResultListener(ProfileModalFragment.REQUEST_LOGOUT,
                getViewLifecycleOwner(), (key, result) -> {
                    if (getChildFragmentManager().findFragmentByTag("logout-confirmation") == null)
                        new LogoutConfirmationFragment().showNow(getChildFragmentManager(), "logout-confirmation");
                });
        getChildFragmentManager().setFragmentResultListener(LogoutConfirmationFragment.CONFIRMED,
                getViewLifecycleOwner(), (key, result) -> ((FrontendNavigator) requireActivity()).showLogin());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (overlay.getVisibility()==View.VISIBLE) sidebarAnimator.close();
                else if (owner && selectedSection!=DASHBOARD) select(view,DASHBOARD);
                else showProfile();
            }
        });
        updateSelection(view);
        if (getChildFragmentManager().findFragmentById(R.id.companyContent)==null) select(view,selectedSection);
    }
    private void showProfile() {
        if(!isAdded() || getChildFragmentManager().isStateSaved()) return;
        androidx.fragment.app.Fragment existing=getChildFragmentManager().findFragmentByTag("profile");
        if(existing instanceof androidx.fragment.app.DialogFragment) {
            android.app.Dialog dialog=((androidx.fragment.app.DialogFragment)existing).getDialog();
            if(dialog!=null && dialog.isShowing()) return;
            getChildFragmentManager().beginTransaction().remove(existing).commitNow();
        }
        ProfileModalFragment.create(owner).showNow(getChildFragmentManager(), "profile");
    }
    private void select(View view,int section) {
        if (!owner && section==DASHBOARD) return;
        selectedSection=section;
        updateSelection(view);
        Fragment screen=section==DASHBOARD ? new DashboardFragment()
            : section==SALES ? new SalesFragment() : new InventoryFragment();
        getChildFragmentManager().beginTransaction().setReorderingAllowed(true)
            .replace(R.id.companyContent,screen).commit();
    }
    private void updateSelection(View view) {
        int[] ids={R.id.companyNavDashboard,R.id.companyNavInventory,R.id.companyNavSales};
        int[] titles={R.string.company_dashboard,R.string.inventory,R.string.sales};
        for(int i=0;i<ids.length;i++) view.findViewById(ids[i]).setSelected(i==selectedSection);
        ((TextView)view.findViewById(R.id.companyTitle)).setText(titles[selectedSection]);
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); out.putInt("section",selectedSection);
    }
    @Override public void onDestroyView() {
        if (sidebarAnimator != null) {
            sidebarAnimator.dispose();
            sidebarAnimator = null;
        }
        super.onDestroyView();
    }
}

