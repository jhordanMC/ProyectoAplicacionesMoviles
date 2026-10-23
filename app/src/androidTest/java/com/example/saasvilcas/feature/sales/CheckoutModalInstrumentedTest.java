package com.example.saasvilcas.feature.sales;

import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.fragment.app.Fragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.sales.presentation.SalesFragment;
import java.util.Collections;
import static org.junit.Assert.*;





@RunWith(AndroidJUnit4.class)
public class CheckoutModalInstrumentedTest {
    @Test public void everyCheckoutStepInflatesWithItsRealStyles() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            ContextThemeWrapper context=new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.style.Theme_SaaSVilcas);
            LinearLayout parent=new LinearLayout(context);
            int[] layouts={R.layout.sales_dialog_checkout,R.layout.checkout_item_line,R.layout.checkout_modal_receipt,
                R.layout.checkout_modal_confirmation,R.layout.checkout_confirmation_product,R.layout.checkout_confirmation_field};
            for(int layout:layouts) assertNotNull(LayoutInflater.from(context).inflate(layout,parent,false));
        });
    }
    @Test public void completeSaleOpensCheckoutAndCashConfirmation() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                VilcasApplication app=(VilcasApplication)activity.getApplication();
                // Test-only session; no login endpoint or email is invoked.
                app.sessions.set(new AuthSession("test-access","test-refresh","user","company","Prueba Android",
                        "test@example.com","Tienda",Collections.singletonList("Dueño de empresa")));
                com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); activity.showCompany(true); activity.getSupportFragmentManager().executePendingTransactions();
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                company.getChildFragmentManager().executePendingTransactions();
                com.example.saasvilcas.feature.sales.presentation.SalesViewModel model=new androidx.lifecycle.ViewModelProvider(company, com.example.saasvilcas.feature.sales.presentation.SalesViewModel.factory(new com.example.saasvilcas.feature.sales.domain.SalesRepository() {
                    public java.util.List<com.example.saasvilcas.feature.sales.domain.SalesProduct> getCatalog() { return new com.example.saasvilcas.feature.sales.data.DemoSalesRepository().getCatalog(); }
                    public com.example.saasvilcas.feature.sales.domain.SaleResult submit(com.example.saasvilcas.feature.sales.domain.SaleDraft draft) { return new com.example.saasvilcas.feature.sales.domain.SaleResult("sale-ui-test",3500); }
                })).get(com.example.saasvilcas.feature.sales.presentation.SalesViewModel.class);
                com.example.saasvilcas.feature.sales.domain.SalesProduct urban=model.product("urban");
                model.put(null,urban,"38",urban.sites.get(0),1);
                SalesFragment sales=new SalesFragment();
                company.getChildFragmentManager().beginTransaction().replace(R.id.companyContent,sales).commitNow();
                ((android.widget.GridLayout)sales.requireView().findViewById(R.id.salesProducts)).getChildAt(0).performClick();
                sales.getChildFragmentManager().executePendingTransactions();
                androidx.fragment.app.DialogFragment editor=(androidx.fragment.app.DialogFragment)sales.getChildFragmentManager().findFragmentByTag("cart-editor");
                assertNotNull(editor); assertTrue("Product selector must use original branded modal",editor.getShowsDialog());
                assertTrue(editor.requireDialog().isShowing());
                assertNotNull(editor.requireView().findViewById(R.id.modalBrandHeader));
                assertEquals(android.view.View.GONE,editor.requireView().findViewById(R.id.salesDialogSize).getVisibility());
                com.google.android.material.chip.ChipGroup chips=editor.requireView().findViewById(R.id.salesDialogSizeChips);
                assertTrue(chips.getChildCount()>0); chips.getChildAt(0).performClick();
                assertEquals(((android.widget.TextView)chips.getChildAt(0)).getText().toString(),((android.widget.Spinner)editor.requireView().findViewById(R.id.salesDialogSize)).getSelectedItem().toString());


            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(400);
            android.graphics.Bitmap editorCapture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"sales-editor-original.png"))) {
                editorCapture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
            } catch(java.io.IOException error) { throw new AssertionError(error); }
            editorCapture.recycle();
            scenario.onActivity(activity->{
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                ((androidx.fragment.app.DialogFragment)sales.getChildFragmentManager().findFragmentByTag("cart-editor")).dismissNow();
            });
            com.example.saasvilcas.FilterPanelAssertions.hideAndShow(scenario);
            scenario.onActivity(activity->tap(activity.getWindow().getDecorView(),activity.findViewById(R.id.salesCartToggle)));
            android.os.SystemClock.sleep(400); // Allow cart transition and layout to finish.
            scenario.onActivity(activity->{
                android.widget.ScrollView cart=activity.findViewById(R.id.salesCartBody);
                cart.scrollTo(0,cart.getChildAt(0).getHeight());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->tap(activity.getWindow().getDecorView(),activity.findViewById(R.id.salesCheckout)));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.os.SystemClock.sleep(400);
            android.graphics.Bitmap capture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(capture);
            java.io.File screenshot=new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    .getExternalFilesDir(null),"checkout-visible.png");
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(screenshot)) {
                capture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
            } catch(java.io.IOException error) { throw new AssertionError(error); }
            capture.recycle();
            scenario.onActivity(activity->{
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                sales.getChildFragmentManager().executePendingTransactions();
                Fragment checkout=sales.getChildFragmentManager().findFragmentByTag("sales-summary");
                assertNotNull("Touch must open checkout",checkout);
                assertTrue(((androidx.fragment.app.DialogFragment)checkout).requireDialog().isShowing());
                assertTrue("Checkout must have visible dimensions",checkout.requireView().getHeight()>0);
                assertEquals(1f,checkout.requireView().getAlpha(),.01f);
                android.widget.ScrollView scroll=(android.widget.ScrollView)checkout.requireView().findViewById(R.id.modalBody).getParent().getParent();
                scroll.scrollTo(0,scroll.getChildAt(0).getHeight());
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                androidx.fragment.app.DialogFragment checkout=(androidx.fragment.app.DialogFragment)sales.getChildFragmentManager().findFragmentByTag("sales-summary");
                tap(checkout.requireDialog().getWindow().getDecorView(),checkout.requireView().findViewById(R.id.checkoutContinue));
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                Fragment checkout=sales.getChildFragmentManager().findFragmentByTag("sales-summary");
                checkout.getChildFragmentManager().executePendingTransactions();
                androidx.fragment.app.DialogFragment confirmation=(androidx.fragment.app.DialogFragment)checkout.getChildFragmentManager().findFragmentByTag("checkout-confirmation");
                assertNotNull("Touch must open confirmation",confirmation);
                assertTrue(confirmation.requireDialog().isShowing());
                confirmation.requireView().findViewById(R.id.confirmationSubmit).performClick();
            });
            long end=android.os.SystemClock.uptimeMillis()+5000;
            boolean[] ready={false};
            while(!ready[0]&&android.os.SystemClock.uptimeMillis()<end) {
                scenario.onActivity(activity->{
                    CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                    Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                    Fragment checkout=sales.getChildFragmentManager().findFragmentByTag("sales-summary");
                    Fragment confirmation=checkout.getChildFragmentManager().findFragmentByTag("checkout-confirmation");
                    ready[0]=confirmation.requireView().findViewById(R.id.successTicket)!=null;
                });
                android.os.SystemClock.sleep(25);
            }
            assertTrue("Success must display original result actions",ready[0]);
            android.os.SystemClock.sleep(400);
            scenario.onActivity(activity->{
                CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                Fragment sales=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
                Fragment checkout=sales.getChildFragmentManager().findFragmentByTag("sales-summary");
                androidx.fragment.app.DialogFragment result=(androidx.fragment.app.DialogFragment)checkout.getChildFragmentManager().findFragmentByTag("checkout-confirmation");
                assertEquals("ID: sale-ui-test",((android.widget.TextView)result.requireView().findViewById(R.id.successId)).getText().toString());
                assertTrue(result.requireView().findViewById(R.id.successNormal).isShown());
                assertTrue("Success should shrink instead of retaining the review height",result.requireView().getHeight()<activity.getWindow().getDecorView().getHeight()*.8f);
            });
            android.graphics.Bitmap successCapture=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"sales-success-original.png"))) { successCapture.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output); } catch(java.io.IOException error) { throw new AssertionError(error); }
            successCapture.recycle();
            scenario.onActivity(activity->((VilcasApplication)activity.getApplication()).sessions.clear());
        }
    }
    /** Touch traverses the decor hierarchy, including overlays and scroll interception. */
    private static void tap(android.view.View decor,android.view.View target) {
        assertTrue(target.isShown());
        int[] position=new int[2]; target.getLocationOnScreen(position);
        int[] origin=new int[2]; decor.getLocationOnScreen(origin);
        float x=position[0]-origin[0]+target.getWidth()/2f;
        float y=position[1]-origin[1]+target.getHeight()/2f;
        long time=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,x,y,0);
        android.view.MotionEvent up=android.view.MotionEvent.obtain(time,time+50,android.view.MotionEvent.ACTION_UP,x,y,0);
        down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
        decor.dispatchTouchEvent(down); decor.dispatchTouchEvent(up);
        down.recycle(); up.recycle();
    }
}





