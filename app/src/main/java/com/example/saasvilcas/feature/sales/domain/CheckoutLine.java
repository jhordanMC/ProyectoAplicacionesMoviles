package com.example.saasvilcas.feature.sales.domain;

import java.util.List;
import java.util.HashSet;

/** A payment allocation, independent of views; receipt is a local URI until backend integration. */
public final class CheckoutLine {
    public int method;
    public String amount,received,receipt="";
    public CheckoutLine(int method,String amount,String received) { this.method=method; this.amount=amount; this.received=received; }
    public long cents() { return CheckoutPayment.cents(amount); }
    public long change() { return method==0?Math.max(0,CheckoutPayment.cents(received)-cents()):0; }
    public static void validate(long total,List<CheckoutLine> lines,boolean split,boolean requireReceipts) {
        if(total<=0 || lines.isEmpty()) throw new IllegalArgumentException("El carrito está vacío o no tiene importe a cobrar.");
        if(split && lines.size()<2) throw new IllegalArgumentException("Agrega al menos dos métodos o usa un solo método.");
        HashSet<Integer> methods=new HashSet<>(); long sum=0;
        for(CheckoutLine line:lines) {
            if(line.method<0 || line.method>3 || !methods.add(line.method)) throw new IllegalArgumentException("No repitas el mismo método de pago.");
            long amount=line.cents();
            if(amount<=0) throw new IllegalArgumentException("Cada método agregado debe tener un monto mayor a cero.");
            try { sum=Math.addExact(sum,amount); } catch(ArithmeticException error) { throw new IllegalArgumentException("Monto inválido."); }
            if(line.method==0 && CheckoutPayment.cents(line.received)<amount)
                throw new IllegalArgumentException("El efectivo recibido no cubre el importe asignado.");
            if(requireReceipts && line.method!=0 && line.receipt.isEmpty())
                throw new IllegalArgumentException("Adjunta los comprobantes de los pagos digitales.");
        }
        if(sum!=total) throw new IllegalArgumentException("El total asignado debe coincidir con el total a cobrar.");
    }
}
