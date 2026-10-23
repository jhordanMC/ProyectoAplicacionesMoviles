package com.example.saasvilcas.feature.auth.domain;

public interface AuthRepository {
    LoginChallenge login(String email,String password) throws AuthException;
    AuthSession verify(String challengeToken,String code) throws AuthException;
    void logout(String refreshToken) throws AuthException;
    default void recover(String email) throws AuthException { throw new AuthException("La recuperación no está disponible."); }
}
