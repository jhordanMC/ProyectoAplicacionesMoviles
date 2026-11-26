package com.example.saasvilcas.feature.sales.domain;

/** HU-19: a sale registered in the company, as the owner sees it in the bell and in the system notification. */
public final class SaleNotice {
    public final String id,companyId,userId,site,seller,summary,payments;
    public final long totalCents,createdAt;
    public final boolean read;
    public SaleNotice(String id,String companyId,String userId,String site,String seller,String summary,String payments,
            long totalCents,long createdAt,boolean read) {
        this.id=id; this.companyId=companyId; this.userId=userId; this.site=site==null?"":site; this.seller=seller==null?"":seller;
        this.summary=summary==null?"":summary; this.payments=payments==null?"":payments;
        this.totalCents=totalCents; this.createdAt=createdAt; this.read=read;
    }
    public SaleNotice withDetail(String site,String summary,String payments) {
        return new SaleNotice(id,companyId,userId,site,seller,summary,payments,totalCents,createdAt,read);
    }
    public SaleNotice asRead() { return new SaleNotice(id,companyId,userId,site,seller,summary,payments,totalCents,createdAt,true); }
    public boolean hasDetail() { return !summary.isEmpty(); }
}
