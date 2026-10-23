package com.example.saasvilcas.feature.sales.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class SalesProduct {
    public final String id, name, gender;
    public final long priceCents;
    public final int stock;
    public final List<String> sizes, sites;
    public final List<SalesVariant> variants;
    public final String imageUrl;
    public final boolean realStock;
    public SalesProduct(String id,String name,long priceCents,int stock,String gender,List<String> sizes,List<String> sites) {
        if(priceCents<0 || stock<0) throw new IllegalArgumentException("Invalid product");
        this.id=id; this.name=name; this.priceCents=priceCents; this.stock=stock; this.gender=gender;
        this.sizes=Collections.unmodifiableList(new ArrayList<>(sizes));
        this.sites=Collections.unmodifiableList(new ArrayList<>(sites));
        this.variants=Collections.emptyList(); this.imageUrl=""; this.realStock=false;
    }
    public SalesProduct(String id,String name,long priceCents,int stock,String gender,String imageUrl,List<SalesVariant> variants) {
        if(priceCents<0 || stock<0) throw new IllegalArgumentException("Invalid product");
        this.id=id; this.name=name; this.priceCents=priceCents; this.stock=stock; this.gender=gender;
        this.imageUrl=imageUrl; this.realStock=true;
        this.variants=Collections.unmodifiableList(new ArrayList<>(variants));
        java.util.LinkedHashSet<String> sizes=new java.util.LinkedHashSet<>(),sites=new java.util.LinkedHashSet<>();
        for(SalesVariant variant:variants) { sizes.add(variant.size); sites.add(variant.siteName); }
        this.sizes=Collections.unmodifiableList(new ArrayList<>(sizes));
        this.sites=Collections.unmodifiableList(new ArrayList<>(sites));
    }
}
