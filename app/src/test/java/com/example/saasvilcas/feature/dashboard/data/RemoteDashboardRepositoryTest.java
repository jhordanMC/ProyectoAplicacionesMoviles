package com.example.saasvilcas.feature.dashboard.data;

import com.example.saasvilcas.feature.dashboard.domain.*;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class RemoteDashboardRepositoryTest {
    private static final long TODAY=DashboardFilter.day(2026,9,4);
    private static final DashboardFilter FILTER=DashboardFilter.forPeriod(1,TODAY,TODAY,TODAY);
    private static JSONObject summary() throws Exception {
        return new JSONObject().put("ingresos_periodo",200).put("ganancia_neta_periodo",-40).put("cantidad_ventas",3)
            .put("ticket_promedio",66.67).put("crecimiento_vs_periodo_anterior_pct",-20)
            .put("ingresos_con_costo_periodo",200).put("margen_basado_en_ventas_reales",true)
            .put("hay_gastos_recurrentes_periodo",true).put("hay_gastos_unicos_periodo",false);
    }
    private static final class Fake implements ApiClient {
        final List<String> paths=new ArrayList<>(); String response="[]"; ApiException error; Runnable after;
        public String request(String method,String path,String type,String body,String bearer) throws ApiException {
            assertEquals("GET",method); assertNull(type); assertNull(body); assertEquals("access",bearer);
            paths.add(path); if(after!=null) after.run(); if(error!=null) throw error; return response;
        }
    }
    private RemoteDashboardRepository repository(Fake api) { return new RemoteDashboardRepository(api,()->"access",()->TODAY+12*3600000); }
    @Test public void financialSummaryUsesSelectedDatesAndPreservesLosses() throws Exception {
        Fake api=new Fake(); api.response=summary().toString(); DashboardPanel data=repository(api).load(DashboardSection.SUMMARY,FILTER);
        assertEquals("/finanzas/resumen?desde=2026-09-28&hasta=2026-10-04",api.paths.get(0));
        assertEquals(-40,data.metrics.profit,.001); assertEquals(200,data.metrics.sold,.001); assertEquals(3,data.metrics.saleCount);
        assertEquals(66.67,data.metrics.average,.001); assertTrue(data.metrics.completeCost); assertTrue(data.metrics.recurrentExpenses);
    }
    @Test public void incompleteCostsAreFlaggedInsteadOfClaimingRealProfitForAllSales() throws Exception {
        Fake api=new Fake(); api.response=summary().put("ingresos_con_costo_periodo",50).toString();
        assertFalse(repository(api).load(DashboardSection.SUMMARY,FILTER).metrics.completeCost);
        api.response=summary().put("ingresos_periodo",0).put("ingresos_con_costo_periodo",0).put("margen_basado_en_ventas_reales",false).toString();
        assertTrue(repository(api).load(DashboardSection.SUMMARY,FILTER).metrics.completeCost);
    }
    @Test public void stockValueUsesCurrentInventoryRatherThanPeriodRevenue() {
        Fake api=new Fake(); api.response="{\"valor_inventario_costo\":1250.50}";
        assertEquals(1250.5,repository(api).load(DashboardSection.INVENTORY,FILTER).inventoryValue,.001);
        assertEquals("/dashboard/resumen",api.paths.get(0));
    }
    @Test public void allPeriodEndpointsUseSameRangeAndChartUsesNaturalEnd() {
        Fake api=new Fake(); RemoteDashboardRepository repo=repository(api);
        long wednesday=DashboardFilter.day(2026,8,30); DashboardFilter week=DashboardFilter.forPeriod(1,wednesday,wednesday,wednesday);
        for(DashboardSection section:Arrays.asList(DashboardSection.PRODUCTS,DashboardSection.CATEGORIES,DashboardSection.PAYMENTS)) {
            repo.load(section,week); assertTrue(api.paths.get(api.paths.size()-1).endsWith("desde=2026-09-28&hasta=2026-09-30"));
        }
        repo.load(DashboardSection.TREND,week);
        assertEquals("/dashboard/ventas-ganancia-por-periodo?desde=2026-09-28&hasta=2026-10-04",api.paths.get(api.paths.size()-1));
    }
    @Test public void rankingCountsAndDynamicCategoriesAndPaymentsAreParsed() {
        Fake api=new Fake(); api.response="[{\"id_producto\":\"one\",\"nombre\":\"Motor\",\"unidades\":2}]";
        DashboardPanel panel=repository(api).load(DashboardSection.PRODUCTS,FILTER);
        assertEquals(1,panel.products.size()); assertEquals(Integer.valueOf(2),panel.products.get(0).quantity); assertEquals("",panel.products.get(0).imageUrl);
        api.response="[{\"nombre\":\"Motores\",\"unidades\":2,\"ingresos\":5600}]";
        assertEquals(5600,repository(api).load(DashboardSection.CATEGORIES,FILTER).entries.get(0).amount,.001);
        api.response="[{\"metodo\":\"tarjeta\",\"cantidad\":17,\"monto\":900}]";
        assertEquals(17,repository(api).load(DashboardSection.PAYMENTS,FILTER).entries.get(0).count);
    }
    @Test public void emptyResponsesStayEmptyAndSingleTrendPointCanBeNegative() {
        Fake api=new Fake(); RemoteDashboardRepository repo=repository(api);
        assertTrue(repo.load(DashboardSection.PRODUCTS,FILTER).products.isEmpty()); assertTrue(repo.load(DashboardSection.PAYMENTS,FILTER).entries.isEmpty());
        api.response="[{\"etiqueta\":\"Lun\",\"ingresos\":100,\"ganancia_neta\":-7.50}]";
        assertEquals(100,repo.load(DashboardSection.TREND,FILTER).points.get(0).income,.001);
        assertEquals(-7.5,repo.load(DashboardSection.TREND,FILTER).points.get(0).profit,.001);
    }
    @Test public void recurrentExpensesAreGroupedWithSameMonthlyConversionAsWeb() {
        Fake api=new Fake(); api.response="[{\"tipo_gasto\":\"alquiler\",\"monto\":10,\"frecuencia\":\"semanal\"},"
            +"{\"tipo_gasto\":\"alquiler\",\"monto\":2,\"frecuencia\":\"diario\"},{\"tipo_gasto\":\"servicios\",\"monto\":30,\"frecuencia\":\"mensual\"}]";
        DashboardPanel data=repository(api).load(DashboardSection.EXPENSES,FILTER);
        assertEquals(2,data.entries.size()); assertEquals("alquiler",data.entries.get(0).name); assertEquals(103.3,data.entries.get(0).amount,.001);
    }
    @Test public void catalogLoadsEveryPageSoLowStockIsNotLimitedToFirstPage() {
        List<String> paths=new ArrayList<>();
        ApiClient api=(method,path,type,body,bearer)->{
            paths.add(path); boolean first=path.endsWith("offset=0");
            return "{\"items\":[{\"id_producto\":\""+(first?"first":"second")+"\",\"nombre\":\"Motor\",\"stock_total\":"+(first?50:0)+",\"imagen_url\":null}],"
                +"\"hay_mas\":"+first+",\"siguiente_offset\":"+(first?300:301)+"}";
        };
        DashboardPanel data=new RemoteDashboardRepository(api,()->"access").load(DashboardSection.CATALOG,FILTER);
        assertEquals(2,data.products.size()); assertEquals(Integer.valueOf(0),data.products.get(1).quantity);
        assertEquals("/inventario/productos?limit=300&offset=300",paths.get(1));
    }
    @Test public void invalidPaginationFailsInsteadOfPublishingIncompleteAlerts() {
        Fake api=new Fake(); api.response="{\"items\":[],\"hay_mas\":true,\"siguiente_offset\":0}";
        try { repository(api).load(DashboardSection.CATALOG,FILTER); fail(); } catch(DashboardException error) { assertEquals(502,error.status); }
    }
    @Test public void noSalesHasUnknownStockUntilEnrichedAndReturnsUsesThirtyDays() {
        Fake api=new Fake(); api.response="[{\"id_producto\":\"one\",\"nombre\":\"Motor\"}]";
        assertNull(repository(api).load(DashboardSection.NO_SALES,FILTER).products.get(0).quantity);
        assertEquals("/dashboard/productos-sin-ventas?limit=20",api.paths.get(0));
        api.response="[{\"nombre\":\"Motor\",\"cantidad\":3}]"; DashboardPanel returned=repository(api).load(DashboardSection.RETURNS,FILTER);
        assertEquals(Integer.valueOf(3),returned.products.get(0).quantity);
        assertEquals("/dashboard/top-productos-devueltos?limit=3&desde=2026-09-04&hasta=2026-10-04",api.paths.get(1));
    }
    @Test public void sessionChangesDiscardResponseAndMissingSessionDoesNotCallApi() {
        Fake api=new Fake(); AtomicReference<String> token=new AtomicReference<>("access"); api.after=()->token.set("other");
        try { new RemoteDashboardRepository(api,token::get).load(DashboardSection.PRODUCTS,FILTER); fail(); }
        catch(DashboardException error) { assertEquals(401,error.status); }
        int calls=api.paths.size();
        try { new RemoteDashboardRepository(api,()->null).load(DashboardSection.PRODUCTS,FILTER); fail(); }
        catch(DashboardException error) { assertEquals(401,error.status); } assertEquals(calls,api.paths.size());
    }
    @Test public void authorizationAndNetworkErrorsKeepTheirStatusAndNeverBecomeZeros() {
        Fake api=new Fake();
        for(int status:new int[]{0,401,403,500}) {
            api.error=new ApiException("No disponible",status);
            try { repository(api).load(DashboardSection.INVENTORY,FILTER); fail(); }
            catch(DashboardException error) { assertEquals(status,error.status); }
        }
    }
    @Test public void missingAndNonFiniteMoneyAreRejected() throws Exception {
        Fake api=new Fake();
        for(String response:Arrays.asList("{}",summary().put("ganancia_neta_periodo","NaN").toString())) {
            api.response=response;
            try { repository(api).load(DashboardSection.SUMMARY,FILTER); fail(); }
            catch(DashboardException error) { assertEquals(502,error.status); }
        }
    }
}
