package com.example.saasvilcas.feature.sales.domain;
public final class SalesException extends Exception {
    public final int status;
    public final boolean uncertain;
    public SalesException(String message,int status,boolean uncertain) { super(message); this.status=status; this.uncertain=uncertain; }
}
