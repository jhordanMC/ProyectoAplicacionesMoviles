package com.example.saasvilcas.feature.sales.domain;

/**
 * Sale recorded without connection. The id doubles as the idempotency key, so resending it after a lost
 * response cannot create a second sale. The payload is the confirm request body; digital payments keep a
 * local receipt file until it is uploaded.
 */
public final class PendingSale {
    public enum Status { PENDING, CONFLICT, FAILED }
    public final String id,companyId,userId,registerId,payload,summary,message;
    public final long createdAt,totalCents;
    public final Status status;
    public final int attempts;
    public PendingSale(String id,String companyId,String userId,String registerId,String payload,String summary,
            long createdAt,long totalCents,Status status,String message,int attempts) {
        this.id=id; this.companyId=companyId; this.userId=userId; this.registerId=registerId; this.payload=payload;
        this.summary=summary; this.createdAt=createdAt; this.totalCents=totalCents; this.status=status;
        this.message=message==null?"":message; this.attempts=attempts;
    }
    public PendingSale with(Status status,String message,int attempts) {
        return new PendingSale(id,companyId,userId,registerId,payload,summary,createdAt,totalCents,status,message,attempts);
    }
    public PendingSale withPayload(String payload) {
        return new PendingSale(id,companyId,userId,registerId,payload,summary,createdAt,totalCents,status,message,attempts);
    }
}
