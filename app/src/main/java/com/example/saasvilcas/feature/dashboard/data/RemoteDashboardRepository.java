package com.example.saasvilcas.feature.dashboard.data;

import com.example.saasvilcas.feature.dashboard.domain.*;
import com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Read-only FastAPI adapter. Uses the same finance/ranking sources as Angular. */
public final class RemoteDashboardRepository implements DashboardRepository {
    private final ApiClient api;
    private final Supplier<String> tokens;
    private final LongSupplier clock;
    public RemoteDashboardRepository(ApiClient api,Supplier<String> tokens) { this(api,tokens,System::currentTimeMillis); }
    public RemoteDashboardRepository(ApiClient api,Supplier<String> tokens,LongSupplier clock) {
        this.api=api; this.tokens=tokens; this.clock=clock;
    }
    @Override public DashboardPanel load(DashboardSection section,DashboardFilter filter) {
        String owner=bearer();
        try {
            switch(section) {
                case SUMMARY: return summary(new JSONObject(read("/finanzas/resumen?"+filter.query(),owner)));
                case INVENTORY: return DashboardPanel.inventory(number(new JSONObject(read("/dashboard/resumen",owner)),"valor_inventario_costo"));
                case PRODUCTS: return products(new JSONArray(read("/dashboard/ranking-productos?limit=10&"+filter.query(),owner)),"unidades");
                case CATEGORIES: return entries(new JSONArray(read("/dashboard/top-categorias?limit=5&"+filter.query(),owner)),"nombre","ingresos","unidades");
                case PAYMENTS: return entries(new JSONArray(read("/finanzas/ventas-por-metodo-pago?"+filter.query(),owner)),"metodo","monto","cantidad");
                case EXPENSES: return expenses(new JSONArray(read("/finanzas/gastos-recurrentes",owner)));
                case TREND: return trend(new JSONArray(read("/dashboard/ventas-ganancia-por-periodo?"+filter.chartQuery(),owner)));
                case CATALOG: return catalog(owner);
                case NO_SALES: return products(new JSONArray(read("/dashboard/productos-sin-ventas?limit=20",owner)),null);
                case RETURNS:
                    long today=DashboardFilter.today(clock.getAsLong());
                    Calendar start=Calendar.getInstance(TimeZone.getTimeZone("UTC")); start.setTimeInMillis(today); start.add(Calendar.DAY_OF_MONTH,-30);
                    return products(new JSONArray(read("/dashboard/top-productos-devueltos?limit=3&desde="
                            +DashboardFilter.iso(start.getTimeInMillis())+"&hasta="+DashboardFilter.iso(today),owner)),"cantidad");
                default: throw new IllegalArgumentException("Sección desconocida.");
            }
        } catch(JSONException|NumberFormatException error) {
            throw new DashboardException("No se pudieron interpretar los datos de esta sección.",502);
        }
    }
    private String bearer() {
        String value=tokens.get();
        if(value==null||value.isEmpty()) throw new DashboardException("Tu sesión expiró. Vuelve a iniciar sesión.",401);
        return value;
    }
    private String read(String path,String owner) {
        ensureSession(owner);
        try {
            String response=api.request("GET",path,null,null,owner);
            ensureSession(owner); return response;
        } catch(ApiException error) { throw new DashboardException(error.getMessage(),error.status); }
    }
    private void ensureSession(String owner) {
        if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        if(!owner.equals(bearer())) throw new DashboardException("La sesión cambió. Vuelve a iniciar sesión.",401);
    }
    private DashboardPanel summary(JSONObject value) throws JSONException {
        double sold=number(value,"ingresos_periodo"),covered=number(value,"ingresos_con_costo_periodo");
        boolean complete=sold==0 || (value.getBoolean("margen_basado_en_ventas_reales") && covered+0.005>=sold);
        return DashboardPanel.metrics(new Metrics(number(value,"ganancia_neta_periodo"),sold,number(value,"ticket_promedio"),
                number(value,"crecimiento_vs_periodo_anterior_pct"),count(value,"cantidad_ventas"),
                value.getBoolean("hay_gastos_recurrentes_periodo"),value.getBoolean("hay_gastos_unicos_periodo"),complete));
    }
    private DashboardPanel products(JSONArray values,String quantity) throws JSONException {
        List<Product> products=new ArrayList<>();
        for(int i=0;i<values.length();i++) {
            JSONObject value=values.getJSONObject(i);
            products.add(new Product(text(value,"id_producto"),value.getString("nombre"),quantity==null?null:count(value,quantity),text(value,"imagen_url")));
        }
        return DashboardPanel.products(products);
    }
    private DashboardPanel catalog(String owner) throws JSONException {
        Map<String,Product> products=new LinkedHashMap<>(); int offset=0;
        for(int pageNumber=0;pageNumber<100;pageNumber++) {
            JSONObject page=new JSONObject(read("/inventario/productos?limit=300&offset="+offset,owner));
            JSONArray items=page.getJSONArray("items");
            for(int i=0;i<items.length();i++) {
                JSONObject item=items.getJSONObject(i);
                Product product=new Product(item.getString("id_producto"),item.getString("nombre"),count(item,"stock_total"),text(item,"imagen_url"));
                products.put(product.id,product);
            }
            if(!page.getBoolean("hay_mas")) return DashboardPanel.products(new ArrayList<>(products.values()));
            int next=page.getInt("siguiente_offset");
            if(items.length()==0 || next<=offset) throw new JSONException("Invalid pagination");
            offset=next;
        }
        throw new DashboardException("El catálogo es demasiado grande para cargar las alertas. Inténtalo más tarde.",502);
    }
    private DashboardPanel entries(JSONArray values,String label,String amount,String count) throws JSONException {
        List<Entry> entries=new ArrayList<>();
        for(int i=0;i<values.length();i++) {
            JSONObject value=values.getJSONObject(i);
            double total=number(value,amount); if(total<0) throw new JSONException("Negative amount");
            entries.add(new Entry(value.getString(label),total,count(value,count)));
        }
        return DashboardPanel.entries(entries);
    }
    private DashboardPanel expenses(JSONArray values) throws JSONException {
        Map<String,BigDecimal> totals=new LinkedHashMap<>();
        for(int i=0;i<values.length();i++) {
            JSONObject value=values.getJSONObject(i); String frequency=value.getString("frecuencia");
            BigDecimal multiplier;
            switch(frequency) {
                case "mensual": multiplier=BigDecimal.ONE; break;
                case "semanal": multiplier=new BigDecimal("4.33"); break;
                case "diario": multiplier=new BigDecimal("30"); break;
                default: throw new JSONException("Invalid frequency");
            }
            double amount=number(value,"monto"); if(amount<0) throw new JSONException("Negative expense");
            String type=value.getString("tipo_gasto");
            totals.put(type,totals.getOrDefault(type,BigDecimal.ZERO).add(BigDecimal.valueOf(amount).multiply(multiplier)));
        }
        List<Entry> entries=new ArrayList<>();
        for(Map.Entry<String,BigDecimal> total:totals.entrySet()) entries.add(new Entry(total.getKey(),total.getValue().doubleValue(),0));
        entries.sort((a,b)->Double.compare(b.amount,a.amount)); return DashboardPanel.entries(entries);
    }
    private DashboardPanel trend(JSONArray values) throws JSONException {
        List<Point> points=new ArrayList<>();
        for(int i=0;i<values.length();i++) {
            JSONObject value=values.getJSONObject(i); points.add(new Point(value.getString("etiqueta"),number(value,"ingresos"),number(value,"ganancia_neta")));
        }
        return DashboardPanel.trend(points);
    }
    private double number(JSONObject object,String field) throws JSONException {
        if(object.isNull(field)) throw new JSONException("Missing numeric field");
        double value=Double.parseDouble(object.get(field).toString());
        if(!Double.isFinite(value)) throw new JSONException("Invalid numeric field"); return value;
    }
    private int count(JSONObject object,String field) throws JSONException {
        int count=object.getInt(field); if(count<0) throw new JSONException("Negative count"); return count;
    }
    private String text(JSONObject object,String field) { return object.isNull(field)?"":object.optString(field,""); }
}
