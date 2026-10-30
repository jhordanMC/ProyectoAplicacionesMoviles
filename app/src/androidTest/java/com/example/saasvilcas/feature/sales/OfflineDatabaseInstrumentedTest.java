package com.example.saasvilcas.feature.sales;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.saasvilcas.feature.sales.data.OfflineDatabase;
import com.example.saasvilcas.feature.sales.domain.PendingSale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OfflineDatabaseInstrumentedTest {
    private OfflineDatabase database;
    private OfflineDatabase.Store store;
    @Before public void open() {
        database=Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().getTargetContext(),OfflineDatabase.class).build();
        store=new OfflineDatabase.Store(database);
    }
    @After public void close() { database.close(); }
    private static PendingSale sale(String id,String user,long createdAt) {
        return new PendingSale(id,"company",user,"store","{\"detalles\":[]}","1 × Motor (40)",createdAt,10025,PendingSale.Status.PENDING,"",0);
    }
    @Test public void queueIsScopedByUserAndOrderedByTime() {
        store.add(sale("b","user",2)); store.add(sale("a","user",1)); store.add(sale("c","other",0));
        List<PendingSale> queue=store.list("company","user");
        assertEquals(Arrays.asList("a","b"),Arrays.asList(queue.get(0).id,queue.get(1).id));
        store.update(queue.get(0).with(PendingSale.Status.CONFLICT,"El stock cambió.",1));
        PendingSale updated=store.find("a");
        assertEquals(PendingSale.Status.CONFLICT,updated.status); assertEquals("El stock cambió.",updated.message); assertEquals(1,updated.attempts);
        store.delete("a");
        assertNull(store.find("a")); assertEquals(1,store.list("company","user").size());
    }
    @Test public void filteredSearchesKeepTheCatalogOrder() {
        store.saveProducts("company",Arrays.asList("p1","p2"),Arrays.asList("{\"n\":1}","{\"n\":2}"),0);
        store.saveProducts("company",Collections.singletonList("p1"),Collections.singletonList("{\"n\":11}"),-1);
        store.saveProducts("company",Collections.singletonList("p3"),Collections.singletonList("{\"n\":3}"),-1);
        assertEquals(Arrays.asList("{\"n\":11}","{\"n\":2}","{\"n\":3}"),store.products("company"));
        assertTrue(store.products("other").isEmpty());
        store.saveSites("company","[]"); store.saveSites("company","[1]");
        assertEquals("[1]",store.sites("company")); assertNull(store.sites("other"));
    }
}
