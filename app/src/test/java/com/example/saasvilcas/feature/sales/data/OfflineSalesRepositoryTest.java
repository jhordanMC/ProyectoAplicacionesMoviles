package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import org.junit.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class OfflineSalesRepositoryTest {
    private static final String PRODUCT="{\"id_producto\":\"product\",\"nombre\":\"Motor\",\"precio_venta\":\"100.25\",\"stock_total\":5,\"sexo\":\"hombre\",\"imagen_url\":\"\",\"variantes\":[{\"id_variante\":\"variant\",\"talla\":\"40\",\"stock_disponible\":5,\"id_ubicacion_origen\":\"store\",\"nombre_ubicacion\":\"Tienda\"}]}";
    private static final String SUCCESS="{\"id_venta\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\",\"total\":100.25,\"estado\":\"pagada\"}";
    private final FakeApi api=new FakeApi();
    private final MemoryStore store=new MemoryStore();
    private final MemoryFiles files=new MemoryFiles();
    private final FakeSession session=new FakeSession();
    private boolean online=true;
    private long now=1000;
    private final OfflineSalesRepository repository=new OfflineSalesRepository(
            new RemoteSalesRepository(api,()->session.token,uri->new byte[]{7,7}),store,files,uri->new byte[]{7,7},
            session,()->online,()->now++);

    private SaleDraft draft(int method,int quantity) {
        SalesVariant variant=new SalesVariant("variant","40","store","Tienda",5);
        SalesProduct product=new SalesProduct("product","Motor",10025,5,"hombre","",Collections.singletonList(variant));
        CheckoutLine payment=new CheckoutLine(method,String.valueOf(new java.math.BigDecimal(10025L*quantity).movePointLeft(2)),"500");
        payment.receipt=method==0?"":"content://image/photo";
        return new SaleDraft("store",Collections.singletonList(new CartLine(product,variant,quantity,0)),Collections.singletonList(payment));
    }

    @Test public void offlineSaleIsQueuedWithReceiptCopyAndNoRequest() throws Exception {
        online=false;
        SaleDraft draft=draft(1,1);
        SaleResult result=repository.submit(draft);
        assertTrue(result.pending); assertEquals(draft.id,result.id); assertEquals(10025,result.totalCents);
        assertTrue(api.requests.isEmpty()); assertEquals(0,api.uploads);
        PendingSale saved=store.find(draft.id);
        assertEquals(PendingSale.Status.PENDING,saved.status);
        JSONObject body=new JSONObject(saved.payload);
        assertEquals(draft.id,body.getString("clave_idempotencia"));
        JSONObject payment=body.getJSONArray("pagos").getJSONObject(0);
        assertFalse(payment.has("url_comprobante")); assertEquals(1,files.data.size());
        assertEquals(1,repository.status().pending);
    }
    @Test public void catalogWorksOfflineAndDiscountsQueuedUnits() throws Exception {
        repository.loadSites(); repository.loadProducts(new SalesFilter("","","",""),0);
        online=false; repository.submit(draft(0,2));
        api.down=new ApiException("Sin conexión");
        assertEquals("Tienda",repository.loadSites().get(0).name);
        SalesProduct product=repository.loadProducts(new SalesFilter("","","",""),0).items.get(0);
        assertEquals(3,product.stock); assertEquals(3,product.variants.get(0).stock);
        assertEquals(1,repository.loadProducts(new SalesFilter("mot","40","store","Hombre"),0).items.size());
        assertTrue(repository.loadProducts(new SalesFilter("","","","Niña"),0).items.isEmpty());
        assertTrue(repository.loadProducts(new SalesFilter("","","",""),30).items.isEmpty());
    }
    @Test public void offlineWithoutSavedCatalogExplainsWhatToDo() {
        api.down=new ApiException("Sin conexión");
        SalesException error=assertThrows(SalesException.class,()->repository.loadProducts(new SalesFilter("","","",""),0));
        assertTrue(error.getMessage().contains("catálogo"));
    }
    @Test public void syncSendsOldestFirstWithSameKeyAndUploadsReceiptOnce() throws Exception {
        online=false;
        SaleDraft first=draft(1,1),second=draft(0,1);
        repository.submit(first); repository.submit(second);
        online=true;
        api.confirms.add(new ApiException("No se pudo conectar con VILCAS."));
        assertTrue(repository.sync());
        assertEquals(1,api.uploads); assertEquals(0,files.data.size());
        assertTrue(new JSONObject(store.find(first.id).payload).getJSONArray("pagos").getJSONObject(0).has("url_comprobante"));
        api.confirms.add(SUCCESS); api.confirms.add(SUCCESS);
        assertFalse(repository.sync());
        assertEquals(1,api.uploads);
        assertEquals(first.id,new JSONObject(api.bodies.get(0)).getString("clave_idempotencia"));
        assertEquals(first.id,new JSONObject(api.bodies.get(1)).getString("clave_idempotencia"));
        assertEquals(second.id,new JSONObject(api.bodies.get(2)).getString("clave_idempotencia"));
        assertTrue(store.sales.isEmpty()); assertEquals(0,repository.status().pending);
    }
    @Test public void stockConflictIsKeptForReviewAndDoesNotBlockTheQueue() throws Exception {
        online=false;
        SaleDraft first=draft(0,1),second=draft(0,1);
        repository.submit(first); repository.submit(second);
        api.confirms.add(new ApiException("El stock cambió.",409)); api.confirms.add(SUCCESS);
        assertFalse(repository.sync());
        assertEquals(PendingSale.Status.CONFLICT,store.find(first.id).status);
        assertEquals("El stock cambió.",store.find(first.id).message);
        assertNull(store.find(second.id));
        assertEquals(1,repository.status().attention.size()); assertEquals(0,repository.status().pending);
        repository.retry(first.id);
        assertEquals(PendingSale.Status.PENDING,store.find(first.id).status);
        repository.discard(first.id);
        assertNull(store.find(first.id));
    }
    @Test public void expiredTokenIsRenewedOnceBeforeGivingUp() throws Exception {
        online=false; repository.submit(draft(0,1));
        api.confirms.add(new ApiException("Tu sesión expiró.",401)); api.confirms.add(SUCCESS);
        assertFalse(repository.sync());
        assertEquals(1,session.refreshes); assertEquals("renewed",api.bearer); assertTrue(store.sales.isEmpty());
    }
    @Test public void rejectedRenewalKeepsTheSaleForLater() throws Exception {
        online=false; SaleDraft draft=draft(0,1); repository.submit(draft);
        session.canRefresh=false; api.confirms.add(new ApiException("Tu sesión expiró.",401));
        assertTrue(repository.sync());
        assertEquals(PendingSale.Status.PENDING,store.find(draft.id).status);
    }
    @Test public void onlyTheSignedInUsersQueueIsSent() {
        store.add(new PendingSale("other-sale","company","other","store","{}","",1,100,PendingSale.Status.PENDING,"",0));
        assertFalse(repository.sync());
        assertTrue(api.requests.isEmpty()); assertNotNull(store.find("other-sale"));
    }
    @Test public void failedReceiptUploadBeforeConfirmQueuesTheSale() throws Exception {
        api.uploadDown=new ApiException("No se pudo conectar con VILCAS.");
        SaleResult result=repository.submit(draft(2,1));
        assertTrue(result.pending); assertTrue(api.bodies.isEmpty());
    }
    @Test public void uncertainOnlineConfirmIsNotQueued() {
        api.confirms.add(new ApiException("La conexión tardó demasiado."));
        SalesException error=assertThrows(SalesException.class,()->repository.submit(draft(0,1)));
        assertTrue(error.uncertain); assertTrue(store.sales.isEmpty());
    }

    private static final class FakeApi implements ApiClient {
        final List<String> requests=new ArrayList<>(),bodies=new ArrayList<>();
        final Deque<Object> confirms=new ArrayDeque<>();
        ApiException down,uploadDown; int uploads; String bearer;
        @Override public String request(String method,String path,String type,String body,String bearer) throws ApiException {
            requests.add(method+" "+path); this.bearer=bearer;
            if(down!=null) throw down;
            if(path.startsWith("/ventas/pos/mis-sedes")) return "[{\"id_sede\":\"store\",\"nombre\":\"Tienda\",\"tipo\":\"local\"}]";
            if(path.startsWith("/ventas/pos/productos")) return "{\"items\":["+PRODUCT+"],\"hay_mas\":false,\"siguiente_offset\":1}";
            bodies.add(body);
            Object next=confirms.remove();
            if(next instanceof ApiException) throw (ApiException)next;
            return (String)next;
        }
        @Override public String upload(String path,String fileName,String mime,byte[] bytes,String bearer) throws ApiException {
            if(uploadDown!=null) throw uploadDown;
            if(down!=null) throw down;
            uploads++; return "{\"url\":\"/uploads/ventas/r"+uploads+".webp\"}";
        }
    }
    private static final class FakeSession implements OfflineSalesRepository.Session {
        String token="access"; int refreshes; boolean canRefresh=true;
        @Override public String companyId() { return "company"; }
        @Override public String userId() { return "user"; }
        @Override public boolean refresh() { refreshes++; if(canRefresh) token="renewed"; return canRefresh; }
    }
    private static final class MemoryFiles implements OfflineSalesRepository.ReceiptFiles {
        final Map<String,byte[]> data=new HashMap<>();
        @Override public String save(String saleId,int index,byte[] jpeg) { String path=saleId+"_"+index; data.put(path,jpeg); return path; }
        @Override public byte[] read(String path) throws IOException { byte[] bytes=data.get(path); if(bytes==null) throw new IOException(); return bytes; }
        @Override public void delete(String path) { data.remove(path); }
    }
    private static final class MemoryStore implements OfflineSalesStore {
        final LinkedHashMap<String,PendingSale> sales=new LinkedHashMap<>();
        final Map<String,String> sites=new HashMap<>();
        final Map<String,LinkedHashMap<String,String>> products=new HashMap<>();
        @Override public void add(PendingSale sale) { sales.put(sale.id,sale); }
        @Override public void update(PendingSale sale) { sales.put(sale.id,sale); }
        @Override public void delete(String id) { sales.remove(id); }
        @Override public PendingSale find(String id) { return sales.get(id); }
        @Override public List<PendingSale> list(String companyId,String userId) {
            ArrayList<PendingSale> result=new ArrayList<>();
            for(PendingSale sale:sales.values()) if(sale.companyId.equals(companyId) && sale.userId.equals(userId)) result.add(sale);
            result.sort(Comparator.comparingLong(sale->sale.createdAt));
            return result;
        }
        @Override public void saveSites(String companyId,String json) { sites.put(companyId,json); }
        @Override public String sites(String companyId) { return sites.get(companyId); }
        @Override public void saveProducts(String companyId,List<String> ids,List<String> json,int firstPosition) {
            LinkedHashMap<String,String> saved=products.computeIfAbsent(companyId,key->new LinkedHashMap<>());
            for(int i=0;i<ids.size();i++) saved.put(ids.get(i),json.get(i));
        }
        @Override public List<String> products(String companyId) {
            return new ArrayList<>(products.getOrDefault(companyId,new LinkedHashMap<>()).values());
        }
    }
}
