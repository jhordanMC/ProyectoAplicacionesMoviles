package com.example.saasvilcas.feature.sales.data;

import com.example.saasvilcas.shared.network.ApiClient;
import com.example.saasvilcas.shared.network.ApiException;
import org.json.JSONException;
import org.json.JSONObject;

/** Tells the backend which phone receives the owner's sale pushes (POST /notificaciones/dispositivos). */
public final class SalePushRegistration {
    public interface Session { String token(); boolean refresh(); }
    private final ApiClient api;
    private final Session session;
    public SalePushRegistration(ApiClient api,Session session) { this.api=api; this.session=session; }

    /** False when it could not register; the next sign-in or token change tries again. */
    public boolean register(String pushToken) {
        String bearer=session.token();
        if(bearer==null || pushToken==null || pushToken.isEmpty()) return false;
        try {
            send("/notificaciones/dispositivos",pushToken,bearer);
            return true;
        } catch(ApiException error) {
            if(error.status!=401 || !session.refresh()) return false;
            try { send("/notificaciones/dispositivos",pushToken,session.token()); return true; }
            catch(ApiException again) { return false; }
        }
    }
    /** On sign-out, with the session that is closing; failures are ignored because the token stays tied to that user only. */
    public void remove(String pushToken,String bearer) {
        if(bearer==null || pushToken==null || pushToken.isEmpty()) return;
        try { send("/notificaciones/dispositivos/eliminar",pushToken,bearer); } catch(ApiException ignored) { }
    }
    private void send(String path,String pushToken,String bearer) throws ApiException {
        String body;
        try { body=new JSONObject().put("token",pushToken).put("plataforma","android").toString(); }
        catch(JSONException error) { throw new ApiException("Token inválido"); }
        api.request("POST",path,"application/json",body,bearer);
    }
}
