package com.example.saasvilcas.feature.sales.domain;

public final class SalesSite {
    public final String id,name,type;
    public SalesSite(String id,String name,String type) { this.id=id; this.name=name; this.type=type; }
    public boolean isRegister() { return "local".equals(type); }
    @Override public String toString() { return name; }
}
