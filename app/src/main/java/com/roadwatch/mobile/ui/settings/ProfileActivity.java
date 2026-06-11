package com.roadwatch.mobile.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.ui.BaseActivity;
import com.roadwatch.mobile.ui.auth.LoginActivity;

/**
 * User profile screen €” view/edit name/email/phone, with confirm-required
 * Delete Account flow that wipes the local session.
 */
public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputLayout tilName, tilEmail, tilPhone;
    private TextInputEditText etName, etEmail, etPhone;
    private TextView tvDisplayName, tvDisplayEmail;
    private MaterialButton btnEditProfile, btnDeleteAccount;

    private SessionManager session;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupToolbar(getString(R.string.profile_title));

        session = new SessionManager(this);

        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        tvDisplayName = findViewById(R.id.tvDisplayName);
        tvDisplayEmail = findViewById(R.id.tvDisplayEmail);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        loadProfile();

        btnEditProfile.setOnClickListener(v -> {
            if (isEditMode) saveProfile();
            else enterEditMode();
        });

        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void loadProfile() {
        String name = orDefault(session.getName(), "Citizen User");
        String email = orDefault(session.getEmail(), "citizen@roadwatch.com");
        String phone = orDefault(session.getPhone(), "+91 ");

        etName.setText(name);
        etEmail.setText(email);
        etPhone.setText(phone);
        tvDisplayName.setText(name);
        tvDisplayEmail.setText(email);

        Log.i(TAG, "Profile loaded for email=" + email);
    }

    private String orDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Edit / Save ”€”€”€”€”€”€”€”€”€”€”€

    private void enterEditMode() {
        isEditMode = true;
        tilName.setEnabled(true);
        tilEmail.setEnabled(true);
        tilPhone.setEnabled(true);
        btnEditProfile.setText(R.string.save_profile);
        etName.requestFocus();
    }

    private void saveProfile() {
        String name = textOf(etName);
        String email = textOf(etEmail);
        String phone = textOf(etPhone);

        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.profile_name));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.profile_email));
            return;
        }
        tilName.setError(null);
        tilEmail.setError(null);

        // Persist locally; backend sync would go here when available.
        session.saveSession(session.getToken(), email);
        session.saveProfile(name, phone, session.getAvatarUri());

        tvDisplayName.setText(name);
        tvDisplayEmail.setText(email);

        tilName.setEnabled(false);
        tilEmail.setEnabled(false);
        tilPhone.setEnabled(false);
        btnEditProfile.setText(R.string.edit_profile);
        isEditMode = false;

        Toast.makeText(this, R.string.save_profile, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Profile saved name=" + name + " email=" + email);
    }

    private String textOf(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    // ”€”€”€”€”€”€”€”€”€”€”€ Delete Account ”€”€”€”€”€”€”€”€”€”€”€

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_account)
                .setMessage(R.string.delete_account_warning)
                .setPositiveButton(R.string.delete_account, (d, w) -> performDeleteAccount())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performDeleteAccount() {
        // Backend deletion call would go here. For now we wipe local state.
        session.clear();
        Log.i(TAG, "Account deleted locally €” redirecting to login");
        Toast.makeText(this, R.string.delete_account, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
