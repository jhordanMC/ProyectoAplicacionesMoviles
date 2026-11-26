package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SaleNoticeRepositoryTest {
    private static String item(String id,String date) {
        return "{\"id_venta\":\""+id+"\",\"total\":190.0,\"estado\":\"pagada\",\"nombre_vendedor\":\"Ana Ruiz\",\"cantidad_items\":1,\"creado_en\":\""+date+"\"}";
    }
    private static final String DETAIL="{\"detalles\":[{\"cantidad\":2,\"nombre_producto\":\"Motor\",\"talla\":\"40\",\"nombre_local\":\"Tienda\",\"nombre_almacen\":null,\"imagen_url\":\"/media/productos/motor.jpg\"}],\"pagos\":[{\"metodo\":\"yape\",\"monto\":190.0}]}";
    private final FakeApi api=new FakeApi();
    private final MemoryStore store=new MemoryStore();
    private final FakeSession session=new FakeSession();
    private final SaleNoticeRepository repository=new SaleNoticeRepository(api,store,session);

    @Test public void firstCheckRecordsHistoryWithoutNotifying() {
        api.list=list(item("a","2026-10-30T05:00:00Z"));
        assertTrue(repository.check().isEmpty());
        assertTrue(store.findNotice("a","user").read);
        assertTrue(session.baseline);
    }
    @Test public void newSaleIsNotifiedOnceWithSiteSellerAndDetail() {
        session.baseline=true; api.list=list(item("b","2026-10-30T05:00:00.123456+00:00"));
        List<SaleNotice> fresh=repository.check();
        assertEquals(1,fresh.size());
        SaleNotice notice=fresh.get(0);
        assertEquals(19000,notice.totalCents); assertEquals("Tienda",notice.site); assertEquals("Ana Ruiz",notice.seller);
        assertEquals("2 × Motor (40)",notice.summary); assertEquals("/media/productos/motor.jpg",notice.photo); assertEquals("Yape S/ 190.00",notice.payments); assertFalse(notice.read);
        assertEquals(1793336400000L,notice.createdAt);
        assertTrue(repository.check().isEmpty());
    }
    @Test public void oldestNewSaleIsHandledFirst() {
        session.baseline=true; api.list=list(item("new","2026-10-30T06:00:00Z"),item("old","2026-10-30T05:00:00Z"));
        List<SaleNotice> fresh=repository.check();
        assertEquals("old",fresh.get(0).id); assertEquals("new",fresh.get(1).id);
    }
    @Test public void onlyTheOwnerChecksSales() {
        session.owner=false; api.list=list(item("a","2026-10-30T05:00:00Z"));
        assertTrue(repository.check().isEmpty());
        assertTrue(api.paths.isEmpty());
    }
    @Test public void connectionFailureLosesNothingAndRetriesNextTime() {
        session.baseline=true; api.failure=new ApiException("Sin conexión");
        assertTrue(repository.check().isEmpty());
        api.failure=null; api.list=list(item("a","2026-10-30T05:00:00Z"));
        assertEquals(1,repository.check().size());
    }
    @Test public void failedDetailStillNotifiesTheSale() {
        session.baseline=true; api.list=list(item("a","2026-10-30T05:00:00Z")); api.detailFailure=new ApiException("Caída",500);
        SaleNotice notice=repository.check().get(0);
        assertEquals("Ana Ruiz",notice.seller); assertFalse(notice.hasDetail()); assertEquals("",notice.photo);
    }
    @Test public void expiredTokenIsRenewedOnce() {
        session.baseline=true; api.list=list(item("a","2026-10-30T05:00:00Z")); api.rejectToken="access";
        assertEquals(1,repository.check().size());
        assertEquals(1,session.refreshes);
    }
    @Test public void openingMarksReadAndCompletesDetail() {
        api.list=list(item("a","2026-10-30T05:00:00Z")); repository.check();
        assertFalse(store.findNotice("a","user").hasDetail());
        repository.open("a");
        assertTrue(store.findNotice("a","user").read); assertEquals("Tienda",store.findNotice("a","user").site);
    }
    @Test public void markAllReadClearsTheUnreadCount() {
        session.baseline=true; api.list=list(item("a","2026-10-30T05:00:00Z"),item("b","2026-10-30T06:00:00Z")); repository.check();
        repository.markAllRead();
        for(SaleNotice notice:store.notices("company","user")) assertTrue(notice.read);
    }
    @Test public void listenerReceivesTheNotices() {
        final List<List<SaleNotice>> received=new ArrayList<>();
        repository.setListener(received::add);
        session.baseline=true; api.list=list(item("a","2026-10-30T05:00:00Z")); repository.check();
        assertEquals(1,received.get(received.size()-1).size());
    }
    private static String list(String... items) { return "{\"total\":"+items.length+",\"items\":["+String.join(",",items)+"]}"; }

    private final class FakeApi implements ApiClient {
        final List<String> paths=new ArrayList<>(); String list="{\"total\":0,\"items\":[]}",rejectToken; ApiException failure,detailFailure;
        @Override public String request(String method,String path,String type,String body,String bearer) throws ApiException {
            paths.add(path);
            if(failure!=null) throw failure;
            if(bearer.equals(rejectToken)) throw new ApiException("Tu sesión expiró.",401);
            if(path.startsWith("/ventas?")) return list;
            if(detailFailure!=null) throw detailFailure;
            return DETAIL;
        }
    }
    private static final class FakeSession implements SaleNoticeRepository.Session {
        boolean owner=true,baseline; int refreshes; String token="access";
        @Override public String companyId() { return "company"; }
        @Override public String userId() { return "user"; }
        @Override public String token() { return token; }
        @Override public boolean owner() { return owner; }
        @Override public boolean refresh() { refreshes++; token="renewed"; return true; }
        @Override public boolean baselineDone() { return baseline; }
        @Override public void markBaseline() { baseline=true; }
    }
    private static final class MemoryStore implements SaleNoticeStore {
        final LinkedHashMap<String,SaleNotice> rows=new LinkedHashMap<>();
        @Override public SaleNotice findNotice(String id,String userId) { return rows.get(id); }
        @Override public List<SaleNotice> notices(String companyId,String userId) { return new ArrayList<>(rows.values()); }
        @Override public void saveNotice(SaleNotice notice) { rows.put(notice.id,notice); }
        @Override public void markAllRead(String companyId,String userId) { for(String id:new ArrayList<>(rows.keySet())) rows.put(id,rows.get(id).asRead()); }
        @Override public void trimNotices(String companyId,String userId,int keep) { }
        @Override public void clearNotices(String companyId,String userId) { rows.clear(); }
    }
}
