package com.example.saasvilcas.feature.sales.domain;
public final class SaleResult {
    public final String id;
    public final long totalCents;
    /** True when the sale was stored on the device and will be sent once the connection returns. */
    public final boolean pending;
    public SaleResult(String id,long totalCents) { this(id,totalCents,false); }
    public SaleResult(String id,long totalCents,boolean pending) { this.id=id; this.totalCents=totalCents; this.pending=pending; }
}
