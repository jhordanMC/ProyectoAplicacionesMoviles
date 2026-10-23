package com.example.saasvilcas.feature.auth.data;

import com.example.saasvilcas.feature.auth.domain.*;
import com.example.saasvilcas.shared.network.ApiClient;
import com.example.saasvilcas.shared.network.ApiException;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.json.*;

/** Adapts the FastAPI IAM contract to the domain, independent of Android views. */
public final class RemoteAuthRepository implements AuthRepository {
    private final ApiClient api;
    public RemoteAuthRepository(ApiClient api) { this.api=api; }
    @Override public LoginChallenge login(String email,String password) throws AuthException {
        try {
            String form="username="+URLEncoder.encode(email,"UTF-8")+"&password="+URLEncoder.encode(password,"UTF-8");
            JSONObject result=new JSONObject(api.request("POST","/iam/auth/login","application/x-www-form-urlencoded",form,null));
            return new LoginChallenge(result.getString("login_token"),result.getInt("expira_en_segundos"));
        } catch(ApiException error) { throw new AuthException(error.getMessage()); }
        catch(Exception error) { throw new AuthException("La API devolvió una respuesta de login inválida."); }
    }
    @Override public AuthSession verify(String challengeToken,String code) throws AuthException {
        String refresh=null;
        boolean verified=false;
        try {
            JSONObject body=new JSONObject().put("login_token",challengeToken).put("codigo",code);
            String response=api.request("POST","/iam/auth/verificar-2fa","application/json",body.toString(),null);
            verified=true;
            JSONObject tokens=new JSONObject(response);
            String access=tokens.getString("access_token"); refresh=tokens.getString("refresh_token");
            if(!"bearer".equalsIgnoreCase(tokens.optString("token_type","bearer"))) throw new JSONException("Unsupported token type");
            JSONObject profile=new JSONObject(api.request("GET","/iam/mi-perfil",null,null,access));
            if(!"activo".equals(profile.getString("estado"))) throw new IllegalArgumentException("La cuenta no está activa.");
            ArrayList<String> roles=new ArrayList<>(); JSONArray values=profile.getJSONArray("roles");
            for(int i=0;i<values.length();i++) roles.add(values.getString(i));
            return new AuthSession(access,refresh,profile.getString("id_usuario"),profile.getString("id_empresa"),
                (profile.getString("nombres")+" "+profile.getString("apellidos")).trim(),profile.getString("email"),
                profile.getString("nombre_empresa"),roles);
        } catch(Exception error) {
            if(refresh!=null) try { logout(refresh); } catch(AuthException ignored) { }
            String message=error instanceof ApiException || error instanceof IllegalArgumentException?
                    error.getMessage():"La API devolvió una respuesta de verificación inválida.";
            if(verified) message += " Vuelve a iniciar sesión.";
            throw new AuthException(message,verified);
        }
    }
    @Override public void logout(String refreshToken) throws AuthException {
        try { api.request("POST","/iam/auth/logout","application/json",new JSONObject().put("refresh_token",refreshToken).toString(),null); }
        catch(ApiException error) { throw new AuthException(error.getMessage()); }
        catch(JSONException error) { throw new AuthException("No se pudo cerrar la sesión remota."); }
    }
    @Override public void recover(String email) throws AuthException {
        try {
            api.request("POST","/iam/auth/recuperar-password","application/json",new JSONObject().put("email",email.trim()).toString(),null);
        } catch(ApiException error) {throw new AuthException(error.getMessage());}
        catch(JSONException error) {throw new AuthException("Revisa el correo electrónico.");}
    }
}

