package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.feature.sales.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * HU-19. Compares the company's latest sales with the ones already known on this phone. Sales made offline
 * reach the list when they sync, so their notice arrives then. The first check only records the history:
 * the owner is notified of what happens from that moment on, not of every past sale.
 */
public final class SaleNoticeRepository {
    public interface Session {
        String companyId();
        String userId();
        String token();
        boolean owner();
        boolean refresh();
        boolean baselineDone();
        void markBaseline();
    }
    public interface Listener { void changed(List<SaleNotice> notices); }
    private static final int KEEP=200;
    private final ApiClient api;
    private final SaleNoticeStore store;
    private final Session session;
    private volatile Listener listener;
    public SaleNoticeRepository(ApiClient api,SaleNoticeStore store,Session session) { this.api=api; this.store=store; this.session=session; }
    public void setListener(Listener listener) { this.listener=listener; }

    /** Returns the sales that are new for this phone; connection problems simply wait for the next check. */
    public synchronized List<SaleNotice> check() {
        ArrayList<SaleNotice> fresh=new ArrayList<>();
        String company=session.companyId(),user=session.userId();
        if(company==null || user==null || !session.owner()) { publish(); return fresh; }
        boolean baseline=!session.baselineDone();
        try {
            JSONArray items=new JSONObject(get("/ventas?estado=pagada&limit=50")).getJSONArray("items");
            for(int i=items.length()-1;i>=0;i--) {
                JSONObject item=items.getJSONObject(i);
                String id=item.getString("id_venta");
                if(store.findNotice(id,user)!=null) continue;
                SaleNotice notice=new SaleNotice(id,company,user,"",item.isNull("nombre_vendedor")?"":item.getString("nombre_vendedor"),"","",
                        cents(item.get("total")),time(item.getString("creado_en")),baseline);
                if(!baseline) { notice=enrich(notice); fresh.add(notice); }
                store.saveNotice(notice);
            }
            if(baseline) session.markBaseline();
            store.trimNotices(company,user,KEEP);
        } catch(ApiException|JSONException|RuntimeException ignored) { }
        publish();
        return fresh;
    }
    /** Marks a notice as read and completes its detail if it was recorded without one. */
    public synchronized void open(String id) {
        String user=session.userId();
        SaleNotice notice=user==null?null:store.findNotice(id,user);
        if(notice==null) return;
        notice=notice.asRead();
        if(!notice.hasDetail()) notice=enrich(notice);
        store.saveNotice(notice);
        publish();
    }
    public synchronized void markAllRead() {
        String company=session.companyId(),user=session.userId();
        if(company==null || user==null) return;
        store.markAllRead(company,user);
        publish();
    }
    /** Called when the user signs out, so the next sign-in starts from a new history. */
    public synchronized void forget(String company,String user) { store.clearNotices(company,user); publish(); }
    public void publish() {
        Listener current=listener;
        if(current==null) return;
        String company=session.companyId(),user=session.userId();
        current.changed(company==null || user==null?Collections.emptyList():store.notices(company,user));
    }
    private SaleNotice enrich(SaleNotice notice) {
        try {
            JSONObject sale=new JSONObject(get("/ventas/"+notice.id));
            JSONArray details=sale.getJSONArray("detalles"),pays=sale.getJSONArray("pagos");
            String site=""; StringBuilder summary=new StringBuilder(),payments=new StringBuilder();
            for(int i=0;i<details.length();i++) {
                JSONObject item=details.getJSONObject(i);
                if(site.isEmpty()) site=item.isNull("nombre_local")?(item.isNull("nombre_almacen")?"":item.getString("nombre_almacen")):item.getString("nombre_local");
                if(summary.length()>0) summary.append(", ");
                summary.append(item.getInt("cantidad")).append(" × ").append(item.isNull("nombre_producto")?"Producto":item.getString("nombre_producto"));
                if(!item.isNull("talla")) summary.append(" (").append(item.getString("talla")).append(')');
            }
            for(int i=0;i<pays.length();i++) {
                JSONObject pay=pays.getJSONObject(i); String method=pay.getString("metodo");
                if(payments.length()>0) payments.append(", ");
                payments.append(method.substring(0,1).toUpperCase(Locale.ROOT)).append(method.substring(1))
                        .append(" S/ ").append(BigDecimal.valueOf(cents(pay.get("monto")),2).toPlainString());
            }
            return notice.withDetail(site,summary.toString(),payments.toString());
        } catch(ApiException|JSONException|RuntimeException error) { return notice; }
    }
    private String get(String path) throws ApiException {
        String token=session.token();
        if(token==null || token.isEmpty()) throw new ApiException("Sin sesión",401);
        try { return api.request("GET",path,null,null,token); }
        catch(ApiException error) {
            if(error.status==401 && session.refresh()) return api.request("GET",path,null,null,session.token());
            throw error;
        }
    }
    private static long cents(Object value) { return new BigDecimal(value.toString()).setScale(2,java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact(); }
    /** ISO 8601 from the API; java.time is not available at minSdk 24. */
    static long time(String value) {
        String clean=value.replaceFirst("\\.\\d+","").replace("Z","+00:00");
        if(!clean.matches(".*[+-]\\d{2}:\\d{2}$")) clean+="+00:00";
        try { return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).parse(clean).getTime(); }
        catch(ParseException error) { return System.currentTimeMillis(); }
    }
}
