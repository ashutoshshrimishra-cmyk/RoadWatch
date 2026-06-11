package com.roadwatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.LoginResponse;
import com.roadwatch.mobile.ui.dashboard.MainActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        EditText etFullName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);
        MaterialButton btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLogin = findViewById(R.id.tvLogin);

        SessionManager sessionManager = new SessionManager(this);

        btnSignUp.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSignUp.setEnabled(false);

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);
            body.put("name", fullName);

            ApiService api = ApiClient.getUnauthenticatedClient(this).create(ApiService.class);
            api.citizenRegister(body).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null
                            && response.body().getToken() != null) {
                        LoginResponse login = response.body();
                        sessionManager.saveSession(login.getToken(), email, login.getExpiresIn());
                        sessionManager.saveProfile(fullName, null, null);
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "Account created!", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        });
                        return;
                    }
                    runOnUiThread(() -> {
                        btnSignUp.setEnabled(true);
                        String msg = response.code() == 409
                                ? "Email already registered. Try logging in."
                                : "Sign up failed (HTTP " + response.code() + ")";
                        Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    runOnUiThread(() -> {
                        btnSignUp.setEnabled(true);
                        Toast.makeText(SignupActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
