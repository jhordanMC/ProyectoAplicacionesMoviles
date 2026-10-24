package com.example.saasvilcas.feature.profile.data;

import com.example.saasvilcas.feature.profile.domain.*;
import com.example.saasvilcas.feature.auth.domain.AuthSession;
import com.example.saasvilcas.shared.network.*;
import org.json.*;
import java.util.*;

public final class RemoteProfileRepository implements ProfileRepository {
    public interface ImageSource { byte[] jpeg(String uri) throws Exception; }
    private final ApiClient api; private final ImageSource images;
    public RemoteProfileRepository(ApiClient api,ImageSource images) { this.api=api; this.images=images; }
    private String text(JSONObject json,String key) { return json.isNull(key)?"":json.optString(key,""); }
    private JSONObject json(String method,String path,String token,JSONObject body) throws ApiException {
        String response=api.request(method,path,body==null?null:"application/json",body==null?null:body.toString(),token);
        try { return new JSONObject(response); } catch(JSONException e) { throw new ApiException("La respuesta del servidor no es válida."); }
    }
    private JSONObject body(Object... pairs) throws ApiException {
        try { JSONObject result=new JSONObject(); for(int i=0;i<pairs.length;i+=2) result.put((String)pairs[i],pairs[i+1]); return result; }
        catch(JSONException e) { throw new ApiException("Revisa los datos enviados."); }
    }
    private UserProfile parse(JSONObject value) throws ApiException {
        try {
            if(!"activo".equals(value.getString("estado"))) throw new ApiException("Tu cuenta no está activa.",401);
            List<String> roles=new ArrayList<>(); JSONArray array=value.getJSONArray("roles");
            for(int i=0;i<array.length();i++) roles.add(array.getString(i));
            return new UserProfile(value.getString("id_usuario"),value.getString("id_empresa"),
                (value.getString("nombres")+" "+value.getString("apellidos")).trim(),value.getString("email"),value.getString("nombre_empresa"),
                text(value,"telefono"),text(value,"dni"),text(value,"avatar_url"),roles);
        } catch(JSONException e) { throw new ApiException("No se pudo leer tu perfil."); }
    }
    @Override public UserProfile profile(String token) throws ApiException { return parse(json("GET","/iam/mi-perfil",token,null)); }
    @Override public UserProfile avatar(String token,String uri) throws ApiException {
        try { return parse(new JSONObject(api.upload("/iam/mi-perfil/avatar","avatar.jpg","image/jpeg",images.jpeg(uri),token))); }
        catch(ApiException e) { throw e; } catch(Exception e) { throw new ApiException("No se pudo subir la foto. Selecciona otra imagen."); }
    }
    @Override public List<Announcement> announcements(String token) throws ApiException {
        String response=api.request("GET","/anuncios/activos",null,null,token);
        try {
            JSONArray values=new JSONArray(response); List<Announcement> result=new ArrayList<>();
            for(int i=0;i<values.length();i++) {
                JSONObject value=values.getJSONObject(i); List<Announcement.Option> options=new ArrayList<>();
                JSONArray items=value.optJSONArray("opciones");
                if(items!=null) for(int j=0;j<items.length();j++) { JSONObject option=items.getJSONObject(j);
                    options.add(new Announcement.Option(option.getString("id_opcion"),option.getString("texto"),option.isNull("votos")?null:option.optInt("votos"))); }
                result.add(new Announcement(value.getString("id_anuncio"),value.getString("titulo"),value.getString("mensaje"),text(value,"imagen_url"),text(value,"expira_en"),text(value,"id_opcion_votada"),value.optBoolean("visto"),value.optBoolean("es_satisfaccion"),options));
            }
            return result;
        } catch(JSONException e) { throw new ApiException("No se pudieron leer los anuncios."); }
    }
    @Override public void seen(String token,String id) throws ApiException { json("POST","/anuncios/"+id+"/marcar-visto",token,body()); }
    @Override public void vote(String token,String id,String option) throws ApiException { json("POST","/anuncios/"+id+"/votar",token,body("id_opcion",option)); }
    @Override public void rate(String token,int stars,String comment) throws ApiException {
        json("POST","/satisfaccion/topbar",token,body("calificacion",stars,"comentario",comment.trim().isEmpty()?JSONObject.NULL:comment.trim()));
    }
    @Override public int requestPassword(String token,String current,String next) throws ApiException {
        return json("POST","/iam/auth/solicitar-cambio-password",token,body("password_actual",current,"password_nueva",next)).optInt("expira_en_segundos",300);
    }
    @Override public void confirmPassword(String token,String code) throws ApiException { json("POST","/iam/auth/confirmar-cambio-password",token,body("codigo",code)); }
    @Override public AuthSession refresh(AuthSession session) throws ApiException {
        return refresh(session,rotated->{});
    }
    @Override public AuthSession refresh(AuthSession session,java.util.function.Consumer<AuthSession> tokensRotated) throws ApiException {
        JSONObject tokens=json("POST","/iam/auth/refresh",null,body("refresh_token",session.refreshToken));
        try { String access=tokens.getString("access_token"),refresh=tokens.getString("refresh_token");
            tokensRotated.accept(new AuthSession(access,refresh,session.userId,session.companyId,session.name,session.email,session.companyName,
                    session.roles));
            return profile(access).session(access,refresh); }
        catch(JSONException|IllegalArgumentException e) { throw new ApiException("No se pudo activar la cuenta. Vuelve a iniciar sesión."); }
    }
}
