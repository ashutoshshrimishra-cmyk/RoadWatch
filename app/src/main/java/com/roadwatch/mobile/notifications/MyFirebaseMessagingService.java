package com.roadwatch.mobile.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.NotificationEntity;
import com.roadwatch.mobile.ui.complaints.ComplaintDetailActivity;
import com.roadwatch.mobile.ui.dashboard.MainActivity;
import com.roadwatch.mobile.ui.notifications.NotificationInboxActivity;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles incoming FCM payloads.
 *
 * Expected data shape from backend (data-only message €” survives Doze better
 * than notification messages and gives us full control of the system tray UI):
 *   {
 *     "title":       "Complaint Resolved! ",
 *     "body":        "The pothole at NH-24 has been fixed. Tap to view.",
 *     "channel":     "complaint" | "budget",
 *     "complaintId": "1234"   // optional €” drives deep-link target
 *   }
 *
 * If the backend ever sends a {@code notification} block, we fall back to that
 * for title/body so display still works.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RWMessagingService";
    private static final AtomicInteger NOTIFICATION_ID_SEED = new AtomicInteger(1000);

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "FCM onNewToken €” pushing to backend");
        FcmTokenManager.onNewToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Map<String, String> data = message.getData();
        RemoteMessage.Notification notif = message.getNotification();

        String title = pick(data, "title",
                notif != null ? notif.getTitle() : null,
                "RoadWatch update");
        String body = pick(data, "body",
                notif != null ? notif.getBody() : null,
                "");
        String channelKey = data.get("channel");
        Long complaintId = parseLong(data.get("complaintId"));

        String channelId = "budget".equalsIgnoreCase(channelKey)
                ? NotificationChannels.CHANNEL_BUDGETS
                : NotificationChannels.CHANNEL_COMPLAINTS;

        Log.i(TAG, "Push received: title=" + title
                + " channel=" + channelId
                + " complaintId=" + complaintId);

        // Make sure channels exist even on a cold-started service.
        NotificationChannels.ensureCreated(this);

        // Persist to local inbox first so the badge count reflects the new
        // alert even if the user dismisses the system notification.
        persistToInbox(title, body, channelId, complaintId);

        // Show system notification.
        showSystemNotification(title, body, channelId, complaintId);
    }

    private void persistToInbox(String title, String body, String channelId, Long complaintId) {
        dbExecutor.execute(() -> {
            try {
                NotificationEntity entity = new NotificationEntity();
                entity.title = title;
                entity.body = body;
                entity.channel = channelId;
                entity.complaintId = complaintId;
                entity.receivedAt = System.currentTimeMillis();
                entity.read = false;
                AppDatabase.getDatabase(getApplicationContext())
                        .notificationDao()
                        .insert(entity);
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist notification to inbox", e);
            }
        });
    }

    private void showSystemNotification(String title, String body,
                                        String channelId, Long complaintId) {
        Intent target = buildTargetIntent(complaintId);
        target.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (int) System.currentTimeMillis();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, requestCode, target, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_camera) // RoadWatch monochrome icon
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_SEED.incrementAndGet(), builder.build());
        }
    }

    /**
     * Where the notification-tap should land:
     *   - With a complaintId ’ ComplaintDetailActivity for that report.
     *   - Otherwise ’ NotificationInboxActivity, opened on top of the dashboard.
     */
    private Intent buildTargetIntent(Long complaintId) {
        if (complaintId != null) {
            Intent intent = new Intent(this, ComplaintDetailActivity.class);
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaintId.longValue());
            return intent;
        }
        return new Intent(this, NotificationInboxActivity.class);
    }

    private static String pick(Map<String, String> data, String key,
                               String fallback1, String fallback2) {
        if (data != null) {
            String v = data.get(key);
            if (!TextUtils.isEmpty(v)) return v;
        }
        if (!TextUtils.isEmpty(fallback1)) return fallback1;
        return fallback2;
    }

    private static Long parseLong(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
