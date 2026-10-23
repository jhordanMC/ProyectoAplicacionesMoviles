package com.example.saasvilcas.feature.dashboard.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable read model. Null metrics/value mean unavailable, never a made-up zero. */
public final class DashboardPanel {
    public final Metrics metrics;
    public final Double inventoryValue;
    public final List<Product> products;
    public final List<Entry> entries;
    public final List<Point> points;
    private DashboardPanel(Metrics metrics,Double inventoryValue,List<Product> products,List<Entry> entries,List<Point> points) {
        this.metrics=metrics; this.inventoryValue=inventoryValue;
        this.products=immutable(products); this.entries=immutable(entries); this.points=immutable(points);
    }
    private static <T> List<T> immutable(List<T> list) { return Collections.unmodifiableList(new ArrayList<>(list)); }
    public static DashboardPanel metrics(Metrics value) { return new DashboardPanel(value,null,Collections.emptyList(),Collections.emptyList(),Collections.emptyList()); }
    public static DashboardPanel inventory(double value) { return new DashboardPanel(null,value,Collections.emptyList(),Collections.emptyList(),Collections.emptyList()); }
    public static DashboardPanel products(List<Product> value) { return new DashboardPanel(null,null,value,Collections.emptyList(),Collections.emptyList()); }
    public static DashboardPanel entries(List<Entry> value) { return new DashboardPanel(null,null,Collections.emptyList(),value,Collections.emptyList()); }
    public static DashboardPanel trend(List<Point> value) { return new DashboardPanel(null,null,Collections.emptyList(),Collections.emptyList(),value); }
    public static final class Metrics {
        public final double profit,sold,average,growth;
        public final int saleCount;
        public final boolean recurrentExpenses,uniqueExpenses,completeCost;
        public Metrics(double profit,double sold,double average,double growth,int saleCount,boolean recurrentExpenses,boolean uniqueExpenses,boolean completeCost) {
            this.profit=profit; this.sold=sold; this.average=average; this.growth=growth; this.saleCount=saleCount;
            this.recurrentExpenses=recurrentExpenses; this.uniqueExpenses=uniqueExpenses; this.completeCost=completeCost;
        }
    }
    public static final class Product {
        public final String id,name,imageUrl;
        public final Integer quantity;
        public Product(String id,String name,Integer quantity,String imageUrl) {
            this.id=id; this.name=name; this.quantity=quantity; this.imageUrl=imageUrl;
        }
    }
    public static final class Entry {
        public final String name;
        public final double amount;
        public final int count;
        public Entry(String name,double amount,int count) { this.name=name; this.amount=amount; this.count=count; }
    }
    public static final class Point {
        public final String label;
        public final double income, profit;
        public Point(String label,double income,double profit) { this.label=label; this.income=income; this.profit=profit; }
    }
}
