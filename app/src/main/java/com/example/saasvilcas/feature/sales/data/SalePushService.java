package com.example.saasvilcas.feature.sales.data;

import androidx.annotation.NonNull;
import com.example.saasvilcas.VilcasApplication;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * HU-19: the backend sends a Firebase data message when a sale is confirmed. Android wakes the app even when it is
 * closed, and the usual check builds the notification with the product photo. The message only carries the sale id.
 */
public final class SalePushService extends FirebaseMessagingService {
    @Override public void onMessageReceived(@NonNull RemoteMessage message) {
        if("venta".equals(message.getData().get("tipo"))) ((VilcasApplication)getApplication()).notifyNewSales();
    }
    @Override public void onNewToken(@NonNull String token) { ((VilcasApplication)getApplication()).registerSalePush(token); }
}
