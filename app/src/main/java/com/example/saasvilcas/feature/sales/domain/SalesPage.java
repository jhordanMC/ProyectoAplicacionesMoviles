package com.example.saasvilcas.feature.sales.domain;
import java.util.*;
public final class SalesPage {
    public final List<SalesProduct> items;
    public final boolean hasMore;
    public final int nextOffset;
    public SalesPage(List<SalesProduct> items,boolean hasMore,int nextOffset) {
        this.items=Collections.unmodifiableList(new ArrayList<>(items)); this.hasMore=hasMore; this.nextOffset=nextOffset;
    }
}
