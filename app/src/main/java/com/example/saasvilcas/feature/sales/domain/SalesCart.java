package com.example.saasvilcas.feature.sales.domain;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Local cart. Real stock is reserved by variant and origin; money uses integer cents. */
public final class SalesCart {
    private final Map<String,CartLine> lines=new LinkedHashMap<>();
    public List<CartLine> getLines() { return Collections.unmodifiableList(new ArrayList<>(lines.values())); }
    public void put(String previousKey,SalesProduct product,String size,String site,int quantity) {
        put(previousKey, product, size, site, quantity, 0);
    }
    public void put(String previousKey,SalesProduct product,String size,String site,int quantity,long discountCents) {
        if(quantity<=0 || !product.sizes.contains(size) || !product.sites.contains(site))
            throw new IllegalArgumentException("Invalid selection");
        CartLine candidate=new CartLine(product,size,site,quantity,discountCents);
        String key=candidate.key();
        int alreadyReserved=0;
        for(CartLine line:lines.values()) {
            if(line.product.id.equals(product.id) && !line.key().equals(previousKey))
                alreadyReserved+=line.quantity;
        }
        if((long)alreadyReserved+quantity>product.stock) throw new IllegalArgumentException("Insufficient stock");
        CartLine existing=lines.get(key);
        int mergedQuantity=quantity;
        long mergedDiscount=discountCents;
        if(existing!=null && !key.equals(previousKey)) {
            mergedQuantity+=existing.quantity;
            mergedDiscount+=existing.discountCents;
        }
        if(previousKey!=null) lines.remove(previousKey);
        lines.put(key,new CartLine(product,size,site,mergedQuantity,mergedDiscount));
    }
    public void remove(String key) { lines.remove(key); }
    public void clear() { lines.clear(); }
    public int reserved(String variantId,String siteId,String excludingKey) {
        int reserved=0;
        for(CartLine line:lines.values()) if(line.variantId.equals(variantId) && line.siteId.equals(siteId) && !line.key().equals(excludingKey)) reserved=Math.addExact(reserved,line.quantity);
        return reserved;
    }
    public void put(String previousKey,SalesProduct product,SalesVariant variant,int quantity,long discountCents) {
        if(variant.siteId==null || variant.siteId.isEmpty() || !product.variants.contains(variant))
            throw new IllegalArgumentException("Selecciona un origen válido.");
        if(quantity<=0 || (long)reserved(variant.id,variant.siteId,previousKey)+quantity>variant.stock)
            throw new IllegalArgumentException("No hay stock suficiente en esta talla y sede.");
        CartLine old=lines.get(variant.key());
        if(old!=null && !old.key().equals(previousKey)) {
            quantity=Math.addExact(quantity,old.quantity); discountCents=Math.addExact(discountCents,old.discountCents);
        }
        CartLine next=new CartLine(product,variant,quantity,discountCents);
        if(previousKey!=null) lines.remove(previousKey);
        lines.put(next.key(),next);
    }
    public long totalCents() {
        long total=0;
        for(CartLine line:lines.values()) total+=line.subtotalCents();
        return total;
    }
    public int unitCount() {
        int count=0;
        for(CartLine line:lines.values()) count+=line.quantity;
        return count;
    }
}
