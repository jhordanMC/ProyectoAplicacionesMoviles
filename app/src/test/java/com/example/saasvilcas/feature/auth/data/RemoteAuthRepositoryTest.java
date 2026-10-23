package com.example.saasvilcas.feature.auth.data;

import org.junit.Test;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.util.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.shared.network.ApiClient;
import com.example.saasvilcas.shared.network.ApiException;
import static org.junit.Assert.*;

public class RemoteAuthRepositoryTest {
    @Test public void recoveryPostsJsonWithoutSessionToken() throws Exception {
        FakeApi api=new FakeApi("{\"mensaje\":\"Si el email existe, se envió un link de recuperación\"}");
        new RemoteAuthRepository(api).recover("  ana+tienda@example.com  ");
        assertEquals("/iam/auth/recuperar-password",api.paths.get(0));assertEquals("POST",api.methods.get(0));
        assertEquals("application/json",api.types.get(0));assertNull(api.bearers.get(0));
        assertEquals("ana+tienda@example.com",new JSONObject(api.bodies.get(0)).getString("email"));
    }
    private static final String PROFILE="{\"id_usuario\":\"u1\",\"id_empresa\":\"e1\",\"nombres\":\"Ana\",\"apellidos\":\"Ríos\",\"email\":\"ana@example.com\",\"nombre_empresa\":\"Tienda\",\"estado\":\"activo\",\"roles\":[\"Dueño de empresa\"]}";
    @Test public void loginUsesOAuthFormAndPreservesSpecialCharacters() throws Exception {
        FakeApi api=new FakeApi("{\"login_token\":\"challenge\",\"expira_en_segundos\":300}");
        LoginChallenge result=new RemoteAuthRepository(api).login("ana+tienda@example.com","a&b=+ ñ");
        assertEquals("challenge",result.token); assertEquals(300,result.expiresInSeconds);
        assertEquals("/iam/auth/login",api.paths.get(0));
        assertEquals("application/x-www-form-urlencoded",api.types.get(0));
        Map<String,String> fields=new HashMap<>();
        for(String pair:api.bodies.get(0).split("&")) {
            String[] parts=pair.split("=",2);
            fields.put(URLDecoder.decode(parts[0],"UTF-8"),URLDecoder.decode(parts[1],"UTF-8"));
        }
        assertEquals("ana+tienda@example.com",fields.get("username"));
        assertEquals("a&b=+ ñ",fields.get("password")); assertNull(api.bearers.get(0));
    }
    @Test public void verifiesLeadingZeroCodeBeforeFetchingAuthenticatedProfile() throws Exception {
        FakeApi api=new FakeApi("{\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"token_type\":\"bearer\"}",PROFILE);
        AuthSession session=new RemoteAuthRepository(api).verify("challenge","001234");
        JSONObject sent=new JSONObject(api.bodies.get(0));
        assertEquals("001234",sent.getString("codigo")); assertEquals("challenge",sent.getString("login_token"));
        assertEquals(Arrays.asList("/iam/auth/verificar-2fa","/iam/mi-perfil"),api.paths);
        assertEquals("access",api.bearers.get(1)); assertEquals("GET",api.methods.get(1));
        assertEquals("Ana Ríos",session.name); assertTrue(session.owner);
    }
    @Test public void unsupportedRoleRevokesIssuedRefreshAndRequiresNewLogin() throws Exception {
        FakeApi api=new FakeApi("{\"access_token\":\"access\",\"refresh_token\":\"refresh\"}",
                PROFILE.replace("Dueño de empresa","Administrador"),"{\"mensaje\":\"Sesión cerrada\"}");
        try { new RemoteAuthRepository(api).verify("challenge","001234"); fail(); }
        catch(AuthException error) { assertTrue(error.restartRequired); }
        assertEquals("/iam/auth/logout",api.paths.get(2));
        assertEquals("refresh",new JSONObject(api.bodies.get(2)).getString("refresh_token"));
    }
    @Test public void rejectedCodeDoesNotRequestProfileOrCreateSession() throws Exception {
        List<String> paths=new ArrayList<>();
        ApiClient api=(method,path,type,body,bearer)-> { paths.add(path); throw new ApiException("Código incorrecto"); };
        try { new RemoteAuthRepository(api).verify("challenge","001234"); fail(); }
        catch(AuthException error) { assertFalse(error.restartRequired); assertEquals("Código incorrecto",error.getMessage()); }
        assertEquals(Collections.singletonList("/iam/auth/verificar-2fa"),paths);
    }
    @Test public void invalidChallengeCannotAdvanceToVerification() throws Exception {
        try { new RemoteAuthRepository(new FakeApi("{\"login_token\":\"\",\"expira_en_segundos\":0}")).login("a@b.com","password"); fail(); }
        catch(AuthException expected) { assertFalse(expected.restartRequired); }
    }
    private static final class FakeApi implements ApiClient {
        final List<String> paths=new ArrayList<>(),types=new ArrayList<>(),bodies=new ArrayList<>(),bearers=new ArrayList<>(),methods=new ArrayList<>();
        final Queue<String> responses;
        FakeApi(String... responses) { this.responses=new ArrayDeque<>(Arrays.asList(responses)); }
        public String request(String method,String path,String type,String body,String bearer) {
            paths.add(path); methods.add(method); types.add(type); bodies.add(body); bearers.add(bearer);
            return responses.remove();
        }
    }
}

