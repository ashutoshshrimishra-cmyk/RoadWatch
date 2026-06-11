package com.roadwatch.mobile.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.roadwatch.mobile.BuildConfig;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.data.AppDatabase;
import com.roadwatch.mobile.data.ComplaintDao;
import com.roadwatch.mobile.data.ComplaintEntity;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        ComplaintDao dao = AppDatabase.getDatabase(context).complaintDao();

        List<ComplaintEntity> unsynced = dao.getUnsyncedComplaints();
        if (unsynced.isEmpty()) {
            return Result.success();
        }

        Log.i(TAG, "Sync started pendingCount=" + unsynced.size()
                + " baseUrl=" + BuildConfig.API_BASE_URL
                + " hasToken=" + hasAuthToken(context));

        ApiService apiService = ApiClient.api(context);
        boolean needsRetry = false;

        for (ComplaintEntity complaint : unsynced) {
            try {
                File file = new File(complaint.imagePath);
                if (!file.exists()) {
                    Log.w(TAG, "Skipping missing image file for complaint id=" + complaint.id
                            + " path=" + complaint.imagePath);
                    dao.markSynced(complaint.id);
                    continue;
                }

                RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                        "image", file.getName(), requestFile);

                String rType = complaint.roadType != null ? complaint.roadType : "NH";
                RequestBody roadTypePart = RequestBody.create(rType, MediaType.parse("text/plain"));

                String locationWkt = buildLocationWkt(complaint);
                boolean hasCoordinates = complaint.latitude != null && complaint.longitude != null;
                RequestBody locationBody = createOptionalTextPart(locationWkt);
                RequestBody latitudePart = hasCoordinates
                        ? createOptionalTextPart(String.valueOf(complaint.latitude)) : null;
                RequestBody longitudePart = hasCoordinates
                        ? createOptionalTextPart(String.valueOf(complaint.longitude)) : null;

                String desc = complaint.description != null ? complaint.description : "Road defect reported";
                RequestBody descriptionPart = RequestBody.create(desc, MediaType.parse("text/plain"));

                Log.i(TAG, "Uploading complaint id=" + complaint.id
                        + " url=" + BuildConfig.API_BASE_URL + "complaints"
                        + " file=" + complaint.imagePath
                        + " fileBytes=" + file.length()
                        + " hasCoordinates=" + hasCoordinates
                        + " locationWkt=" + (locationWkt == null ? "" : locationWkt)
                        + " roadType=" + rType
                        + " hasToken=" + hasAuthToken(context));
                if (!hasCoordinates) {
                    Log.w(TAG, "Complaint id=" + complaint.id
                            + " has no latitude/longitude. Uploading without GPS fields.");
                }

                Response<ResponseBody> response = apiService
                        .createComplaint(roadTypePart, locationBody, latitudePart, longitudePart,
                                descriptionPart, imagePart)
                        .execute();

                String responseBody = readResponseBody(response);

                if (response.isSuccessful()) {
                    dao.markSynced(complaint.id);
                    Log.i(TAG, "Uploaded complaint id=" + complaint.id
                            + " http=" + response.code()
                            + " message=" + response.message());
                } else {
                    Log.w(TAG, "Upload failed id=" + complaint.id
                            + " http=" + response.code()
                            + " message=" + response.message()
                            + " body=" + responseBody
                            + " hasToken=" + hasAuthToken(context)
                            + " fileExists=" + file.exists());
                    needsRetry = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "Network upload exception for complaint id=" + complaint.id
                        + " type=" + e.getClass().getSimpleName()
                        + " message=" + e.getMessage(), e);
                needsRetry = true;
            } catch (Exception e) {
                Log.e(TAG, "Unexpected upload exception for complaint id=" + complaint.id
                        + " type=" + e.getClass().getSimpleName()
                        + " message=" + e.getMessage(), e);
                needsRetry = true;
            }
        }

        return needsRetry ? Result.retry() : Result.success();
    }

    private String readResponseBody(Response<ResponseBody> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    private RequestBody createOptionalTextPart(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private boolean hasAuthToken(Context context) {
        String token = new SessionManager(context).getToken();
        return token != null && !token.isEmpty();
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
}
