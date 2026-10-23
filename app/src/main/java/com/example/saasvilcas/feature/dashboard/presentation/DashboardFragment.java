package com.example.saasvilcas.feature.dashboard.presentation;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.R;
import com.example.saasvilcas.VilcasApplication;
import com.example.saasvilcas.app.FrontendNavigator;
import com.example.saasvilcas.feature.dashboard.domain.*;
import com.example.saasvilcas.feature.dashboard.domain.DashboardPanel.*;
import com.example.saasvilcas.shared.ui.*;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.*;

/** Presentation only: existing XML cards receive real, independently loaded data. */
public final class DashboardFragment extends Fragment {
    private DashboardViewModel model;
    private long fromDay,toDay;
    private final EnumMap<DashboardSection,DashboardViewModel.PanelState> rendered=new EnumMap<>(DashboardSection.class);
    private final int[] periodIds={R.id.dashboardPeriod0,R.id.dashboardPeriod1,R.id.dashboardPeriod2,R.id.dashboardPeriod3,R.id.dashboardPeriod4};
    public DashboardFragment() { super(R.layout.fragment_dashboard); }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        com.example.saasvilcas.shared.ui.FilterPanelController.bind(this,view,"dashboard");
        VilcasApplication app=(VilcasApplication)requireActivity().getApplication();
        model=new ViewModelProvider(requireParentFragment(),DashboardViewModel.factory(app.newDashboardRepository())).get(DashboardViewModel.class);
        rendered.clear();
        if(saved!=null) {
            long today=DashboardFilter.today(System.currentTimeMillis());
            model.restoreFilter(DashboardFilter.forPeriod(saved.getInt("period",0),today,saved.getLong("fromDay",today),saved.getLong("toDay",today)));
        }
        fromDay=model.filter().fromDay; toDay=model.filter().toDay;
        Spinner channel=view.findViewById(R.id.dashboardChannel);
        List<String> channels=Arrays.asList(getResources().getStringArray(R.array.dashboard_channels));
        int selected=saved==null?0:Math.max(0,Math.min(channels.size()-1,saved.getInt("channel",0)));
        FilterFieldAdapter.bind(channel,channels,channels.get(selected));
        view.findViewById(R.id.dashboardDateFrom).setOnClickListener(v->selectDate(view,true));
        view.findViewById(R.id.dashboardDateTo).setOnClickListener(v->selectDate(view,false));
        for(int i=0;i<periodIds.length;i++) {
            final int period=i;
            view.findViewById(periodIds[i]).setOnClickListener(v->{
                DashboardFilter filter=DashboardFilter.forPeriod(period,DashboardFilter.today(System.currentTimeMillis()),fromDay,toDay);
                model.select(filter); fromDay=filter.fromDay; toDay=filter.toDay; bindDates(view); bindPeriods(view);
            });
        }
        view.findViewById(R.id.dashboardMoreProducts).setOnClickListener(v->{ model.toggleExpanded(); bindProducts(view,model.state.getValue(),true); });
        view.findViewById(R.id.dashboardProducts).addOnLayoutChangeListener((grid,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol) bindProducts(view,model.state.getValue(),false);
        });
        createAlerts(view); adaptBreakdown(view,R.id.categoriesChart,R.id.categoriesLegend); adaptBreakdown(view,R.id.expensesChart,R.id.expensesLegend);
        for(int metricId:new int[]{R.id.dashboardSold,R.id.dashboardAverage,R.id.dashboardInvested}) adaptMetric(view.findViewById(metricId));
        adaptProfit(view);
        model.state.observe(getViewLifecycleOwner(),state->{
            if(state.expired) { ((FrontendNavigator)requireActivity()).showLogin(); return; }
            for(DashboardSection section:DashboardSection.values()) {
                DashboardViewModel.PanelState previous=rendered.get(section),current=state.panels.get(section);
                if(previous==current) continue;
                rendered.put(section,current);
                boolean animate=current.data!=null && (previous==null||previous.data==null);
                switch(section) {
                    case SUMMARY: bindSummary(view,current); break;
                    case INVENTORY: bindInventory(view,current); break;
                    case PRODUCTS: bindProducts(view,state,animate); break;
                    case CATEGORIES: bindBreakdown(view,current,R.id.dashboardCategoriesCard,R.id.categoriesChart,R.id.categoriesLegend,false); break;
                    case EXPENSES: bindBreakdown(view,current,R.id.dashboardExpensesCard,R.id.expensesChart,R.id.expensesLegend,true); break;
                    case PAYMENTS: bindPayments(view,current); break;
                    case TREND: bindTrend(view,current); break;
                    case NO_SALES: bindAlert(view,state,R.id.dashboardNoSalesCard,section,false); break;
                    case RETURNS: bindAlert(view,state,R.id.dashboardReturnsCard,section,true); break;
                    case CATALOG:
                        bindAlert(view,state,R.id.dashboardStockCard,section,false);
                        bindProducts(view,state,false);
                        bindAlert(view,state,R.id.dashboardNoSalesCard,DashboardSection.NO_SALES,false);
                        bindAlert(view,state,R.id.dashboardReturnsCard,DashboardSection.RETURNS,true); break;
                }
            }
        });
        bindDates(view); bindPeriods(view);
    }
    @Override public void onStart() {
        super.onStart(); model.reload();
        fromDay=model.filter().fromDay; toDay=model.filter().toDay;
        bindDates(requireView()); bindPeriods(requireView());
    }
    private void bindSummary(View view,DashboardViewModel.PanelState state) {
        View card=view.findViewById(R.id.dashboardProfitCard);
        status(card.findViewById(R.id.dashboardPanelStatus),state,DashboardSection.SUMMARY,0);
        Metrics data=state.data==null?null:state.data.metrics;
        label(view,R.id.dashboardProfit,data==null?"—":money(data.profit));
        View badge=view.findViewById(R.id.dashboardProfitBadge),growth=view.findViewById(R.id.dashboardGrowth),note=view.findViewById(R.id.dashboardExpenseNote);
        badge.setVisibility(data==null?View.GONE:View.VISIBLE); growth.setVisibility(data==null?View.GONE:View.VISIBLE); note.setVisibility(data==null?View.GONE:View.VISIBLE);
        String pending=state.loading?getString(R.string.dashboard_loading):state.error;
        bindMetric(view.findViewById(R.id.dashboardSold),R.string.dashboard_total_sold,data==null?null:data.sold,
                data==null?pending:getString(R.string.dashboard_sale_note,data.saleCount),R.drawable.company_ic_sales);
        bindMetric(view.findViewById(R.id.dashboardAverage),R.string.dashboard_average_sale,data==null?null:data.average,
                data==null?pending:getString(R.string.dashboard_average_note),R.drawable.company_ic_sales);
        if(data==null) return;
        int color=!data.completeCost?0xffAD7900:data.profit<0?0xffEF4444:0xff008B43;
        label(view,R.id.dashboardProfitBadge,getString(!data.completeCost?R.string.dashboard_partial_profit:
                data.profit>0?R.string.dashboard_profitable:data.profit<0?R.string.dashboard_losing:R.string.dashboard_break_even));
        ((TextView)badge).setTextColor(color); ((TextView)view.findViewById(R.id.dashboardProfit)).setTextColor(color);
        label(view,R.id.dashboardGrowth,getString(R.string.dashboard_growth_real,data.growth>=0?"↑ +":"↓ ",data.growth));
        ((TextView)growth).setTextColor(data.growth<0?0xffEF4444:0xff008B43);
        int expenses=data.recurrentExpenses?(data.uniqueExpenses?R.string.dashboard_expenses_both:R.string.dashboard_expenses_recurring):
                data.uniqueExpenses?R.string.dashboard_expenses_unique:R.string.dashboard_expenses_none;
        label(view,R.id.dashboardExpenseNote,getString(expenses)+(data.completeCost?"":"\n"+getString(R.string.dashboard_partial_cost_note)));
    }
    private void bindInventory(View view,DashboardViewModel.PanelState state) {
        String note=state.data!=null?getString(R.string.dashboard_invested_note):state.loading?getString(R.string.dashboard_loading):state.error;
        bindMetric(view.findViewById(R.id.dashboardInvested),R.string.dashboard_invested,state.data==null?null:state.data.inventoryValue,note,R.drawable.company_ic_inventory);
        // The inventory metric has its own retry without duplicating the summary's error.
        status(view.findViewById(R.id.dashboardMetricsCard).findViewById(R.id.dashboardPanelStatus),state,DashboardSection.INVENTORY,0);
    }
    private void bindMetric(View row,int title,Double value,String note,int icon) {
        label(row,R.id.metricTitle,getString(title)); label(row,R.id.metricValue,value==null?"—":money(value)); label(row,R.id.metricNote,note);
        ((ImageView)row.findViewById(R.id.metricIcon)).setImageResource(icon);
    }
    private void bindProducts(View view,DashboardViewModel.State state,boolean animate) {
        DashboardViewModel.PanelState panel=state.panels.get(DashboardSection.PRODUCTS);
        status(view.findViewById(R.id.dashboardProductsCard).findViewById(R.id.dashboardPanelStatus),panel,DashboardSection.PRODUCTS,
                panel.data!=null&&panel.data.products.isEmpty()?R.string.dashboard_no_sales_period:0);
        GridLayout grid=view.findViewById(R.id.dashboardProducts); grid.removeAllViews();
        List<Product> products=panel.data==null?Collections.emptyList():panel.data.products;
        int columns=ResponsiveGrid.columns(grid,12); grid.setColumnCount(columns);
        int count=model.isExpanded()?products.size():Math.min(4,products.size());
        for(int i=0;i<count;i++) {
            Product product=products.get(i); View item=getLayoutInflater().inflate(R.layout.dashboard_item_product,grid,false);
            label(item,R.id.productName,product.name); label(item,R.id.productQuantity,getString(R.string.dashboard_units,product.quantity)); bindPhoto(item,metadata(state,product));
            GridLayout.LayoutParams params=new GridLayout.LayoutParams(); params.width=0; params.height=GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec=GridLayout.spec(i%columns,1f); params.rowSpec=GridLayout.spec(i/columns);
            params.setMargins(i%columns==0?0:dp(6),0,i%columns==columns-1?0:dp(6),dp(12)); grid.addView(item,params);
        }
        view.findViewById(R.id.dashboardMoreProducts).setVisibility(products.size()>4?View.VISIBLE:View.GONE);
        label(view,R.id.dashboardMoreProducts,getString(model.isExpanded()?R.string.dashboard_fewer:R.string.dashboard_more));
        if(animate&&!products.isEmpty()) ViewEntranceAnimator.play(grid);
    }
    private Product metadata(DashboardViewModel.State state,Product product) {
        DashboardViewModel.PanelState catalog=state.panels.get(DashboardSection.CATALOG);
        if(catalog.data==null) return product;
        Product match=null;
        for(Product item:catalog.data.products) {
            if(!product.id.isEmpty() && item.id.equals(product.id)) return item;
            if(product.id.isEmpty() && item.name.equals(product.name)) { if(match!=null) return product; match=item; }
        }
        return match==null?product:match;
    }
    private void bindPhoto(View item,Product product) {
        boolean image=!product.imageUrl.isEmpty();
        item.findViewById(R.id.dashboardProductNoImage).setVisibility(image?View.GONE:View.VISIBLE);
        ProductImageLoader.load(item.findViewById(R.id.dashboardProductImage),product.imageUrl);
    }
    private void bindBreakdown(View view,DashboardViewModel.PanelState panel,int cardId,int chartId,int legendId,boolean expenses) {
        List<Entry> entries=panel.data==null?Collections.emptyList():panel.data.entries;
        status(view.findViewById(cardId).findViewById(R.id.dashboardPanelStatus),panel,expenses?DashboardSection.EXPENSES:DashboardSection.CATEGORIES,
                panel.data!=null&&entries.isEmpty()?(expenses?R.string.dashboard_no_expenses:R.string.dashboard_no_categories):0);
        double[] amounts=new double[entries.size()]; LinearLayout legend=view.findViewById(legendId); legend.removeAllViews();
        for(int i=0;i<entries.size();i++) {
            Entry entry=entries.get(i); amounts[i]=entry.amount;
            View row=getLayoutInflater().inflate(R.layout.dashboard_item_breakdown,legend,false);
            row.findViewById(R.id.breakdownColor).setBackgroundColor(DashboardChartView.segmentColor(i));
            label(row,R.id.breakdownLabel,expenses?expenseName(entry.name):entry.name); label(row,R.id.breakdownValue,money(entry.amount)); legend.addView(row);
        }
        DashboardChartView chart=view.findViewById(chartId); chart.showDonut(amounts);
        StringBuilder description=new StringBuilder();
        for(Entry entry:entries) description.append(expenses?expenseName(entry.name):entry.name).append(": ").append(money(entry.amount)).append("; ");
        chart.setContentDescription(description.toString());
    }
    private void adaptBreakdown(View view,int chartId,int legendId) {
        View chart=view.findViewById(chartId); LinearLayout group=(LinearLayout)chart.getParent(); View legend=view.findViewById(legendId);
        group.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            boolean compact=r-l<dp(240)||(getResources().getConfiguration().fontScale>1.2f&&r-l<dp(350));
            int orientation=compact?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL;
            if(group.getOrientation()==orientation) return;
            group.setOrientation(orientation);
            LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)legend.getLayoutParams();
            params.width=compact?LinearLayout.LayoutParams.MATCH_PARENT:0; params.weight=compact?0:1;
            params.setMarginStart(compact?0:dp(12)); legend.setLayoutParams(params);
        });
    }
    private void adaptMetric(View metric) {
        LinearLayout row=(LinearLayout)metric;
        LinearLayout values=(LinearLayout)row.findViewById(R.id.metricValue).getParent();
        View note=row.findViewById(R.id.metricNote);
        row.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            boolean compact=r-l<dp(280)||getResources().getConfiguration().fontScale>1.2f;
            if((note.getParent()==values)==compact) return;
            ((android.view.ViewGroup)note.getParent()).removeView(note);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(compact?LinearLayout.LayoutParams.MATCH_PARENT:dp(72),LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(compact?0:dp(6)); note.setLayoutParams(params);
            if(compact) values.addView(note); else row.addView(note);
        });
    }
    private void adaptProfit(View view) {
        View profit=view.findViewById(R.id.dashboardProfit),growth=view.findViewById(R.id.dashboardGrowth);
        LinearLayout row=(LinearLayout)profit.getParent();
        row.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            boolean compact=r-l<dp(280)||getResources().getConfiguration().fontScale>1.2f;
            int orientation=compact?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL;
            if(row.getOrientation()==orientation) return;
            row.setOrientation(orientation);
            LinearLayout.LayoutParams value=(LinearLayout.LayoutParams)profit.getLayoutParams();
            value.width=compact?LinearLayout.LayoutParams.MATCH_PARENT:0; value.weight=compact?0:1; profit.setLayoutParams(value);
            LinearLayout.LayoutParams comparison=(LinearLayout.LayoutParams)growth.getLayoutParams();
            comparison.width=compact?LinearLayout.LayoutParams.MATCH_PARENT:LinearLayout.LayoutParams.WRAP_CONTENT;
            comparison.topMargin=compact?dp(8):0; growth.setLayoutParams(comparison);
            ((TextView)growth).setGravity(compact?android.view.Gravity.START:android.view.Gravity.CENTER);
        });
    }
    private void bindPayments(View view,DashboardViewModel.PanelState panel) {
        List<Entry> entries=panel.data==null?Collections.emptyList():panel.data.entries;
        status(view.findViewById(R.id.dashboardPaymentsCard).findViewById(R.id.dashboardPanelStatus),panel,DashboardSection.PAYMENTS,
                panel.data!=null&&entries.isEmpty()?R.string.dashboard_no_payments:0);
        LinearLayout list=view.findViewById(R.id.dashboardPayments); list.removeAllViews(); int max=1;
        for(Entry entry:entries) max=Math.max(max,entry.count);
        for(Entry entry:entries) {
            View row=getLayoutInflater().inflate(R.layout.dashboard_item_payment,list,false);
            label(row,R.id.paymentLabel,paymentName(entry.name)); label(row,R.id.paymentCount,String.valueOf(entry.count));
            LinearProgressIndicator progress=row.findViewById(R.id.paymentProgress); progress.setMax(max); progress.setProgress(entry.count);
            row.setContentDescription(paymentName(entry.name)+": "+entry.count+", "+money(entry.amount)); list.addView(row);
        }
    }
    private void bindTrend(View view,DashboardViewModel.PanelState panel) {
        List<Point> points=panel.data==null?Collections.emptyList():panel.data.points;
        status(view.findViewById(R.id.dashboardTrendStatus),panel,DashboardSection.TREND,panel.data!=null&&points.isEmpty()?R.string.dashboard_no_trend:0);
        double[] values=new double[points.size()], incomes=new double[points.size()]; StringBuilder description=new StringBuilder();
        LinearLayout labels=view.findViewById(R.id.dashboardChartLabels); labels.removeAllViews();
        DashboardChartView chart=view.findViewById(R.id.dashboardProfitChart);
        int columnWidth=dp(56);
        for(int i=0;i<points.size();i++) {
            Point point=points.get(i); values[i]=point.profit; incomes[i]=point.income;
            description.append(point.label).append(": Ventas ").append(money(point.income)).append(", Ganancia real ").append(money(point.profit)).append("; ");
            TextView label=(TextView)getLayoutInflater().inflate(R.layout.dashboard_item_chart_label,labels,false);
            label.setText(point.label); label.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); labels.addView(label);
        }
        int width=Math.max(dp(240),dp(64)+columnWidth*points.size());
        chart.getLayoutParams().width=width; chart.requestLayout();
        labels.getLayoutParams().width=width; labels.requestLayout();
        labels.setPadding(dp(64),0,0,0);
        chart.showBars(incomes,values); chart.setContentDescription(description.toString());
        final int minimumChartWidth=dp(64)+columnWidth*points.size();
        chart.post(()->{
            if(!chart.isAttachedToWindow()||chart.getParent()==null||chart.getParent().getParent()==null) return;
            View viewport=(View)chart.getParent().getParent();
            int responsiveWidth=Math.max(viewport.getWidth(),minimumChartWidth);
            if(chart.getLayoutParams().width!=responsiveWidth) {
                chart.getLayoutParams().width=responsiveWidth; chart.requestLayout();
                labels.getLayoutParams().width=responsiveWidth; labels.requestLayout();
            }
        });

    }
    private void createAlerts(View view) {
        LinearLayout alerts=view.findViewById(R.id.dashboardAlerts); alerts.removeAllViews();
        int[] ids={R.id.dashboardNoSalesCard,R.id.dashboardStockCard,R.id.dashboardReturnsCard};
        int[] titles={R.string.dashboard_alert_no_sales,R.string.dashboard_alert_low_stock,R.string.dashboard_alert_returns};
        int[] notes={R.string.dashboard_never_sold_note,R.string.dashboard_stock_note,R.string.dashboard_returns_note_real};
        int[] colors={0xFFFF4C5A,0xFFFFAA00,0xFFA362FF},backgrounds={0xFFFFF0F2,0xFFFFF6DE,0xFFF5EDFF};
        for(int i=0;i<ids.length;i++) {
            View card=getLayoutInflater().inflate(R.layout.dashboard_component_alert,alerts,false); card.setId(ids[i]);
            GradientDrawable background=new GradientDrawable(); background.setColor(backgrounds[i]); background.setCornerRadius(dp(20)); background.setStroke(dp(1),colors[i]); card.setBackground(background);
            TextView title=card.findViewById(R.id.alertTitle); title.setText(titles[i]); title.setTextColor(colors[i]); title.setCompoundDrawableTintList(ColorStateList.valueOf(colors[i]));
            label(card,R.id.alertDescription,getString(notes[i])); alerts.addView(card);
        }
    }
    private void bindAlert(View view,DashboardViewModel.State state,int cardId,DashboardSection section,boolean returns) {
        DashboardViewModel.PanelState panel=state.panels.get(section);
        List<Product> products=new ArrayList<>();
        if(panel.data!=null) {
            for(Product product:panel.data.products) if(section!=DashboardSection.CATALOG || (product.quantity!=null&&product.quantity<10)) products.add(product);
            if(section==DashboardSection.CATALOG) products.sort((a,b)->Integer.compare(a.quantity,b.quantity));
        }
        View card=view.findViewById(cardId); int empty=section==DashboardSection.CATALOG?R.string.dashboard_no_alerts_stock:returns?R.string.dashboard_no_alerts_returns:R.string.dashboard_no_alerts_sales;
        status(card.findViewById(R.id.dashboardPanelStatus),panel,section,panel.data!=null&&products.isEmpty()?empty:0);
        LinearLayout rows=card.findViewById(R.id.alertProducts); rows.removeAllViews();
        for(int i=0;i<Math.min(3,products.size());i++) {
            Product product=products.get(i),details=metadata(state,product);
            View row=getLayoutInflater().inflate(R.layout.dashboard_item_alert_product,rows,false); label(row,R.id.alertProductName,product.name); bindPhoto(row,details);
            Integer quantity=section==DashboardSection.NO_SALES?details.quantity:product.quantity;
            label(row,R.id.alertProductCount,quantity==null?getString(R.string.dashboard_stock_unavailable):getString(returns?R.string.dashboard_returns:R.string.dashboard_units,quantity));
            ((TextView)row.findViewById(R.id.alertProductCount)).setTextColor(section==DashboardSection.NO_SALES?0xffFF4C5A:returns?0xffA362FF:0xffD28B00); rows.addView(row);
        }
    }
    private void status(View status,DashboardViewModel.PanelState panel,DashboardSection section,int emptyMessage) {
        boolean visible=panel.loading||!panel.error.isEmpty()||emptyMessage!=0; status.setVisibility(visible?View.VISIBLE:View.GONE);
        status.findViewById(R.id.dashboardPanelLoading).setVisibility(panel.loading?View.VISIBLE:View.GONE);
        label(status,R.id.dashboardPanelMessage,panel.loading?getString(R.string.dashboard_loading):!panel.error.isEmpty()?panel.error:emptyMessage==0?"":getString(emptyMessage));
        View retry=status.findViewById(R.id.dashboardPanelRetry); retry.setVisibility(!panel.error.isEmpty()&&panel.status!=403?View.VISIBLE:View.GONE);
        retry.setOnClickListener(v->model.retry(section));
    }
    private void bindPeriods(View view) {
        view.findViewById(R.id.dashboardDateFields).setVisibility(model.filter().period==4?View.VISIBLE:View.GONE);
        for(int i=0;i<periodIds.length;i++) {
            TextView period=view.findViewById(periodIds[i]); boolean selected=i==model.filter().period;
            period.setSelected(selected); period.setTextColor(selected?0xffFFFFFF:0xff4B5A63);
            androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(period,ColorStateList.valueOf(selected?0xffFFFFFF:0xff4B5A63));
        }
        View selected=view.findViewById(periodIds[model.filter().period]);
        HorizontalScrollView tabs=(HorizontalScrollView)selected.getParent().getParent();
        tabs.post(()->{
            if(!selected.isAttachedToWindow()) return;
            if(selected.getLeft()<tabs.getScrollX()) tabs.smoothScrollTo(selected.getLeft(),0);
            else if(selected.getRight()>tabs.getScrollX()+tabs.getWidth()-tabs.getPaddingEnd())
                tabs.smoothScrollTo(selected.getRight()-tabs.getWidth()+tabs.getPaddingEnd(),0);
        });
    }
    private void bindDates(View view) {
        SimpleDateFormat dates=new SimpleDateFormat("dd/MM/yyyy",new Locale("es","PE")); dates.setTimeZone(TimeZone.getTimeZone("UTC"));
        label(view,R.id.dashboardDateFrom,dates.format(new Date(fromDay))); label(view,R.id.dashboardDateTo,dates.format(new Date(toDay)));
    }
    private long deviceDate(long day) {
        Calendar utc=Calendar.getInstance(TimeZone.getTimeZone("UTC")); utc.setTimeInMillis(day);
        Calendar device=Calendar.getInstance(); device.clear(); device.set(utc.get(Calendar.YEAR),utc.get(Calendar.MONTH),utc.get(Calendar.DAY_OF_MONTH)); return device.getTimeInMillis();
    }
    private void selectDate(View view,boolean from) {
        Calendar date=Calendar.getInstance(TimeZone.getTimeZone("UTC")); date.setTimeInMillis(from?fromDay:toDay);
        android.app.DatePickerDialog picker=new android.app.DatePickerDialog(requireContext(),R.style.VilcasFilter_DatePickerTheme,(widget,year,month,day)->{
            if(getView()!=view) return;
            long selected=DashboardFilter.day(year,month,day); if(from) fromDay=selected; else toDay=selected;
            bindDates(view); model.select(DashboardFilter.forPeriod(4,DashboardFilter.today(System.currentTimeMillis()),fromDay,toDay));
        },date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH));
        if(from) picker.getDatePicker().setMaxDate(deviceDate(toDay)); else picker.getDatePicker().setMinDate(deviceDate(fromDay)); picker.show();
    }
    private String paymentName(String method) {
        switch(method) {
            case "efectivo": return getString(R.string.dashboard_payment_cash); case "tarjeta": return getString(R.string.dashboard_payment_card);
            case "yape": return "Yape"; case "plin": return "Plin"; case "transferencia": return "Transferencia";
            case "otro": return getString(R.string.dashboard_payment_other); default: return method;
        }
    }
    private String expenseName(String type) {
        switch(type) {
            case "alquiler": return getString(R.string.dashboard_expense_rent); case "sueldos": return getString(R.string.dashboard_expense_salary);
            case "servicios": return getString(R.string.dashboard_expense_services); case "publicidad": return getString(R.string.dashboard_expense_advertising);
            case "otros": return getString(R.string.dashboard_expense_other); default: return type;
        }
    }
    private void label(View root,int id,CharSequence value) { ((TextView)root.findViewById(id)).setText(value); }
    private String money(double value) { return String.format(Locale.US,"S/%.2f",value); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if(model!=null) { out.putInt("period",model.filter().period); out.putLong("fromDay",fromDay); out.putLong("toDay",toDay); }
        if(getView()!=null) out.putInt("channel",((Spinner)getView().findViewById(R.id.dashboardChannel)).getSelectedItemPosition());
    }
}

