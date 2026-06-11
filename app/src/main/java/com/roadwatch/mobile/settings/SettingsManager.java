package com.roadwatch.mobile.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists user preferences for language and theme.
 * Lightweight, plaintext €” these aren't sensitive values.
 */
public class SettingsManager {

    private static final String PREFS = "roadwatch_settings";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_DARK_MODE = "app_dark_mode";

    public static final String LANG_ENGLISH = "en";
    public static final String LANG_HINDI = "hi";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        // Use the context directly instead of getApplicationContext().
        // During Application.attachBaseContext(), getApplicationContext()
        // returns null which causes a NullPointerException.
        Context ctx = context.getApplicationContext();
        if (ctx == null) {
            ctx = context;
        }
        this.prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, LANG_ENGLISH);
    }

    public void setLanguage(String code) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply();
    }

    /** Defaults to light mode. */
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }
}
