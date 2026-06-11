package com.roadwatch.mobile.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.roadwatch.mobile.workers.SyncWorker;

public class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";
    private static final String SYNC_WORK_NAME = "roadwatch_upload_flush";

    private static NetworkMonitor instance;

    private final Context appContext;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> onlineLiveData = new MutableLiveData<>(false);
    private ConnectivityManager.NetworkCallback networkCallback;

    private NetworkMonitor(Context context) {
        appContext = context.getApplicationContext();
        connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        onlineLiveData.setValue(isCurrentlyOnline());
    }

    public static synchronized NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            // FIXED: Always use application context to prevent memory leaks
            instance = new NetworkMonitor(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<Boolean> getOnlineLiveData() {
        return onlineLiveData;
    }

    public boolean isCurrentlyOnline() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public void start() {
        if (networkCallback != null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "Network available €” flushing upload queue");
                onlineLiveData.postValue(true);
                enqueueSync();
            }

            @Override
            public void onLost(Network network) {
                onlineLiveData.postValue(isCurrentlyOnline());
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
        onlineLiveData.postValue(isCurrentlyOnline());
        if (Boolean.TRUE.equals(onlineLiveData.getValue())) {
            enqueueSync();
        }
    }

    public void stop() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering network callback", e);
            } finally {
                networkCallback = null;
            }
        }
    }

    public void flushQueueNow() {
        enqueueSync();
    }

    private void enqueueSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();
        Log.i(TAG, "Enqueueing sync work name=" + SYNC_WORK_NAME + " requestId=" + work.getId());
        WorkManager.getInstance(appContext).enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                work
        );
    }
}
