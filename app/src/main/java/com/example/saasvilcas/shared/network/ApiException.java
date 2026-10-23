package com.example.saasvilcas.shared.network;

/** Transport failure with a user-safe message; contains no credentials or response body. */
public final class ApiException extends Exception {
    public final int status;
    public ApiException(String message) { this(message,0); }
    public ApiException(String message,int status) { super(message); this.status=status; }
}
