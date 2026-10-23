package com.example.saasvilcas.feature.inventory.data;

import java.util.*;
import com.example.saasvilcas.feature.inventory.domain.*;

/** Session-only data source; replace this implementation when persistence is defined. */
public final class DemoInventoryRepository implements InventoryRepository {
    private final Map<String,InventoryProduct> catalog=new LinkedHashMap<>();
    private final Map<String,String[]> warehouseDetails=new HashMap<>();
    private final List<List<String>> lists=Arrays.asList(
        new ArrayList<>(Arrays.asList("motores","Deportivo","Ferretería")),
        new ArrayList<>(Arrays.asList("Importador rodriguez","importaciones mendoza")),
        new ArrayList<>(Arrays.asList("Almacén chacarilla","Local san pedro")));
    public DemoInventoryRepository() {
        seed("1","Motor hidroeléctrico yamaha de 4 tiempos","motores",280000,180000,50);
        seed("2","producto prueba","Deportivo",10000,6000,9);
        seed("3","Motor hidroeléctrico yamaha de 4 tiempos (copia)","motores",280000,180000,50);
        seed("4","Motor hidroeléctrico yamaha de 2 tiempos","motores",280000,180000,50);
        seed("5","caja de herramientas top 52 pzas.","Ferretería",9500,5000,232);
        seed("6","Llave mixta stanley","Ferretería",3500,1500,200);
        seed("7","clavos cabeza plana *100","Ferretería",400,200,252);
    }
    private void seed(String id,String name,String category,long price,long cost,int stock) {
        save(new InventoryProduct(id,name,category,"Unisex",lists.get(1).get(0),lists.get(2).get(0),
            price,cost,stock,"","","",""));
    }
    public List<InventoryProduct> products() { return new ArrayList<>(catalog.values()); }
    public void save(InventoryProduct product) {
        if((!product.category.isEmpty()&&!lists.get(0).contains(product.category))
                || (!product.supplier.isEmpty()&&!lists.get(1).contains(product.supplier)))
            throw new IllegalArgumentException("Selecciona categoría, proveedor y almacén válidos.");
        for(InventoryVariant variant:product.variants)
            if(variant.type.equals("Almacén")&&!lists.get(2).contains(variant.location))
                throw new IllegalArgumentException("Selecciona un almacén válido.");
        catalog.put(product.id,product);
    }
    public void delete(String id) { catalog.remove(id); }
    public List<String> references(int kind) { return new ArrayList<>(lists.get(kind)); }
    public void saveReference(int kind,String previous,String name) {
        name=name.trim();
        if(name.isEmpty()) throw new IllegalArgumentException("Ingresa un nombre.");
        for(String value:lists.get(kind)) if(value.equalsIgnoreCase(name)&&!value.equals(previous))
            throw new IllegalArgumentException("Ya existe un registro con ese nombre.");
        if(previous==null) lists.get(kind).add(name);
        else {
            int index=lists.get(kind).indexOf(previous);
            if(index<0) throw new IllegalArgumentException("El registro ya no existe.");
            lists.get(kind).set(index,name);
            for(InventoryProduct product:products()) catalog.put(product.id,product.renameReference(kind,previous,name));
        }
    }
    public void deleteReference(int kind,String name) {
        for(InventoryProduct product:catalog.values()) {
            if(kind==2) {
                for(InventoryVariant variant:product.variants)
                    if(variant.type.equals("Almacén")&&variant.location.equals(name))
                        throw new IllegalArgumentException("Este almacén tiene stock asignado a productos.");
                continue;
            }
            String value=kind==0?product.category:kind==1?product.supplier:product.warehouse;
            if(value.equals(name)) throw new IllegalArgumentException("Este registro está asignado a productos. Edita esos productos antes de eliminarlo.");
        }
        lists.get(kind).remove(name);
        if(kind==2) warehouseDetails.remove(name);
    }
    public String[] warehouseDetails(String name) {
        String[] details=warehouseDetails.get(name);
        return details==null?new String[]{"",""}:details.clone();
    }
    public void saveWarehouse(String previous,String name,String address,String description) {
        saveReference(2,previous,name);
        if(previous!=null) warehouseDetails.remove(previous);
        warehouseDetails.put(name.trim(),new String[]{address.trim(),description.trim()});
    }
}

