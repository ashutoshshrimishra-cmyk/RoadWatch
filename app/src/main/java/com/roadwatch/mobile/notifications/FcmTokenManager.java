package com.roadwatch.mobile.notifications;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.FcmTokenRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owns the lifecycle of the FCM device token:
 *   1. Asks Firebase for the current token.
 *   2. Persists it locally via {@link SessionManager}.
 *   3. Sends it to the backend at {@code POST /api/users/fcm-token} so the
 *      server can target push notifications at this device.
 *
 * Safe to call on every app start €” the no-op fast path returns immediately
 * if the token is already known and synced.
 */
public final class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";

    private FcmTokenManager() {}

    /**
     * Fetch the current FCM token and ensure it's synced to the backend.
     * Failures are logged and swallowed so this never crashes the app.
     */
    public static void registerWithBackend(Context context) {
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);

        if (TextUtils.isEmpty(session.getToken())) {
            Log.d(TAG, "Skipping FCM token push: user not logged in");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (TextUtils.isEmpty(token)) {
                        Log.w(TAG, "FCM returned an empty token");
                        return;
                    }

                    String existing = session.getFcmToken();
                    boolean isNewToken = !token.equals(existing);
                    if (isNewToken) {
                        session.setFcmToken(token);
                        Log.i(TAG, "Persisted new FCM token (length=" + token.length() + ")");
                    }

                    if (!isNewToken && session.isFcmTokenSynced()) {
                        Log.d(TAG, "FCM token already synced with backend");
                        return;
                    }

                    pushToBackend(appContext, token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Could not fetch FCM token", e));
    }

    /**
     * Called by {@link MyFirebaseMessagingService#onNewToken} when Firebase
     * rotates the device token.
     */
    public static void onNewToken(Context context, String token) {
        if (TextUtils.isEmpty(token)) return;
        Context appContext = context.getApplicationContext();
        SessionManager session = new SessionManager(appContext);
        session.setFcmToken(token);
        Log.i(TAG, "FCM token rotated €” pushing to backend");

        if (TextUtils.isEmpty(session.getToken())) {
            Log.d(TAG, "User not signed in yet €” token will be synced on next login");
            return;
        }
        pushToBackend(appContext, token);
    }

    private static void pushToBackend(Context appContext, String token) {
        ApiService api = ApiClient.api(appContext);
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("token", token);
        body.put("platform", "android");
        api.registerCitizenFcmToken(body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            new SessionManager(appContext).markFcmTokenSynced();
                            Log.i(TAG, "FCM token registered with backend (HTTP "
                                    + response.code() + ")");
                        } else {
                            Log.w(TAG, "Backend rejected FCM token registration HTTP="
                                    + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.w(TAG, "FCM token registration failed: " + t.getMessage());
                    }
                });
    }
}
