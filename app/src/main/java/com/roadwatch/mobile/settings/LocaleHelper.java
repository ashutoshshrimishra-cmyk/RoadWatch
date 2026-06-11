package com.roadwatch.mobile.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * Wraps a Context with the user's preferred locale.
 * Call {@link #wrap(Context)} from {@code attachBaseContext} on every Activity
 * (and the Application) so configuration changes propagate before view inflation.
 */
public final class LocaleHelper {

    private LocaleHelper() {}

    public static Context wrap(Context base) {
        String langCode = new SettingsManager(base).getLanguage();
        return updateResources(base, langCode);
    }

    @SuppressWarnings("deprecation")
    public static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }
}
