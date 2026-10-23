package com.example.saasvilcas.feature.inventory.domain;

public final class InventoryException extends IllegalArgumentException {
    public final int status;
    public final boolean uncertain;
    public InventoryException(String message,int status,boolean uncertain) { super(message); this.status=status; this.uncertain=uncertain; }
}
