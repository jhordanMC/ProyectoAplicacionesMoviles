package com.example.saasvilcas.feature.inventory.data;

import org.junit.Test;
import java.util.*;
import com.example.saasvilcas.feature.inventory.domain.*;
import static org.junit.Assert.*;

public class InventoryVariantsTest {
    private InventoryProduct product(List<InventoryVariant> variants) {
        return new InventoryProduct("multi","Producto","","","",1000,500,"",variants);
    }
    @Test public void totalsAndCopiesPreserveAllAllocations() {
        InventoryProduct p=product(Arrays.asList(new InventoryVariant("39","Almacén","Almacén chacarilla",5,"A",""),
            new InventoryVariant("39","Local","Local san pedro",7,"B","")));
        assertEquals(12,p.stock); assertEquals(2,p.copy("copy","Copia").variants.size());
    }
    @Test(expected=IllegalArgumentException.class) public void repeatedSizeAndLocationAreRejected() {
        product(Arrays.asList(new InventoryVariant("XL","Local","Local san pedro",1,"",""),
            new InventoryVariant("xl","Local","Local san pedro",1,"","")));
    }
    @Test public void warehouseRenameUpdatesEveryAllocation() {
        DemoInventoryRepository repo=new DemoInventoryRepository();
        repo.save(product(Arrays.asList(new InventoryVariant("39","Almacén","Almacén chacarilla",5,"",""),
            new InventoryVariant("40","Almacén","Almacén chacarilla",7,"",""))));
        repo.saveWarehouse("Almacén chacarilla","Depósito","Calle","Descripción");
        InventoryProduct p=repo.products().get(repo.products().size()-1);
        assertEquals("Depósito",p.variants.get(0).location); assertEquals("Depósito",p.variants.get(1).location);
    }
    @Test public void optionalClassificationAndNoAllocationsAreAccepted() {
        DemoInventoryRepository repo=new DemoInventoryRepository(); repo.save(product(Collections.emptyList()));
        assertEquals(0,repo.products().get(repo.products().size()-1).stock);
    }
    @Test(expected=IllegalArgumentException.class) public void missingLocationIsRejected() {
        new InventoryVariant("","Almacén","",0,"","");
    }
}
