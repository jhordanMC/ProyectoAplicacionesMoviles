package com.example.saasvilcas.feature.sales.domain;
public final class SalesFilter {
    public final String search,size,siteId,gender;
    public SalesFilter(String search,String size,String siteId,String gender) {
        this.search=search; this.size=size; this.siteId=siteId; this.gender=gender;
    }
}
