package com.example.saasvilcas.app;

/** Navigation contract; authentication decisions remain outside the navigation host. */
public interface FrontendNavigator {
    void showLogin();
    void showRecovery(String email);
    void showVerification(String email);
    void showCompany(boolean owner);
}

