package com.example.saasvilcas.feature.auth.domain;

/** Account snapshots contain tokens and profile identity, never passwords or pending 2FA codes. */
public final class SessionStore {
    public interface Persistence {
        Snapshot read();
        void write(Snapshot snapshot);
    }
    public static final class Snapshot {
        public final String activeId;
        public final java.util.List<AuthSession> accounts;
        public Snapshot(String activeId,java.util.List<AuthSession> accounts) {
            this.activeId=activeId; this.accounts=new java.util.ArrayList<>(accounts);
        }
    }
    private Persistence persistence;
    private volatile AuthSession current;
    private final java.util.LinkedHashMap<String,AuthSession> accounts=new java.util.LinkedHashMap<>();
    public AuthSession get() { return current; }
    public synchronized void attach(Persistence persistence) {
        Snapshot saved=persistence.read(); this.persistence=persistence;
        accounts.clear(); for(AuthSession account:saved.accounts) accounts.put(account.userId,account);
        current=accounts.get(saved.activeId);
    }
    private void persist() { if(persistence!=null) persistence.write(new Snapshot(current==null?"":current.userId,accounts())); }
    public synchronized void set(AuthSession session) { current=session; if(session!=null) accounts.put(session.userId,session); persist(); }
    public synchronized java.util.List<AuthSession> accounts() { return new java.util.ArrayList<>(accounts.values()); }
    public synchronized AuthSession forget(String id) { if(current!=null && current.userId.equals(id)) return null; AuthSession old=accounts.remove(id); persist(); return old; }
    public synchronized AuthSession clear() { AuthSession old=current; current=null; if(old!=null) accounts.remove(old.userId); persist(); return old; }
}
