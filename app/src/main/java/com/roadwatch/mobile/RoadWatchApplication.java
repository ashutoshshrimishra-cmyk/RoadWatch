package com.roadwatch.mobile;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.roadwatch.mobile.location.LocationService;
import com.google.firebase.FirebaseApp;
import com.roadwatch.mobile.network.NetworkMonitor;
import com.roadwatch.mobile.notifications.NotificationChannels;
import com.roadwatch.mobile.settings.LocaleHelper;
import com.roadwatch.mobile.settings.SettingsManager;

public class RoadWatchApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Ensure Firebase is initialized early so Firebase APIs (FCM, etc.)
        // can be used safely from activities and services.
        FirebaseApp.initializeApp(this);

        // Apply persisted theme so launcher / login screen pick it up.
        boolean dark = new SettingsManager(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        LocationService.getInstance(this).start();
        NetworkMonitor.getInstance(this).start();
        NotificationChannels.ensureCreated(this);
    }
}
