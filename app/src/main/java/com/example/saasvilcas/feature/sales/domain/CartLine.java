package com.example.saasvilcas.feature.sales.domain;

public final class CartLine {
    public final SalesProduct product;
    public final String size, site;
    public final int quantity;
    public final long discountCents;
    public final String variantId,siteId;
    public final int availableStock;
    public CartLine(SalesProduct product,String size,String site,int quantity) {
        this(product, size, site, quantity, 0);
    }
    public CartLine(SalesProduct product,String size,String site,int quantity,long discountCents) {
        if (discountCents < 0 || discountCents > product.priceCents * quantity)
            throw new IllegalArgumentException("Invalid discount");
        this.product=product; this.size=size; this.site=site; this.quantity=quantity;
        this.discountCents=discountCents;
        this.variantId=""; this.siteId=""; this.availableStock=product.stock;
    }
    public CartLine(SalesProduct product,SalesVariant variant,int quantity,long discountCents) {
        if(quantity<=0 || discountCents<0 || discountCents>Math.multiplyExact(product.priceCents,quantity))
            throw new IllegalArgumentException("Revisa la cantidad y el descuento.");
        this.product=product; this.size=variant.size; this.site=variant.siteName; this.quantity=quantity;
        this.discountCents=discountCents; this.variantId=variant.id; this.siteId=variant.siteId; this.availableStock=variant.stock;
    }
    public String key() { return variantId.isEmpty()?product.id+"|"+size+"|"+site:variantId+"|"+siteId; }
    public long subtotalCents() { return product.priceCents*quantity-discountCents; }
}
