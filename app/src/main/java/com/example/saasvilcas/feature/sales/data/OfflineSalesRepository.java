package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import org.json.*;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * HU-18. Keeps the POS working without connection: the last catalog seen online is cached per company,
 * sales confirmed offline are queued on the device and sent oldest first when the connection returns.
 * Each queued sale is resent with the same idempotency key, so a lost response never duplicates it.
 */
public final class OfflineSalesRepository implements SalesRepository {
    public interface Session {
        String companyId();
        String userId();
        /** Renews the access token once after a 401; false when the user has to sign in again. */
        boolean refresh();
    }
    public interface ReceiptFiles {
        String save(String saleId,int index,byte[] jpeg) throws IOException;
        byte[] read(String path) throws IOException;
        void delete(String path);
    }
    public static final class Status {
        public static final Status EMPTY=new Status(0,Collections.emptyList());
        public final int pending;
        /** Sales the server rejected (stock conflict or invalid data); they wait for the seller to decide. */
        public final List<PendingSale> attention;
        public Status(int pending,List<PendingSale> attention) {
            this.pending=pending; this.attention=Collections.unmodifiableList(new ArrayList<>(attention));
        }
    }
    public interface Listener { void changed(Status status); }

    private static final String RECEIPT_FILE="archivo_comprobante";
    private final RemoteSalesRepository remote;
    private final OfflineSalesStore store;
    private final ReceiptFiles files;
    private final ReceiptSource receipts;
    private final Session session;
    private final BooleanSupplier online;
    private final LongSupplier clock;
    private volatile Listener listener;

    public OfflineSalesRepository(RemoteSalesRepository remote,OfflineSalesStore store,ReceiptFiles files,ReceiptSource receipts,
            Session session,BooleanSupplier online,LongSupplier clock) {
        this.remote=remote; this.store=store; this.files=files; this.receipts=receipts;
        this.session=session; this.online=online; this.clock=clock;
    }
    public void setListener(Listener listener) { this.listener=listener; }

    // --- Catalog -------------------------------------------------------------------------------------------

    @Override public List<SalesSite> loadSites() throws SalesException {
        String company=company();
        try {
            String json=authorized(remote::sitesJson);
            store.saveSites(company,json);
            return RemoteSalesRepository.sites(json);
        } catch(SalesException error) {
            if(!unreachable(error)) throw error;
            String cached=store.sites(company);
            if(cached==null) throw noCatalog();
            return RemoteSalesRepository.sites(cached);
        }
    }
    @Override public SalesPage loadProducts(SalesFilter filter,int offset) throws SalesException {
        String company=company();
        try {
            JSONObject json=authorized(()->remote.productsJson(filter,offset));
            SalesPage page=RemoteSalesRepository.page(json,offset);
            cache(company,json,unfiltered(filter)?offset:-1);
            return reserve(page);
        } catch(SalesException error) {
            if(!unreachable(error)) throw error;
            if(offset>0) return new SalesPage(Collections.emptyList(),false,offset);
            List<String> cached=store.products(company);
            if(cached.isEmpty()) throw noCatalog();
            return reserve(new SalesPage(filter(cached,filter),false,cached.size()));
        }
    }
    private void cache(String company,JSONObject page,int position) {
        try {
            JSONArray items=page.getJSONArray("items");
            ArrayList<String> ids=new ArrayList<>(),json=new ArrayList<>();
            for(int i=0;i<items.length();i++) { JSONObject item=items.getJSONObject(i); ids.add(item.getString("id_producto")); json.add(item.toString()); }
            store.saveProducts(company,ids,json,position);
        } catch(JSONException ignored) { }
    }
    private static boolean unfiltered(SalesFilter filter) {
        return blank(filter.search) && blank(filter.size) && blank(filter.siteId) && blank(filter.gender);
    }
    private static List<SalesProduct> filter(List<String> cached,SalesFilter filter) throws SalesException {
        String term=plain(filter.search),gender=plain(filter.gender);
        ArrayList<SalesProduct> result=new ArrayList<>();
        try {
            for(String json:cached) {
                SalesProduct product=RemoteSalesRepository.product(new JSONObject(json));
                if(!term.isEmpty() && !plain(product.name).contains(term)) continue;
                if(!gender.isEmpty() && !gender.equals(plain(product.gender))) continue;
                if(blank(filter.size) && blank(filter.siteId)) { result.add(product); continue; }
                ArrayList<SalesVariant> kept=new ArrayList<>(); int stock=0;
                for(SalesVariant variant:product.variants) {
                    if(!blank(filter.size) && !filter.size.equals(variant.size)) continue;
                    if(!blank(filter.siteId) && !filter.siteId.equals(variant.siteId)) continue;
                    kept.add(variant); stock+=variant.stock;
                }
                if(!kept.isEmpty()) result.add(new SalesProduct(product.id,product.name,product.priceCents,stock,product.gender,product.imageUrl,kept));
            }
        } catch(JSONException error) { throw new SalesException("El catálogo guardado en el celular está dañado. Conéctate para descargarlo de nuevo.",0,false); }
        return result;
    }
    /** Units already sold offline are not on the server yet, so they are taken out of the stock shown. */
    private SalesPage reserve(SalesPage page) {
        Map<String,Integer> reserved=reserved();
        if(reserved.isEmpty()) return page;
        ArrayList<SalesProduct> products=new ArrayList<>();
        for(SalesProduct product:page.items) {
            ArrayList<SalesVariant> variants=new ArrayList<>(); int taken=0;
            for(SalesVariant variant:product.variants) {
                int units=Math.min(variant.stock,reserved.getOrDefault(variant.key(),0)); taken+=units;
                variants.add(new SalesVariant(variant.id,variant.size,variant.siteId,variant.siteName,variant.stock-units));
            }
            products.add(taken==0?product:new SalesProduct(product.id,product.name,product.priceCents,Math.max(0,product.stock-taken),
                    product.gender,product.imageUrl,variants));
        }
        return new SalesPage(products,page.hasMore,page.nextOffset);
    }
    private Map<String,Integer> reserved() {
        HashMap<String,Integer> units=new HashMap<>();
        String company=session.companyId(),user=session.userId();
        if(company==null || user==null) return units;
        for(PendingSale sale:store.list(company,user)) {
            try {
                JSONArray details=new JSONObject(sale.payload).getJSONArray("detalles");
                for(int i=0;i<details.length();i++) {
                    JSONObject item=details.getJSONObject(i);
                    units.merge(item.getString("id_variante")+"|"+item.getString("id_ubicacion_origen"),item.getInt("cantidad"),Integer::sum);
                }
            } catch(JSONException ignored) { }
        }
        return units;
    }

