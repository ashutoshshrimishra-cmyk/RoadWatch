package com.roadwatch.mobile.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.auth.SessionManager;
import com.roadwatch.mobile.auth.FirebaseAuthManager;
import com.roadwatch.mobile.ui.auth.LoginActivity;
import com.roadwatch.mobile.ui.dashboard.MainActivity;

/**
 * Branded splash screen €” the first thing users see at cold start.
 *
 * Behaviour:
 *  1. Fade-in the centred logo cluster, then the bottom tagline shortly after.
 *  2. After {@link #MIN_SPLASH_DURATION_MS} (so the animation actually plays),
 *     check {@link SessionManager#isLoggedIn()} and route to MainActivity or
 *     LoginActivity. {@code finish()} clears the splash from the back stack.
 *
 * The activity is marked {@code noHistory} in the manifest so users can never
 * back-navigate into it from the dashboard.
 *
 * Note: deliberately does not extend BaseActivity €” splash should not honour
 * the user's persisted dark-mode toggle (it's always brand-dark) and shouldn't
 * carry a toolbar.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 1500L;
    private static final long LOGO_FADE_DURATION_MS = 700L;
    private static final long TAGLINE_DELAY_MS = 400L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View logoCluster = findViewById(R.id.logoCluster);
        View taglineGroup = findViewById(R.id.taglineGroup);

        // Fade-in: logo first (with a tiny upward translation), tagline after.
        logoCluster.setTranslationY(24f);
        logoCluster.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(LOGO_FADE_DURATION_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        taglineGroup.animate()
                                .alpha(1f)
                                .setDuration(LOGO_FADE_DURATION_MS)
                                .start();
                    }
                })
                .setStartDelay(150L)
                .start();

        // Tagline gets a short head-start fade after the logo settles.
        taglineGroup.postDelayed(() -> taglineGroup.animate()
                        .alpha(1f)
                        .setDuration(LOGO_FADE_DURATION_MS)
                        .start(),
                LOGO_FADE_DURATION_MS + TAGLINE_DELAY_MS);

        // Route the user once the splash has had time to breathe.
        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser,
                MIN_SPLASH_DURATION_MS);
    }

    private void routeUser() {
        if (isFinishing() || isDestroyed()) return;

        boolean loggedIn = new FirebaseAuthManager().isLoggedIn() || new SessionManager(this).isLoggedIn();
        Intent next = new Intent(this,
                loggedIn ? MainActivity.class : LoginActivity.class);
        next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(next);
        // overridePendingTransition smooths the cross-fade out of the splash.
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Disable back during splash to prevent app exit before routing.
    }
}
