package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.shared.network.*;
import org.json.JSONObject;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SalePushRegistrationTest {
    private final List<String[]> calls=new ArrayList<>();
    private String rejected;
    private final ApiClient api=(method,path,type,body,bearer)->{
        calls.add(new String[]{method,path,body,bearer});
        if(bearer.equals(rejected)) throw new ApiException("Tu sesión expiró.",401);
        return "{\"mensaje\":\"ok\"}";
    };
    private String token="access";
    private int refreshes;
    private final SalePushRegistration registration=new SalePushRegistration(api,new SalePushRegistration.Session() {
        @Override public String token() { return token; }
        @Override public boolean refresh() { refreshes++; token="renewed"; return true; }
    });

    @Test public void registersThePhoneTokenWithTheSession() throws Exception {
        assertTrue(registration.register("firebase-token"));
        String[] call=calls.get(0);
        assertEquals("POST",call[0]); assertEquals("/notificaciones/dispositivos",call[1]); assertEquals("access",call[3]);
        assertEquals("firebase-token",new JSONObject(call[2]).getString("token"));
    }
    @Test public void expiredSessionIsRenewedOnce() {
        rejected="access";
        assertTrue(registration.register("firebase-token"));
        assertEquals(1,refreshes); assertEquals("renewed",calls.get(1)[3]);
    }
    @Test public void withoutSessionNothingIsSent() {
        token=null;
        assertFalse(registration.register("firebase-token"));
        assertTrue(calls.isEmpty());
    }
    @Test public void signOutRemovesWithTheClosingSession() {
        registration.remove("firebase-token","closing");
        assertEquals("/notificaciones/dispositivos/eliminar",calls.get(0)[1]); assertEquals("closing",calls.get(0)[3]);
    }
}
