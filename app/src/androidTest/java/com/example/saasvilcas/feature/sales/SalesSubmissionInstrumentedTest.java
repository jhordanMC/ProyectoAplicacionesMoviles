package com.example.saasvilcas.feature.sales;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.feature.sales.presentation.SalesViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SalesSubmissionInstrumentedTest {
    @Test public void successClearsCartAndRepeatedClicksSendOnce() throws Exception { verify(null,true); }
    @Test public void stockConflictRetainsCart() throws Exception { verify(new SalesException("Stock insuficiente",409,false),false); }
    @Test public void unknownOutcomeRetainsCartAndBlocksRepeat() throws Exception { verify(new SalesException("Timeout",0,true),false); }

    private void verify(SalesException failure,boolean success) throws Exception {
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
        int[] calls={0}; SalesViewModel[] holder=new SalesViewModel[1];
        SalesVariant variant=new SalesVariant("variant","", "warehouse","Depósito",2);
        SalesProduct product=new SalesProduct("product","Producto",3400,2,"unisex","",Collections.singletonList(variant));
        SalesRepository repository=new SalesRepository() {
            @Override public java.util.List<SalesSite> loadSites() { return Collections.singletonList(new SalesSite("warehouse","Depósito","almacen")); }
            @Override public SalesPage loadProducts(SalesFilter filter,int offset) { return new SalesPage(Collections.singletonList(product),false,1); }
            @Override public SaleResult submit(SaleDraft draft) throws SalesException {
                calls[0]++; started.countDown();
                try { if(!release.await(5,TimeUnit.SECONDS)) throw new AssertionError("Release timeout"); }
                catch(InterruptedException error) { throw new AssertionError(error); }
                assertEquals("variant",draft.items.get(0).variantId);
                assertEquals("warehouse",draft.items.get(0).siteId);
                if(failure!=null) throw failure;
                return new SaleResult("sale",3400);
            }
        };
        CountDownLatch loaded=new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            SalesViewModel model=new SalesViewModel(repository); holder[0]=model;
            model.state().observeForever(state->{ if(!state.loading && !model.products().isEmpty()) loaded.countDown(); });
            model.submission().observeForever(result->{ if(result.stage!=SalesViewModel.Submission.Stage.IDLE && !result.busy()) finished.countDown(); });
            model.ensureLoaded();
        });
        assertTrue(loaded.await(5,TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            SalesViewModel model=holder[0]; model.put(null,product,variant,1,0);
            SaleDraft draft=new SaleDraft("local",model.lines(),Collections.singletonList(new CheckoutLine(0,"34","34")));
            model.submit(draft); model.submit(draft);
        });
        assertTrue(started.await(5,TimeUnit.SECONDS)); release.countDown();
        assertTrue(finished.await(5,TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            SalesViewModel model=holder[0];
            assertEquals(success?0:1,model.lines().size());
            assertEquals(success?SalesViewModel.Submission.Stage.SUCCEEDED:failure.uncertain?SalesViewModel.Submission.Stage.UNCERTAIN:SalesViewModel.Submission.Stage.FAILED,model.submission().getValue().stage);
            if(success || failure.uncertain) model.submit(new SaleDraft("local",model.lines(),Collections.emptyList()));
            new androidx.lifecycle.ViewModelStore() {{ put("sales",model); }}.clear();
        });
        assertEquals(1,calls[0]);
    }
}
