package com.example.saasvilcas.feature.dashboard;

import android.view.View;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.click;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.dashboard.domain.*;
import com.example.saasvilcas.feature.dashboard.presentation.*;
import com.example.saasvilcas.shared.ui.FilterFieldAdapter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DashboardApiInstrumentedTest {
    private static DashboardPanel fixture(DashboardSection section,DashboardFilter filter) {
        switch(section) {
            case SUMMARY: return DashboardPanel.metrics(new DashboardPanel.Metrics(filter.period==0?33:77,400,100,-12,4,true,false,true));
            case INVENTORY: return DashboardPanel.inventory(1505);
            case PRODUCTS: return DashboardPanel.products(Arrays.asList(new DashboardPanel.Product("a","Producto de prueba",30,""),new DashboardPanel.Product("b","Segundo producto",25,"")));
            case CATALOG: return DashboardPanel.products(Arrays.asList(new DashboardPanel.Product("a","Producto de prueba",20,""),
                    new DashboardPanel.Product("b","Segundo producto",5,""),new DashboardPanel.Product("c","Producto sin stock",0,"")));
            case NO_SALES: return DashboardPanel.products(Collections.singletonList(new DashboardPanel.Product("a","Producto de prueba",null,"")));
            case RETURNS: return DashboardPanel.products(Collections.singletonList(new DashboardPanel.Product("","Segundo producto",7,"")));
            case TREND: return DashboardPanel.trend(Arrays.asList(new DashboardPanel.Point(filter.period==0?"Hoy":"Lun",100,-10),new DashboardPanel.Point("Mar",200,33)));
            case PAYMENTS: return DashboardPanel.entries(Arrays.asList(new DashboardPanel.Entry("yape",200,20),new DashboardPanel.Entry("efectivo",200,15)));
            default: return DashboardPanel.entries(Arrays.asList(new DashboardPanel.Entry("Primera",40,1),new DashboardPanel.Entry("Segunda",10,1)));
        }
    }
    @Test public void slowSummaryAndPaymentFailureDoNotBlockOtherCardsAndRetryIsIndependent() throws Exception {
        CountDownLatch release=new CountDownLatch(1),started=new CountDownLatch(1); AtomicInteger payments=new AtomicInteger();
        AtomicReference<DashboardViewModel> ref=new AtomicReference<>(); ViewModelStore store=new ViewModelStore();
        DashboardRepository repository=(section,filter)->{
            if(section==DashboardSection.SUMMARY) { started.countDown(); try { release.await(10,TimeUnit.SECONDS); } catch(InterruptedException error) { throw new CancellationException(); } }
            if(section==DashboardSection.PAYMENTS&&payments.getAndIncrement()==0) throw new DashboardException("Sin conexión",0);
            return fixture(section,filter);
        };
        try {
            main(()-> { DashboardViewModel model=new DashboardViewModel(repository); store.put("test",model); ref.set(model); model.reload(); });
            assertTrue(started.await(5,TimeUnit.SECONDS));
            await(ref.get(),state->!state.panels.get(DashboardSection.PAYMENTS).error.isEmpty()&&state.panels.get(DashboardSection.PRODUCTS).data!=null);
            main(()-> { DashboardViewModel.State state=ref.get().state.getValue(); assertTrue(state.panels.get(DashboardSection.SUMMARY).loading);
                assertEquals(2,state.panels.get(DashboardSection.PRODUCTS).data.products.size()); ref.get().retry(DashboardSection.PAYMENTS); });
            await(ref.get(),state->state.panels.get(DashboardSection.PAYMENTS).data!=null);
            assertEquals(2,payments.get()); release.countDown();
            await(ref.get(),state->state.panels.get(DashboardSection.SUMMARY).data!=null);
        } finally { release.countDown(); main(store::clear); }
    }
    @Test public void lateOldPeriodResponseCannotOverwriteNewPeriod() throws Exception {
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),returned=new CountDownLatch(1);
        AtomicBoolean first=new AtomicBoolean(true); AtomicReference<DashboardViewModel> ref=new AtomicReference<>(); ViewModelStore store=new ViewModelStore();
        DashboardRepository repository=(section,filter)->{
            if(section==DashboardSection.SUMMARY&&first.getAndSet(false)) {
                started.countDown(); boolean waiting=true;
                while(waiting) { try { waiting=!release.await(5,TimeUnit.SECONDS); } catch(InterruptedException ignored) { } }
                returned.countDown();
            }
            return fixture(section,filter);
        };
        try {
            main(()-> { DashboardViewModel model=new DashboardViewModel(repository); store.put("test",model); ref.set(model); model.reload(); });
            assertTrue(started.await(5,TimeUnit.SECONDS));
            main(()-> { long today=ref.get().filter().toDay; ref.get().select(DashboardFilter.forPeriod(1,today,today,today)); });
            await(ref.get(),state->state.panels.get(DashboardSection.SUMMARY).data!=null&&state.panels.get(DashboardSection.SUMMARY).data.metrics.profit==77);
            release.countDown(); assertTrue(returned.await(5,TimeUnit.SECONDS)); idle();
            main(()->assertEquals(77,ref.get().state.getValue().panels.get(DashboardSection.SUMMARY).data.metrics.profit,.001));
        } finally { release.countDown(); main(store::clear); }
    }
    @Test public void unauthorizedSessionIsReportedInsteadOfShowingDemoValues() throws Exception {
        AtomicReference<DashboardViewModel> ref=new AtomicReference<>(); ViewModelStore store=new ViewModelStore();
        try {
            main(()-> { DashboardViewModel model=new DashboardViewModel((section,filter)->{ throw new DashboardException("Sesión expirada",401); });
                store.put("test",model); ref.set(model); model.reload(); });
            await(ref.get(),state->state.expired);
            main(()-> { assertTrue(ref.get().state.getValue().expired); assertNull(ref.get().state.getValue().panels.get(DashboardSection.SUMMARY).data); });
        } finally { main(store::clear); }
    }
    @Test public void dashboardBindsCardsFiltersAndStyledDropdownWithoutRemovingToolbarOrSidebar() throws Exception {
        AtomicReference<DashboardViewModel> ref=new AtomicReference<>(); AtomicBoolean failPayments=new AtomicBoolean(true);
        DashboardRepository repository=(section,filter)->{
            if(section==DashboardSection.PAYMENTS&&failPayments.get()) throw new DashboardException("Sin conexión",0);
            return fixture(section,filter);
        };
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                VilcasApplication app=(VilcasApplication)activity.getApplication();
                app.sessions.set(new AuthSession("test-access","test-refresh","user","company","Prueba Android","test@example.com","Tienda",Collections.singletonList("Dueño de empresa")));
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); CompanyFragment company=CompanyFragment.create(true);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.screenHost,company).setMaxLifecycle(company,Lifecycle.State.CREATED).commitNow();
                ref.set(new ViewModelProvider(company,DashboardViewModel.factory(repository)).get(DashboardViewModel.class));
                activity.getSupportFragmentManager().beginTransaction().setMaxLifecycle(company,Lifecycle.State.RESUMED).commitNow();
            });
            await(ref.get(),DashboardApiInstrumentedTest::finished); idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                assertEquals("S/33.00",((TextView)view.findViewById(R.id.dashboardProfit)).getText().toString());
                assertEquals(2,((GridLayout)view.findViewById(R.id.dashboardProducts)).getChildCount());
                assertTrue(activity.findViewById(R.id.companyMenu).isShown()); assertNotNull(activity.findViewById(R.id.companyDrawer));
                View payments=view.findViewById(R.id.dashboardPaymentsCard); assertEquals("Sin conexión",((TextView)payments.findViewById(R.id.dashboardPanelMessage)).getText().toString());
                failPayments.set(false); assertTrue(payments.findViewById(R.id.dashboardPanelRetry).performClick());
                Spinner spinner=view.findViewById(R.id.dashboardChannel);
                View option=((android.widget.ArrayAdapter<?>)spinner.getAdapter()).getDropDownView(1,null,new LinearLayout(activity));
                assertTrue(option.getPaddingStart()>0); assertTrue(option.getMinimumHeight()>=Math.round(48*activity.getResources().getDisplayMetrics().density));
                option.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1,spinner.getWidth()),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                assertTrue(option.getMeasuredHeight()>=option.getMinimumHeight());
                view.findViewById(R.id.dashboardPeriod2).performClick();
            });
            await(ref.get(),state->finished(state)&&state.panels.get(DashboardSection.SUMMARY).data.metrics.profit==77); idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView(); assertEquals("S/77.00",((TextView)view.findViewById(R.id.dashboardProfit)).getText().toString());
                assertTrue(view.findViewById(R.id.dashboardProfitChart).getContentDescription().toString().contains("Lun: Ventas S/100.00, Ganancia real S/-10.00"));
                assertEquals(2,((LinearLayout)view.findViewById(R.id.dashboardPayments)).getChildCount());
                view.findViewById(R.id.dashboardPeriod4).performClick(); assertEquals(View.VISIBLE,view.findViewById(R.id.dashboardDateFields).getVisibility());
            });
            await(ref.get(),DashboardApiInstrumentedTest::finished); idle();
            scenario.recreate(); await(ref.get(),DashboardApiInstrumentedTest::finished); idle();
            scenario.onActivity(activity->{ assertEquals(4,ref.get().filter().period); assertTrue(activity.findViewById(R.id.companyProfile).isShown()); });
            Calendar selectedDate=Calendar.getInstance(TimeZone.getTimeZone("UTC")); selectedDate.setTimeInMillis(ref.get().filter().toDay);
            final int selectedYear=selectedDate.get(Calendar.YEAR)-1;
            scenario.onActivity(activity->dashboard(activity).requireView().findViewById(R.id.dashboardDateFrom).performClick());
            Espresso.onView(isAssignableFrom(DatePicker.class)).perform(new ViewAction() {
                @Override public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(DatePicker.class); }
                @Override public String getDescription() { return "Choose custom dashboard start date"; }
                @Override public void perform(UiController controller,View view) {
                    ((DatePicker)view).updateDate(selectedYear,Calendar.JANUARY,15); controller.loopMainThreadUntilIdle();
                }
            });
            Espresso.onView(withId(android.R.id.button1)).perform(click());
            await(ref.get(),DashboardApiInstrumentedTest::finished); idle();
            scenario.onActivity(activity->{
                assertEquals(DashboardFilter.day(selectedYear,Calendar.JANUARY,15),ref.get().filter().fromDay);
                assertEquals(String.format(Locale.US,"15/01/%04d",selectedYear),((TextView)dashboard(activity).requireView().findViewById(R.id.dashboardDateFrom)).getText().toString());
            });
            final long preservedFrom=ref.get().filter().fromDay;
            scenario.onActivity(activity->dashboard(activity).requireView().findViewById(R.id.filterPanelToggle).performClick());
            idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                assertEquals(View.GONE,view.findViewById(R.id.filterPanelBody).getVisibility());
                assertEquals("Mostrar filtros",((TextView)view.findViewById(R.id.filterPanelLabel)).getText().toString());
                assertEquals(preservedFrom,ref.get().filter().fromDay);
            });
            screenshot("dashboard-filters-collapsed.png");
            scenario.recreate(); idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                assertEquals(View.GONE,view.findViewById(R.id.filterPanelBody).getVisibility());
                view.findViewById(R.id.filterPanelToggle).performClick();
            });
            idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                assertEquals(View.VISIBLE,view.findViewById(R.id.filterPanelBody).getVisibility());
                assertTrue(view.findViewById(R.id.filterPanelBody).getHeight()>0);
                assertEquals(View.VISIBLE,view.findViewById(R.id.dashboardDateFields).getVisibility());
                assertEquals(preservedFrom,ref.get().filter().fromDay);
                view.findViewById(R.id.filterPanelToggle).performClick();
                view.findViewById(R.id.filterPanelToggle).performClick();
            });
            idle();
            scenario.onActivity(activity->assertEquals(View.VISIBLE,dashboard(activity).requireView().findViewById(R.id.filterPanelBody).getVisibility()));
            screenshot("dashboard-api.png");
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                ((ScrollView)view).scrollTo(0,view.findViewById(R.id.dashboardProfitCard).getTop());
            }); idle(); screenshot("dashboard-api-profit.png");
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView();
                ((ScrollView)view).scrollTo(0,view.findViewById(R.id.dashboardMetricsCard).getTop());
            }); idle(); screenshot("dashboard-api-metrics.png");
            scenario.onActivity(activity->((ScrollView)dashboard(activity).requireView()).fullScroll(View.FOCUS_DOWN));
            idle(); screenshot("dashboard-api-alerts.png");
            scenario.onActivity(activity->{
                ScrollView view=(ScrollView)dashboard(activity).requireView(); view.scrollTo(0,0);
                Spinner channel=view.findViewById(R.id.dashboardChannel);
                channel.requestRectangleOnScreen(new android.graphics.Rect(0,0,channel.getWidth(),channel.getHeight()),true);
                channel.performClick();
            }); idle(); screenshot("dashboard-api-dropdown.png");
            InstrumentationRegistry.getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
            idle();
            scenario.onActivity(activity->((VilcasApplication)activity.getApplication()).sessions.clear());
        }
    }
    @Test public void emptyApiListsRenderEmptyStatesAndZeroMetricsWithoutInventedProducts() throws Exception {
        AtomicReference<DashboardViewModel> ref=new AtomicReference<>();
        DashboardRepository repository=(section,filter)->section==DashboardSection.SUMMARY?
            DashboardPanel.metrics(new DashboardPanel.Metrics(0,0,0,0,0,false,false,true)):
            section==DashboardSection.INVENTORY?DashboardPanel.inventory(0):section==DashboardSection.TREND?DashboardPanel.trend(Collections.emptyList()):
            section==DashboardSection.CATEGORIES||section==DashboardSection.EXPENSES||section==DashboardSection.PAYMENTS?
            DashboardPanel.entries(Collections.emptyList()):DashboardPanel.products(Collections.emptyList());
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                ((VilcasApplication)activity.getApplication()).sessions.set(new AuthSession("test-access","test-refresh","user","company","Prueba","test@example.com","Tienda",Collections.singletonList("Dueño de empresa")));
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); CompanyFragment company=CompanyFragment.create(true);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.screenHost,company).setMaxLifecycle(company,Lifecycle.State.CREATED).commitNow();
                ref.set(new ViewModelProvider(company,DashboardViewModel.factory(repository)).get(DashboardViewModel.class));
                activity.getSupportFragmentManager().beginTransaction().setMaxLifecycle(company,Lifecycle.State.RESUMED).commitNow();
            });
            await(ref.get(),DashboardApiInstrumentedTest::finished); idle();
            scenario.onActivity(activity->{
                View view=dashboard(activity).requireView(); assertEquals("S/0.00",((TextView)view.findViewById(R.id.dashboardProfit)).getText().toString());
                assertEquals(0,((GridLayout)view.findViewById(R.id.dashboardProducts)).getChildCount());
                assertEquals(View.GONE,view.findViewById(R.id.dashboardMoreProducts).getVisibility());
                TextView message=view.findViewById(R.id.dashboardProductsCard).findViewById(R.id.dashboardPanelMessage);
                assertEquals(activity.getString(R.string.dashboard_no_sales_period),message.getText().toString());
                ((VilcasApplication)activity.getApplication()).sessions.clear();
            });
        }
    }
    private static DashboardFragment dashboard(MainActivity activity) {
        CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
        for(Fragment fragment:company.getChildFragmentManager().getFragments()) if(fragment instanceof DashboardFragment) return (DashboardFragment)fragment;
        throw new AssertionError("Dashboard missing");
    }
    private static boolean finished(DashboardViewModel.State state) {
        for(DashboardViewModel.PanelState panel:state.panels.values()) if(panel.loading||(panel.data==null&&panel.error.isEmpty())) return false;
        return true;
    }
    private static void await(DashboardViewModel model,java.util.function.Predicate<DashboardViewModel.State> predicate) throws Exception {
        CountDownLatch latch=new CountDownLatch(1); AtomicReference<androidx.lifecycle.Observer<DashboardViewModel.State>> observer=new AtomicReference<>();
        observer.set(state->{ if(predicate.test(state)) latch.countDown(); });
        main(()->model.state.observeForever(observer.get()));
        try { assertTrue("Dashboard did not reach expected state",latch.await(15,TimeUnit.SECONDS)); }
        finally { main(()->model.state.removeObserver(observer.get())); }
    }
    private static void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static void screenshot(String name) throws java.io.IOException {
        android.graphics.Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        if(bitmap==null) return;
        java.io.File file=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),name);
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)) { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output); }
        bitmap.recycle();
    }
    private static void idle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); android.os.SystemClock.sleep(450); }
}


