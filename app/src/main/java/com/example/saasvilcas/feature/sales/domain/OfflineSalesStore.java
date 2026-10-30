package com.example.saasvilcas.feature.sales.domain;

import java.util.List;

/** Device storage for the sales queue and the last catalog seen online, scoped by company. */
public interface OfflineSalesStore {
    void add(PendingSale sale);
    void update(PendingSale sale);
    void delete(String id);
    PendingSale find(String id);
    /** Oldest first, so sales reach the server in the order they happened. */
    List<PendingSale> list(String companyId,String userId);
    void saveSites(String companyId,String json);
    String sites(String companyId);
    /** Upserts raw product objects; a non-negative position keeps the server's catalog order. */
    void saveProducts(String companyId,List<String> ids,List<String> json,int firstPosition);
    List<String> products(String companyId);
}
