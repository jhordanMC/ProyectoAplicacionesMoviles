package com.example.saasvilcas.feature.sales.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import com.example.saasvilcas.feature.sales.domain.OfflineSalesStore;
import com.example.saasvilcas.feature.sales.domain.PendingSale;
import java.util.ArrayList;
import java.util.List;

/** Room database for HU-18. The file is excluded from cloud backups and device transfers (see res/xml). */
@Database(entities={OfflineDatabase.PendingSaleRow.class,OfflineDatabase.SitesRow.class,OfflineDatabase.ProductRow.class},
        version=1,exportSchema=false)
public abstract class OfflineDatabase extends RoomDatabase {
    public static final String NAME="vilcas_offline.db";
    public abstract Dao dao();

    public static OfflineDatabase open(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),OfflineDatabase.class,NAME).build();
    }

    @Entity(tableName="ventas_pendientes",indices={@Index({"companyId","userId","createdAt"})})
    public static final class PendingSaleRow {
        @PrimaryKey @NonNull public String id="";
        @NonNull public String companyId="",userId="",registerId="",payload="",summary="",status="",message="";
        public long createdAt,totalCents;
        public int attempts;
    }
    @Entity(tableName="sedes_guardadas")
    public static final class SitesRow {
        @PrimaryKey @NonNull public String companyId="";
        @NonNull public String json="";
    }
    @Entity(tableName="productos_guardados",primaryKeys={"companyId","productId"})
    public static final class ProductRow {
        @NonNull public String companyId="",productId="",json="";
        public long position;
    }

    @androidx.room.Dao
    public interface Dao {
        @Insert(onConflict=OnConflictStrategy.ABORT) void insert(PendingSaleRow row);
        @Update void update(PendingSaleRow row);
        @Query("DELETE FROM ventas_pendientes WHERE id=:id") void delete(String id);
        @Query("SELECT * FROM ventas_pendientes WHERE id=:id") PendingSaleRow find(String id);
        @Query("SELECT * FROM ventas_pendientes WHERE companyId=:company AND userId=:user ORDER BY createdAt, id")
        List<PendingSaleRow> list(String company,String user);
        @Insert(onConflict=OnConflictStrategy.REPLACE) void saveSites(SitesRow row);
        @Query("SELECT json FROM sedes_guardadas WHERE companyId=:company") String sites(String company);
        @Query("SELECT position FROM productos_guardados WHERE companyId=:company AND productId=:product") Long position(String company,String product);
        @Query("SELECT COALESCE(MAX(position),-1) FROM productos_guardados WHERE companyId=:company") long lastPosition(String company);
        @Insert(onConflict=OnConflictStrategy.REPLACE) void saveProduct(ProductRow row);
        @Query("SELECT json FROM productos_guardados WHERE companyId=:company ORDER BY position") List<String> products(String company);
    }

    /** Adapter between the domain store and Room. Must be used outside the main thread. */
    public static final class Store implements OfflineSalesStore {
        private final OfflineDatabase database;
        public Store(OfflineDatabase database) { this.database=database; }
        @Override public void add(PendingSale sale) { database.dao().insert(row(sale)); }
        @Override public void update(PendingSale sale) { database.dao().update(row(sale)); }
        @Override public void delete(String id) { database.dao().delete(id); }
        @Override public PendingSale find(String id) { PendingSaleRow row=database.dao().find(id); return row==null?null:sale(row); }
        @Override public List<PendingSale> list(String companyId,String userId) {
            ArrayList<PendingSale> sales=new ArrayList<>();
            for(PendingSaleRow row:database.dao().list(companyId,userId)) sales.add(sale(row));
            return sales;
        }
        @Override public void saveSites(String companyId,String json) {
            SitesRow row=new SitesRow(); row.companyId=companyId; row.json=json; database.dao().saveSites(row);
        }
        @Override public String sites(String companyId) { return database.dao().sites(companyId); }
        @Override public void saveProducts(String companyId,List<String> ids,List<String> json,int firstPosition) {
            database.runInTransaction(()->{
                long next=database.dao().lastPosition(companyId)+1;
                for(int i=0;i<ids.size();i++) {
                    ProductRow row=new ProductRow(); row.companyId=companyId; row.productId=ids.get(i); row.json=json.get(i);
                    Long known=database.dao().position(companyId,ids.get(i));
                    // Filtered searches update the data but keep the catalog order of the unfiltered list.
                    row.position=firstPosition>=0?firstPosition+i:known!=null?known:next++;
                    database.dao().saveProduct(row);
                }
            });
        }
        @Override public List<String> products(String companyId) { return database.dao().products(companyId); }
        private static PendingSaleRow row(PendingSale sale) {
            PendingSaleRow row=new PendingSaleRow();
            row.id=sale.id; row.companyId=sale.companyId; row.userId=sale.userId; row.registerId=sale.registerId;
            row.payload=sale.payload; row.summary=sale.summary; row.status=sale.status.name(); row.message=sale.message;
            row.createdAt=sale.createdAt; row.totalCents=sale.totalCents; row.attempts=sale.attempts;
            return row;
        }
        private static PendingSale sale(PendingSaleRow row) {
            return new PendingSale(row.id,row.companyId,row.userId,row.registerId,row.payload,row.summary,row.createdAt,
                    row.totalCents,PendingSale.Status.valueOf(row.status),row.message,row.attempts);
        }
    }
}
