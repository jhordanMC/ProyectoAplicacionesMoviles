package com.example.saasvilcas.feature.dashboard.domain;

public final class DashboardException extends RuntimeException {
    public final int status;
    public DashboardException(String message,int status) { super(message); this.status=status; }
}
