package com.example.saasvilcas.feature.sales.domain;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class SalesCartTest {
    @Test public void discountsMergeAndEditingReplacesDiscount() {
        SalesCart cart = new SalesCart(); SalesProduct product = product();
        cart.put(null,product,"40","San Pedro",1,500);
        cart.put(null,product,"40","San Pedro",1,1000);
        assertEquals(17500,cart.totalCents());
        cart.put(cart.getLines().get(0).key(),product,"40","San Pedro",1,200);
        assertEquals(9300,cart.totalCents());
    }
    @Test(expected=IllegalArgumentException.class) public void discountCannotExceedGrossTotal() {
        new SalesCart().put(null,product(),"40","San Pedro",1,9501);
    }
    private SalesProduct product() {
        return new SalesProduct("running","Running",9500,5,"Unisex",
                Arrays.asList("40","41"),Arrays.asList("San Pedro","Central"));
    }
    @Test public void repeatedSelectionMergesAndCalculatesTotalInCents() {
        SalesCart cart=new SalesCart(); SalesProduct product=product();
        cart.put(null,product,"40","San Pedro",1);
        cart.put(null,product,"40","San Pedro",2);
        assertEquals(1,cart.getLines().size()); assertEquals(3,cart.unitCount());
        assertEquals(28500,cart.totalCents());
    }
    @Test public void editingReplacesQuantityRatherThanAddingIt() {
        SalesCart cart=new SalesCart(); SalesProduct product=product();
        cart.put(null,product,"40","San Pedro",2);
        cart.put(cart.getLines().get(0).key(),product,"40","San Pedro",3);
        assertEquals(3,cart.unitCount()); assertEquals(28500,cart.totalCents());
    }
    @Test public void changingSizeMergesWithExistingSelection() {
        SalesCart cart=new SalesCart(); SalesProduct product=product();
        cart.put(null,product,"40","San Pedro",2);
        String oldKey=cart.getLines().get(0).key();
        cart.put(null,product,"41","San Pedro",1);
        cart.put(oldKey,product,"41","San Pedro",2);
        assertEquals(1,cart.getLines().size()); assertEquals(3,cart.unitCount());
        assertEquals("41",cart.getLines().get(0).size);
    }
    @Test public void stockLimitIncludesOtherSizesAndFailedEditPreservesCart() {
        SalesCart cart=new SalesCart(); SalesProduct product=product();
        cart.put(null,product,"40","San Pedro",3);
        cart.put(null,product,"41","Central",2);
        String key=cart.getLines().get(1).key();
        assertThrows(IllegalArgumentException.class,()->cart.put(key,product,"41","Central",3));
        assertEquals(5,cart.unitCount()); assertEquals(47500,cart.totalCents());
    }
    @Test public void removeUpdatesTotalAndEmptyCart() {
        SalesCart cart=new SalesCart(); cart.put(null,product(),"40","San Pedro",1);
        cart.remove(cart.getLines().get(0).key());
        assertEquals(0,cart.totalCents()); assertTrue(cart.getLines().isEmpty());
    }
    @Test public void invalidQuantityAndUnavailableVariantAreRejected() {
        SalesCart cart=new SalesCart(); SalesProduct product=product();
        assertThrows(IllegalArgumentException.class,()->cart.put(null,product,"40","San Pedro",0));
        assertThrows(IllegalArgumentException.class,()->cart.put(null,product,"99","San Pedro",1));
        assertTrue(cart.getLines().isEmpty());
    }
}
