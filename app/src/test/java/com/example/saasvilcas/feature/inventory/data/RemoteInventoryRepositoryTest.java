package com.example.saasvilcas.feature.inventory.data;

import com.example.saasvilcas.feature.inventory.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RemoteInventoryRepositoryTest {
    private static JSONObject product() throws Exception {
        JSONObject stock=new JSONObject().put("id_almacen","warehouse").put("id_local",JSONObject.NULL)
            .put("nombre_almacen","Depósito").put("cantidad",8).put("version",7);
        JSONObject variant=new JSONObject().put("talla",JSONObject.NULL).put("sku","SKU").put("codigo_barras","")
            .put("stock",new JSONArray().put(stock));
        return new JSONObject().put("id_producto","product").put("nombre","Motor").put("precio_venta","100.25")
            .put("costo_compra",80).put("stock_total",8).put("sexo","unisex").put("id_categoria","cat")
            .put("id_proveedor","supplier").put("nombre_categoria","Motores").put("nombre_proveedor","Proveedor")
            .put("imagen_url","/uploads/image.webp").put("variantes",new JSONArray().put(variant));
    }
    private static final class Fake implements ApiClient {
        final List<String> paths=new ArrayList<>(),methods=new ArrayList<>();
        JSONObject body; String token; ApiException failure; int uploadCalls;
        boolean more, twoProducts; int next=1;
        public String request(String method,String path,String type,String data,String bearer) throws ApiException {
            paths.add(path); methods.add(method); token=bearer;
            if(!method.equals("GET")&&failure!=null) throw failure;
            try {
                if(data!=null) body=new JSONObject(data);
                if(path.endsWith("/categorias")) return method.equals("GET")?"[{\"id_categoria\":\"cat\",\"nombre\":\"Motores\"}]":"{\"id_categoria\":\"new-cat\",\"nombre\":\"Nueva\"}";
                if(path.endsWith("/proveedores")) return "[{\"id_proveedor\":\"supplier\",\"razon_social\":\"Proveedor\"}]";
                if(path.endsWith("/almacenes")) return "[{\"id_almacen\":\"warehouse\",\"nombre\":\"Depósito\",\"direccion\":\"Calle 1\"}]";
                if(path.endsWith("/locales")) return "[{\"id_local\":\"local\",\"nombre\":\"Tienda\"}]";
                if(path.contains("productos?")) return new JSONObject().put("items",twoProducts ? new JSONArray().put(product()).put(product().put("id_producto","second")) : new JSONArray().put(product())).put("hay_mas",more).put("siguiente_offset",next).toString();
                if(method.equals("DELETE")) return "{\"mensaje\":\"Eliminado\"}";
                return product().toString();
            } catch(Exception error) { throw new AssertionError(error); }
        }
        public String upload(String path,String name,String mime,byte[] bytes,String bearer) {
            uploadCalls++; assertEquals("/inventario/productos/upload-imagen",path); assertEquals("image/jpeg",mime);
            return "{\"url\":\"/uploads/new.webp\"}";
        }
    }
    private RemoteInventoryRepository repo(Fake fake) { return new RemoteInventoryRepository(fake,()->"access",uri->new byte[]{1,2}); }
    private InventoryProduct changed(InventoryProduct source,int stock,String photo) {
        InventoryVariant v=source.variants.get(0);
        return new InventoryProduct(source.id,source.name,source.category,source.gender,source.supplier,source.price,source.cost,photo,
            Collections.singletonList(new InventoryVariant(v.size,v.type,v.location,stock,v.sku,v.barcode,v.locationId,v.version)));
    }
    @Test public void detailAndSaveKeepProductInOriginalPosition() throws Exception {
        Fake fake=new Fake(); fake.twoProducts=true;
        RemoteInventoryRepository repository=repo(fake); repository.refresh();
        InventoryProduct full=repository.detail("product");
        assertEquals("product",repository.products().get(0).id);
        repository.save(full);
        assertEquals("product",repository.products().get(0).id);
        assertEquals("second",repository.products().get(1).id);
    }
    @Test public void readsSummaryAndFullVariantWithVersion() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        assertEquals(8,repository.products().get(0).stock); assertTrue(repository.products().get(0).variants.isEmpty());
        InventoryProduct full=repository.detail("product"); assertEquals(10025,full.price);
        assertEquals("",full.size); assertEquals("warehouse",full.variants.get(0).locationId); assertEquals(Integer.valueOf(7),full.variants.get(0).version);
        assertEquals(Collections.singletonList("Tienda"),repository.references(3)); assertEquals("access",fake.token);
    }
    @Test public void editStockSendsPatchAndVersionAndWarehouseId() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        InventoryProduct full=repository.detail("product"); repository.save(changed(full,6,full.photo));
        assertEquals("PATCH",fake.methods.get(fake.methods.size()-1)); JSONObject row=fake.body.getJSONArray("variantes").getJSONObject(0);
        assertEquals("warehouse",row.getString("id_almacen")); assertFalse(row.has("id_local")); assertEquals(7,row.getInt("version"));
        assertTrue(row.isNull("talla")); assertEquals("cat",fake.body.getString("id_categoria"));
    }
    @Test public void metadataEditDoesNotReplaceStock() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        InventoryProduct full=repository.detail("product"); repository.save(full); assertFalse(fake.body.has("variantes"));
    }
    @Test public void newProductUploadsLocalPhotoAndUsesPost() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        repository.save(new InventoryProduct("new","Nuevo","Motores","Niño","Proveedor",10025,5000,"content://image",
            Collections.singletonList(new InventoryVariant("","Local","Tienda",3,"",""))));
        assertEquals(1,fake.uploadCalls); assertEquals("POST",fake.methods.get(fake.methods.size()-1));
        assertEquals("nino",fake.body.getString("sexo")); assertEquals("/uploads/new.webp",fake.body.getString("imagen_url"));
        assertEquals("local",fake.body.getJSONArray("variantes").getJSONObject(0).getString("id_local"));
    }
    @Test public void conflictPreservesCachedProduct() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        InventoryProduct full=repository.detail("product"); fake.failure=new ApiException("Stock cambió",409);
        try { repository.save(changed(full,6,full.photo)); fail(); } catch(InventoryException error) { assertEquals(409,error.status); assertFalse(error.uncertain); }
        assertEquals(8,repository.products().get(0).stock);
    }
    @Test public void timeoutDoesNotPretendProductDeleted() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh(); fake.failure=new ApiException("Timeout");
        try { repository.delete("product"); fail(); } catch(InventoryException error) { assertTrue(error.uncertain); }
        assertEquals(1,repository.products().size());
    }
    @Test public void referencesUseIdsAndWarehouseAddressIsRead() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        assertEquals("Calle 1",repository.warehouseDetails("Depósito")[0]); repository.saveReference(0,null,"Nueva");
        assertTrue(repository.references(0).contains("Nueva")); repository.deleteReference(0,"Motores");
        assertEquals("/inventario/categorias/cat",fake.paths.get(fake.paths.size()-1)); assertFalse(repository.references(0).contains("Motores"));
    }
    @Test public void filtersEncodeSearchAndUseCategoryId() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh();
        repository.filter("A & B","Motores","Niña"); repository.refresh();
        String path=fake.paths.get(fake.paths.size()-1); assertTrue(path.contains("A+%26+B")); assertTrue(path.contains("id_categoria=cat")); assertTrue(path.contains("sexo=nina"));
    }
    @Test public void paginationMustAdvance() throws Exception {
        Fake fake=new Fake(); fake.more=true; fake.next=0;
        try { repo(fake).refresh(); fail(); } catch(InventoryException error) { assertFalse(error.uncertain); }
    }
    @Test public void missingSessionMakesNoRequests() {
        Fake fake=new Fake(); RemoteInventoryRepository repository=new RemoteInventoryRepository(fake,()->null,uri->new byte[0]);
        try { repository.refresh(); fail(); } catch(InventoryException error) { assertEquals(401,error.status); }
        assertTrue(fake.paths.isEmpty());
    }
    @Test public void nullClearingUnsupportedByBackendIsNotReportedAsSuccess() throws Exception {
        Fake fake=new Fake(); RemoteInventoryRepository repository=repo(fake); repository.refresh(); InventoryProduct full=repository.detail("product");
        int requests=fake.paths.size();
        try { repository.save(new InventoryProduct(full.id,full.name,"",full.gender,full.supplier,full.price,full.cost,full.photo,full.variants)); fail(); }
        catch(IllegalArgumentException error) { assertTrue(error.getMessage().contains("no permite quitar")); }
        assertEquals(requests,fake.paths.size());
    }
}
