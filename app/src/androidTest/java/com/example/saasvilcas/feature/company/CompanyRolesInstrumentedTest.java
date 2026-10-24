package com.example.saasvilcas.feature.company;

import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.dashboard.presentation.DashboardFragment;
import com.example.saasvilcas.feature.inventory.presentation.InventoryFragment;
import com.example.saasvilcas.feature.sales.presentation.SalesFragment;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Each company role opens on its own section and only lists the sections it may use. */
@RunWith(AndroidJUnit4.class)
public class CompanyRolesInstrumentedTest {
    @Test public void inventoryRoleOnlyGetsInventory() {
        check(new String[]{"Inventario"},InventoryFragment.class,false,true,false);
    }
    @Test public void sellerOnlyGetsSales() {
        check(new String[]{"Vendedor"},SalesFragment.class,false,false,true);
    }
    @Test public void ownerGetsEverything() {
        check(new String[]{"Dueño de empresa"},DashboardFragment.class,true,true,true);
    }
    private void check(String[] roles,Class<? extends Fragment> home,boolean dashboard,boolean inventory,boolean sales) {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                VilcasApplication app=(VilcasApplication)activity.getApplication();
                app.sessions.set(new AuthSession("test-access","test-refresh","user","company","Prueba","test@example.com","Tienda",Arrays.asList(roles)));
                try {
                    com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity);
                    activity.showCompany(app.sessions.get().owner); activity.getSupportFragmentManager().executePendingTransactions();
                    CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                    company.getChildFragmentManager().executePendingTransactions();
                    assertTrue(home.isInstance(company.getChildFragmentManager().findFragmentById(R.id.companyContent)));
                    View view=company.requireView();
                    assertEquals(dashboard,view.findViewById(R.id.companyNavDashboard).getVisibility()==View.VISIBLE);
                    assertEquals(inventory,view.findViewById(R.id.companyNavInventory).getVisibility()==View.VISIBLE);
                    assertEquals(sales,view.findViewById(R.id.companyNavSales).getVisibility()==View.VISIBLE);
                    // A hidden section cannot be opened by tapping its (gone) entry either.
                    view.findViewById(R.id.companyNavDashboard).performClick();
                    company.getChildFragmentManager().executePendingTransactions();
                    assertEquals(dashboard,company.getChildFragmentManager().findFragmentById(R.id.companyContent) instanceof DashboardFragment);
                } finally { app.sessions.clear(); }
            });
        }
    }
}
