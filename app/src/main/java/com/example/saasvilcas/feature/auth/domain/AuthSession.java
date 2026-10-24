package com.example.saasvilcas.feature.auth.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Only the company roles implemented by the mobile app are accepted: owner, seller and inventory. */
public final class AuthSession {
    public static final String OWNER_ROLE="Dueño de empresa", SELLER_ROLE="Vendedor", INVENTORY_ROLE="Inventario";
    public final String accessToken,refreshToken,userId,companyId,name,email,companyName;
    public final boolean owner,seller,inventory;
    /** Recognised system role names, kept so refreshed or persisted sessions keep the same access. */
    public final List<String> roles;
    public AuthSession(String accessToken,String refreshToken,String userId,String companyId,
            String name,String email,String companyName,List<String> roles) {
        if(accessToken.isEmpty() || refreshToken.isEmpty() || userId.isEmpty() || companyId.isEmpty())
            throw new IllegalArgumentException("La sesión recibida está incompleta.");
        boolean isOwner=false,isSeller=false,isInventory=false;
        for(String role:roles) {
            String normalized=Normalizer.normalize(role,Normalizer.Form.NFD).replaceAll("\\p{M}","").trim().toLowerCase(Locale.ROOT);
            isOwner |= normalized.equals("dueno de empresa") || normalized.equals("dueno");
            isSeller |= normalized.equals("vendedor");
            isInventory |= normalized.equals("inventario");
        }
        if(!isOwner && !isSeller && !isInventory)
            throw new IllegalArgumentException("Esta versión móvil admite cuentas de dueño de empresa, vendedor e inventario.");
        this.accessToken=accessToken; this.refreshToken=refreshToken; this.userId=userId;
        this.companyId=companyId; this.name=name; this.email=email; this.companyName=companyName;
        this.owner=isOwner; this.seller=isSeller; this.inventory=isInventory;
        List<String> known=new ArrayList<>();
        if(isOwner) known.add(OWNER_ROLE);
        if(isSeller) known.add(SELLER_ROLE);
        if(isInventory) known.add(INVENTORY_ROLE);
        this.roles=Collections.unmodifiableList(known);
    }
    /** Dashboard is owner-only. */
    public boolean canSeeDashboard() { return owner; }
    /** In the mobile app sellers only sell; the inventory role only manages stock. */
    public boolean canSeeInventory() { return owner || inventory; }
    public boolean canSeeSales() { return owner || seller; }
    public String initials() {
        StringBuilder result=new StringBuilder();
        for(String part:name.trim().split("\\s+")) {
            if(!part.isEmpty()) result.appendCodePoint(part.codePointAt(0));
            if(result.codePointCount(0,result.length())>=2) break;
        }
        return result.length()==0?"?":result.toString().toUpperCase(Locale.ROOT);
    }
}
