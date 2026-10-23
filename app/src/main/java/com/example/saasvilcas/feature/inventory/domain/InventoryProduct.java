package com.example.saasvilcas.feature.inventory.domain;

/** Immutable product used by the local frontend catalog. Amounts are integer cents. */
public final class InventoryProduct {
    public final String id, name, category, gender, supplier, warehouse, photo, size, sku, barcode;
    public final long price, cost;
    public final int stock;
    public final java.util.List<InventoryVariant> variants;
    public InventoryProduct(String id,String name,String category,String gender,String supplier,long price,long cost,int stock,String photo) {
        this.id=id; this.name=name; this.category=category; this.gender=gender; this.supplier=supplier;
        this.price=price; this.cost=cost; this.stock=stock; this.photo=photo; this.variants=java.util.Collections.emptyList();
        this.warehouse=""; this.size=""; this.sku=""; this.barcode="";
    }
    public InventoryProduct(String id,String name,String category,String gender,String supplier,
            String warehouse,long price,long cost,int stock,String photo,String size,String sku,String barcode) {
        this(id,name,category,gender,supplier,price,cost,photo,java.util.Collections.singletonList(
            new InventoryVariant(size,"Almacén",warehouse,stock,sku,barcode)));
    }
    public InventoryProduct(String id,String name,String category,String gender,String supplier,
            long price,long cost,String photo,java.util.List<InventoryVariant> variants) {
        long stock=0;
        java.util.Set<String> keys=new java.util.HashSet<>();
        for(InventoryVariant variant:variants) {
            if(!keys.add(variant.key())) throw new IllegalArgumentException("Ya existe esa talla en la misma sede.");
            stock+=variant.stock;
        }
        if(name.trim().isEmpty() || price<0 || cost<0 || stock<0)
            throw new IllegalArgumentException("Revisa nombre, precios y stock.");
        if(stock>Integer.MAX_VALUE) throw new IllegalArgumentException("El stock total es demasiado grande.");
        this.id=id; this.name=name.trim(); this.category=category; this.gender=gender;
        this.supplier=supplier; this.price=price; this.cost=cost;
        this.stock=(int)stock; this.photo=photo;
        this.variants=java.util.Collections.unmodifiableList(new java.util.ArrayList<>(variants));
        InventoryVariant first=variants.isEmpty()?null:variants.get(0);
        this.warehouse=first==null?"":first.location; this.size=first==null?"":first.size;
        this.sku=first==null?"":first.sku; this.barcode=first==null?"":first.barcode;
    }
    public InventoryProduct copy(String id,String name) {
        return new InventoryProduct(id,name,category,gender,supplier,price,cost,photo,variants);
    }
    public InventoryProduct renameReference(int kind,String oldName,String newName) {
        java.util.List<InventoryVariant> updated=new java.util.ArrayList<>();
        for(InventoryVariant v:variants) updated.add(new InventoryVariant(v.size,v.type,
            kind==2&&v.type.equals("Almacén")&&v.location.equals(oldName)?newName:v.location,v.stock,v.sku,v.barcode));
        return new InventoryProduct(id,name,kind==0&&category.equals(oldName)?newName:category,gender,
            kind==1&&supplier.equals(oldName)?newName:supplier,price,cost,photo,updated);
    }
}

