package com.example.saasvilcas.shared.network;



public interface ApiClient {
    String request(String method,String path,String contentType,String body,String bearer) throws ApiException;
    default String upload(String path,String fileName,String mime,byte[] bytes,String bearer) throws ApiException {
        throw new ApiException("La subida de archivos no está disponible.");
    }
}

