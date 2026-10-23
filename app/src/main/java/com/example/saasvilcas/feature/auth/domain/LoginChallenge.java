package com.example.saasvilcas.feature.auth.domain;

/** Temporary email challenge; deliberately never persisted or logged. */
public final class LoginChallenge {
    public final String token;
    public final int expiresInSeconds;
    public LoginChallenge(String token,int expiresInSeconds) {
        if(token==null || token.isEmpty() || expiresInSeconds<=0 || expiresInSeconds>3600)
            throw new IllegalArgumentException("Respuesta de verificación inválida.");
        this.token=token; this.expiresInSeconds=expiresInSeconds;
    }
}
