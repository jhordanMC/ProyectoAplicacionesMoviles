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
    @Test public void noticesAreOrderedTrimmedAndMarkedRead() {
        for(int i=1;i<=3;i++) store.saveNotice(new com.example.saasvilcas.feature.sales.domain.SaleNotice("n"+i,"company","user","Tienda","Ana","","",100,i,false));
        store.trimNotices("company","user",2);
        List<com.example.saasvilcas.feature.sales.domain.SaleNotice> notices=store.notices("company","user");
        assertEquals(2,notices.size()); assertEquals("n3",notices.get(0).id);
        store.markAllRead("company","user");
        assertTrue(store.findNotice("n2","user").read);
        store.clearNotices("company","user");
        assertTrue(store.notices("company","user").isEmpty());
    }
    @Test public void migrationKeepsTheSalesQueueOfVersionOne() {
        android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="migracion_prueba.db"; context.deleteDatabase(name);
        android.database.sqlite.SQLiteDatabase old=context.openOrCreateDatabase(name,0,null);
        old.execSQL("CREATE TABLE `ventas_pendientes` (`id` TEXT NOT NULL, `companyId` TEXT NOT NULL, `userId` TEXT NOT NULL, `registerId` TEXT NOT NULL, `payload` TEXT NOT NULL, `summary` TEXT NOT NULL, `status` TEXT NOT NULL, `message` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `totalCents` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        old.execSQL("CREATE INDEX `index_ventas_pendientes_companyId_userId_createdAt` ON `ventas_pendientes` (`companyId`, `userId`, `createdAt`)");
        old.execSQL("CREATE TABLE `sedes_guardadas` (`companyId` TEXT NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`companyId`))");
        old.execSQL("CREATE TABLE `productos_guardados` (`companyId` TEXT NOT NULL, `productId` TEXT NOT NULL, `json` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`companyId`, `productId`))");
        old.execSQL("INSERT INTO ventas_pendientes VALUES ('s1','company','user','store','{}','1 × Motor','PENDING','',1,100,0)");
        old.setVersion(1); old.close();
        OfflineDatabase migrated=Room.databaseBuilder(context,OfflineDatabase.class,name).addMigrations(OfflineDatabase.MIGRATION_1_2).build();
        try {
            OfflineDatabase.Store current=new OfflineDatabase.Store(migrated);
            assertNotNull(current.find("s1"));
            current.saveNotice(new com.example.saasvilcas.feature.sales.domain.SaleNotice("n","company","user","","","","",100,1,false));
            assertNotNull(current.findNotice("n","user"));
        } finally { migrated.close(); context.deleteDatabase(name); }
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
