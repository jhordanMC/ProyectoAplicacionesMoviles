package com.example.saasvilcas.feature.inventory.domain;

import java.util.List;

public interface InventoryRepository {
    default void refresh() { }
    default void filter(String search,String category,String gender) { }
    default void invalidateReferences() { }
    default boolean loadMore() { return false; }
    default boolean hasMore() { return false; }
    default InventoryProduct detail(String id) {
        for(InventoryProduct p:products()) if(p.id.equals(id)) return p;
        throw new IllegalArgumentException("El producto ya no existe.");
    }
    List<InventoryProduct> products();
    void save(InventoryProduct product);
    void delete(String id);
    List<String> references(int kind);
    void saveReference(int kind,String previous,String name);
    void deleteReference(int kind,String name);
    String[] warehouseDetails(String name);
    void saveWarehouse(String previous,String name,String address,String description);
}

