package com.roadwatch.mobile.ui.reports;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.ReportRepository;
import com.roadwatch.mobile.network.dto.AlertType;
import com.roadwatch.mobile.network.dto.ReportDto;
import com.roadwatch.mobile.network.dto.RoadAlertDto;
import com.roadwatch.mobile.ui.BaseActivity;
import com.roadwatch.mobile.ui.alerts.RoadAlertsActivity;
import com.roadwatch.mobile.ui.complaints.ComplaintDetailActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportMapActivity extends BaseActivity {

    private static final String TAG = "ReportMapActivity";
    private static final int REQ_LOCATION = 401;
    private static final double DEFAULT_USER_ZOOM = 15.0;
    private static final double DEFAULT_INDIA_ZOOM = 5.0;
    private static final GeoPoint INDIA_CENTER = new GeoPoint(20.5937, 78.9629);

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private ComplaintInfoWindowAdapter infoWindowAdapter;
    private FusedLocationProviderClient fusedLocation;
    private ComplaintClusterRenderer renderer;

    private ProgressBar mapProgress;
    private TextView tvMapError;
    private FloatingActionButton fabMyLocation;
    private FloatingActionButton fabFilter;

    private List<ReportDto> loadedReports = new ArrayList<>();
    private final List<Marker> reportMarkers = new ArrayList<>();

    private final List<RoadAlertDto> loadedAlerts = new ArrayList<>();
    private final List<Marker> alertMarkerList = new ArrayList<>();
    private final Map<Marker, RoadAlertDto> alertMarkerLookup = new HashMap<>();
    private boolean showAlerts = true;

    private int activeFilter = FILTER_ALL;
    private static final int FILTER_ALL         = -1;
    private static final int FILTER_PENDING     = ComplaintClusterItem.BUCKET_PENDING;
    private static final int FILTER_IN_PROGRESS = ComplaintClusterItem.BUCKET_IN_PROGRESS;
    private static final int FILTER_RESOLVED    = ComplaintClusterItem.BUCKET_RESOLVED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this,
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_report_map);
        setupToolbar("Pothole Map");

        mapProgress   = findViewById(R.id.mapProgress);
        tvMapError    = findViewById(R.id.tvMapError);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabFilter     = findViewById(R.id.fabFilter);

        mapView = findViewById(R.id.mapFragment);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_INDIA_ZOOM);
        mapView.getController().setCenter(INDIA_CENTER);

        renderer = new ComplaintClusterRenderer(this, mapView);
        infoWindowAdapter = new ComplaintInfoWindowAdapter(mapView);

        fabMyLocation.setOnClickListener(v -> centerOnMyLocation(true));
        fabFilter.setOnClickListener(v -> showFilterDialog());

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        enableMyLocationLayer();
        zoomToUserLocation();
        loadReports();
        loadAlerts();
    }

    private void loadReports() {
        mapProgress.setVisibility(View.VISIBLE);
        tvMapError.setVisibility(View.GONE);

        ioExecutor.execute(() -> {
            try {
                List<ReportDto> reports = new ReportRepository(this).fetchReports();
                runOnUiThread(() -> {
                    loadedReports = reports;
                    mapProgress.setVisibility(View.GONE);
                    plotReports();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load reports", e);
                runOnUiThread(() -> {
                    mapProgress.setVisibility(View.GONE);
                    if (!loadedReports.isEmpty()) {
                        plotReports();
                    } else {
                        tvMapError.setVisibility(View.VISIBLE);
                        tvMapError.setText("Could not load reports: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void plotReports() {
        for (Marker m : reportMarkers) mapView.getOverlays().remove(m);
        reportMarkers.clear();

        int plotted = 0;
        for (ReportDto report : loadedReports) {
            if (report.location == null) continue;
            ComplaintClusterItem item = new ComplaintClusterItem(report);
            if (activeFilter != FILTER_ALL && item.getBucket() != activeFilter) continue;

            Marker marker = renderer.createMarker(item);
            marker.setOnMarkerClickListener((m, mv) -> {
                infoWindowAdapter.setReport(report);
                m.showInfoWindow();
                return true;
            });

            mapView.getOverlays().add(marker);
            reportMarkers.add(marker);
            plotted++;
        }

        mapView.invalidate();
        Log.i(TAG, "Plotted " + plotted + " reports (filter=" + activeFilter + ")");

        if (plotted == 0) {
            tvMapError.setVisibility(View.VISIBLE);
            tvMapError.setText(activeFilter == FILTER_ALL
                    ? "No reports with GPS yet."
                    : "No reports match this filter.");
        } else {
            tvMapError.setVisibility(View.GONE);
        }
    }

    private void enableMyLocationLayer() {
        if (!hasLocationPermission()) return;
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void zoomToUserLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        centerOnMyLocation(false);
    }

    @SuppressLint("MissingPermission")
    private void centerOnMyLocation(boolean fromButton) {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        fusedLocation.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                if (fromButton) Toast.makeText(this,
                        "Waiting for GPS fix - try again in a moment",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            GeoPoint me = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapView.getController().animateTo(me);
            mapView.getController().setZoom(DEFAULT_USER_ZOOM);
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationLayer();
                centerOnMyLocation(false);
            } else {
                Toast.makeText(this,
                        "Location permission denied - showing all reports instead",
                        Toast.LENGTH_SHORT).show();
                fitCameraToReports();
            }
        }
    }

    private void fitCameraToReports() {
        if (loadedReports.isEmpty()) return;
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        boolean any = false;
        for (ReportDto r : loadedReports) {
            if (r.location == null) continue;
            minLat = Math.min(minLat, r.location.latitude);
            maxLat = Math.max(maxLat, r.location.latitude);
            minLon = Math.min(minLon, r.location.longitude);
            maxLon = Math.max(maxLon, r.location.longitude);
            any = true;
        }
        if (any) {
            mapView.zoomToBoundingBox(
                    new BoundingBox(maxLat, maxLon, minLat, minLon), true, 100);
        }
    }

    private void showFilterDialog() {
        String[] options = {"All reports", "Pending", "Under repair", "Resolved"};
        int currentIndex = (activeFilter == FILTER_ALL) ? 0 : activeFilter + 1;
        new AlertDialog.Builder(this)
                .setTitle("Filter by status")
                .setSingleChoiceItems(options, currentIndex, (dialog, which) -> {
                    activeFilter = (which == 0) ? FILTER_ALL : (which - 1);
                    plotReports();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadAlerts() {
        ApiService api = ApiClient.api(this);
        api.getAlerts(null, null, 25.0).enqueue(new Callback<List<RoadAlertDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<RoadAlertDto>> call,
                                   @NonNull Response<List<RoadAlertDto>> response) {
                loadedAlerts.clear();
                if (response.isSuccessful() && response.body() != null)
                    loadedAlerts.addAll(response.body());
                Log.i(TAG, "Loaded " + loadedAlerts.size() + " road alerts");
                plotAlerts();
            }

            @Override
            public void onFailure(@NonNull Call<List<RoadAlertDto>> call,
                                  @NonNull Throwable t) {
                Log.w(TAG, "Could not load road alerts: " + t.getMessage());
            }
        });
    }

    private void plotAlerts() {
        for (Marker m : alertMarkerList) mapView.getOverlays().remove(m);
        alertMarkerList.clear();
        alertMarkerLookup.clear();

        if (!showAlerts) { mapView.invalidate(); return; }

        long now = System.currentTimeMillis();
        int rendered = 0;

        for (RoadAlertDto alert : loadedAlerts) {
            if (alert.isExpired(now)) continue;
            if (alert.latitude == null || alert.longitude == null) continue;

            AlertType type = alert.resolveType();
            Drawable icon = ContextCompat.getDrawable(this, type.markerRes);

            String snippet = "Reported " + DateUtils.timeAgo(alert.resolveCreatedAt(), now)
                    + " - " + alert.resolveUpvotes() + " confirms";

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(alert.latitude, alert.longitude));
            marker.setTitle(type.displayLabel);
            marker.setSnippet(snippet);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            if (icon != null) marker.setIcon(icon);

            final RoadAlertDto alertRef = alert;
            marker.setOnMarkerClickListener((m, mv) -> {
                m.showInfoWindow();
                return true;
            });

            mapView.getOverlays().add(marker);
            alertMarkerList.add(marker);
            alertMarkerLookup.put(marker, alertRef);
            rendered++;
        }

        mapView.invalidate();
        Log.i(TAG, "Plotted " + rendered + " active alerts on map");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
        ioExecutor.shutdown();
    }

    private static class DateUtils {
        static String timeAgo(long millis, long now) {
            long diff = Math.max(0, now - millis);
            long minutes = diff / 60_000L;
            if (minutes < 1) return "just now";
            if (minutes < 60) return minutes + " min ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + " hr ago";
            return (hours / 24) + " d ago";
        }
    }
}