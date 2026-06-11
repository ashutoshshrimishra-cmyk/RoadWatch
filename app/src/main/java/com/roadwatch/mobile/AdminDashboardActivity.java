package com.roadwatch.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.ui.auth.LoginActivity;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);

        // UI Binding
        drawerLayout = findViewById(R.id.drawer_layout);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        NavigationView navigationView = findViewById(R.id.nav_view);

        MaterialCardView cardIncomingQuery = findViewById(R.id.cardIncomingQuery);
        MaterialCardView cardSuccess = findViewById(R.id.cardSuccess);
        MaterialCardView cardReject = findViewById(R.id.cardReject);
        MaterialCardView cardAppendix = findViewById(R.id.cardAppendix);

        // Sidebar Header Text Setup (Wireframe Specs: Mr. Sakshi / PWD)
        View headerView = navigationView.getHeaderView(0);
        TextView tvHeaderName = headerView.findViewById(R.id.navHeaderName);
        TextView tvHeaderDept = headerView.findViewById(R.id.navHeaderEmail); // Reusing existing ID from nav_header.xml

        if (tvHeaderName != null) tvHeaderName.setText("Mr. Sakshi");
        if (tvHeaderDept != null) tvHeaderDept.setText("PWD Department");

        // Navigation Drawer Toggle Setup
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                0, 0);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // 2x2 Grid Click Listeners
        cardIncomingQuery.setOnClickListener(v -> {
            // TODO: Route to Module 4 (Complaint List / Detail)
            Toast.makeText(this, "Opening Incoming Queries...", Toast.LENGTH_SHORT).show();
        });

        cardSuccess.setOnClickListener(v -> 
            Toast.makeText(this, "Successful Uploads Log", Toast.LENGTH_SHORT).show());

        cardReject.setOnClickListener(v -> 
            Toast.makeText(this, "Rejected by AI Verification Log", Toast.LENGTH_SHORT).show());

        cardAppendix.setOnClickListener(v -> 
            Toast.makeText(this, "Opening Appendix Reports...", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_officer_home) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_officer_inbox) {
            Toast.makeText(this, "Inbox Opening...", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_officer_settings) {
            Toast.makeText(this, "Settings...", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_officer_logout) {
            handleLogout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleLogout() {
        sessionManager.clear(); // Assuming this is the method inside your SessionManager
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}