    // --- Sales ---------------------------------------------------------------------------------------------

    @Override public SaleResult submit(SaleDraft draft) throws SalesException {
        if(!online.getAsBoolean()) return enqueue(draft);
        try { return authorized(()->remote.submit(draft)); }
        catch(SalesException error) {
            // No response and nothing confirmed yet (for example, the receipt upload never left the phone).
            if(error.status==0 && !error.uncertain) return enqueue(draft);
            throw error;
        }
    }
    public SaleResult enqueue(SaleDraft draft) throws SalesException {
        String company=company(),user=session.userId();
        ArrayList<String> saved=new ArrayList<>();
        try {
            ArrayList<String> urls=new ArrayList<>();
            for(int i=0;i<draft.payments.size();i++) urls.add("");
            JSONObject body=RemoteSalesRepository.payload(draft,urls);
            JSONArray payments=body.getJSONArray("pagos");
            for(int i=0;i<draft.payments.size();i++) {
                CheckoutLine payment=draft.payments.get(i);
                if(payment.method==0) continue;
                if(payment.receipt.isEmpty()) throw new SalesException("Adjunta el comprobante de cada pago digital.",422,false);
                String path=files.save(draft.id,i,receipts.jpeg(payment.receipt)); saved.add(path);
                payments.getJSONObject(i).remove("url_comprobante");
                payments.getJSONObject(i).put(RECEIPT_FILE,path);
            }
            StringBuilder summary=new StringBuilder();
            for(CartLine item:draft.items) {
                if(summary.length()>0) summary.append(", ");
                summary.append(item.quantity).append(" × ").append(item.product.name).append(" (").append(item.size).append(')');
            }
            store.add(new PendingSale(draft.id,company,user,draft.registerId,body.toString(),summary.toString(),
                    clock.getAsLong(),draft.totalCents(),PendingSale.Status.PENDING,"",0));
        } catch(JSONException|IOException error) {
            for(String path:saved) files.delete(path);
            throw new SalesException("No se pudo guardar la venta en el celular.",0,false);
        } catch(SalesException error) {
            for(String path:saved) files.delete(path);
            throw error;
        }
        publish();
        return new SaleResult(draft.id,draft.totalCents(),true);
    }

