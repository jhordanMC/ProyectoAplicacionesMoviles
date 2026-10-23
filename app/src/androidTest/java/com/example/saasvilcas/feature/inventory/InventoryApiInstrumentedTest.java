package com.example.saasvilcas.feature.inventory;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.lifecycle.*;
import androidx.fragment.app.Fragment;
import android.view.*;
import android.widget.*;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.feature.company.presentation.CompanyFragment;
import com.example.saasvilcas.feature.inventory.data.DemoInventoryRepository;
import com.example.saasvilcas.feature.inventory.domain.*;
import com.example.saasvilcas.feature.inventory.presentation.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InventoryApiInstrumentedTest {
    @Test public void successfulOperationRemainsAvailableAfterAutomaticReload() throws Exception {
        Repository repository=new Repository(); repository.fail=false;
        InventoryViewModel[] model=new InventoryViewModel[1]; CountDownLatch done=new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()-> {
            model[0]=new InventoryViewModel(repository);
            model[0].state.observeForever(result->{ if(!result.busy&&!result.loading&&model[0].outcome("save")!=null) done.countDown(); });
            model[0].save(new InventoryProduct("new","Nuevo","","","",100,50,"",Collections.emptyList()),"save");
        });
        assertTrue(done.await(5,TimeUnit.SECONDS)); idle();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()-> {
            assertTrue(model[0].outcome("save").success);
            new ViewModelStore() {{ put("inventory",model[0]); }}.clear();
        });
    }
    private static final class Repository implements InventoryRepository {
        final DemoInventoryRepository data=new DemoInventoryRepository();
        volatile boolean fail=true; int writes;
        public List<InventoryProduct> products() { return data.products(); }
        public void save(InventoryProduct p) { writes++; if(fail) throw new InventoryException("Stock cambió: actualiza el producto.",409,false); data.save(p); }
        public void delete(String id) { data.delete(id); }
        public List<String> references(int kind) { return kind==3?Collections.singletonList("Local de prueba"):data.references(kind); }
        public void saveReference(int kind,String previous,String name) { data.saveReference(kind,previous,name); }
        public void deleteReference(int kind,String name) { data.deleteReference(kind,name); }
        public String[] warehouseDetails(String name) { return data.warehouseDetails(name); }
        public void saveWarehouse(String previous,String name,String address,String description) { data.saveWarehouse(previous,name,address,description); }
    }
    @Test public void editorRetainsDraftOnApiFailureAndClosesOnlyAfterSuccess() throws Exception {
        Repository repository=new Repository(); InventoryViewModel[] models=new InventoryViewModel[1];
        CountDownLatch loaded=new CountDownLatch(1),failed=new CountDownLatch(1),saved=new CountDownLatch(1);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            try {
                scenario.onActivity(activity->{
                    ((VilcasApplication)activity.getApplication()).sessions.set(new AuthSession("test","test","user","company","Prueba","test@example.com","Tienda",Collections.singletonList("Dueño de empresa")));
                    com.example.saasvilcas.feature.profile.ProfileFixtures.install(activity); activity.showCompany(true); activity.getSupportFragmentManager().executePendingTransactions();
                    CompanyFragment company=(CompanyFragment)activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
                    company.getChildFragmentManager().executePendingTransactions();
                    InventoryViewModel model=new ViewModelProvider(company,new ViewModelProvider.Factory() {
                        public <T extends ViewModel> T create(Class<T> type) { return type.cast(new InventoryViewModel(repository)); }
                    }).get(InventoryViewModel.class); models[0]=model;
                    model.state.observeForever(result->{
                        if(!result.loading&&!result.busy&&!model.products().isEmpty()) loaded.countDown();
                        if(result.message.startsWith("Stock cambió")) failed.countDown();
                        if(result.success) saved.countDown();
                    });
                    company.getChildFragmentManager().beginTransaction().replace(R.id.companyContent,new InventoryFragment()).commitNow();
                });
                assertTrue(loaded.await(5,TimeUnit.SECONDS)); idle();
                com.example.saasvilcas.FilterPanelAssertions.hideAndShow(scenario);
                scenario.onActivity(activity->tap(activity.getWindow().getDecorView(),activity.findViewById(R.id.inventoryNew))); idle();
                android.graphics.Bitmap screenshot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull(screenshot);
                try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"inventory-original-modal.png"))) {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);
                }
                screenshot.recycle();
                scenario.onActivity(activity->{
                    Fragment editor=editor(activity); assertNotNull(editor);
                    assertTrue(((androidx.fragment.app.DialogFragment)editor).getShowsDialog());
                    assertTrue(activity.findViewById(R.id.companyContent).isShown());
                    EditText name=findName(editor.requireView()); assertNotNull(name); name.setText("Producto desde Android");
                    tapSave(editor);
                });
                assertTrue(failed.await(5,TimeUnit.SECONDS)); idle();
                scenario.onActivity(activity->{
                    Fragment editor=editor(activity); assertNotNull("Failed request must keep editor",editor);
                    assertEquals("Producto desde Android",findName(editor.requireView()).getText().toString());
                    assertTrue(((TextView)editor.requireView().findViewById(R.id.inventoryFormError)).getText().toString().contains("Stock cambió"));
                    repository.fail=false; tapSave(editor);
                });
                assertTrue(saved.await(5,TimeUnit.SECONDS)); idle();
                scenario.onActivity(activity->{ assertNull(editor(activity)); assertEquals(2,repository.writes); });
            } finally { scenario.onActivity(activity->((VilcasApplication)activity.getApplication()).sessions.clear()); }
        }
    }
    private static Fragment editor(MainActivity activity) {
        Fragment company=activity.getSupportFragmentManager().findFragmentById(R.id.screenHost);
        Fragment inventory=company.getChildFragmentManager().findFragmentById(R.id.companyContent);
        inventory.getChildFragmentManager().executePendingTransactions();
        return inventory.getChildFragmentManager().findFragmentByTag("product-editor");
    }
    private static EditText findName(View view) {
        if(view instanceof EditText && "Ej: Zapatos Ejecutivos Confort".contentEquals(((EditText)view).getHint())) return (EditText)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) { EditText found=findName(((ViewGroup)view).getChildAt(i)); if(found!=null) return found; }
        return null;
    }
    private static void idle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); android.os.SystemClock.sleep(450); }
    private static void tapSave(Fragment editor) {
        View save=editor.requireView().findViewById(R.id.inventoryFormSave);
        android.graphics.Rect bounds=new android.graphics.Rect();
        assertTrue(save.getGlobalVisibleRect(bounds));
        assertEquals(save.getHeight(), bounds.height());
        tap(((androidx.fragment.app.DialogFragment)editor).requireDialog().getWindow().getDecorView(),editor.requireView().findViewById(R.id.inventoryFormSave));
    }
    private static void tap(View decor,View target) {
        assertNotNull(target); assertTrue(target.isShown()); assertTrue(target.isEnabled());
        int[] position=new int[2],origin=new int[2]; target.getLocationOnScreen(position); decor.getLocationOnScreen(origin);
        float x=position[0]-origin[0]+target.getWidth()/2f,y=position[1]-origin[1]+target.getHeight()/2f;
        long now=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent down=android.view.MotionEvent.obtain(now,now,0,x,y,0),up=android.view.MotionEvent.obtain(now,now+50,1,x,y,0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN); up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        decor.dispatchTouchEvent(down); decor.dispatchTouchEvent(up); down.recycle(); up.recycle();
    }
}


