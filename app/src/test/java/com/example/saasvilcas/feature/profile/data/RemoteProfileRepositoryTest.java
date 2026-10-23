package com.example.saasvilcas.feature.profile.data;
import org.junit.Test;
import org.json.*;
import java.util.*;
import com.example.saasvilcas.shared.network.*;
import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.feature.profile.domain.*;
import static org.junit.Assert.*;
public class RemoteProfileRepositoryTest {
    private static final String PROFILE="{\"id_usuario\":\"u\",\"id_empresa\":\"c\",\"nombre_empresa\":\"Tienda\",\"nombres\":\"Ana\",\"apellidos\":\"Ríos\",\"email\":\"ana@example.com\",\"telefono\":null,\"dni\":\"12345678\",\"avatar_url\":\"/uploads/avatar.jpg\",\"estado\":\"activo\",\"roles\":[\"Vendedor\"]}";
    static class Api implements ApiClient {
        List<String> paths=new ArrayList<>(),bodies=new ArrayList<>(),tokens=new ArrayList<>();
        String response="{}";int status;byte[] uploaded;
        public String request(String method,String path,String type,String body,String bearer) throws ApiException {
            paths.add(path);bodies.add(body);tokens.add(bearer);if(status>0) throw new ApiException("Rechazado",status);return response;
        }
        public String upload(String path,String name,String mime,byte[] bytes,String token) throws ApiException {
            uploaded=bytes; assertEquals("avatar.jpg",name);assertEquals("image/jpeg",mime);return request("POST",path,mime,null,token);
        }
    }
    @Test public void profileAndAvatarUseAuthenticatedRealFields() throws Exception {
        Api api=new Api();api.response=PROFILE;RemoteProfileRepository repo=new RemoteProfileRepository(api,uri->new byte[]{1,2});
        UserProfile value=repo.profile("access");assertEquals("Ana Ríos",value.name);assertEquals("",value.phone);assertEquals("12345678",value.dni);
        assertFalse(value.session("access","refresh").owner);repo.avatar("access","content://photo");
        assertEquals(Arrays.asList("/iam/mi-perfil","/iam/mi-perfil/avatar"),api.paths);assertEquals(Arrays.asList("access","access"),api.tokens);assertArrayEquals(new byte[]{1,2},api.uploaded);
    }
    @Test public void mutationsMatchWebContractsAndKeepLeadingZeroCode() throws Exception {
        Api api=new Api();RemoteProfileRepository repo=new RemoteProfileRepository(api,uri->new byte[0]);
        repo.rate("access",5,"  Excelente  ");assertEquals(5,new JSONObject(api.bodies.get(0)).getInt("calificacion"));assertEquals("Excelente",new JSONObject(api.bodies.get(0)).getString("comentario"));
        repo.rate("access",4,"  ");assertTrue(new JSONObject(api.bodies.get(1)).isNull("comentario"));
        api.response="{\"expira_en_segundos\":180}";assertEquals(180,repo.requestPassword("access","actual","nueva123"));
        assertEquals("actual",new JSONObject(api.bodies.get(2)).getString("password_actual"));
        repo.confirmPassword("access","001234");assertEquals("001234",new JSONObject(api.bodies.get(3)).getString("codigo"));
        repo.seen("access","ad1");repo.vote("access","ad1","op2");
        assertEquals(Arrays.asList("/satisfaccion/topbar","/satisfaccion/topbar","/iam/auth/solicitar-cambio-password","/iam/auth/confirmar-cambio-password","/anuncios/ad1/marcar-visto","/anuncios/ad1/votar"),api.paths);
        assertEquals("op2",new JSONObject(api.bodies.get(5)).getString("id_opcion"));for(String token:api.tokens) assertEquals("access",token);
    }
    @Test public void announcementsDoNotInventHiddenVotes() throws Exception {
        Api api=new Api();api.response="[{\"id_anuncio\":\"a\",\"titulo\":\"Encuesta\",\"mensaje\":\"Elige\",\"visto\":false,\"id_opcion_votada\":null,\"opciones\":[{\"id_opcion\":\"o\",\"texto\":\"Sí\",\"votos\":null}]}]";
        Announcement item=new RemoteProfileRepository(api,uri->new byte[0]).announcements("access").get(0);assertFalse(item.seen);assertEquals("",item.votedOption);assertNull(item.options.get(0).votes);
        assertEquals("/anuncios/activos",api.paths.get(0));
    }
    @Test public void serverRejectionsRemainVisible() throws Exception {
        Api api=new Api();api.status=409;try {new RemoteProfileRepository(api,uri->new byte[0]).vote("access","a","o");fail();}catch(ApiException e){assertEquals(409,e.status);assertEquals("Rechazado",e.getMessage());}
    }
    @Test public void accountActivationRotatesRefreshBeforeReadingNewProfile() throws Exception {
        Api api=new Api() {
            @Override public String request(String method,String path,String type,String body,String token) throws ApiException {
                response=path.equals("/iam/auth/refresh")?"{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}":PROFILE;
                return super.request(method,path,type,body,token);
            }
        };
        AuthSession old=new AuthSession("old-access","old-refresh","u","c","Ana","a@example.com","T",Collections.singletonList("Vendedor"));
        AuthSession fresh=new RemoteProfileRepository(api,uri->new byte[0]).refresh(old);
        assertEquals("new-access",fresh.accessToken);assertEquals("new-refresh",fresh.refreshToken);
        assertEquals(Arrays.asList("/iam/auth/refresh","/iam/mi-perfil"),api.paths);
        assertNull(api.tokens.get(0));assertEquals("new-access",api.tokens.get(1));
        assertEquals("old-refresh",new JSONObject(api.bodies.get(0)).getString("refresh_token"));
    }
    @Test public void switchingSessionKeepsOldAccountAndLogoutForgetsActiveOnly() {
        SessionStore store=new SessionStore();AuthSession a=new AuthSession("a","ar","a","c","Ana","a@example.com","T",Collections.singletonList("Vendedor"));AuthSession b=new AuthSession("b","br","b","d","Bea","b@example.com","T",Collections.singletonList("Vendedor"));
        store.set(a);store.set(b);assertEquals(2,store.accounts().size());assertNull(store.forget("b"));assertSame(b,store.clear());assertEquals(1,store.accounts().size());assertSame(a,store.accounts().get(0));
    }
    @Test public void rotatedTokensAreAvailableEvenWhenProfileRequestFails() throws Exception {
        Api api=new Api() {
            @Override public String request(String method,String path,String type,String body,String token) throws ApiException {
                if(path.equals("/iam/mi-perfil")) throw new ApiException("Sin conexión");
                return "{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}";
            }
        };
        AuthSession old=new AuthSession("old-access","old-refresh","u","c","Ana","a@example.com","T",Collections.singletonList("Vendedor"));
        java.util.concurrent.atomic.AtomicReference<AuthSession> rotated=new java.util.concurrent.atomic.AtomicReference<>();
        try {new RemoteProfileRepository(api,uri->new byte[0]).refresh(old,rotated::set);fail();}
        catch(ApiException expected) {assertEquals(0,expected.status);}
        assertEquals("new-refresh",rotated.get().refreshToken);assertEquals("u",rotated.get().userId);
    }
}
