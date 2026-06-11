package com.roadwatch.mobile.ui.complaint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.roadwatch.mobile.BuildConfig;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.ComplaintEntity;
import com.roadwatch.mobile.data.LastLocationEntity;
import com.roadwatch.mobile.location.LocationService;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.NetworkMonitor;
import com.roadwatch.mobile.workers.SyncWorker;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComplaintActivity extends AppCompatActivity {

    private static final String TAG = "ComplaintActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private ImageCapture imageCapture;
    private File outputDirectory;
    private ExecutorService cameraExecutor;
    private LocationService locationService;
    private String pendingImagePath;
    private ImageButton captureButton;
    private boolean cameraReady;
    private FrameLayout loadingOverlay;
    private TextView loadingText;

    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION  // FIXED: Added coarse location as backup
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        captureButton = findViewById(R.id.captureButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);
        
        captureButton.setEnabled(false);
        captureButton.setOnClickListener(v -> takePhoto());

        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();
        locationService = LocationService.getInstance(this);
        locationService.start();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                PreviewView viewFinder = findViewById(R.id.viewFinder);
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                cameraReady = true;
                runOnUiThread(() -> {
                    // FIXED: Enhanced lifecycle safety check
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    try {
                        captureButton.setEnabled(true);
                        // Use application context to prevent WindowManager$BadTokenException
                        Toast.makeText(getApplicationContext(), "Camera ready — tap to capture", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating camera UI", e);
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera binding failed", e);
                runOnUiThread(() -> {
                    // FIXED: Enhanced lifecycle safety check
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    try {
                        // Use application context to prevent crashes
                        Toast.makeText(getApplicationContext(), "Camera failed to start. Try again.", Toast.LENGTH_LONG).show();
                        finish();
                    } catch (Exception e2) {
                        Log.e(TAG, "Error showing camera failure message", e2);
                        finish();
                    }
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        // FIXED: Enhanced safety checks before photo capture
        if (!cameraReady || imageCapture == null) {
            Toast.makeText(getApplicationContext(), "Camera is still starting…", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // FIXED: Double-check permissions before proceeding
        if (!allPermissionsGranted()) {
            Toast.makeText(getApplicationContext(), "Camera and location permissions required", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            return;
        }
        
        if (outputDirectory == null) {
            outputDirectory = getOutputDirectory();
            if (outputDirectory == null) {
                Toast.makeText(getApplicationContext(), "Cannot create photo directory", Toast.LENGTH_LONG).show();
                return;
            }
        }

        captureButton.setEnabled(false);
        
        // FIXED: Add timestamp to filename for better organization
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        File photoFile = new File(outputDirectory, "complaint_" + timestamp + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.i(TAG, "Photo saved to: " + photoFile.getAbsolutePath());
                        saveToRoomAndSync(photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        runOnUiThread(() -> {
                            // FIXED: Enhanced lifecycle and null safety checks
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            try {
                                if (captureButton != null) {
                                    captureButton.setEnabled(true);
                                }
                                Toast.makeText(getApplicationContext(),
                                        "Capture failed: " + exception.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            } catch (Exception uiError) {
                                Log.e(TAG, "Error updating UI after capture failure", uiError);
                            }
                        });
                    }
                });
    }

    private void saveToRoomAndSync(String imagePath) {
        // FIXED: Enhanced location permission checking with graceful fallback
        boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
        
        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(TAG, "No location permissions, requesting them");
            pendingImagePath = imagePath;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 
                    100);
            return;
        }

        // FIXED: Enhanced location fetching with timeout and fallback
        locationService.refreshCurrentLocation(new LocationService.LocationListener() {
            @Override
            public void onLocation(com.roadwatch.mobile.data.LastLocationEntity cached) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    if (cached != null) {
                        Log.i(TAG, "Using fresh location: " + cached.latitude + ", " + cached.longitude);
                        finalizeComplaintSave(imagePath, cached.latitude, cached.longitude);
                    } else {
                        // Fallback to cached location
                        locationService.getCachedLocationAsync(new LocationService.LocationListener() {
                            @Override
                            public void onLocation(com.roadwatch.mobile.data.LastLocationEntity fallback) {
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    
                                    if (fallback != null) {
                                        Log.i(TAG, "Using cached location: " + fallback.latitude + ", " + fallback.longitude);
                                        finalizeComplaintSave(imagePath, fallback.latitude, fallback.longitude);
                                    } else {
                                        Log.w(TAG, "No location available, saving without GPS coordinates");
                                        Toast.makeText(getApplicationContext(), 
                                                "Saving without GPS — enable location for map pin.", 
                                                Toast.LENGTH_SHORT).show();
                                        finalizeComplaintSave(imagePath, null, null);
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void finalizeComplaintSave(String imagePath, Double latitude, Double longitude) {
        runOnUiThread(() -> {
            // FIXED: Enhanced lifecycle safety with proper error handling
            if (isFinishing() || isDestroyed()) {
                return;
            }
            try {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.VISIBLE);
                }
                if (loadingText != null) {
                    loadingText.setText("Analyzing and uploading...");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating loading UI", e);
            }
        });

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Compress the captured photo in place before upload
                com.roadwatch.mobile.ml.ImageCompressor.compressInPlace(new File(imagePath));

                ComplaintEntity entity = new ComplaintEntity();
                entity.imagePath = imagePath;
                if (latitude != null && longitude != null) {
                    entity.latitude = latitude;
                    entity.longitude = longitude;
                    entity.setLocation("POINT(" + longitude + " " + latitude + ")");
                } else {
                    LastLocationEntity fallback = locationService.getCachedLocation();
                    if (fallback != null) {
                        entity.latitude = fallback.latitude;
                        entity.longitude = fallback.longitude;
                        entity.setLocation("POINT(" + fallback.longitude + " " + fallback.latitude + ")");
                    } else {
                        entity.setLocation(null);
                    }
                }
                entity.timestamp = System.currentTimeMillis();
                
                // FIXED: Enhanced null safety for Intent extras
                Intent intent = getIntent();
                String description = "";
                String roadType = "NH";
                
                if (intent != null) {
                    if (intent.hasExtra(ComplaintFormActivity.EXTRA_DESCRIPTION)) {
                        String extraDesc = intent.getStringExtra(ComplaintFormActivity.EXTRA_DESCRIPTION);
                        description = (extraDesc != null && !extraDesc.trim().isEmpty()) 
                                ? extraDesc : "Road defect reported";
                    } else {
                        description = "Road defect reported";
                    }
                    
                    if (intent.hasExtra(ComplaintFormActivity.EXTRA_ROAD_TYPE)) {
                        String extraRoadType = intent.getStringExtra(ComplaintFormActivity.EXTRA_ROAD_TYPE);
                        roadType = (extraRoadType != null && !extraRoadType.trim().isEmpty()) 
                                ? extraRoadType : "NH";
                    }
                }
                
                entity.description = description;
                entity.roadType = roadType;
                entity.isSynced = false;
                entity.setSeverity(null);

                long complaintId = AppDatabase.getDatabase(ComplaintActivity.this)
                        .complaintDao().insert(entity);
                
                Log.i(TAG, "Saved complaint locally id=" + complaintId + " image=" + imagePath);

                // Try immediate sync if online
                if (NetworkMonitor.getInstance(ComplaintActivity.this).isCurrentlyOnline()) {
                    boolean syncSuccess = attemptImmediateSync(entity, complaintId);
                    if (syncSuccess) {
                        runOnUiThread(() -> {
                            // FIXED: Enhanced lifecycle safety checks
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            try {
                                if (loadingOverlay != null) {
                                    loadingOverlay.setVisibility(View.GONE);
                                }
                                navigateToSuccess(complaintId, false);
                            } catch (Exception e) {
                                Log.e(TAG, "Error navigating to success", e);
                            }
                        });
                        return;
                    }
                }

                // Fallback: enqueue WorkManager for later sync
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                OneTimeWorkRequest syncWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build();

                WorkManager.getInstance(ComplaintActivity.this).enqueue(syncWorkRequest);
                Log.i(TAG, "Enqueued SyncWorker for later sync id=" + complaintId);

                runOnUiThread(() -> {
                    // FIXED: Enhanced lifecycle safety checks
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    try {
                        if (loadingOverlay != null) {
                            loadingOverlay.setVisibility(View.GONE);
                        }
                        navigateToSuccess(complaintId, true);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to success with offline mode", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to save complaint", e);
                runOnUiThread(() -> {
                    // FIXED: Enhanced lifecycle and null safety checks
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    try {
                        if (loadingOverlay != null) {
                            loadingOverlay.setVisibility(View.GONE);
                        }
                        if (captureButton != null) {
                            captureButton.setEnabled(true);
                        }
                        Toast.makeText(getApplicationContext(),
                                "Could not save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (Exception uiError) {
                        Log.e(TAG, "Error updating UI after save failure", uiError);
                    }
                });
            }
        });
    }

    private boolean attemptImmediateSync(ComplaintEntity complaint, long complaintId) {
        try {
            File file = new File(complaint.imagePath);
            if (!file.exists()) {
                Log.w(TAG, "Image file missing, cannot sync immediately");
                return false;
            }

            ApiService apiService = ApiClient.api(this);

            RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image", file.getName(), requestFile);

            String rType = complaint.roadType != null ? complaint.roadType : "NH";
            RequestBody roadTypePart = RequestBody.create(rType, MediaType.parse("text/plain"));

            String locationWkt = buildLocationWkt(complaint);
            boolean hasCoordinates = complaint.latitude != null && complaint.longitude != null;
            
            // Fixed the Retrofit null exception bug by passing empty string instead of null
            RequestBody locationBody = createOptionalTextPart(locationWkt);
            RequestBody latitudePart = createOptionalTextPart(hasCoordinates ? String.valueOf(complaint.latitude) : "");
            RequestBody longitudePart = createOptionalTextPart(hasCoordinates ? String.valueOf(complaint.longitude) : "");

            String desc = complaint.description != null ? complaint.description : "Road defect reported";
            RequestBody descriptionPart = RequestBody.create(desc, MediaType.parse("text/plain"));

            Log.i(TAG, "Attempting immediate async sync for complaint id=" + complaintId);

            // Replaced .execute() with async .enqueue()
            apiService.createComplaint(roadTypePart, locationBody, latitudePart, longitudePart, descriptionPart, imagePart)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                new Thread(() -> {
                                    AppDatabase.getDatabase(ComplaintActivity.this).complaintDao().markSynced((int) complaintId);
                                }).start();
                                Log.i(TAG, "Immediate sync successful for id=" + complaintId + " http=" + response.code());
                            } else {
                                Log.w(TAG, "Immediate sync failed http=" + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                            Log.e(TAG, "Immediate sync network error: " + t.getMessage(), t);
                        }
                    });

            return true; 
        } catch (Exception e) {
            Log.e(TAG, "Immediate sync unexpected error", e);
            return false;
        }
    }

    private void navigateToSuccess(long complaintId, boolean isOffline) {
        Intent intent = new Intent(this, SubmissionSuccessActivity.class);
        intent.putExtra(SubmissionSuccessActivity.EXTRA_COMPLAINT_ID, complaintId);
        intent.putExtra(SubmissionSuccessActivity.EXTRA_IS_OFFLINE, isOffline);
        startActivity(intent);
        finish();
    }

    private RequestBody createOptionalTextPart(String value) {
        // Fix: Replace null with empty string so Retrofit's @Part doesn't crash internally
        if (value == null || value.trim().isEmpty()) {
            value = ""; 
        }
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private String buildLocationWkt(ComplaintEntity complaint) {
        if (complaint.latitude != null && complaint.longitude != null) {
            return String.format(java.util.Locale.US, "POINT (%f %f)",
                    complaint.longitude, complaint.latitude);
        }
        if (complaint.location != null && !complaint.location.isEmpty()) {
            return complaint.location;
        }
        return "";
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private File getOutputDirectory() {
        // FIXED: Robust null handling to prevent NPE crashes
        File[] mediaDirs = getExternalMediaDirs();
        
        // First try external media directory
        if (mediaDirs != null && mediaDirs.length > 0 && mediaDirs[0] != null) {
            File mediaDir = new File(mediaDirs[0], "RoadWatch");
            if (!mediaDir.exists()) {
                try {
                    mediaDir.mkdirs();
                } catch (SecurityException e) {
                    Log.w(TAG, "Failed to create external media dir: " + e.getMessage());
                }
            }
            if (mediaDir.exists() && mediaDir.canWrite()) {
                return mediaDir;
            }
        }
        
        // Robust fallback using application context
        File appFilesDir = getApplicationContext().getFilesDir();
        File fallback = new File(appFilesDir, "RoadWatch");
        if (!fallback.exists()) {
            try {
                fallback.mkdirs();
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to create fallback directory: " + e.getMessage());
                // Return app files dir as last resort
                return appFilesDir;
            }
        }
        return fallback;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // FIXED: Enhanced permission checking with detailed feedback
            boolean cameraGranted = false;
            boolean locationGranted = false;
            
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (Manifest.permission.CAMERA.equals(permissions[i])) {
                        cameraGranted = true;
                    } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) ||
                               Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                        locationGranted = true;
                    }
                }
            }
            
            if (cameraGranted && locationGranted) {
                startCamera();
            } else if (cameraGranted && !locationGranted) {
                // Allow camera usage but warn about location
                startCamera();
                Toast.makeText(getApplicationContext(), 
                        "Location permission denied. Photos will be saved without GPS coordinates.", 
                        Toast.LENGTH_LONG).show();
            } else if (!cameraGranted) {
                Toast.makeText(getApplicationContext(), 
                        "Camera permission is required to take photos.", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == 100) {
            // Location permission for existing photo
            boolean hasLocationPermission = false;
            for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED && 
                    (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i]) ||
                     Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i]))) {
                    hasLocationPermission = true;
                    break;
                }
            }
            
            if (hasLocationPermission && pendingImagePath != null) {
                saveToRoomAndSync(pendingImagePath);
                pendingImagePath = null;
            } else if (pendingImagePath != null) {
                // Save without location
                finalizeComplaintSave(pendingImagePath, null, null);
                pendingImagePath = null;
            }
        }
    }
}