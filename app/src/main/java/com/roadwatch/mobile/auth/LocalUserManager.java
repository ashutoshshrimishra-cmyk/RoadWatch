package com.roadwatch.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class LocalUserManager {

    private static final String PREFS_NAME = "roadwatch_local_users";
    private static final String KEY_FULL_NAME = "full_name_%s";
    private static final String KEY_PASSWORD = "password_%s";

    private final SharedPreferences prefs;

    public LocalUserManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUser(String username, String password, String fullName) {
        if (TextUtils.isEmpty(username) || password == null) return;

        prefs.edit()
                .putString(String.format(KEY_FULL_NAME, username), fullName)
                .putString(String.format(KEY_PASSWORD, username), password)
                .apply();
    }

    public boolean validateUser(String username, String password) {
        if (TextUtils.isEmpty(username) || password == null) return false;
        String storedPassword = prefs.getString(String.format(KEY_PASSWORD, username), null);
        return password.equals(storedPassword);
    }

    public String getFullName(String username) {
        if (TextUtils.isEmpty(username)) return null;
        return prefs.getString(String.format(KEY_FULL_NAME, username), null);
    }
}
