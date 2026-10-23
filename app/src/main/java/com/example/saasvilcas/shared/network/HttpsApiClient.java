package com.example.saasvilcas.shared.network;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Platform HTTPS transport: no credential logging, redirects, retries or main-thread I/O. */
public final class HttpsApiClient implements ApiClient {
    private final String baseUrl;
    public HttpsApiClient(String baseUrl) {
        if(!baseUrl.startsWith("https://")) throw new IllegalArgumentException("La API debe usar HTTPS.");
        this.baseUrl=baseUrl.replaceAll("/+$","");
    }
    @Override public String request(String method,String path,String contentType,String body,String bearer) throws ApiException {
        return send(method,path,contentType,body==null?null:body.getBytes(StandardCharsets.UTF_8),bearer);
    }
    @Override public String upload(String path,String fileName,String mime,byte[] bytes,String bearer) throws ApiException {
        if(fileName==null||!fileName.matches("[A-Za-z0-9._-]+")||!"image/jpeg".equals(mime)) throw new ApiException("El formato de imagen no es válido.");
        String boundary="Vilcas"+java.util.UUID.randomUUID().toString().replace("-","");
        try {
            ByteArrayOutputStream body=new ByteArrayOutputStream();
            body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\""+fileName+"\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(bytes); body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            return send("POST",path,"multipart/form-data; boundary="+boundary,body.toByteArray(),bearer);
        } catch(IOException error) { throw new ApiException("No se pudo preparar la imagen."); }
    }
    private String send(String method,String path,String contentType,byte[] bytes,String bearer) throws ApiException {
        HttpsURLConnection connection=null;
        try {
            connection=(HttpsURLConnection)new URL(baseUrl+path).openConnection();
            connection.setRequestMethod(method); connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept","application/json");
            if(bearer!=null) connection.setRequestProperty("Authorization","Bearer "+bearer);
            if(bytes!=null) {
                connection.setDoOutput(true); connection.setRequestProperty("Content-Type",contentType);
                connection.setFixedLengthStreamingMode(bytes.length);
                try(OutputStream output=connection.getOutputStream()) { output.write(bytes); }
            }
            int status=connection.getResponseCode();
            String response=read(status>=200 && status<300?connection.getInputStream():connection.getErrorStream());
            if(status<200 || status>=300) throw new ApiException(errorMessage(status,response),status);
            return response;
        } catch(SocketTimeoutException error) {
            throw new ApiException("La conexión tardó demasiado. Inténtalo nuevamente.");
        } catch(IOException error) {
            throw new ApiException("No se pudo conectar con VILCAS. Revisa tu conexión a internet.");
        } finally { if(connection!=null) connection.disconnect(); }
    }
    private String read(InputStream input) throws IOException {
        if(input==null) return "";
        try(InputStream stream=input; ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int count;
            while((count=stream.read(buffer))!=-1) {
                if(output.size()+count>1_048_576) throw new IOException("Response too large");
                output.write(buffer,0,count);
            }
            return new String(output.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    private String errorMessage(int status,String response) {
        if(status>=500) return "El servicio no está disponible en este momento. Inténtalo más tarde.";
        if(status==429) return "Demasiados intentos. Espera unos minutos antes de volver a intentar.";
        if(status==400 || status==401 || status==403 || status==409 || status==413 || status==422) {
            try {
                Object detail=new JSONObject(response).opt("detail");
                if(detail instanceof String && !((String)detail).isEmpty() && ((String)detail).length()<=240)
                    return (String)detail;
            } catch(Exception ignored) { }
        }
        if(status==401) return "Tu sesión expiró. Vuelve a iniciar sesión.";
        if(status==403) return "Tu cuenta no tiene permiso para realizar esta operación.";
        if(status==409) return "El stock cambió. Actualiza el carrito antes de confirmar.";
        if(status==422) return "Revisa los datos enviados. No cumplen las validaciones del servidor.";
        if(status==413) return "El comprobante supera el tamaño permitido.";
        return "No se pudo completar la operación.";
    }
}

