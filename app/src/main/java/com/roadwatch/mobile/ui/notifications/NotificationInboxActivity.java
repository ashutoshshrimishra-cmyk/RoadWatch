package com.roadwatch.mobile.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.NotificationDao;
import com.roadwatch.mobile.data.NotificationEntity;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.NotificationDto;
import com.roadwatch.mobile.network.dto.PagedNotificationsDto;
import com.roadwatch.mobile.ui.BaseActivity;
import com.roadwatch.mobile.ui.complaints.ComplaintDetailActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * In-app inbox that syncs with the backend notification API.
 *
 * Flow:
 *   1. On open: show loading spinner
 *   2. Fetch from GET /api/citizen/me/notifications
 *   3. Save new notifications to local Room DB (dedup via serverId)
 *   4. LiveData observer displays from Room DB
 *   5. Mark all as read on backend when opening inbox
 *
 * Top-right menu offers "Mark all read" and "Clear all".
 */
public class NotificationInboxActivity extends BaseActivity {

    private static final String TAG = "NotificationInbox";

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private NotificationAdapter adapter;
    private NotificationDao dao;
    private LinearLayout emptyState;
    private RecyclerView rv;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_inbox);
        setupToolbar("Notifications");

        emptyState = findViewById(R.id.emptyState);
        rv = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);

        adapter = new NotificationAdapter(this::onNotificationClicked);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        dao = AppDatabase.getDatabase(this).notificationDao();
        dao.observeAll().observe(this, list -> {
            adapter.submit(list);
            boolean empty = list == null || list.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        syncFromBackend();
    }

    /**
     * Fetch notifications from backend, persist new ones to Room, then
     * call mark-read so the server knows the user opened the inbox.
     */
    private void syncFromBackend() {
        if (!new SessionManager(this).isLoggedIn()) {
            Log.d(TAG, "User not logged in — skipping backend sync");
            return;
        }

        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));

        ApiService api = ApiClient.api(this);
        api.getMyNotifications(0, 50).enqueue(new Callback<PagedNotificationsDto>() {
            @Override
            public void onResponse(@NonNull Call<PagedNotificationsDto> call,
                                   @NonNull Response<PagedNotificationsDto> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().content != null) {
                    List<NotificationDto> serverNotifs = response.body().content;
                    Log.i(TAG, "Fetched " + serverNotifs.size() + " notifications from backend");

                    dbExecutor.execute(() -> {
                        try {
                            persistNewNotifications(serverNotifs);
                        } catch (Exception e) {
                            Log.e(TAG, "Error persisting synced notifications", e);
                        }
                        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    });

                    // Mark all as read on backend since user opened inbox
                    markAllReadOnBackend(api);
                } else {
                    Log.w(TAG, "Backend notifications fetch failed http=" + response.code());
                    runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PagedNotificationsDto> call, @NonNull Throwable t) {
                Log.w(TAG, "Backend notifications network error: " + t.getMessage());
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));
            }
        });
    }

    /**
     * Insert backend notifications into Room, skipping duplicates
     * (matched by serverId).
     */
    private void persistNewNotifications(List<NotificationDto> serverNotifs) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        int inserted = 0;

        for (NotificationDto dto : serverNotifs) {
            if (dto.id == null) continue;

            // Skip if already persisted (matched by server ID)
            if (dao.countByServerId(dto.id) > 0) continue;

            NotificationEntity entity = new NotificationEntity();
            entity.serverId = dto.id;
            entity.title = dto.title != null ? dto.title : "Notification";
            entity.body = dto.body != null ? dto.body : "";
            entity.channel = "rw_complaint_updates";
            entity.complaintId = dto.complaintId;
            entity.read = dto.read;

            // Parse createdAt to millis; fall back to current time
            if (dto.createdAt != null) {
                try {
                    Date parsed = sdf.parse(dto.createdAt);
                    entity.receivedAt = parsed != null ? parsed.getTime() : System.currentTimeMillis();
                } catch (Exception e) {
                    entity.receivedAt = System.currentTimeMillis();
                }
            } else {
                entity.receivedAt = System.currentTimeMillis();
            }

            dao.insert(entity);
            inserted++;
        }

        Log.i(TAG, "Synced " + inserted + " new notifications from backend to Room");
    }

    /**
     * Tell the backend the user has seen all notifications.
     */
    private void markAllReadOnBackend(ApiService api) {
        api.markNotificationsRead().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                                   @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "Backend mark-all-read succeeded");
                } else {
                    Log.w(TAG, "Backend mark-all-read failed http=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "Backend mark-all-read network error: " + t.getMessage());
            }
        });
    }

    private void onNotificationClicked(NotificationEntity item) {
        Log.i(TAG, "Notification tapped id=" + item.id
                + " complaintId=" + item.complaintId);

        // Mark read off the main thread.
        if (!item.read) {
            dbExecutor.execute(() -> dao.markRead(item.id));
        }

        if (item.complaintId != null) {
            Intent intent = new Intent(this, ComplaintDetailActivity.class);
            intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, item.complaintId);
            startActivity(intent);
        }
    }

    // —– Menu —–

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inbox, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_mark_all_read) {
            dbExecutor.execute(() -> dao.markAllRead());
            // Also sync to backend
            markAllReadOnBackend(ApiClient.api(this));
            return true;
        }
        if (id == R.id.action_clear_inbox) {
            new AlertDialog.Builder(this)
                    .setTitle("Clear all notifications?")
                    .setMessage("This removes the entire inbox history. The unread badge will reset to zero.")
                    .setPositiveButton("Clear", (d, w) -> dbExecutor.execute(() -> dao.clearAll()))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
