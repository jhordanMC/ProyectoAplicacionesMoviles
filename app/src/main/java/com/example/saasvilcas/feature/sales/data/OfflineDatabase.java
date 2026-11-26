package com.example.saasvilcas.feature.sales.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import com.example.saasvilcas.feature.sales.domain.OfflineSalesStore;
import com.example.saasvilcas.feature.sales.domain.PendingSale;
import com.example.saasvilcas.feature.sales.domain.SaleNotice;
import com.example.saasvilcas.feature.sales.domain.SaleNoticeStore;
import java.util.ArrayList;
import java.util.List;

/** Room database for HU-18 and HU-19. The file is excluded from cloud backups and device transfers (see res/xml). */
@Database(entities={OfflineDatabase.PendingSaleRow.class,OfflineDatabase.SitesRow.class,OfflineDatabase.ProductRow.class,OfflineDatabase.NoticeRow.class},
        version=2,exportSchema=false)
public abstract class OfflineDatabase extends RoomDatabase {
    public static final String NAME="vilcas_offline.db";
    public abstract Dao dao();
    /** Keeps the sales queue of phones that already had version 1. */
    public static final androidx.room.migration.Migration MIGRATION_1_2=new androidx.room.migration.Migration(1,2) {
        @Override public void migrate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `avisos_venta` (`id` TEXT NOT NULL, `companyId` TEXT NOT NULL, `userId` TEXT NOT NULL, `site` TEXT NOT NULL, `seller` TEXT NOT NULL, `summary` TEXT NOT NULL, `payments` TEXT NOT NULL, `totalCents` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `read` INTEGER NOT NULL, PRIMARY KEY(`id`, `userId`))");
        }
    };

    public static OfflineDatabase open(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),OfflineDatabase.class,NAME).addMigrations(MIGRATION_1_2).build();
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

    @Entity(tableName="avisos_venta",primaryKeys={"id","userId"})
    public static final class NoticeRow {
        @NonNull public String id="",companyId="",userId="",site="",seller="",summary="",payments="";
        public long totalCents,createdAt;
        public boolean read;
    }

    @androidx.room.Dao
    public interface Dao {
        @Query("SELECT * FROM avisos_venta WHERE id=:id AND userId=:user") NoticeRow findNotice(String id,String user);
        @Query("SELECT * FROM avisos_venta WHERE companyId=:company AND userId=:user ORDER BY createdAt DESC, id") List<NoticeRow> notices(String company,String user);
        @Insert(onConflict=OnConflictStrategy.REPLACE) void saveNotice(NoticeRow row);
        @Query("UPDATE avisos_venta SET read=1 WHERE companyId=:company AND userId=:user") void markAllRead(String company,String user);
        @Query("DELETE FROM avisos_venta WHERE companyId=:company AND userId=:user AND id NOT IN (SELECT id FROM avisos_venta WHERE companyId=:company AND userId=:user ORDER BY createdAt DESC LIMIT :keep)") void trimNotices(String company,String user,int keep);
        @Query("DELETE FROM avisos_venta WHERE companyId=:company AND userId=:user") void clearNotices(String company,String user);
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
    public static final class Store implements OfflineSalesStore,SaleNoticeStore {
        @Override public SaleNotice findNotice(String id,String userId) { NoticeRow row=database.dao().findNotice(id,userId); return row==null?null:notice(row); }
        @Override public List<SaleNotice> notices(String companyId,String userId) {
            ArrayList<SaleNotice> notices=new ArrayList<>();
            for(NoticeRow row:database.dao().notices(companyId,userId)) notices.add(notice(row));
            return notices;
        }
        @Override public void saveNotice(SaleNotice notice) {
            NoticeRow row=new NoticeRow();
            row.id=notice.id; row.companyId=notice.companyId; row.userId=notice.userId; row.site=notice.site; row.seller=notice.seller;
            row.summary=notice.summary; row.payments=notice.payments; row.totalCents=notice.totalCents; row.createdAt=notice.createdAt; row.read=notice.read;
            database.dao().saveNotice(row);
        }
        @Override public void markAllRead(String companyId,String userId) { database.dao().markAllRead(companyId,userId); }
        @Override public void trimNotices(String companyId,String userId,int keep) { database.dao().trimNotices(companyId,userId,keep); }
        @Override public void clearNotices(String companyId,String userId) { database.dao().clearNotices(companyId,userId); }
        private static SaleNotice notice(NoticeRow row) {
            return new SaleNotice(row.id,row.companyId,row.userId,row.site,row.seller,row.summary,row.payments,row.totalCents,row.createdAt,row.read);
        }
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
