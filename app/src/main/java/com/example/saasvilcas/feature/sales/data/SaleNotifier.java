package com.example.saasvilcas.feature.sales.data;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.saasvilcas.MainActivity;
import com.example.saasvilcas.R;
import com.example.saasvilcas.feature.sales.domain.SaleNotice;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/** Posts the system notification with the team's design: photo of the product sold, place and seller, and the amount in green. */
public final class SaleNotifier {
    public static final String EXTRA_NOTICE="sale_notice_id";
    private static final String CHANNEL="ventas";
    private static final int MAX_SEPARATE=5,SUMMARY_ID=19;
    private final Context context;
    public SaleNotifier(Context context) {
        this.context=context.getApplicationContext();
        if(Build.VERSION.SDK_INT>=26) {
            NotificationChannel channel=new NotificationChannel(CHANNEL,this.context.getString(R.string.sales_notice_channel),NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(this.context.getString(R.string.sales_notice_channel_description));
            this.context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    public void post(List<SaleNotice> notices) {
        if(notices.isEmpty() || !allowed()) return;
        NotificationManagerCompat manager=NotificationManagerCompat.from(context);
        try {
            if(notices.size()>MAX_SEPARATE) {
                String title=context.getResources().getQuantityString(R.plurals.sales_notice_many,notices.size(),notices.size());
                manager.notify(SUMMARY_ID,base(title,context.getString(R.string.sales_notice_many_message),null).build());
                return;
            }
            for(SaleNotice notice:notices) {
                String place=notice.site;
                if(!notice.seller.isEmpty()) place=(place.isEmpty()?"":place+" • ")+context.getString(R.string.sales_notice_seller_line,notice.seller);
                String amount=money(notice.totalCents);
                // Android strips color and weight from standard text, so the expanded design uses its own view.
                android.widget.RemoteViews design=new android.widget.RemoteViews(context.getPackageName(),R.layout.sales_notification_sale);
                design.setTextViewText(R.id.saleNotificationPlace,place);
                design.setTextViewText(R.id.saleNotificationAmount,amount);
                // The app logo already heads the notification, so the image beside the text is the product sold.
                android.graphics.Bitmap photo=photo(notice.photo);
                design.setViewVisibility(R.id.saleNotificationPhoto,photo==null?android.view.View.GONE:android.view.View.VISIBLE);
                if(photo!=null) design.setImageViewBitmap(R.id.saleNotificationPhoto,photo);
                manager.notify(notice.id.hashCode(),base(context.getString(R.string.sales_notice_title),place.isEmpty()?amount:amount+" • "+place,notice.id)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle()).setCustomBigContentView(design).build());
            }
        } catch(SecurityException ignored) { }
    }
    public void cancelAll() { NotificationManagerCompat.from(context).cancelAll(); }
    private boolean allowed() {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return false;
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
    private NotificationCompat.Builder base(String title,String text,String noticeId) {
        Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(noticeId!=null) open.putExtra(EXTRA_NOTICE,noticeId);
        PendingIntent pending=PendingIntent.getActivity(context,noticeId==null?SUMMARY_ID:noticeId.hashCode(),open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(context,CHANNEL).setSmallIcon(R.drawable.sales_ic_notification)
                .setColor(ContextCompat.getColor(context,R.color.vl_text_green)).setContentTitle(title).setContentText(text)
                .setContentIntent(pending).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }
    /** Square, rounded and small: notifications reject large bitmaps. Runs on the worker thread that posts. */
    private static android.graphics.Bitmap photo(String url) {
        android.graphics.Bitmap source=com.example.saasvilcas.shared.ui.ProductImageLoader.fetch(url,512);
        if(source==null) return null;
        int side=Math.min(source.getWidth(),source.getHeight()),size=192;
        android.graphics.Bitmap result=android.graphics.Bitmap.createBitmap(size,size,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(result);
        android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG|android.graphics.Paint.FILTER_BITMAP_FLAG);
        canvas.drawRoundRect(0,0,size,size,28,28,paint);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        int left=(source.getWidth()-side)/2,top=(source.getHeight()-side)/2;
        canvas.drawBitmap(source,new android.graphics.Rect(left,top,left+side,top+side),new android.graphics.Rect(0,0,size,size),paint);
        return result;
    }
    public static String money(long cents) { return String.format(Locale.US,"S/ %.2f",BigDecimal.valueOf(cents,2)); }
}
