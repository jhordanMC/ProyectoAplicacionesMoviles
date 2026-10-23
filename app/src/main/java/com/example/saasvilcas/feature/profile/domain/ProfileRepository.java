package com.example.saasvilcas.feature.profile.domain;

import com.example.saasvilcas.shared.network.ApiException;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import java.util.List;
public interface ProfileRepository {
    UserProfile profile(String token) throws ApiException;
    UserProfile avatar(String token,String uri) throws ApiException;
    List<Announcement> announcements(String token) throws ApiException;
    void seen(String token,String id) throws ApiException;
    void vote(String token,String id,String option) throws ApiException;
    void rate(String token,int stars,String comment) throws ApiException;
    int requestPassword(String token,String current,String next) throws ApiException;
    void confirmPassword(String token,String code) throws ApiException;
    AuthSession refresh(AuthSession session) throws ApiException;
    default AuthSession refresh(AuthSession session,java.util.function.Consumer<AuthSession> tokensRotated) throws ApiException {
        AuthSession refreshed=refresh(session); tokensRotated.accept(refreshed); return refreshed;
    }
}
