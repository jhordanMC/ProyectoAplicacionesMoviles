package com.example.saasvilcas.feature.sales.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.example.saasvilcas.VilcasApplication;
import java.util.concurrent.TimeUnit;

/** Checks for new sales with the app closed. Android does not run periodic work more often than every 15 minutes. */
public final class SaleNoticeWorker extends Worker {
    private static final String NAME="vilcas-avisos-venta";
    public SaleNoticeWorker(@NonNull Context context,@NonNull WorkerParameters parameters) { super(context,parameters); }
    @NonNull @Override public Result doWork() {
        ((VilcasApplication)getApplicationContext()).notifyNewSales();
        return Result.success();
    }
    public static void schedule(Context context) {
        PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(SaleNoticeWorker.class,15,TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME,ExistingPeriodicWorkPolicy.KEEP,request);
    }
    public static void cancel(Context context) { WorkManager.getInstance(context).cancelUniqueWork(NAME); }
}
