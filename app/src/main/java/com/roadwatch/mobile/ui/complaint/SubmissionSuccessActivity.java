package com.roadwatch.mobile.ui.complaint;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.ui.BaseActivity;
import com.roadwatch.mobile.ui.complaints.ComplaintDetailActivity;
import com.roadwatch.mobile.ui.dashboard.MainActivity;

public class SubmissionSuccessActivity extends BaseActivity {

    public static final String EXTRA_COMPLAINT_ID = "complaint_id";
    public static final String EXTRA_IS_OFFLINE = "is_offline";

    private ImageView checkmarkIcon;
    private TextView successTitle;
    private TextView successMessage;
    private Button viewReportButton;
    private Button backHomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission_success);

        setupToolbar("Success");

        checkmarkIcon = findViewById(R.id.checkmarkIcon);
        successTitle = findViewById(R.id.successTitle);
        successMessage = findViewById(R.id.successMessage);
        viewReportButton = findViewById(R.id.viewReportButton);
        backHomeButton = findViewById(R.id.backHomeButton);

        long complaintId = getIntent().getLongExtra(EXTRA_COMPLAINT_ID, -1);
        boolean isOffline = getIntent().getBooleanExtra(EXTRA_IS_OFFLINE, false);

        if (isOffline) {
            successTitle.setText("Saved for Later");
            successMessage.setText("Your complaint has been saved locally and will sync automatically when you're back online.");
            viewReportButton.setVisibility(View.GONE);
        } else {
            successTitle.setText("Complaint Registered!");
            successMessage.setText("Our AI is now analyzing the road defect. You'll be notified once verification is complete.");
            
            if (complaintId > 0) {
                viewReportButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ComplaintDetailActivity.class);
                    intent.putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaintId);
                    startActivity(intent);
                    finish();
                });
            } else {
                viewReportButton.setVisibility(View.GONE);
            }
        }

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        animateCheckmark();
    }

    private void animateCheckmark() {
        checkmarkIcon.setScaleX(0f);
        checkmarkIcon.setScaleY(0f);
        checkmarkIcon.setAlpha(0f);

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0f, 1f,
                0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnimation.setDuration(600);
        scaleAnimation.setFillAfter(true);

        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                checkmarkIcon.setAlpha(1f);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                checkmarkIcon.setScaleX(1f);
                checkmarkIcon.setScaleY(1f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        checkmarkIcon.postDelayed(() -> checkmarkIcon.startAnimation(scaleAnimation), 200);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
