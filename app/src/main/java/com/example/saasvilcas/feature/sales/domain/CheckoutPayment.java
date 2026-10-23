package com.example.saasvilcas.feature.sales.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Checkout arithmetic uses integer cents, including mixed payments and cash change. */
public final class CheckoutPayment {
    private CheckoutPayment() { }
    public static long cents(String text) {
        try {
            long result=new BigDecimal(text.trim().replace(',','.')).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
            if(result<0) throw new IllegalArgumentException();
            return result;
        } catch(RuntimeException error) { throw new IllegalArgumentException("Monto inválido"); }
    }
    public static long change(long total,long[] amounts,long received,boolean split) {
        if(total<=0 || amounts.length!=4 || received<0) throw new IllegalArgumentException("Monto inválido");
        long sum=0; int methods=0;
        try {
            for(long amount:amounts) {
                if(amount<0) throw new IllegalArgumentException("Monto inválido");
                sum=Math.addExact(sum,amount); if(amount>0) methods++;
            }
        } catch(ArithmeticException error) { throw new IllegalArgumentException("Monto inválido"); }
        if(sum!=total || (split && methods<2)) throw new IllegalArgumentException("La suma de los métodos debe coincidir con el total y usar al menos dos métodos al dividir.");
        if(amounts[0]>0 && received<amounts[0]) throw new IllegalArgumentException("El monto recibido no cubre el pago en efectivo.");
        return amounts[0]>0?received-amounts[0]:0;
    }
}
