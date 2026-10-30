package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Supplier;

/** FastAPI POS adapter. Variant/location IDs are transmitted, never display names or client prices. */
public final class RemoteSalesRepository implements SalesRepository {
    private final ApiClient api;
    private final Supplier<String> token;
    private final ReceiptSource receipts;
    private final Map<String,String> uploaded=new HashMap<>();
    private String uploadOwner="";
    public RemoteSalesRepository(ApiClient api,Supplier<String> token,ReceiptSource receipts) {
        this.api=api; this.token=token; this.receipts=receipts;
    }
    private String bearer() throws SalesException {
        String value=token.get();
        if(value==null || value.isEmpty()) throw new SalesException("Tu sesión expiró. Vuelve a iniciar sesión.",401,false);
        return value;
    }
    @Override public List<SalesSite> loadSites() throws SalesException { return sites(sitesJson()); }
    /** Raw site list, kept so the catalog can be used without connection. */
    public String sitesJson() throws SalesException {
        try { return api.request("GET","/ventas/pos/mis-sedes",null,null,bearer()); }
        catch(ApiException error) { throw mapped(error,false); }
    }
    public static List<SalesSite> sites(String json) throws SalesException {
        try {
            JSONArray items=new JSONArray(json);
            ArrayList<SalesSite> sites=new ArrayList<>();
            for(int i=0;i<items.length();i++) {
                JSONObject site=items.getJSONObject(i);
                sites.add(new SalesSite(site.getString("id_sede"),site.getString("nombre"),site.getString("tipo")));
            }
            return sites;
        } catch(JSONException error) { throw invalid(false); }
    }
    @Override public SalesPage loadProducts(SalesFilter filter,int offset) throws SalesException {
        return page(productsJson(filter,offset),offset);
    }
    public JSONObject productsJson(SalesFilter filter,int offset) throws SalesException {
        try {
            String path="/ventas/pos/productos?limit=30&offset="+offset;
            path+=param("busqueda",filter.search)+param("talla","Sin talla".equals(filter.size)?"":filter.size)
                    +param("id_ubicacion_filtro",filter.siteId)+param("sexo",gender(filter.gender));
            return new JSONObject(api.request("GET",path,null,null,bearer()));
        } catch(ApiException error) { throw mapped(error,false); }
        catch(Exception error) { if(error instanceof SalesException) throw (SalesException)error; throw invalid(false); }
    }
    public static SalesPage page(JSONObject page,int offset) throws SalesException {
        try {
            JSONArray items=page.getJSONArray("items"); ArrayList<SalesProduct> products=new ArrayList<>();
            for(int i=0;i<items.length();i++) products.add(product(items.getJSONObject(i)));
            int next=page.getInt("siguiente_offset"); boolean more=page.getBoolean("hay_mas");
            if(more && (items.length()==0 || next<=offset)) throw new JSONException("Invalid pagination");
            return new SalesPage(products,more,next);
        } catch(Exception error) { throw invalid(false); }
    }
    public static SalesProduct product(JSONObject item) throws JSONException {
        ArrayList<SalesVariant> variants=new ArrayList<>(); JSONArray values=item.getJSONArray("variantes");
        for(int i=0;i<values.length();i++) {
            JSONObject variant=values.getJSONObject(i);
            if(variant.isNull("id_ubicacion_origen")) continue;
            variants.add(new SalesVariant(variant.getString("id_variante"),nullable(variant,"talla"),
                    variant.getString("id_ubicacion_origen"),nullable(variant,"nombre_ubicacion"),variant.getInt("stock_disponible")));
        }
        return new SalesProduct(item.getString("id_producto"),item.getString("nombre"),cents(item.get("precio_venta")),
                item.getInt("stock_total"),nullable(item,"sexo"),nullable(item,"imagen_url"),variants);
    }
    @Override public synchronized SaleResult submit(SaleDraft draft) throws SalesException {
        String owner=bearer();
        if(!owner.equals(uploadOwner)) { uploaded.clear(); uploadOwner=owner; }
        JSONObject body;
        try {
            ArrayList<String> urls=new ArrayList<>();
            for(CheckoutLine payment:draft.payments) {
                String url="";
                if(payment.method!=0) {
                    if(payment.receipt.isEmpty()) throw new SalesException("Adjunta el comprobante de cada pago digital.",422,false);
                    url=uploaded.get(payment.receipt);
                    if(url==null) {
                        ensureSession(owner);
                        url=uploadReceipt(receipts.jpeg(payment.receipt),owner);
                        uploaded.put(payment.receipt,url);
                    }
                }
                urls.add(url);
            }
            body=payload(draft,urls);
        } catch(JSONException error) { throw invalid(false); }
        ensureSession(owner);
        return confirm(body,owner);
    }
    /** Uploads one receipt JPEG and returns the server-relative URL to send in the payment. */
    public String uploadReceipt(byte[] jpeg,String owner) throws SalesException {
        try {
            String url=new JSONObject(api.upload("/ventas/pos/upload-comprobante","comprobante.jpg","image/jpeg",jpeg,owner)).getString("url");
            if(!url.startsWith("/")) throw new JSONException("Invalid receipt URL");
            return url;
        } catch(ApiException error) { throw mapped(error,false); }
        catch(JSONException error) { throw invalid(false); }
    }
    public String uploadReceipt(byte[] jpeg) throws SalesException { return uploadReceipt(jpeg,bearer()); }
    public SaleResult confirm(JSONObject body) throws SalesException { return confirm(body,bearer()); }
    private SaleResult confirm(JSONObject body,String owner) throws SalesException {
        try {
            JSONObject response=new JSONObject(api.request("POST","/ventas/pos/confirmar","application/json",body.toString(),owner));
            String id=response.getString("id_venta"); UUID.fromString(id);
            if(!"pagada".equals(response.getString("estado"))) throw new JSONException("Sale not paid");
            return new SaleResult(id,cents(response.get("total")));
        } catch(ApiException error) { throw mapped(error,error.status==0 || error.status>=500); }
        catch(Exception error) { throw invalid(true); }
    }
    public static JSONObject payload(SaleDraft draft,List<String> urls) throws JSONException {
        if(draft.items.isEmpty() || draft.registerId==null || draft.registerId.isEmpty()) throw new IllegalArgumentException("Selecciona una caja y productos.");
        JSONArray details=new JSONArray(),payments=new JSONArray();
        for(CartLine item:draft.items) {
            if(item.variantId.isEmpty() || item.siteId.isEmpty()) throw new IllegalArgumentException("El carrito contiene productos de demostración.");
            details.put(new JSONObject().put("id_variante",item.variantId).put("id_ubicacion_origen",item.siteId)
                    .put("cantidad",item.quantity).put("descuento_monto",BigDecimal.valueOf(item.discountCents,2)));
        }
        String[] methods={"efectivo","yape","plin","transferencia"};
        for(int i=0;i<draft.payments.size();i++) {
            CheckoutLine line=draft.payments.get(i);
            JSONObject payment=new JSONObject().put("metodo",methods[line.method]).put("monto",BigDecimal.valueOf(line.cents(),2));
            if(line.method==0) payment.put("monto_recibido",BigDecimal.valueOf(CheckoutPayment.cents(line.received),2));
            else payment.put("url_comprobante",urls.get(i));
            payments.put(payment);
        }
        return new JSONObject().put("clave_idempotencia",draft.id).put("id_local",draft.registerId)
                .put("detalles",details).put("pagos",payments);
    }
    private void ensureSession(String expected) throws SalesException {
        if(!expected.equals(bearer())) throw new SalesException("La sesión cambió. Vuelve a abrir ventas.",401,false);
    }
    private static SalesException mapped(ApiException error,boolean uncertain) { return new SalesException(error.getMessage(),error.status,uncertain); }
    private static SalesException invalid(boolean uncertain) { return new SalesException("La API devolvió una respuesta de ventas inválida.",0,uncertain); }
    private static String nullable(JSONObject object,String key) { return object.isNull(key)?"":object.optString(key,""); }
    private static long cents(Object value) { return new BigDecimal(value.toString()).setScale(2,java.math.RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
    private static String param(String key,String value) throws java.io.UnsupportedEncodingException { return value==null||value.trim().isEmpty()?"":"&"+key+"="+URLEncoder.encode(value.trim(),"UTF-8"); }
    private static String gender(String value) {
        switch(value.toLowerCase(Locale.ROOT)) { case "niño": return "nino"; case "niña": return "nina"; default: return value.toLowerCase(Locale.ROOT); }
    }
}
