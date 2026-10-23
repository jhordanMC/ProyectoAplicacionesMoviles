package com.example.saasvilcas.feature.inventory.data;

import com.example.saasvilcas.feature.inventory.domain.*;
import com.example.saasvilcas.shared.network.*;
import com.example.saasvilcas.shared.media.*;
import org.json.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

/** Worker-thread FastAPI adapter. Cached snapshots are the only synchronous UI reads. */
public final class RemoteInventoryRepository implements InventoryRepository {
    private final ApiClient api;
    private final Supplier<String> tokens;
    private final ImageSource photos;
    private final List<InventoryProduct> catalog=new ArrayList<>();
    private final Map<String,JSONObject> originals=new HashMap<>();
    private final List<List<JSONObject>> references=new ArrayList<>();
    private final Map<String,String> uploads=new HashMap<>();
    private String owner="";
    private int offset;
    private boolean more;
    private boolean referencesLoaded,filterOnly;
    private String query="";
    @Override public synchronized void invalidateReferences() { referencesLoaded=false; }
    @Override public synchronized void filter(String search,String category,String gender) {
        try {
            query="&busqueda="+java.net.URLEncoder.encode(search.trim(),"UTF-8")+"&sexo="+genderApi(gender);
            if(gender.isEmpty()) query=query.replace("&sexo=","");
            if(!category.isEmpty()) query+="&id_categoria="+referenceId(0,category);
            filterOnly=true;
        } catch(java.io.UnsupportedEncodingException error) { throw new IllegalArgumentException(error); }
    }
    private static final String[] RESOURCES={"categorias","proveedores","almacenes","locales"};
    private static final String[] IDS={"id_categoria","id_proveedor","id_almacen","id_local"};
    public RemoteInventoryRepository(ApiClient api,Supplier<String> tokens,ImageSource photos) {
        this.api=api; this.tokens=tokens; this.photos=photos;
        for(int i=0;i<4;i++) references.add(new ArrayList<>());
    }
    private String token() {
        String value=tokens.get();
        if(value==null||value.isEmpty()) throw new InventoryException("Tu sesión expiró. Vuelve a iniciar sesión.",401,false);
        return value;
    }
    private String request(String method,String path,JSONObject body) {
        try { return api.request(method,"/inventario/"+path,body==null?null:"application/json",body==null?null:body.toString(),token()); }
        catch(ApiException error) { throw new InventoryException(error.getMessage(),error.status,!method.equals("GET")&&(error.status==0||error.status>=500)); }
    }
    @Override public void refresh() {
        String session=token();
        List<List<JSONObject>> loaded=new ArrayList<>();
        try {
            boolean reloadReferences;
            synchronized(this) { reloadReferences=!referencesLoaded||!owner.equals(session)||!filterOnly; filterOnly=false; }
            if(reloadReferences) for(String resource:RESOURCES) {
                JSONArray values=new JSONArray(request("GET",resource,null)); List<JSONObject> list=new ArrayList<>();
                for(int i=0;i<values.length();i++) if(values.getJSONObject(i).optBoolean("esta_activo",true)) list.add(values.getJSONObject(i));
                loaded.add(list);
            }
            JSONObject page=new JSONObject(request("GET","productos?limit=30&offset=0"+query,null));
            if(!session.equals(token())) throw new InventoryException("La sesión cambió. Vuelve a abrir inventario.",401,false);
            synchronized(this) {
                if(!owner.equals(session)) { originals.clear(); uploads.clear(); }
                owner=session;
                if(reloadReferences) { references.clear(); references.addAll(loaded); referencesLoaded=true; }
                catalog.clear(); applyPage(page,0);
            }
        } catch(JSONException error) { throw invalid(false); }
    }
    @Override public boolean loadMore() {
        int start; synchronized(this) { if(!more) return false; start=offset; }
        try { JSONObject page=new JSONObject(request("GET","productos?limit=30&offset="+start+query,null)); synchronized(this) { applyPage(page,start); } return true; }
        catch(JSONException error) { throw invalid(false); }
    }
    private void applyPage(JSONObject page,int start) throws JSONException {
        JSONArray items=page.getJSONArray("items"); int next=page.getInt("siguiente_offset"); boolean has=page.getBoolean("hay_mas");
        if(has&&(items.length()==0||next<=start)) throw new JSONException("Invalid pagination");
        List<InventoryProduct> parsed=new ArrayList<>();
        for(int i=0;i<items.length();i++) parsed.add(parse(items.getJSONObject(i),false));
        for(InventoryProduct product:parsed) { replaceProduct(product); }
        offset=next; more=has;
    }
    private void replaceProduct(InventoryProduct product) {
        for(int index=0;index<catalog.size();index++) {
            if(catalog.get(index).id.equals(product.id)) { catalog.set(index,product); return; }
        }
        catalog.add(product);
    }
    @Override public synchronized boolean hasMore() { return more; }
    @Override public synchronized List<InventoryProduct> products() { return new ArrayList<>(catalog); }
    @Override public InventoryProduct detail(String id) {
        try {
            JSONObject value=new JSONObject(request("GET","productos/"+id,null));
            InventoryProduct product=parse(value,true);
            synchronized(this) { originals.put(id,value); replaceProduct(product); }
            return product;
        } catch(JSONException error) { throw invalid(false); }
    }
    private InventoryProduct parse(JSONObject value,boolean detailed) throws JSONException {
        String id=value.getString("id_producto"),name=value.getString("nombre"),category=text(value,"nombre_categoria"),supplier=text(value,"nombre_proveedor"),gender=genderDisplay(text(value,"sexo")),photo=text(value,"imagen_url");
        long price=cents(value.get("precio_venta")),cost=cents(value.get("costo_compra"));
        if(!detailed) return new InventoryProduct(id,name,category,gender,supplier,price,cost,value.getInt("stock_total"),photo);
        List<InventoryVariant> variants=new ArrayList<>(); JSONArray values=value.getJSONArray("variantes");
        for(int i=0;i<values.length();i++) {
            JSONObject variant=values.getJSONObject(i); if(!variant.optBoolean("esta_activo",true)) continue;
            JSONArray stocks=variant.getJSONArray("stock");
            for(int j=0;j<stocks.length();j++) {
                JSONObject stock=stocks.getJSONObject(j); boolean local=!stock.isNull("id_local");
                variants.add(new InventoryVariant(text(variant,"talla"),local?"Local":"Almacén",
                    text(stock,local?"nombre_local":"nombre_almacen"),stock.getInt("cantidad"),text(variant,"sku"),text(variant,"codigo_barras"),
                    stock.getString(local?"id_local":"id_almacen"),stock.getInt("version")));
            }
        }
        return new InventoryProduct(id,name,category,gender,supplier,price,cost,photo,variants);
    }
    @Override public void save(InventoryProduct product) {
        boolean submitted=false;
        JSONObject original; synchronized(this) { original=originals.get(product.id); }
        String session=token(),photo=product.photo;
        if(photo.startsWith("content:")) {
            synchronized(this) { photo=uploads.get(product.photo); }
            if(photo==null) {
                try {
                    byte[] bytes=photos.jpeg(product.photo);
                    photo=new JSONObject(api.upload("/inventario/productos/upload-imagen","producto.jpg","image/jpeg",bytes,session)).getString("url");
                    if(!photo.startsWith("/")) throw new JSONException("Invalid image URL");
                    synchronized(this) { uploads.put(product.photo,photo); }
                } catch(ApiException error) { throw new InventoryException(error.getMessage(),error.status,false); }
                catch(ImageException error) { throw new InventoryException(error.getMessage(),0,false); }
                catch(JSONException error) { throw invalid(false); }
            }
        }
        try {
            if(original!=null && ((!text(original,"id_categoria").isEmpty()&&product.category.isEmpty())
                    ||(!text(original,"id_proveedor").isEmpty()&&product.supplier.isEmpty())
                    ||(!text(original,"sexo").isEmpty()&&product.gender.isEmpty())))
                throw new IllegalArgumentException("El backend aún no permite quitar categoría, proveedor o sexo. Puedes reemplazarlos por otro valor.");
            JSONObject body=new JSONObject().put("nombre",product.name).put("id_categoria",referenceId(0,product.category))
                .put("id_proveedor",referenceId(1,product.supplier)).put("sexo",nullable(genderApi(product.gender)))
                .put("precio_venta",BigDecimal.valueOf(product.price,2)).put("costo_compra",BigDecimal.valueOf(product.cost,2)).put("imagen_url",photo);
            JSONArray variants=new JSONArray();
            for(InventoryVariant variant:product.variants) {
                int kind=variant.type.equals("Local")?3:2;
                Object location=variant.locationId.isEmpty()?referenceId(kind,variant.location):variant.locationId;
                if(location==JSONObject.NULL) throw new IllegalArgumentException("Selecciona una sede válida.");
                JSONObject row=new JSONObject().put("talla",nullable(variant.size)).put("cantidad",variant.stock)
                    .put("sku",nullable(variant.sku)).put("codigo_barras",nullable(variant.barcode)).put(kind==3?"id_local":"id_almacen",location);
                if(variant.version!=null) row.put("version",variant.version);
                if(original!=null) for(InventoryVariant old:parse(original,true).variants) {
                    if(old.key().equals(variant.key())&&((!old.sku.isEmpty()&&variant.sku.isEmpty())||(!old.barcode.isEmpty()&&variant.barcode.isEmpty())))
                        throw new IllegalArgumentException("El backend aún no permite borrar SKU o código de barras existentes. Puedes reemplazarlos por otro código.");
                }
                variants.put(row);
            }
            // Metadata-only edits must not replace variants (or erase variants with no stock rows).
            if(original==null || !variantsUnchanged(product,parse(original,true))) body.put("variantes",variants);
            if(!session.equals(token())) throw new InventoryException("La sesión cambió. Vuelve a abrir inventario.",401,false);
            submitted=true;
            String response=request(original==null?"POST":"PATCH",original==null?"productos":"productos/"+product.id,body);
            JSONObject saved=new JSONObject(response); InventoryProduct parsed=parse(saved,true);
            synchronized(this) { originals.put(parsed.id,saved); replaceProduct(parsed); }
        } catch(JSONException error) { throw invalid(submitted); }
        catch(IllegalArgumentException error) {
            if(submitted&&!(error instanceof InventoryException)) throw invalid(true);
            throw error;
        }
    }
    private static boolean variantsUnchanged(InventoryProduct a,InventoryProduct b) {
        if(a.variants.size()!=b.variants.size()) return false;
        for(int i=0;i<a.variants.size();i++) {
            InventoryVariant x=a.variants.get(i),y=b.variants.get(i);
            if(!x.key().equals(y.key())||x.stock!=y.stock||!x.sku.equals(y.sku)||!x.barcode.equals(y.barcode)) return false;
        }
        return true;
    }
    @Override public void delete(String id) {
        verifyDelete(request("DELETE","productos/"+id,null));
        synchronized(this) { catalog.removeIf(p->p.id.equals(id)); originals.remove(id); }
    }
    @Override public synchronized List<String> references(int kind) {
        List<String> values=new ArrayList<>(); for(JSONObject value:references.get(kind)) values.add(text(value,kind==1?"razon_social":"nombre")); return values;
    }
    private synchronized Object referenceId(int kind,String name) {
        if(name.isEmpty()) return JSONObject.NULL;
        String found=null;
        for(JSONObject value:references.get(kind)) if(text(value,kind==1?"razon_social":"nombre").equals(name)) {
            if(found!=null) throw new IllegalArgumentException("Hay sedes con el mismo nombre. Cambia sus nombres para diferenciarlas.");
            found=text(value,IDS[kind]);
        }
        if(found==null) throw new IllegalArgumentException("La referencia “"+name+"” ya no está disponible. Actualiza inventario.");
        return found;
    }
    @Override public void saveReference(int kind,String previous,String name) { saveReference(kind,previous,name,null,null); }
    private void saveReference(int kind,String previous,String name,String address,String description) {
        if(name.trim().isEmpty()) throw new IllegalArgumentException("Ingresa un nombre.");
        try {
            JSONObject body=new JSONObject().put(kind==1?"razon_social":"nombre",name.trim());
            if(kind==2) body.put("direccion",nullable(address)).put("descripcion",nullable(description));
            String path=RESOURCES[kind]+(previous==null?"":"/"+referenceId(kind,previous));
            JSONObject saved=new JSONObject(request(previous==null?"POST":"PATCH",path,body));
            String id=saved.getString(IDS[kind]);
            synchronized(this) { references.get(kind).removeIf(p->text(p,IDS[kind]).equals(id)); references.get(kind).add(saved); }
        } catch(JSONException error) { throw invalid(true); }
    }
    @Override public void deleteReference(int kind,String name) {
        String id=referenceId(kind,name).toString(); verifyDelete(request("DELETE",RESOURCES[kind]+"/"+id,null));
        synchronized(this) { references.get(kind).removeIf(p->text(p,IDS[kind]).equals(id)); }
    }
    @Override public synchronized String[] warehouseDetails(String name) {
        for(JSONObject value:references.get(2)) if(text(value,"nombre").equals(name)) return new String[]{text(value,"direccion"),text(value,"descripcion")};
        return new String[]{"",""};
    }
    @Override public void saveWarehouse(String previous,String name,String address,String description) { saveReference(2,previous,name,address,description); }
    private static Object nullable(String value) { return value==null||value.trim().isEmpty()?JSONObject.NULL:value; }
    private static void verifyDelete(String response) {
        try { new JSONObject(response).getString("mensaje"); } catch(JSONException error) { throw invalid(true); }
    }
    private static String text(JSONObject object,String key) { return object.isNull(key)?"":object.optString(key,""); }
    private static long cents(Object value) { return new BigDecimal(value.toString()).setScale(2,java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact(); }
    private static String genderApi(String value) { return value.toLowerCase(Locale.ROOT).replace("niño","nino").replace("niña","nina"); }
    private static String genderDisplay(String value) { switch(value) { case "hombre": return "Hombre"; case "mujer": return "Mujer"; case "nino": return "Niño"; case "nina": return "Niña"; case "unisex": return "Unisex"; default: return ""; } }
    private static InventoryException invalid(boolean write) { return new InventoryException("La API devolvió una respuesta de inventario inválida.",0,write); }
}
