package com.example.saasvilcas.feature.sales.data;
import com.example.saasvilcas.feature.sales.domain.SalesException;
/** Resolves a local camera/gallery URI to a bounded JPEG, outside the UI thread. */
public interface ReceiptSource { byte[] jpeg(String localUri) throws SalesException; }
