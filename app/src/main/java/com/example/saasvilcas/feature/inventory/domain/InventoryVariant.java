package com.example.saasvilcas.feature.inventory.domain;

import java.util.Locale;

/** One stock allocation for a size at a local or warehouse. */
public final class InventoryVariant {
    public final String size, type, location, sku, barcode;
    public final int stock;
    public final String locationId;
    public final Integer version;
    public InventoryVariant(String size,String type,String location,int stock,String sku,String barcode) {
        this(size,type,location,stock,sku,barcode,"",null);
    }
    public InventoryVariant(String size,String type,String location,int stock,String sku,String barcode,String locationId,Integer version) {
        if(size.trim().length()>20||sku.trim().length()>100||barcode.trim().length()>50
                ||stock<0||location.trim().isEmpty()||(!type.equals("Local")&&!type.equals("Almacén")))
            throw new IllegalArgumentException("Revisa talla, sede, stock, SKU y código de barras.");
        this.size=size.trim(); this.type=type; this.location=location; this.stock=stock;
        this.sku=sku.trim(); this.barcode=barcode.trim();
        this.locationId=locationId; this.version=version;
    }
    public String key() { return type+"|"+(locationId.isEmpty()?location:locationId)+"|"+size.toLowerCase(Locale.ROOT); }
}

