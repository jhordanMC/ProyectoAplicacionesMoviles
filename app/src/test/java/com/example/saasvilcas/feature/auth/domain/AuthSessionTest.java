package com.example.saasvilcas.feature.auth.domain;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AuthSessionTest {
    private AuthSession session(String... roles) {
        return new AuthSession("access","refresh","user","company","Ana Ríos","ana@example.com","Tienda",Arrays.asList(roles));
    }
    @Test public void ownerWinsWhenAccountAlsoHasSellerRole() { assertTrue(session("Vendedor","Dueño de empresa").owner); }
    @Test public void sellerIsNotOwner() { assertFalse(session("Vendedor").owner); }
    @Test(expected=IllegalArgumentException.class) public void unknownRoleDoesNotBecomeSeller() { session("Administrador"); }
    @Test public void logoutClearsSession() {
        SessionStore store=new SessionStore(); AuthSession old=session("Vendedor"); store.set(old);
        assertSame(old,store.clear()); assertNull(store.get()); assertNull(store.clear());
    }
}
