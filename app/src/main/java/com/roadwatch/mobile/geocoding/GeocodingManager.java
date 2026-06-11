package com.roadwatch.mobile.geocoding;

import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * FREE Geocoding Manager using OpenStreetMap Nominatim API
 * Replaces Google Geocoding API to eliminate billing requirements
 */
public class GeocodingManager {

    private static final String TAG = "GeocodingManager";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/";
    private static final String USER_AGENT = "RoadWatch-Android/1.0";
    
    private static GeocodingManager instance;
    private final NominatimService nominatimService;

    public interface GeocodingCallback {
        void onSuccess(List<NominatimResult> results);
        void onError(String error);
    }

    public interface ReverseGeocodingCallback {
        void onSuccess(NominatimResult result);
        void onError(String error);
    }

    private GeocodingManager() {
        // Configure OkHttp with proper User-Agent (required by Nominatim)
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(
                        chain.request()
                                .newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build()
                ))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(NOMINATIM_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        nominatimService = retrofit.create(NominatimService.class);
        Log.i(TAG, "Initialized FREE Nominatim geocoding service");
    }

    public static synchronized GeocodingManager getInstance() {
        if (instance == null) {
            instance = new GeocodingManager();
        }
        return instance;
    }

    /**
     * Forward geocoding: Address/place name to coordinates
     * 
     * @param query Address or place name to search for
     * @param limit Maximum number of results (1-50)
     * @param callback Result callback
     */
    public void forwardGeocode(String query, int limit, GeocodingCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onError("Search query cannot be empty");
            return;
        }

        Log.i(TAG, "Forward geocoding: " + query);
        
        nominatimService.forwardGeocode(
                query.trim(),
                "json",
                Math.max(1, Math.min(limit, 50)), // Limit between 1-50
                1, // Include address details
                1, // Include extra tags
                "en" // English language
        ).enqueue(new Callback<List<NominatimResult>>() {
            @Override
            public void onResponse(Call<List<NominatimResult>> call, Response<List<NominatimResult>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NominatimResult> results = response.body();
                    Log.i(TAG, "Forward geocoding success: " + results.size() + " results");
                    callback.onSuccess(results);
                } else {
                    String error = "Geocoding failed: HTTP " + response.code();
                    Log.w(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<List<NominatimResult>> call, Throwable t) {
                String error = "Geocoding network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    /**
     * Reverse geocoding: Coordinates to address
     * 
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param callback Result callback
     */
    public void reverseGeocode(double latitude, double longitude, ReverseGeocodingCallback callback) {
        Log.i(TAG, "Reverse geocoding: " + latitude + ", " + longitude);
        
        nominatimService.reverseGeocode(
                latitude,
                longitude,
                "json",
                1, // Include address details
                1, // Include extra tags
                "en" // English language
        ).enqueue(new Callback<NominatimResult>() {
            @Override
            public void onResponse(Call<NominatimResult> call, Response<NominatimResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NominatimResult result = response.body();
                    Log.i(TAG, "Reverse geocoding success: " + result.getFriendlyAddress());
                    callback.onSuccess(result);
                } else {
                    String error = "Reverse geocoding failed: HTTP " + response.code();
                    Log.w(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<NominatimResult> call, Throwable t) {
                String error = "Reverse geocoding network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    /**
     * Convenience method for single result forward geocoding
     */
    public void forwardGeocodeSingle(String query, ReverseGeocodingCallback callback) {
        forwardGeocode(query, 1, new GeocodingCallback() {
            @Override
            public void onSuccess(List<NominatimResult> results) {
                if (results != null && !results.isEmpty()) {
                    callback.onSuccess(results.get(0));
                } else {
                    callback.onError("No results found for: " + query);
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}