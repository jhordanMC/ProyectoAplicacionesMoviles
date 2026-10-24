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
    @Test public void ownerSeesEverySection() {
        AuthSession owner=session("Dueño de empresa");
        assertTrue(owner.canSeeDashboard()); assertTrue(owner.canSeeInventory()); assertTrue(owner.canSeeSales());
    }
    @Test public void sellerOnlySeesSalesInTheApp() {
        AuthSession seller=session("Vendedor");
        assertFalse(seller.canSeeDashboard()); assertFalse(seller.canSeeInventory()); assertTrue(seller.canSeeSales());
    }
    @Test public void inventoryRoleOnlySeesInventory() {
        AuthSession inventory=session("Inventario");
        assertFalse(inventory.owner); assertFalse(inventory.canSeeDashboard());
        assertTrue(inventory.canSeeInventory()); assertFalse(inventory.canSeeSales());
    }
    @Test public void sellerAndInventorySeeBothSections() {
        AuthSession both=session("Vendedor","Inventario");
        assertFalse(both.canSeeDashboard()); assertTrue(both.canSeeInventory()); assertTrue(both.canSeeSales());
    }
    @Test public void rolesSurviveCopiesOfTheSession() {
        AuthSession inventory=session("inventario");
        assertEquals(java.util.Collections.singletonList(AuthSession.INVENTORY_ROLE),inventory.roles);
        AuthSession copy=new AuthSession("a2","r2","user","company","Ana","a@example.com","Tienda",inventory.roles);
        assertTrue(copy.inventory); assertFalse(copy.seller);
    }
    @Test public void logoutClearsSession() {
        SessionStore store=new SessionStore(); AuthSession old=session("Vendedor"); store.set(old);
        assertSame(old,store.clear()); assertNull(store.get()); assertNull(store.clear());
    }
}
