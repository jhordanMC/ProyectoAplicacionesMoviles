package com.example.saasvilcas.feature.sales.domain;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SalesVariantStockTest {
    @Test public void sameVariantAtDifferentOriginsHasIndependentStock() {
        SalesVariant local=new SalesVariant("variant",null,"local","Sede",2),warehouse=new SalesVariant("variant",null,"warehouse","Sede",3);
        SalesProduct product=product(local,warehouse); SalesCart cart=new SalesCart();
        cart.put(null,product,local,2,0); cart.put(null,product,warehouse,3,0);
        assertEquals(2,cart.getLines().size()); assertEquals(5,cart.unitCount());
        assertEquals("Sin talla",cart.getLines().get(0).size);
        assertThrows(IllegalArgumentException.class,()->cart.put(null,product,local,1,0));
        assertEquals(5,cart.unitCount());
    }
    @Test public void editDoesNotDoubleReserveItsOwnQuantity() {
        SalesVariant variant=new SalesVariant("variant","41","local","Sede",3); SalesProduct product=product(variant);
        SalesCart cart=new SalesCart(); cart.put(null,product,variant,2,25);
        String key=cart.getLines().get(0).key(); cart.put(key,product,variant,3,50);
        assertEquals(3,cart.unitCount()); assertEquals(250,cart.totalCents());
    }
    @Test public void mergingReservesAndMergesDiscountExactlyOnce() {
        SalesVariant variant=new SalesVariant("variant","41","local","Sede",3); SalesProduct product=product(variant);
        SalesCart cart=new SalesCart(); cart.put(null,product,variant,1,10); cart.put(null,product,variant,2,20);
        assertEquals(3,cart.unitCount()); assertEquals(270,cart.totalCents());
    }
    private SalesProduct product(SalesVariant... variants) { return new SalesProduct("product","Motor",100,5,"","",Arrays.asList(variants)); }
}
