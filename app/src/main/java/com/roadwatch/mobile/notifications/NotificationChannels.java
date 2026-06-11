package com.roadwatch.mobile.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Centralised registration for the notification channels used by RoadWatch.
 *
 * Android 8.0+ requires a channel for every notification. Calling
 * {@link #ensureCreated(Context)} on app start is idempotent €” re-creating
 * an existing channel updates only the user-mutable parts (name, description).
 */
public final class NotificationChannels {

    public static final String CHANNEL_COMPLAINTS = "rw_complaint_updates";
    public static final String CHANNEL_BUDGETS = "rw_budget_alerts";

    private NotificationChannels() {}

    public static void ensureCreated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel complaints = new NotificationChannel(
                CHANNEL_COMPLAINTS,
                "Complaint Updates",
                NotificationManager.IMPORTANCE_HIGH);
        complaints.setDescription("Status changes on potholes you've reported");
        complaints.enableLights(true);
        complaints.enableVibration(true);

        NotificationChannel budgets = new NotificationChannel(
                CHANNEL_BUDGETS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        budgets.setDescription("Updates on road maintenance budgets in your area");

        manager.createNotificationChannel(complaints);
        manager.createNotificationChannel(budgets);
    }
}
