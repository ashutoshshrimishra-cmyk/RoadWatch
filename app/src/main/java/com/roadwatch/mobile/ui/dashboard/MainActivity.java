package com.roadwatch.mobile.ui.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.ComplaintDao;
import com.roadwatch.mobile.network.NetworkMonitor;
import com.roadwatch.mobile.notifications.FcmTokenManager;
import com.roadwatch.mobile.ui.alerts.RoadAlertsActivity;
import com.roadwatch.mobile.ui.auth.LoginActivity;
import com.roadwatch.mobile.ui.authorities.AuthorityListActivity;
import com.roadwatch.mobile.ui.chat.ChatbotActivity;
import com.roadwatch.mobile.ui.complaint.ComplaintFormActivity;
import com.roadwatch.mobile.ui.complaints.ComplaintListActivity;
import com.roadwatch.mobile.ui.notifications.NotificationInboxActivity;
import com.roadwatch.mobile.ui.reports.ReportMapActivityOSM;
import com.roadwatch.mobile.ui.reports.RoadFinancialStatusActivity;
import com.roadwatch.mobile.ui.roads.RoadListActivity;
import com.roadwatch.mobile.ui.settings.ProfileActivity;
import com.roadwatch.mobile.ui.settings.SettingsActivity;

/**
 * Main Dashboard Activity with improved UI and Navigation Drawer.
 * 
 * IMPROVEMENTS APPLIED:
 * - Camera icon instead of microphone for road defect reporting
 * - Waving hand icon + Person icon in header
 * - Organized Navigation Drawer with Material Icons
 * - Road animation placeholder for dynamic feel
 * - Proper navigation logging for all menu items
 * - Consistent back button handling for secondary pages
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView syncStatusText;
    private ComplaintDao dao;
    private int pendingUnsynced;
    private boolean isOnline = true;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView roadAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = com.roadwatch.mobile.data.AppDatabase.getDatabase(this).complaintDao();
        syncStatusText = findViewById(R.id.syncStatusText);
        roadAnimationView = findViewById(R.id.ivRoadAnimation);

        TextView greetingText = findViewById(R.id.tvGreeting);
        if (greetingText != null) {
            greetingText.setText(R.string.greeting_citizen);
        }

        setupDrawer();
        setupBottomNavigation();
        setupAnimations();
        setupNotificationBell();
        setupProfileShortcut();

        // Push the FCM token to the backend on every dashboard open. The
        // helper short-circuits when the token is already known and synced.
        FcmTokenManager.registerWithBackend(this);

        // Android 13+ needs explicit user opt-in for notifications.
        requestPostNotificationsIfNeeded();

        // Observe total complaints count
        dao.getTotalComplaintsLiveData().observe(this, count -> {
            TextView heroNumber = findViewById(R.id.heroNumber);
            if (heroNumber != null) {
                heroNumber.setText(String.valueOf(count != null ? count : 0));
            }
        });

        // Observe unsynced complaints
        dao.getUnsyncedCountLiveData().observe(this, count -> {
            pendingUnsynced = count != null ? count : 0;
            updateSyncLabel(pendingUnsynced, isOnline);
        });

        // Observe network status
        NetworkMonitor.getInstance(this).getOnlineLiveData().observe(this, online -> {
            isOnline = Boolean.TRUE.equals(online);
            updateSyncLabel(pendingUnsynced, isOnline);
        });

        // Setup recent captures RecyclerView
        androidx.recyclerview.widget.RecyclerView rvRecentCaptures = findViewById(R.id.rvRecentCaptures);
        if (rvRecentCaptures != null) {
            rvRecentCaptures.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            RecentCaptureAdapter adapter = new RecentCaptureAdapter();
            rvRecentCaptures.setAdapter(adapter);
            dao.getRecentCapturesLiveData().observe(this, adapter::setComplaints);
        }
    }

    /**
     * Setup animations for dynamic UI elements
     */
    private void setupAnimations() {
        // Pulse animation for sync status
        if (syncStatusText != null) {
            android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse);
            syncStatusText.startAnimation(pulse);
        }

        // Slide animation for road icon
        if (roadAnimationView != null) {
            android.view.animation.Animation slideAnimation = 
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_up_down);
            roadAnimationView.startAnimation(slideAnimation);
        }
    }

    /** Android 13+ requires runtime opt-in for notifications. */
    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                301);
    }

    private void setupProfileShortcut() {
        View profileCard = findViewById(R.id.profileIconCard);
        if (profileCard != null) {
            profileCard.setOnClickListener(v -> {
                Log.i(TAG, "Profile icon tapped");
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }
    }

    /**
     * Wire the top-bar notification bell — taps open the inbox, and the
     * red badge reflects the live unread count from Room.
     */
    private void setupNotificationBell() {
        FrameLayout bellContainer = findViewById(R.id.notificationBellContainer);
        TextView badge = findViewById(R.id.tvNotificationBadge);

        if (bellContainer != null) {
            bellContainer.setOnClickListener(v -> {
                Log.i(TAG, "Notification bell tapped ’ opening inbox");
                startActivity(new Intent(this, NotificationInboxActivity.class));
            });
        }

        AppDatabase.getDatabase(this)
                .notificationDao()
                .observeUnreadCount()
                .observe(this, count -> {
                    int unread = count != null ? count : 0;
                    if (badge == null) return;
                    if (unread <= 0) {
                        badge.setVisibility(View.GONE);
                    } else {
                        badge.setVisibility(View.VISIBLE);
                        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                    }
                });
    }

    /**
     * Setup Navigation Drawer with improved menu items and icons
     */
    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navView);
        ImageButton hamburgerButton = findViewById(R.id.hamburgerButton);

        if (hamburgerButton != null && drawerLayout != null && navigationView != null) {
            hamburgerButton.setOnClickListener(v -> {
                Log.d(TAG, "Hamburger menu clicked - opening drawer");
                drawerLayout.openDrawer(navigationView);
            });
        }

        // Bind nav header to current session user
        bindNavHeader();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            String itemTitle = item.getTitle() != null ? item.getTitle().toString() : "Unknown";
            
            Log.i(TAG, "Navigate to: " + itemTitle + " (id=" + id + ")");
            if (drawerLayout != null && navigationView != null) {
            drawerLayout.closeDrawer(navigationView);
        }

            // Home
            if (id == R.id.nav_home) {
                Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
                return true;
            }
            
            // My Complaints - Track sent reports
            if (id == R.id.nav_my_complaints) {
                Log.i(TAG, "Navigate to [My Complaints]");
                startActivity(new Intent(this, ComplaintListActivity.class));
                return true;
            }
            
            // Road Budget - Financial status
            if (id == R.id.nav_road_budget) {
                Log.i(TAG, "Navigate to [Road Budget]");
                startActivity(new Intent(this, RoadFinancialStatusActivity.class));
                return true;
            }

            // Road Intelligence - Browse roads
            if (id == R.id.nav_road_intelligence) {
                Log.i(TAG, "Navigate to [Road Intelligence]");
                startActivity(new Intent(this, RoadListActivity.class));
                return true;
            }

            // Authority Directory
            // Authority section removed completely
            
            // Map View
            if (id == R.id.nav_map) {
                Log.i(TAG, "Navigate to [FREE OSM Map View]");
                startActivity(new Intent(this, ReportMapActivityOSM.class));
                return true;
            }
            
            // Live Road Alerts
            if (id == R.id.nav_road_alerts) {
                Log.i(TAG, "Navigate to [Live Road Alerts]");
                startActivity(new Intent(this, RoadAlertsActivity.class));
                return true;
            }
            
            // Report Issue (Camera)
            if (id == R.id.nav_report) {
                Log.i(TAG, "Navigate to [Report Issue]");
                startActivity(new Intent(this, ComplaintFormActivity.class));
                return true;
            }
            
            // AI Assistant
            if (id == R.id.nav_chat) {
                Log.i(TAG, "Navigate to [AI Assistant]");
                startActivity(new Intent(this, ChatbotActivity.class));
                return true;
            }
            
            // Profile
            if (id == R.id.nav_profile) {
                Log.i(TAG, "Navigate to [Profile]");
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            
            // Settings
            if (id == R.id.nav_settings) {
                Log.i(TAG, "Navigate to [Settings]");
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            
            // Logout
            if (id == R.id.nav_logout) {
                Log.i(TAG, "Navigate to [Logout] - confirming");
                confirmLogout();
                return true;
            }
            
            return false;
        });
    }

    /**
     * Confirm logout with a dialog before clearing the session.
     */
    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout, (d, w) -> handleLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Bind nav header to show current session user (name + email).
     */
    private void bindNavHeader() {
        if (navigationView == null) return;
        View header = navigationView.getHeaderCount() > 0
                ? navigationView.getHeaderView(0) : null;
        if (header == null) return;

        SessionManager session = new SessionManager(this);
        TextView name = header.findViewById(R.id.navHeaderName);
        TextView email = header.findViewById(R.id.navHeaderEmail);

        String displayName = session.getName();
        String displayEmail = session.getEmail();
        if (name != null && displayName != null && !displayName.isEmpty()) {
            name.setText(displayName);
        }
        if (email != null && displayEmail != null && !displayEmail.isEmpty()) {
            email.setText(displayEmail);
        }
    }

    /**
     * Handle user logout - clear session and redirect to login
     */
    private void handleLogout() {
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.clear();
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Setup Bottom Navigation with improved handling
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                return true;
            }
            
            if (id == R.id.nav_map) {
                Log.i(TAG, "Bottom Nav: Navigate to [FREE OSM Map]");
                startActivity(new Intent(this, ReportMapActivityOSM.class));
                bottomNav.setSelectedItemId(R.id.nav_home);
                return false;
            }
            
            if (id == R.id.nav_report) {
                Log.i(TAG, "Bottom Nav: Navigate to [Report Issue]");
                startActivity(new Intent(this, ComplaintFormActivity.class));
                bottomNav.setSelectedItemId(R.id.nav_home);
                return false;
            }
            
            if (id == R.id.nav_chat) {
                Log.i(TAG, "Bottom Nav: Navigate to [AI Assistant]");
                startActivity(new Intent(this, ChatbotActivity.class));
                bottomNav.setSelectedItemId(R.id.nav_home);
                return false;
            }
            
            return false;
        });
    }

    /**
     * Update sync status label based on network and unsynced count
     */
    private void updateSyncLabel(Integer unsyncedCount, boolean online) {
        if (syncStatusText == null) return;
        
        int unsynced = unsyncedCount != null ? unsyncedCount : 0;
        
        if (!online) {
            syncStatusText.setText(unsynced == 0 ? "Offline" : unsynced + " Queued (Offline)");
            return;
        }
        
        syncStatusText.setText(unsynced == 0 ? "All Synced" : unsynced + " Pending Sync");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh in case Profile activity updated name/email
        bindNavHeader();
    }

    @Override
    public void onBackPressed() {
        // Close drawer if open, otherwise exit
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }
}
