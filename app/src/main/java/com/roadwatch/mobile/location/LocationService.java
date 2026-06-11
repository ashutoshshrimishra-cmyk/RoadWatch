package com.roadwatch.mobile.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.LastLocationEntity;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocationService {

    private static final String TAG = "LocationService";
    private static LocationService instance;

    private final Context appContext;
    private final FusedLocationProviderClient fusedClient;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // FIXED: Use WeakReference to prevent memory leaks
    private java.lang.ref.WeakReference<LocationCallback> locationCallbackRef;
    private volatile LastLocationEntity cached;

    private LocationService(Context context) {
        // FIXED: Always use application context to prevent leaks
        appContext = context.getApplicationContext();
        fusedClient = LocationServices.getFusedLocationProviderClient(appContext);
        loadCachedFromRoom();
    }

    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            // FIXED: Always use application context for singleton
            instance = new LocationService(context.getApplicationContext());
        }
        return instance;
    }

    public void start() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission missing");
            return;
        }
        if (locationCallbackRef != null && locationCallbackRef.get() != null) {
            Log.d(TAG, "Location updates already started");
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
                .setMinUpdateIntervalMillis(15_000L)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    persistLocation(location.getLatitude(), location.getLongitude());
                }
            }
        };

        // FIXED: Store in WeakReference to prevent leaks
        locationCallbackRef = new java.lang.ref.WeakReference<>(locationCallback);

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) {
                    persistLocation(loc.getLatitude(), loc.getLongitude());
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to start location updates", e);
        }
    }

    public void stop() {
        // FIXED: Properly clean up weak reference
        if (locationCallbackRef != null && locationCallbackRef.get() != null) {
            try {
                fusedClient.removeLocationUpdates(locationCallbackRef.get());
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates", e);
            }
            locationCallbackRef.clear();
            locationCallbackRef = null;
        }
    }

    // FIXED: Add cleanup method for proper resource management
    public void cleanup() {
        stop();
        if (ioExecutor != null && !ioExecutor.isShutdown()) {
            ioExecutor.shutdown();
            try {
                if (!ioExecutor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    ioExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                ioExecutor.shutdownNow();
            }
        }
    }

    /** In-memory only €” safe on main thread. */
    public LastLocationEntity getCachedLocation() {
        return cached;
    }

    public void getCachedLocationAsync(LocationListener listener) {
        if (cached != null) {
            mainHandler.post(() -> listener.onLocation(cached));
            return;
        }
        ioExecutor.execute(() -> {
            try {
                LastLocationEntity fromDb = AppDatabase.getDatabase(appContext)
                        .lastLocationDao()
                        .getLastLocation();
                if (fromDb != null) {
                    cached = fromDb;
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to read cached location", e);
            }
            LastLocationEntity result = cached;
            mainHandler.post(() -> listener.onLocation(result));
        });
    }

    public void refreshCurrentLocation(LocationListener listener) {
        if (!hasLocationPermission()) {
            getCachedLocationAsync(listener);
            return;
        }
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            persistLocation(location.getLatitude(), location.getLongitude());
                            listener.onLocation(cached);
                        } else {
                            getCachedLocationAsync(listener);
                        }
                    })
                    .addOnFailureListener(e -> getCachedLocationAsync(listener));
        } catch (SecurityException e) {
            getCachedLocationAsync(listener);
        }
    }

    public interface LocationListener {
        void onLocation(LastLocationEntity location);
    }

    private void loadCachedFromRoom() {
        ioExecutor.execute(() -> {
            try {
                cached = AppDatabase.getDatabase(appContext).lastLocationDao().getLastLocation();
            } catch (Exception e) {
                Log.e(TAG, "Failed to load cached location", e);
            }
        });
    }

    private void persistLocation(double lat, double lng) {
        LastLocationEntity entity = new LastLocationEntity();
        entity.latitude = lat;
        entity.longitude = lng;
        entity.updatedAt = System.currentTimeMillis();
        cached = entity;
        ioExecutor.execute(() -> {
            try {
                AppDatabase.getDatabase(appContext).lastLocationDao().upsert(entity);
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist location", e);
            }
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }
}
