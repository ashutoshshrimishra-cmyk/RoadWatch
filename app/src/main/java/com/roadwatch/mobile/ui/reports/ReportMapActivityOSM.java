package com.roadwatch.mobile.ui.reports;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

// FREE osmdroid imports - NO BILLING REQUIRED
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.ReportRepository;
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

/**
 * FREE Interactive map using OpenStreetMap - NO BILLING REQUIRED
 * Complete replacement for Google Maps to eliminate licensing costs
 */
public class ReportMapActivityOSM extends BaseActivity {

    private static final String TAG = "ReportMapOSM";
    private static final int REQ_LOCATION = 401;
    private static final double DEFAULT_USER_ZOOM = 15.0;
    private static final GeoPoint INDIA_CENTER = new GeoPoint(20.5937, 78.9629);

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // FREE osmdroid components
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocation;
    private ProgressBar mapProgress;
    private TextView tvMapError;
    private FloatingActionButton fabMyLocation, fabFilter;

    private List<ReportDto> loadedReports = new ArrayList<>();
    private final List<Marker> reportMarkers = new ArrayList<>();
    private final Map<Marker, ReportDto> markerToReport = new HashMap<>();
    private int activeFilter = FILTER_ALL;

    // Filter constants
    private static final int FILTER_ALL = -1;
    private static final int FILTER_PENDING = 0;
    private static final int FILTER_IN_PROGRESS = 1;
    private static final int FILTER_RESOLVED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // IMPORTANT: Initialize FREE osmdroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue("RoadWatch/1.0");
        
        setContentView(R.layout.activity_report_map_osm);
        setupToolbar("Pothole Map (FREE OSM)");

        fusedLocation = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupMap();
        setupFabs();
        
        loadReports();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        mapProgress = findViewById(R.id.mapProgress);
        tvMapError = findViewById(R.id.tvMapError);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabFilter = findViewById(R.id.fabFilter);
    }

    private void setupMap() {
        // FREE OpenStreetMap setup - no API keys required
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Free OSM tiles
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);
        
        mapController = mapView.getController();
        mapController.setZoom(5.0);
        mapController.setCenter(INDIA_CENTER);

        // Add FREE My Location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        Log.i(TAG, "FREE osmdroid map initialized successfully");
        zoomToUserLocation();
    }

    private void setupFabs() {
        fabMyLocation.setOnClickListener(v -> centerOnMyLocation(true));
        fabFilter.setOnClickListener(v -> showFilterDialog());
    }
    // ═══════════════════ Data Loading ═══════════════════

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
                    tvMapError.setVisibility(View.VISIBLE);
                    tvMapError.setText("Could not load reports: " + e.getMessage());
                });
            }
        });
    }

    private void plotReports() {
        // Clear existing markers
        for (Marker marker : reportMarkers) {
            mapView.getOverlays().remove(marker);
        }
        reportMarkers.clear();
        markerToReport.clear();

        int plotted = 0;
        for (ReportDto report : loadedReports) {
            if (report.location == null) continue;
            
            int bucket = getReportStatusBucket(report);
            if (activeFilter != FILTER_ALL && bucket != activeFilter) {
                continue;
            }

            // Create FREE osmdroid marker
            Marker marker = new Marker(mapView);
            GeoPoint position = new GeoPoint(report.location.latitude, report.location.longitude);
            marker.setPosition(position);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Set marker title and info
            marker.setTitle("Road Defect Report");
            String snippet = "Status: " + getStatusLabel(bucket);
            if (report.roadType != null) {
                snippet += " • " + report.roadType;
            }
            marker.setSnippet(snippet);
            
            // Handle marker click
            marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                ReportDto clickedReport = markerToReport.get(clickedMarker);
                if (clickedReport != null && clickedReport.id != null) {
                    Intent intent = new Intent(this, ComplaintDetailActivity.class);
                    intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, clickedReport.id);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Report not synced yet", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            mapView.getOverlays().add(marker);
            reportMarkers.add(marker);
            markerToReport.put(marker, report);
            plotted++;
        }

        mapView.invalidate();
        Log.i(TAG, "Plotted " + plotted + " of " + loadedReports.size() + " reports");
        
        if (plotted == 0) {
            tvMapError.setVisibility(View.VISIBLE);
            tvMapError.setText("No reports with GPS coordinates yet");
        } else {
            tvMapError.setVisibility(View.GONE);
        }
    }
    private int getReportStatusBucket(ReportDto report) {
        if (report.status == null) return FILTER_PENDING;
        String status = report.status.toLowerCase();
        if (status.contains("progress") || status.contains("repair")) {
            return FILTER_IN_PROGRESS;
        } else if (status.contains("resolved") || status.contains("completed")) {
            return FILTER_RESOLVED;
        }
        return FILTER_PENDING;
    }

    private String getStatusLabel(int bucket) {
        switch (bucket) {
            case FILTER_PENDING: return "Pending";
            case FILTER_IN_PROGRESS: return "Under Repair";
            case FILTER_RESOLVED: return "Resolved";
            default: return "Unknown";
        }
    }

    // ═══════════════════ Location Services ═══════════════════

    @SuppressLint("MissingPermission")
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
                if (fromButton) {
                    Toast.makeText(this, "Waiting for GPS fix", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            GeoPoint userLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setZoom(DEFAULT_USER_ZOOM);
            mapController.animateTo(userLocation);
            Log.i(TAG, "Centered on user location " + userLocation);
        });
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
            centerOnMyLocation(false);
        }
    }
    // ═══════════════════ Filter Dialog ═══════════════════

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

    // ═══════════════════ Lifecycle ═══════════════════

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume(); // IMPORTANT: osmdroid lifecycle
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause(); // IMPORTANT: osmdroid lifecycle
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
        mapView.onDetach(); // IMPORTANT: osmdroid cleanup
    }
}