    /**
     * Sends the queue oldest first. Returns true when it has to run again later: no connection, the server
     * is unavailable or the session could not be renewed. Sales the server rejects stay on the device,
     * marked for the seller to review, and do not block the ones behind them.
     */
    public synchronized boolean sync() {
        String company=session.companyId(),user=session.userId();
        if(company==null || user==null) return false;
        boolean refreshed=false,again=false;
        List<PendingSale> queue=store.list(company,user);
        for(int i=0;i<queue.size();i++) {
            PendingSale sale=store.find(queue.get(i).id);
            if(sale==null || sale.status!=PendingSale.Status.PENDING) continue;
            try {
                JSONObject body=new JSONObject(sale.payload);
                JSONArray payments=body.getJSONArray("pagos");
                ArrayList<String> uploaded=new ArrayList<>();
                for(int p=0;p<payments.length();p++) {
                    JSONObject payment=payments.getJSONObject(p);
                    if(!payment.has(RECEIPT_FILE)) continue;
                    String path=payment.getString(RECEIPT_FILE);
                    payment.put("url_comprobante",remote.uploadReceipt(files.read(path)));
                    payment.remove(RECEIPT_FILE); uploaded.add(path);
                }
                if(!uploaded.isEmpty()) {
                    // The receipt URL is kept so a later retry does not upload the same photo again.
                    sale=sale.withPayload(body.toString()); store.update(sale);
                    for(String path:uploaded) files.delete(path);
                }
                remote.confirm(body);
                store.delete(sale.id);
                publish();
            } catch(SalesException error) {
                if(error.status==401 && !refreshed) {
                    refreshed=true;
                    if(session.refresh()) { i--; continue; }
                }
                if(error.status==0 || error.status==401 || error.status==429 || error.status>=500) {
                    store.update(sale.with(PendingSale.Status.PENDING,error.getMessage(),sale.attempts+1));
                    again=true; break;
                }
                PendingSale.Status status=error.status==409?PendingSale.Status.CONFLICT:PendingSale.Status.FAILED;
                store.update(sale.with(status,error.getMessage(),sale.attempts+1));
                publish();
            } catch(IOException error) {
                store.update(sale.with(PendingSale.Status.FAILED,"No se encontró la foto del comprobante guardada en el celular.",sale.attempts+1));
                publish();
            } catch(JSONException error) {
                store.update(sale.with(PendingSale.Status.FAILED,"La venta guardada en el celular está dañada.",sale.attempts+1));
                publish();
            }
        }
        publish();
        return again;
    }
    /** Puts a reviewed sale back in the queue, for example after the stock was corrected. */
    public void retry(String id) {
        PendingSale sale=store.find(id);
        if(sale!=null) { store.update(sale.with(PendingSale.Status.PENDING,"",sale.attempts)); publish(); }
    }
    /** Drops a sale the seller decided not to send, together with its stored receipt photos. */
    public void discard(String id) {
        PendingSale sale=store.find(id);
        if(sale==null) return;
        try {
            JSONArray payments=new JSONObject(sale.payload).getJSONArray("pagos");
            for(int i=0;i<payments.length();i++) {
                String path=payments.getJSONObject(i).optString(RECEIPT_FILE,"");
                if(!path.isEmpty()) files.delete(path);
            }
        } catch(JSONException ignored) { }
        store.delete(id); publish();
    }
    public Status status() {
        String company=session.companyId(),user=session.userId();
        if(company==null || user==null) return Status.EMPTY;
        int pending=0; ArrayList<PendingSale> attention=new ArrayList<>();
        for(PendingSale sale:store.list(company,user)) {
            if(sale.status==PendingSale.Status.PENDING) pending++; else attention.add(sale);
        }
        return new Status(pending,attention);
    }
    public void publish() { Listener current=listener; if(current!=null) current.changed(status()); }

    private String company() throws SalesException {
        String company=session.companyId();
        if(company==null || session.userId()==null) throw new SalesException("Tu sesión expiró. Vuelve a iniciar sesión.",401,false);
        return company;
    }
    private interface Call<T> { T run() throws SalesException; }
    /** A seller who started offline may hold an expired token when the signal returns; renew it once. */
    private <T> T authorized(Call<T> call) throws SalesException {
        try { return call.run(); }
        catch(SalesException error) { if(error.status==401 && session.refresh()) return call.run(); throw error; }
    }
    private static boolean unreachable(SalesException error) { return error.status==0; }
    private static SalesException noCatalog() {
        return new SalesException("Sin conexión y sin catálogo guardado. Conéctate una vez para descargarlo.",0,false);
    }
    private static boolean blank(String value) { return value==null || value.trim().isEmpty(); }
    private static String plain(String value) {
        if(value==null) return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").trim().toLowerCase(Locale.ROOT);
    }
}
