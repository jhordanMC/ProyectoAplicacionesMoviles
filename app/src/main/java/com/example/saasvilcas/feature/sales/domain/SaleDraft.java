package com.example.saasvilcas.feature.sales.domain;
import java.util.*;
/** Immutable snapshot: editing the screen cannot alter a request already being sent. The id is the idempotency key. */
public final class SaleDraft {
    public final String id;
    public final String registerId;
    public final List<CartLine> items;
    public final List<CheckoutLine> payments;
    public SaleDraft(String registerId,List<CartLine> items,List<CheckoutLine> payments) {
        this(UUID.randomUUID().toString(),registerId,items,payments);
    }
    public SaleDraft(String id,String registerId,List<CartLine> items,List<CheckoutLine> payments) {
        this.id=id; this.registerId=registerId; this.items=Collections.unmodifiableList(new ArrayList<>(items));
        ArrayList<CheckoutLine> copy=new ArrayList<>();
        for(CheckoutLine payment:payments) {
            CheckoutLine line=new CheckoutLine(payment.method,payment.amount,payment.received); line.receipt=payment.receipt; copy.add(line);
        }
        this.payments=Collections.unmodifiableList(copy);
    }
    public long totalCents() { long total=0; for(CartLine item:items) total=Math.addExact(total,item.subtotalCents()); return total; }
}
