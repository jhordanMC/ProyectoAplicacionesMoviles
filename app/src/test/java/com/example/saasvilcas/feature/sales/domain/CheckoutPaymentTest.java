package com.example.saasvilcas.feature.sales.domain;
import org.junit.Test;
import static org.junit.Assert.*;

public class CheckoutPaymentTest {
    @Test public void exactCashHasNoChange() { assertEquals(0,CheckoutPayment.change(9500,new long[]{9500,0,0,0},9500,false)); }
    @Test public void cashChangeIsCalculatedInCents() { assertEquals(500,CheckoutPayment.change(9500,new long[]{9500,0,0,0},10000,false)); }
    @Test public void mixedChangeAppliesOnlyToCashPortion() { assertEquals(1000,CheckoutPayment.change(9500,new long[]{4000,5500,0,0},5000,true)); }
    @Test public void digitalPaymentHasNoCashChange() { assertEquals(0,CheckoutPayment.change(9500,new long[]{0,9500,0,0},0,false)); }
    @Test(expected=IllegalArgumentException.class) public void insufficientCashIsRejected() { CheckoutPayment.change(9500,new long[]{9500,0,0,0},9400,false); }
    @Test(expected=IllegalArgumentException.class) public void mixedAmountsMustMatchTotal() { CheckoutPayment.change(9500,new long[]{4000,5400,0,0},4000,true); }
    @Test(expected=IllegalArgumentException.class) public void splitRequiresMultipleMethods() { CheckoutPayment.change(9500,new long[]{9500,0,0,0},9500,true); }
    @Test public void acceptsCommaWithoutFloatingPointRounding() { assertEquals(9510,CheckoutPayment.cents("95,10")); }
    @Test(expected=IllegalArgumentException.class) public void excessPrecisionIsRejected() { CheckoutPayment.cents("95.001"); }
    @Test(expected=IllegalArgumentException.class) public void negativePaymentIsRejected() { CheckoutPayment.cents("-1"); }
}
