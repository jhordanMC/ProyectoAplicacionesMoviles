package com.example.saasvilcas.feature.inventory.data;

import org.junit.Test;
import com.example.saasvilcas.feature.inventory.domain.InventoryProduct;
import static org.junit.Assert.*;

public class DemoInventoryRepositoryTest {
    @Test public void warehouseDetailsSurviveRenameAndAreRemovedWithWarehouse() {
        DemoInventoryRepository repo=new DemoInventoryRepository();
        repo.saveWarehouse(null,"Depósito","Calle 123","Zona principal");
        repo.saveWarehouse("Depósito","Depósito nuevo","Calle 456","Zona secundaria");
        assertArrayEquals(new String[]{"Calle 456","Zona secundaria"},repo.warehouseDetails("Depósito nuevo"));
        assertArrayEquals(new String[]{"",""},repo.warehouseDetails("Depósito"));
        repo.deleteReference(2,"Depósito nuevo");
        assertArrayEquals(new String[]{"",""},repo.warehouseDetails("Depósito nuevo"));
    }
    @Test public void createEditDuplicateAndDeletePreserveCatalog() {
        DemoInventoryRepository repo=new DemoInventoryRepository(); int initial=repo.products().size();
        InventoryProduct product=repo.products().get(0).copy("new","Nuevo"); repo.save(product);
        assertEquals(initial+1,repo.products().size());
        repo.save(product.copy("new","Editado")); assertEquals(initial+1,repo.products().size());
        repo.save(product.copy("copy","Copia")); assertEquals(initial+2,repo.products().size());
        repo.delete("new"); repo.delete("copy"); assertEquals(initial,repo.products().size());
    }
    @Test public void renamingReferenceUpdatesProducts() {
        DemoInventoryRepository repo=new DemoInventoryRepository(); repo.saveReference(0,"motores","Motores nuevos");
        assertEquals("Motores nuevos",repo.products().get(0).category);
        assertFalse(repo.references(0).contains("motores"));
    }
    @Test(expected=IllegalArgumentException.class) public void assignedReferenceCannotBeDeleted() {
        new DemoInventoryRepository().deleteReference(0,"motores");
    }
    @Test public void unusedReferenceCanBeCreatedRenamedAndDeleted() {
        DemoInventoryRepository repo=new DemoInventoryRepository();
        repo.saveReference(2,null,"Temporal"); repo.saveReference(2,"Temporal","Temporal nuevo");
        assertTrue(repo.references(2).contains("Temporal nuevo"));
        repo.deleteReference(2,"Temporal nuevo"); assertFalse(repo.references(2).contains("Temporal nuevo"));
    }
    @Test(expected=IllegalArgumentException.class) public void namesMustBeUniqueIgnoringCase() {
        new DemoInventoryRepository().saveReference(0,null,"MOTORES");
    }
    @Test(expected=IllegalArgumentException.class) public void negativeStockIsRejected() {
        new InventoryProduct("x","Nombre","motores","Unisex","Proveedor","Almacén",100,0,-1,"","","","");
    }
}
