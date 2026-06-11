package com.roadwatch.mobile.ui.complaint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.LastLocationEntity;
import com.roadwatch.mobile.location.LocationService;

public class ComplaintFormActivity extends AppCompatActivity {

    public static final String EXTRA_ROAD_TYPE = "road_type";
    public static final String EXTRA_DESCRIPTION = "description";

    private static final int PERM_REQUEST = 200;

    private TextView tvLocationStatus;
    private LocationService locationService;
    private EditText etDescription;
    private Spinner spinnerRoadType;
    private boolean openingCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_form);

        MaterialToolbar toolbar = findViewById(R.id.formToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        spinnerRoadType = findViewById(R.id.spinnerRoadType);
        etDescription = findViewById(R.id.etDescription);
        MaterialButton btnOpenCamera = findViewById(R.id.btnOpenCamera);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.road_types, R.layout.spinner_item_visible);
        adapter.setDropDownViewResource(R.layout.spinner_item_visible);
        spinnerRoadType.setAdapter(adapter);

        locationService = LocationService.getInstance(this);
        ensurePermissionsAndStartLocation();
        refreshLocationUi();

        btnOpenCamera.setOnClickListener(v -> openCameraScreen());
    }

    private void openCameraScreen() {
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Please describe the problem first", Toast.LENGTH_SHORT).show();
            etDescription.requestFocus();
            return;
        }

        if (!hasCameraPermission()) {
            openingCamera = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERM_REQUEST);
            return;
        }

        launchCamera(description);
    }

    private void launchCamera(String description) {
        LastLocationEntity gps = locationService.getCachedLocation();
        if (gps == null) {
            Toast.makeText(this,
                    "Turn on GPS and wait a few seconds so location can be attached.",
                    Toast.LENGTH_LONG).show();
            refreshLocationUi();
            return;
        }

        String roadTypeLabel = spinnerRoadType.getSelectedItem().toString();
        String roadTypeCode = roadTypeLabel.split(" ")[0];

        Intent intent = new Intent(this, ComplaintActivity.class);
        intent.putExtra(EXTRA_ROAD_TYPE, roadTypeCode);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        startActivity(intent);
    }

    private void ensurePermissionsAndStartLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERM_REQUEST);
            return;
        }
        locationService.start();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            locationService.start();
        }
        refreshLocationUi();
    }

    private void refreshLocationUi() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        locationService.refreshCurrentLocation(location -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (location != null) {
                tvLocationStatus.setText(String.format(
                        "Location attached: %.5f, %.5f",
                        location.latitude,
                        location.longitude
                ));
            } else {
                tvLocationStatus.setText("No GPS yet — enable location. Photo can still be saved offline.");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERM_REQUEST) {
            return;
        }
        if (hasLocationPermission()) {
            locationService.start();
            refreshLocationUi();
        }
        if (openingCamera) {
            openingCamera = false;
            if (hasCameraPermission()) {
                String description = etDescription.getText().toString().trim();
                if (!description.isEmpty()) {
                    launchCamera(description);
                }
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_LONG).show();
            }
        }
    }
}
