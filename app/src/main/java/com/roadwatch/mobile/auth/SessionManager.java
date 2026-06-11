package com.roadwatch.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Stores the JWT and basic user identity in EncryptedSharedPreferences.
 *
 * Falls back to standard SharedPreferences if Tink/Keystore is unavailable
 * (for example on emulators with broken keystore state) so the app keeps working.
 *
 * Auth model matches the production backend exactly:
 *   - Single bearer JWT (no refresh token).
 *   - Token is opaque to the client; server validates + rejects via 401 when expired.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREFS_ENCRYPTED = "roadwatch_session_secure";
    private static final String PREFS_FALLBACK = "roadwatch_session";

    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";
    private static final String KEY_USERNAME = "user_username";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_PHONE = "user_phone";
    private static final String KEY_AVATAR = "user_avatar_uri";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_FCM_TOKEN_SYNCED = "fcm_token_synced";
    private static final String KEY_ROLE = "user_role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = createPrefs(context.getApplicationContext());
    }

    private SharedPreferences createPrefs(Context appContext) {
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_ENCRYPTED,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            Log.w(TAG, "EncryptedSharedPreferences unavailable, using plaintext fallback: "
                    + e.getMessage());
            return appContext.getSharedPreferences(PREFS_FALLBACK, Context.MODE_PRIVATE);
        }
    }

    /**
     * Save the JWT after a successful login.
     *
     * @param token             bearer JWT returned by /api/auth/login
     * @param username          username used to log in (kept for the profile UI)
     * @param expiresInSeconds  remaining lifetime of the token in seconds; 0 if unknown
     */
    public void saveSession(String token, String username, long expiresInSeconds) {
        long expiresAtMillis = expiresInSeconds > 0
                ? System.currentTimeMillis() + (expiresInSeconds * 1000L)
                : 0L;
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USERNAME, username)
                .putLong(KEY_TOKEN_EXPIRES_AT, expiresAtMillis)
                .apply();
    }

    /** Backwards-compatible 2-arg overload for callers that don't have the expiry. */
    public void saveSession(String token, String username) {
        saveSession(token, username, 0L);
    }

    public void saveProfile(String name, String phone, String avatarUri) {
        prefs.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_PHONE, phone)
                .putString(KEY_AVATAR, avatarUri)
                .apply();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public String getUsername() { return prefs.getString(KEY_USERNAME, null); }

    /** Save the user role ("CITIZEN" or "ADMIN") after login. */
    public void saveRole(String role) {
        prefs.edit().putString(KEY_ROLE, role).apply();
    }

    /** Returns the persisted role, defaulting to "CITIZEN" for backwards compatibility. */
    public String getRole() {
        String role = prefs.getString(KEY_ROLE, null);
        return role != null ? role : "CITIZEN";
    }
    /** Alias kept for older callers that asked for "email" €” username is what's stored. */
    public String getEmail() { return getUsername(); }
    public String getName()  { return prefs.getString(KEY_NAME, null); }
    public String getPhone() { return prefs.getString(KEY_PHONE, null); }
    public String getAvatarUri() { return prefs.getString(KEY_AVATAR, null); }
    public long getTokenExpiresAt() { return prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L); }

    /** Most recent FCM device token, used by Step 8 push-notification flow. */
    public String getFcmToken() { return prefs.getString(KEY_FCM_TOKEN, null); }

    public void setFcmToken(String token) {
        prefs.edit()
                .putString(KEY_FCM_TOKEN, token)
                .putBoolean(KEY_FCM_TOKEN_SYNCED, false)
                .apply();
    }

    /** True once the current FCM token has been pushed to the backend. */
    public boolean isFcmTokenSynced() {
        return prefs.getBoolean(KEY_FCM_TOKEN_SYNCED, false);
    }

    public void markFcmTokenSynced() {
        prefs.edit().putBoolean(KEY_FCM_TOKEN_SYNCED, true).apply();
    }

    /** True only when a real backend JWT is stored (not Firebase/local placeholders). */
    public boolean isLoggedIn() {
        return isBackendJwt(getToken());
    }

    public static boolean isBackendJwt(String token) {
        if (token == null || token.isEmpty()) return false;
        if (token.startsWith("firebase:") || "local_user_token".equals(token)) return false;
        return token.split("\\.").length == 3;
    }

    /** Clears all session data €” used on logout, account deletion, or 401 from server. */
    public void clear() {
        prefs.edit().clear().apply();
        Log.i(TAG, "Session cleared");
    }
}
