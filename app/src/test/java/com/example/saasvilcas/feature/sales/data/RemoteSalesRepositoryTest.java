package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RemoteSalesRepositoryTest {
    private static final String PRODUCT="{\"id_producto\":\"product\",\"nombre\":\"Motor\",\"precio_venta\":\"100.25\",\"stock_total\":5,\"sexo\":null,\"imagen_url\":\"/uploads/motor.webp\",\"variantes\":[{\"id_variante\":\"variant\",\"talla\":null,\"stock_disponible\":5,\"id_ubicacion_origen\":\"warehouse\",\"nombre_ubicacion\":\"Depósito\"}]}";
    private static final String SUCCESS="{\"id_venta\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\",\"total\":100.25,\"estado\":\"pagada\"}";
    private RemoteSalesRepository repo(FakeApi api) { return new RemoteSalesRepository(api,()->"access",uri->new byte[]{1,2,3}); }
    private SaleDraft draft(int method) {
        SalesVariant variant=new SalesVariant("variant",null,"warehouse","Depósito",5);
        SalesProduct product=new SalesProduct("product","Motor",10025,5,"","",Collections.singletonList(variant));
        CheckoutLine payment=new CheckoutLine(method,"100.25","110"); payment.receipt=method==0?"":"content://image/photo";
        return new SaleDraft("cash-register",Collections.singletonList(new CartLine(product,variant,1,0)),Collections.singletonList(payment));
    }
    @Test public void catalogParsesDecimalAndNullSizeAndEncodesFilters() throws Exception {
        FakeApi api=new FakeApi(); api.responses.add("{\"items\":["+PRODUCT+"],\"hay_mas\":true,\"siguiente_offset\":30}");
        SalesPage page=repo(api).loadProducts(new SalesFilter("Motor & A","","warehouse","Niño"),0);
        assertEquals(10025,page.items.get(0).priceCents); assertEquals("Sin talla",page.items.get(0).variants.get(0).size);
        assertTrue(page.hasMore); assertEquals(30,page.nextOffset);
        assertTrue(api.paths.get(0).contains("busqueda=Motor+%26+A")); assertTrue(api.paths.get(0).contains("sexo=nino"));
        assertTrue(api.paths.get(0).contains("id_ubicacion_filtro=warehouse")); assertEquals("access",api.bearer);
    }
    @Test public void cashPayloadUsesVariantOriginAndRegisterAndNeverSendsClientPrice() throws Exception {
        FakeApi api=new FakeApi(); api.responses.add(SUCCESS);
        SaleResult result=repo(api).submit(draft(0)); JSONObject body=new JSONObject(api.body);
        assertEquals(10025,result.totalCents); assertEquals("cash-register",body.getString("id_local"));
        JSONObject item=body.getJSONArray("detalles").getJSONObject(0);
        assertEquals("variant",item.getString("id_variante")); assertEquals("warehouse",item.getString("id_ubicacion_origen"));
        assertFalse(item.has("precio_unitario")); assertFalse(item.has("nombre"));
        JSONObject payment=body.getJSONArray("pagos").getJSONObject(0);
        assertEquals("efectivo",payment.getString("metodo")); assertEquals(110,payment.getDouble("monto_recibido"),0);
        assertFalse(payment.has("url_comprobante")); assertEquals(0,api.uploadCount);
    }
    @Test public void digitalProofUploadsBeforeConfirmAndRetriesReuseUploadedUrl() throws Exception {
        FakeApi api=new FakeApi(); api.uploadResponse="{\"url\":\"/uploads/ventas/proof.webp\"}";
        api.failure=new ApiException("Stock cambió",409);
        RemoteSalesRepository repository=repo(api);
        assertThrows(SalesException.class,()->repository.submit(draft(1)));
        api.failure=null; api.responses.add(SUCCESS); repository.submit(draft(1));
        assertEquals(1,api.uploadCount);
        JSONObject payment=new JSONObject(api.body).getJSONArray("pagos").getJSONObject(0);
        assertEquals("yape",payment.getString("metodo")); assertEquals("/uploads/ventas/proof.webp",payment.getString("url_comprobante"));
        assertFalse(payment.has("monto_recibido")); assertEquals("upload",api.operations.get(0));
    }
    @Test public void splitPaymentsPreserveExactAmountsAndCashOnlyReceived() throws Exception {
        SaleDraft first=draft(0); CheckoutLine cash=new CheckoutLine(0,"40.25","50"),plin=new CheckoutLine(2,"60","0");
        JSONObject payload=RemoteSalesRepository.payload(new SaleDraft(first.registerId,first.items,Arrays.asList(cash,plin)),Arrays.asList("","/proof.webp"));
        JSONArray payments=payload.getJSONArray("pagos");
        assertEquals(40.25,payments.getJSONObject(0).getDouble("monto"),0);
        assertEquals(60,payments.getJSONObject(1).getDouble("monto"),0); assertFalse(payments.getJSONObject(1).has("monto_recibido"));
    }
    @Test public void stockConflictIsDefiniteButTimeoutAfterConfirmIsUncertain() {
        FakeApi api=new FakeApi(); api.failure=new ApiException("Stock cambió",409);
        SalesException conflict=assertThrows(SalesException.class,()->repo(api).submit(draft(0)));
        assertEquals(409,conflict.status); assertFalse(conflict.uncertain);
        api.failure=new ApiException("Timeout");
        assertTrue(assertThrows(SalesException.class,()->repo(api).submit(draft(0))).uncertain);
    }
    @Test public void failedProofNeverSubmitsSaleAndIsSafeToRetry() {
        FakeApi api=new FakeApi(); api.uploadFailure=new ApiException("No se pudo subir");
        SalesException error=assertThrows(SalesException.class,()->repo(api).submit(draft(1)));
        assertFalse(error.uncertain); assertTrue(api.paths.isEmpty());
    }
    @Test public void malformedSuccessfulResponseCannotBeReportedAsRegistered() {
        FakeApi api=new FakeApi(); api.responses.add("{}");
        assertTrue(assertThrows(SalesException.class,()->repo(api).submit(draft(0))).uncertain);
    }
    @Test public void noSessionMakesNoApiCall() {
        FakeApi api=new FakeApi(); RemoteSalesRepository repository=new RemoteSalesRepository(api,()->null,uri->new byte[0]);
        assertEquals(401,assertThrows(SalesException.class,repository::loadSites).status); assertTrue(api.paths.isEmpty());
    }
    @Test public void invalidPaginationDoesNotCreateAnInfiniteRequestLoop() {
        FakeApi api=new FakeApi(); api.responses.add("{\"items\":[],\"hay_mas\":true,\"siguiente_offset\":0}");
        assertThrows(SalesException.class,()->repo(api).loadProducts(new SalesFilter("","","",""),0));
    }
    private static final class FakeApi implements ApiClient {
        final List<String> paths=new ArrayList<>(),operations=new ArrayList<>(); final Queue<String> responses=new ArrayDeque<>();
        String bearer,body,uploadResponse; int uploadCount; ApiException failure,uploadFailure;
        public String request(String method,String path,String type,String body,String bearer) throws ApiException {
            paths.add(path); operations.add("request"); this.body=body; this.bearer=bearer;
            if(failure!=null) throw failure; return responses.remove();
        }
        public String upload(String path,String fileName,String mime,byte[] bytes,String bearer) throws ApiException {
            uploadCount++; operations.add("upload"); if(uploadFailure!=null) throw uploadFailure; return uploadResponse;
        }
    }
}
