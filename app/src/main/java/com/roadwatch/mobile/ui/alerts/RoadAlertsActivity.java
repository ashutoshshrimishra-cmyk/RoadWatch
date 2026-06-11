package com.roadwatch.mobile.ui.alerts;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.LastLocationEntity;
import com.roadwatch.mobile.location.LocationService;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.AlertType;
import com.roadwatch.mobile.network.dto.RoadAlertCreateRequest;
import com.roadwatch.mobile.network.dto.RoadAlertDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Live feed of temporary road hazards (Step 9).
 *
 *  € Pulls {@code GET /api/alerts} with the user's lat/lng + 10 km radius
 *    so the backend can filter server-side; client-side haversine filter
 *    runs as a safety net.
 *  € Hides expired alerts (>4 h old + no recent upvotes).
 *  € Sorts by recency, then upvote count.
 *  € FAB opens a 2-tap quick-report dialog (icon ’ submit) that auto-attaches
 *    the user's GPS location.
 *  € Each card has a "Helpful" button that POSTs an upvote, which the backend
 *    uses to extend the alert's lifetime.
 */
public class RoadAlertsActivity extends BaseActivity
        implements RoadAlertAdapter.OnHelpfulClickListener {

    private static final String TAG = "RoadAlertsActivity";
    private static final double FILTER_RADIUS_KM = 10.0;

    private RecyclerView rv;
    private RoadAlertAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private ProgressBar loadingSpinner;
    private TextView tvAlertCount;
    private TextView tvRadiusLabel;
    private ExtendedFloatingActionButton fabQuickReport;

    private ApiService api;
    private LocationService locationService;
    private LastLocationEntity userLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_alerts);
        setupToolbar("Live Road Alerts");

        rv = findViewById(R.id.rvAlerts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyState = findViewById(R.id.emptyState);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        tvAlertCount = findViewById(R.id.tvAlertCount);
        tvRadiusLabel = findViewById(R.id.tvRadiusLabel);
        fabQuickReport = findViewById(R.id.fabQuickReport);

        adapter = new RoadAlertAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.mint_green);
        swipeRefresh.setOnRefreshListener(this::loadAlerts);

        fabQuickReport.setOnClickListener(v -> openQuickReportDialog());

        api = ApiClient.api(this);
        locationService = LocationService.getInstance(this);
        userLocation = locationService.getCachedLocation();

        loadAlerts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If we get a fresh GPS fix between launches, re-filter and re-sort.
        LastLocationEntity refreshed = locationService.getCachedLocation();
        if (refreshed != null && (userLocation == null
                || refreshed.updatedAt > userLocation.updatedAt)) {
            userLocation = refreshed;
            loadAlerts();
        }
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Data ”€”€”€”€”€”€”€”€”€”€”€

    private void loadAlerts() {
        Double lat = userLocation != null ? userLocation.latitude : null;
        Double lng = userLocation != null ? userLocation.longitude : null;

        if (!swipeRefresh.isRefreshing()) {
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        emptyState.setVisibility(View.GONE);

        api.getAlerts(lat, lng, FILTER_RADIUS_KM)
                .enqueue(new Callback<List<RoadAlertDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<RoadAlertDto>> call,
                                           @NonNull Response<List<RoadAlertDto>> response) {
                        loadingSpinner.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "Failed to load alerts http=" + response.code());
                            showAlerts(Collections.emptyList());
                            Toast.makeText(RoadAlertsActivity.this,
                                    "Could not load alerts (HTTP " + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showAlerts(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<RoadAlertDto>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "Network error loading alerts", t);
                        loadingSpinner.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(RoadAlertsActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showAlerts(Collections.emptyList());
                    }
                });
    }

    private void showAlerts(List<RoadAlertDto> raw) {
        long now = System.currentTimeMillis();
        List<RoadAlertDto> active = new ArrayList<>();

        for (RoadAlertDto alert : raw) {
            if (alert == null) continue;
            if (alert.isExpired(now)) continue;

            // Client-side radius filter €” safe even if backend ignored params.
            if (userLocation != null
                    && alert.latitude != null && alert.longitude != null) {
                double dist = AlertGeo.distanceKm(
                        userLocation.latitude, userLocation.longitude,
                        alert.latitude, alert.longitude);
                if (dist > FILTER_RADIUS_KM) continue;
            }

            active.add(alert);
        }

        // Sort: most recent activity first, then by upvote count desc.
        Collections.sort(active, (a, b) -> {
            long ta = a.resolveLastUpvoteAt();
            long tb = b.resolveLastUpvoteAt();
            if (ta != tb) return Long.compare(tb, ta);
            return Integer.compare(b.resolveUpvotes(), a.resolveUpvotes());
        });

        Double ulat = userLocation != null ? userLocation.latitude : null;
        Double ulng = userLocation != null ? userLocation.longitude : null;
        adapter.submit(active, ulat, ulng);

        tvAlertCount.setText(String.format(Locale.US, "%d active", active.size()));
        if (userLocation == null) {
            tvRadiusLabel.setText("Showing all live alerts");
        } else {
            tvRadiusLabel.setText("Live alerts within 10 km");
        }
        emptyState.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(active.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Helpful (upvote) ”€”€”€”€”€”€”€”€”€”€”€

    @Override
    public void onHelpfulClicked(RoadAlertDto alert, int adapterPosition) {
        if (alert.id == null) {
            Toast.makeText(this, "Cannot upvote a local-only alert", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic update so the user gets instant feedback.
        alert.upvotedByMe = true;
        alert.upvotes = alert.resolveUpvotes() + 1;
        alert.lastUpvoteAt = System.currentTimeMillis();
        adapter.update(adapterPosition, alert);

        api.upvoteAlert(alert.id).enqueue(new Callback<RoadAlertDto>() {
            @Override
            public void onResponse(@NonNull Call<RoadAlertDto> call,
                                   @NonNull Response<RoadAlertDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.update(adapterPosition, response.body());
                    Log.i(TAG, "Upvote OK for alert id=" + alert.id);
                } else {
                    Log.w(TAG, "Upvote failed http=" + response.code());
                    // Server returned non-200 €” keep optimistic state but warn.
                    Toast.makeText(RoadAlertsActivity.this,
                            "Could not confirm upvote €” will retry on next refresh",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RoadAlertDto> call,
                                  @NonNull Throwable t) {
                Log.w(TAG, "Upvote network error", t);
                Toast.makeText(RoadAlertsActivity.this,
                        "Offline €” vote will sync when you're back online",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Quick Report Dialog ”€”€”€”€”€”€”€”€”€”€”€

    private void openQuickReportDialog() {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_quick_report, null, false);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        view.findViewById(R.id.optionAccident).setOnClickListener(v -> {
            submitQuickAlert(AlertType.ACCIDENT);
            dialog.dismiss();
        });
        view.findViewById(R.id.optionWater).setOnClickListener(v -> {
            submitQuickAlert(AlertType.WATER_LOGGING);
            dialog.dismiss();
        });
        view.findViewById(R.id.optionTraffic).setOnClickListener(v -> {
            submitQuickAlert(AlertType.HEAVY_TRAFFIC);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitQuickAlert(AlertType type) {
        // We need a fresh GPS fix to attach the location.
        locationService.refreshCurrentLocation(loc -> {
            if (loc == null) {
                Toast.makeText(this,
                        "Could not get your location €” enable GPS and try again",
                        Toast.LENGTH_LONG).show();
                return;
            }
            postAlert(type, loc.latitude, loc.longitude);
        });
    }

    private void postAlert(AlertType type, double latitude, double longitude) {
        Toast.makeText(this, "Reporting " + type.displayLabel + "€", Toast.LENGTH_SHORT).show();

        RoadAlertCreateRequest body = new RoadAlertCreateRequest(
                type, /* description */ null, latitude, longitude);
        api.createAlert(body).enqueue(new Callback<RoadAlertDto>() {
            @Override
            public void onResponse(@NonNull Call<RoadAlertDto> call,
                                   @NonNull Response<RoadAlertDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(RoadAlertsActivity.this,
                            "Alert posted “", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Posted alert type=" + type.wireValue
                            + " id=" + response.body().id);
                    loadAlerts();
                } else {
                    Toast.makeText(RoadAlertsActivity.this,
                            "Failed to post alert (HTTP " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RoadAlertDto> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "Network error posting alert", t);
                Toast.makeText(RoadAlertsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
