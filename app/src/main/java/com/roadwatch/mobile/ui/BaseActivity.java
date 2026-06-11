package com.roadwatch.mobile.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.roadwatch.mobile.settings.LocaleHelper;
import com.roadwatch.mobile.settings.SettingsManager;

/**
 * Base Activity that provides:
 *  - Consistent toolbar with back button for secondary pages.
 *  - Per-activity locale wrapping so language changes apply immediately.
 *  - Per-activity theme (Light/Dark) so theme toggle takes effect on next screen.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyThemeMode();
        super.onCreate(savedInstanceState);
    }

    private void applyThemeMode() {
        boolean dark = new SettingsManager(this).isDarkMode();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Setup toolbar with back button and custom title. */
    protected void setupToolbar(String title) {
        Toolbar toolbar = findViewById(com.roadwatch.mobile.R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle(title);
            }
        }
    }

    protected void setupToolbar() {
        Toolbar toolbar = findViewById(com.roadwatch.mobile.R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
