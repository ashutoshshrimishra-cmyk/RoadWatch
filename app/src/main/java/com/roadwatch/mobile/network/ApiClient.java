package com.roadwatch.mobile.network;
import android.content.Context;
import android.util.Log;
import com.roadwatch.mobile.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final int CONNECT_TIMEOUT_SEC = 60;
    private static final int READ_TIMEOUT_SEC = 90;
    private static Retrofit retrofit;
    private static Retrofit retrofitNoAuth;
    public static synchronized Retrofit getClient(Context context) {
        if (retrofit == null) {
            retrofit = buildRetrofit(context, true);
        }
        return retrofit;
    }
    public static synchronized Retrofit getUnauthenticatedClient(Context context) {
        if (retrofitNoAuth == null) {
            retrofitNoAuth = buildRetrofit(context, false);
        }
        return retrofitNoAuth;
    }
    private static Retrofit buildRetrofit(Context context, boolean withAuth) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS);
        if (withAuth) {
            clientBuilder.addInterceptor(new AuthInterceptor(context.getApplicationContext()));
        }
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                    message -> Log.d(TAG, message));
            logging.redactHeader("Authorization");
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            clientBuilder.addInterceptor(logging);
        }
        String baseUrl = "https://roadwatch-api.duckdns.org";
        // FIXED: Enhanced null safety for API_BASE_URL
        if (baseUrl == null || baseUrl.trim().isEmpty() || "null".equals(baseUrl)) {
            Log.e(TAG, "API_BASE_URL is null or empty. Check local.properties file.");
            // Fallback to .env file URL or hardcoded URL
            baseUrl = "https://roadwatch-api.duckdns.org";
            Log.w(TAG, "Using fallback API URL: " + baseUrl);
        }
        
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        
        Log.i(TAG, "API Base URL configured: " + baseUrl);
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    public static ApiService api(Context context) {
        return getClient(context).create(ApiService.class);
    }
}