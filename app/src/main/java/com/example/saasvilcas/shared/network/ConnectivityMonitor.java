package com.example.saasvilcas.shared.network;

import android.content.Context;
import android.net.*;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/** Process-wide internet availability, from the system's default network. */
public final class ConnectivityMonitor {
    private final MutableLiveData<Boolean> online;
    private volatile boolean current;
    public ConnectivityMonitor(Context context) {
        ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        current=usable(manager.getNetworkCapabilities(manager.getActiveNetwork()));
        online=new MutableLiveData<>(current);
        manager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override public void onCapabilitiesChanged(@NonNull Network network,@NonNull NetworkCapabilities capabilities) { set(usable(capabilities)); }
            @Override public void onLost(@NonNull Network network) { set(false); }
        });
    }
    private static boolean usable(NetworkCapabilities capabilities) {
        return capabilities!=null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
    private void set(boolean value) { if(current!=value) { current=value; online.postValue(value); } }
    public boolean isOnline() { return current; }
    public LiveData<Boolean> online() { return online; }
}
