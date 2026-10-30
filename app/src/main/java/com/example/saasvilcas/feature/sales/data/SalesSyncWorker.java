package com.example.saasvilcas.feature.sales.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.example.saasvilcas.VilcasApplication;
import java.util.concurrent.TimeUnit;

/** Sends the offline sales queue when there is network, even if the app was closed after the sale. */
public final class SalesSyncWorker extends Worker {
    private static final String NAME="vilcas-sincronizar-ventas";
    public SalesSyncWorker(@NonNull Context context,@NonNull WorkerParameters parameters) { super(context,parameters); }
    @NonNull @Override public Result doWork() {
        VilcasApplication app=(VilcasApplication)getApplicationContext();
        return app.offlineSales.sync()?Result.retry():Result.success();
    }
    public static void schedule(Context context) {
        OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(SalesSyncWorker.class)
                .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(NAME,ExistingWorkPolicy.APPEND_OR_REPLACE,request);
    }
}
