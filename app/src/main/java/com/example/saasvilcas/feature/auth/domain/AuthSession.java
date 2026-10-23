package com.example.saasvilcas.feature.auth.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Only the two company roles currently implemented by the mobile app are accepted. */
public final class AuthSession {
    public final String accessToken,refreshToken,userId,companyId,name,email,companyName;
    public final boolean owner;
    public AuthSession(String accessToken,String refreshToken,String userId,String companyId,
            String name,String email,String companyName,List<String> roles) {
        if(accessToken.isEmpty() || refreshToken.isEmpty() || userId.isEmpty() || companyId.isEmpty())
            throw new IllegalArgumentException("La sesión recibida está incompleta.");
        boolean isOwner=false,isSeller=false;
        for(String role:roles) {
            String normalized=Normalizer.normalize(role,Normalizer.Form.NFD).replaceAll("\\p{M}","").trim().toLowerCase(Locale.ROOT);
            isOwner |= normalized.equals("dueno de empresa") || normalized.equals("dueno");
            isSeller |= normalized.equals("vendedor");
        }
        if(!isOwner && !isSeller) throw new IllegalArgumentException("Esta versión móvil admite cuentas de dueño de empresa y vendedor.");
        this.accessToken=accessToken; this.refreshToken=refreshToken; this.userId=userId;
        this.companyId=companyId; this.name=name; this.email=email; this.companyName=companyName;
        this.owner=isOwner;
    }
    public String initials() {
        StringBuilder result=new StringBuilder();
        for(String part:name.trim().split("\\s+")) {
            if(!part.isEmpty()) result.appendCodePoint(part.codePointAt(0));
            if(result.codePointCount(0,result.length())>=2) break;
        }
        return result.length()==0?"?":result.toString().toUpperCase(Locale.ROOT);
    }
}
