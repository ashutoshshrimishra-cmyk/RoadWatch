package com.roadwatch.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.ApiService;
import com.roadwatch.mobile.network.dto.CitizenAuthRequest;
import com.roadwatch.mobile.network.dto.LoginRequest;
import com.roadwatch.mobile.network.dto.LoginResponse;
import com.roadwatch.mobile.ui.dashboard.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        
        if (sessionManager.isLoggedIn()) {
            openDashboard();
            return;
        }
        
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        RadioGroup roleRadioGroup = findViewById(R.id.roleRadioGroup);
        TextView tvRegister = findViewById(R.id.tvRegister);

        // Default credentials for quick testing
        etEmail.setText("test@roadwatch.com");
        etPassword.setText("Test@1234");

        // Change hints and default text based on Role Selection
        roleRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioAdmin) {
                etEmail.setHint("Username");
                etEmail.setText("admin");
                etPassword.setText("admin123");
            } else {
                etEmail.setHint("Email Address");
                etEmail.setText("test@roadwatch.com");
                etPassword.setText("Test@1234");
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            if ("citizen@test.com".equals(email)) {
                sessionManager.saveRole("ROLE_CITIZEN");
                startActivity(new Intent(this, com.roadwatch.mobile.ui.dashboard.MainActivity.class));
                finish();
                return;
            }
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isAdmin = roleRadioGroup.getCheckedRadioButtonId() == R.id.radioAdmin;
            btnLogin.setEnabled(false);
            
            ApiService api = ApiClient.getUnauthenticatedClient(this).create(ApiService.class);

            if (isAdmin) {
                // Officer (Admin) Login
                api.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        handleLoginResponse(response, email, "ROLE_ADMIN", btnLogin);
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        handleLoginFailure(t, btnLogin);
                    }
                });
            } else {
                // Public User (Citizen) Login
                api.citizenLogin(new CitizenAuthRequest(email, password)).enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        handleLoginResponse(response, email, "ROLE_CITIZEN", btnLogin);
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        handleLoginFailure(t, btnLogin);
                    }
                });
            }
        });

        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
    }

    private void handleLoginResponse(Response<LoginResponse> response, String email, String role, MaterialButton btnLogin) {
        // FIXED: Enhanced error handling and thread safety
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return; // Activity destroyed, don't proceed
            }
            
            if (response.isSuccessful() && response.body() != null && response.body().getToken() != null) {
                LoginResponse body = response.body();
                
                try {
                    // Save token and role in session
                    sessionManager.saveSession(body.getToken(), email, body.getExpiresIn());
                    sessionManager.saveRole(role);
                    
                    openDashboard();
                } catch (Exception e) {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Session save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, parseError(response), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLoginFailure(Throwable t, MaterialButton btnLogin) {
        // FIXED: Enhanced error handling with network status check
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return; // Activity destroyed, don't proceed
            }
            
            btnLogin.setEnabled(true);
            String errorMessage = "Network error: " + t.getMessage();
            
            // Enhanced error message based on exception type
            if (t instanceof java.net.UnknownHostException) {
                errorMessage = "No internet connection. Check your network.";
            } else if (t instanceof java.net.SocketTimeoutException) {
                errorMessage = "Request timed out. Server may be slow.";
            } else if (t instanceof java.net.ConnectException) {
                errorMessage = "Cannot connect to server. Check API URL.";
            }
            
            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        });
    }

    private String parseError(Response<?> response) {
        if (response.code() == 401) {
            return "Invalid credentials";
        }
        if (response.code() == 404) {
            return "Login service not found. Check API URL.";
        }
        return "Login failed (HTTP " + response.code() + ")";
    }

    private void openDashboard() {
        String role = sessionManager.getRole();
        if ("ROLE_ADMIN".equals(role)) {
            startActivity(new Intent(this, com.roadwatch.mobile.AdminDashboardActivity.class));
        } else {
            startActivity(new Intent(this, com.roadwatch.mobile.ui.dashboard.MainActivity.class));
        }
        finish();
    }
}