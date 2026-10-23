package com.example.saasvilcas.feature.sales.domain;

/** Stock belongs to a variant at a specific origin, even when the product has no size. */
public final class SalesVariant {
    public final String id,size,siteId,siteName;
    public final int stock;
    public SalesVariant(String id,String size,String siteId,String siteName,int stock) {
        if(stock<0) throw new IllegalArgumentException("Stock inválido.");
        this.id=id; this.size=size==null||size.trim().isEmpty()?"Sin talla":size;
        this.siteId=siteId; this.siteName=siteName; this.stock=stock;
    }
    public String key() { return id+"|"+siteId; }
}
