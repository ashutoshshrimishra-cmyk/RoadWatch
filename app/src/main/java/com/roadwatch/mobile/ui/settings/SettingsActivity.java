package com.roadwatch.mobile.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.settings.LocaleHelper;
import com.roadwatch.mobile.settings.SettingsManager;
import com.roadwatch.mobile.ui.BaseActivity;
import com.roadwatch.mobile.ui.auth.LoginActivity;

/**
 * Settings screen €” language switching, dark mode toggle, info pages, logout.
 *
 * Theme and locale changes are persisted in {@link SettingsManager} and
 * applied through {@link com.roadwatch.mobile.ui.BaseActivity#attachBaseContext}
 * so every screen picks up the new values on next launch.
 */
public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";

    private SettingsManager settings;
    private MaterialSwitch switchDarkMode;
    private TextView tvCurrentLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupToolbar(getString(R.string.settings_title));

        settings = new SettingsManager(this);

        switchDarkMode = findViewById(R.id.switchDarkMode);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        LinearLayout rowDarkMode = findViewById(R.id.rowDarkMode);
        LinearLayout rowLanguage = findViewById(R.id.rowLanguage);
        LinearLayout rowPrivacy = findViewById(R.id.rowPrivacy);
        LinearLayout rowContact = findViewById(R.id.rowContact);
        LinearLayout rowAbout = findViewById(R.id.rowAbout);
        LinearLayout rowLogout = findViewById(R.id.rowLogout);

        // ”€”€”€”€”€ Dark mode toggle ”€”€”€”€”€
        switchDarkMode.setChecked(settings.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            settings.setDarkMode(checked);
            Log.i(TAG, "Dark mode toggled to " + checked);
            recreate();
        });
        rowDarkMode.setOnClickListener(v -> switchDarkMode.toggle());

        // ”€”€”€”€”€ Language picker ”€”€”€”€”€
        updateLanguageLabel();
        rowLanguage.setOnClickListener(v -> showLanguageDialog());

        // ”€”€”€”€”€ Info rows ”€”€”€”€”€
        rowPrivacy.setOnClickListener(v -> showInfoDialog(
                R.string.privacy_policy, R.string.privacy_body));
        rowAbout.setOnClickListener(v -> showInfoDialog(
                R.string.about_roadwatch, R.string.about_body));
        rowContact.setOnClickListener(v -> openContactEmail());

        // ”€”€”€”€”€ Logout ”€”€”€”€”€
        rowLogout.setOnClickListener(v -> confirmLogout());
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Language ”€”€”€”€”€”€”€”€”€”€”€

    private void updateLanguageLabel() {
        String current = settings.getLanguage();
        tvCurrentLanguage.setText(SettingsManager.LANG_HINDI.equals(current)
                ? R.string.language_hindi
                : R.string.language_english);
    }

    private void showLanguageDialog() {
        String[] options = {
                getString(R.string.language_english),
                getString(R.string.language_hindi)
        };
        int currentIndex = SettingsManager.LANG_HINDI.equals(settings.getLanguage()) ? 1 : 0;

        new AlertDialog.Builder(this)
                .setTitle(R.string.section_language)
                .setSingleChoiceItems(options, currentIndex, (dialog, which) -> {
                    String code = (which == 1)
                            ? SettingsManager.LANG_HINDI
                            : SettingsManager.LANG_ENGLISH;
                    if (!code.equals(settings.getLanguage())) {
                        settings.setLanguage(code);
                        Log.i(TAG, "Language changed to " + code);
                        LocaleHelper.updateResources(getApplicationContext(), code);
                        dialog.dismiss();
                        recreate();
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Info dialogs ”€”€”€”€”€”€”€”€”€”€”€

    private void showInfoDialog(int titleRes, int bodyRes) {
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(bodyRes)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void openContactEmail() {
        String email = getString(R.string.contact_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "RoadWatch Feedback");
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.contact_developer)));
        } catch (Exception e) {
            Log.w(TAG, "No email app available", e);
            Toast.makeText(this, email, Toast.LENGTH_LONG).show();
        }
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Logout ”€”€”€”€”€”€”€”€”€”€”€

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout, (d, w) -> performLogout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performLogout() {
        new SessionManager(this).clear();
        Log.i(TAG, "User logged out €” session cleared");
        Toast.makeText(this, R.string.logout, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
