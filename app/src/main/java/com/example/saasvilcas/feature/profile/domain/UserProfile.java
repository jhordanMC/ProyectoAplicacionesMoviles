package com.example.saasvilcas.feature.profile.domain;

import com.example.saasvilcas.feature.auth.domain.AuthSession;
import java.util.List;

public final class UserProfile {
    public final String userId,companyId,name,email,company,phone,dni,avatar;
    public final List<String> roles;
    public UserProfile(String userId,String companyId,String name,String email,String company,String phone,String dni,String avatar,List<String> roles) {
        this.userId=userId; this.companyId=companyId; this.name=name; this.email=email; this.company=company;
        this.phone=phone; this.dni=dni; this.avatar=avatar; this.roles=java.util.Collections.unmodifiableList(roles);
    }
    public AuthSession session(String access,String refresh) { return new AuthSession(access,refresh,userId,companyId,name,email,company,roles); }
}
