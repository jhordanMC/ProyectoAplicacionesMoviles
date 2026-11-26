package com.example.saasvilcas.feature.sales.domain;

import java.util.List;

/** Device storage for the owner's sale notifications, scoped by company and user. */
public interface SaleNoticeStore {
    SaleNotice findNotice(String id,String userId);
    /** Newest first. */
    List<SaleNotice> notices(String companyId,String userId);
    void saveNotice(SaleNotice notice);
    void markAllRead(String companyId,String userId);
    void trimNotices(String companyId,String userId,int keep);
    void clearNotices(String companyId,String userId);
}
