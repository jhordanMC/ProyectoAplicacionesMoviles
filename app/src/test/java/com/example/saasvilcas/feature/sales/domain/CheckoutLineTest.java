package com.example.saasvilcas.feature.sales.domain;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class CheckoutLineTest {
    private CheckoutLine cash(String amount,String received) { return new CheckoutLine(0,amount,received); }
    private CheckoutLine digital(int method,String amount,String receipt) { CheckoutLine line=new CheckoutLine(method,amount,"0"); line.receipt=receipt; return line; }
    @Test public void singleCashDoesNotRequireReceipt() { CheckoutLine.validate(9500,Arrays.asList(cash("95","100")),false,true); }
    @Test(expected=IllegalArgumentException.class) public void digitalRequiresReceiptAtConfirmation() { CheckoutLine.validate(9500,Arrays.asList(digital(1,"95","")),false,true); }
    @Test public void mayOpenReceiptStepBeforeReceiptIsAttached() { CheckoutLine.validate(9500,Arrays.asList(digital(1,"95","")),false,false); }
    @Test public void validMixedPaymentWithPerMethodProofAndChange() {
        CheckoutLine cash=cash("40","50");
        CheckoutLine.validate(9500,Arrays.asList(cash,digital(1,"55","content://receipt/1")),true,true);
        assertEquals(1000,cash.change());
    }
    @Test(expected=IllegalArgumentException.class) public void eachDigitalLineNeedsItsOwnReceipt() {
        CheckoutLine.validate(9500,Arrays.asList(digital(1,"40","content://receipt/1"),digital(2,"55","")),true,true);
    }
    @Test(expected=IllegalArgumentException.class) public void cannotDuplicateMethod() {
        CheckoutLine.validate(9500,Arrays.asList(digital(1,"40","a"),digital(1,"55","b")),true,true);
    }
    @Test(expected=IllegalArgumentException.class) public void zeroAllocationIsInvalid() {
        CheckoutLine.validate(9500,Arrays.asList(cash("95","95"),digital(1,"0","a")),true,true);
    }
    @Test(expected=IllegalArgumentException.class) public void cashMustCoverItsAllocation() { CheckoutLine.validate(9500,Arrays.asList(cash("95","90")),false,true); }
    @Test(expected=IllegalArgumentException.class) public void splitMustMatchTotal() {
        CheckoutLine.validate(9500,Arrays.asList(cash("40","40"),digital(1,"50","a")),true,true);
    }
}
