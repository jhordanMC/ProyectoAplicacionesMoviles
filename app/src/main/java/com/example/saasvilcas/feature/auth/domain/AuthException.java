package com.example.saasvilcas.feature.auth.domain;

public final class AuthException extends Exception {
    public final boolean restartRequired;
    public AuthException(String message) { this(message,false); }
    public AuthException(String message,boolean restartRequired) {
        super(message); this.restartRequired=restartRequired;
    }
}
