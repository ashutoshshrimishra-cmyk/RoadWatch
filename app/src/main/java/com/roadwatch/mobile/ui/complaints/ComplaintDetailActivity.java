package com.roadwatch.mobile.ui.complaints;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.ComplaintDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Complaint Detail screen.
 *
 * Shows:
 *  - Hero image
 *  - Info card (description, road type, department, severity)
 *  - Vertical status timeline:
 *       Reported  ’   Verification  ’  ” Action  ’   Resolved
 */
public class ComplaintDetailActivity extends BaseActivity {

    /** @deprecated Use {@link #EXTRA_COMPLAINT_ID} instead. */
    @Deprecated
    public static final String EXTRA_ID = "complaint_id";
    public static final String EXTRA_COMPLAINT_ID = "complaint_id";
    private static final String TAG = "ComplaintDetailActivity";

    // ”€”€ Timeline step definitions ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private static final int STEP_REPORTED    = 0;
    private static final int STEP_ACCEPTED    = 1;
    private static final int STEP_FORWARDED   = 2;
    private static final int STEP_IN_PROGRESS = 3;
    private static final int STEP_RESOLVED    = 4;

    // ”€”€ Views ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private ImageView    ivHeroImage;
    private TextView     tvDescription;
    private LinearLayout llTimeline;

    // Info row views (each row has tvLabel + tvValue)
    private TextView tvRoadTypeLabel, tvRoadTypeValue;
    private TextView tvDeptLabel,     tvDeptValue;
    private TextView tvSevLabel,      tvSevValue;

    // ”€”€ Lifecycle ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_detail);
        setupToolbar("Complaint Details");

        long id = getIntent().getLongExtra(EXTRA_ID, -1L);
        if (id == -1L) {
            Toast.makeText(this, "Invalid complaint", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        fetchComplaint(id);
    }

    // ”€”€ Setup ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void bindViews() {
        ivHeroImage   = findViewById(R.id.ivHeroImage);
        tvDescription = findViewById(R.id.tvDescription);
        llTimeline    = findViewById(R.id.llTimeline);

        // Info rows €” each <include> exposes tvLabel and tvValue
        View rowRoadType   = findViewById(R.id.rowRoadType);
        View rowDepartment = findViewById(R.id.rowDepartment);
        View rowSeverity   = findViewById(R.id.rowSeverity);

        tvRoadTypeLabel = rowRoadType.findViewById(R.id.tvLabel);
        tvRoadTypeValue = rowRoadType.findViewById(R.id.tvValue);
        tvDeptLabel     = rowDepartment.findViewById(R.id.tvLabel);
        tvDeptValue     = rowDepartment.findViewById(R.id.tvValue);
        tvSevLabel      = rowSeverity.findViewById(R.id.tvLabel);
        tvSevValue      = rowSeverity.findViewById(R.id.tvValue);

        tvRoadTypeLabel.setText("Road Type");
        tvDeptLabel.setText("Department");
        tvSevLabel.setText("Severity");
    }

    // ”€”€ Network ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void fetchComplaint(long id) {
        Log.i(TAG, "Fetching complaint id=" + id);

        ApiClient.api(this)
                .getComplaintById(id)
                .enqueue(new Callback<ComplaintDto>() {

                    @Override
                    public void onResponse(Call<ComplaintDto> call,
                                           Response<ComplaintDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            populate(response.body());
                        } else {
                            Log.w(TAG, "HTTP " + response.code());
                            Toast.makeText(ComplaintDetailActivity.this,
                                    "Could not load details (HTTP " + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ComplaintDto> call, Throwable t) {
                        Log.e(TAG, "Network failure", t);
                        Toast.makeText(ComplaintDetailActivity.this,
                                "Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ”€”€ Populate UI ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void populate(ComplaintDto c) {
        // Description
        tvDescription.setText(
                c.description != null && !c.description.isEmpty()
                        ? c.description : "Road defect reported");

        // Info rows
        tvRoadTypeValue.setText(c.roadType   != null ? c.roadType   : "N/A");
        tvDeptValue.setText(    c.department != null ? c.department : "Not assigned");

        if (c.severity != null && !c.severity.isEmpty()) {
            tvSevValue.setText(c.severity);
            tvSevValue.setTextColor(severityColor(c.severity));
        } else {
            tvSevValue.setText("N/A");
        }

        // AI Verdict section €” show if backend returned AI analysis
        TextView tvAiLabel = findViewById(R.id.tvAiLabel);
        TextView tvAiConfidence = findViewById(R.id.tvAiConfidence);
        View aiCard = findViewById(R.id.cardAiVerdict);
        if (aiCard != null) {
            if (c.aiLabel != null && !c.aiLabel.isEmpty()) {
                aiCard.setVisibility(View.VISIBLE);
                if (tvAiLabel != null) tvAiLabel.setText(c.aiLabel.replace("_", " "));
                if (tvAiConfidence != null && c.aiConfidence != null) {
                    tvAiConfidence.setText(String.format(java.util.Locale.US,
                            "%.0f%% confidence", c.aiConfidence * 100));
                }
            } else {
                aiCard.setVisibility(View.GONE);
            }
        }

        // Admin notes section
        TextView tvAdminNotes = findViewById(R.id.tvAdminNotes);
        View notesCard = findViewById(R.id.cardAdminNotes);
        if (notesCard != null) {
            if (c.adminNotes != null && !c.adminNotes.isEmpty()) {
                notesCard.setVisibility(View.VISIBLE);
                if (tvAdminNotes != null) tvAdminNotes.setText(c.adminNotes);
            } else {
                notesCard.setVisibility(View.GONE);
            }
        }

        // Department response section
        TextView tvDeptResponse = findViewById(R.id.tvDeptResponse);
        TextView tvAssignedOfficer = findViewById(R.id.tvAssignedOfficer);
        TextView tvExpectedDate = findViewById(R.id.tvExpectedDate);
        View deptCard = findViewById(R.id.cardDeptResponse);
        if (deptCard != null) {
            boolean hasDeptInfo = (c.deptResponse != null && !c.deptResponse.isEmpty())
                    || (c.assignedOfficer != null && !c.assignedOfficer.isEmpty());
            if (hasDeptInfo) {
                deptCard.setVisibility(View.VISIBLE);
                if (tvDeptResponse != null && c.deptResponse != null)
                    tvDeptResponse.setText(c.deptResponse);
                if (tvAssignedOfficer != null && c.assignedOfficer != null)
                    tvAssignedOfficer.setText("Officer: " + c.assignedOfficer);
                if (tvExpectedDate != null && c.expectedCompletion != null)
                    tvExpectedDate.setText("Expected: " + formatTimestamp(c.expectedCompletion));
            } else {
                deptCard.setVisibility(View.GONE);
            }
        }

        // Hero image – load real photo via Glide
        androidx.core.widget.ImageViewCompat.setImageTintList(ivHeroImage, null);
        if (c.imageUrl != null && !c.imageUrl.isEmpty()) {
            String fullUrl = c.imageUrl;
            // If relative URL, prepend backend base
            if (!fullUrl.startsWith("http")) {
                String base = com.roadwatch.mobile.BuildConfig.API_BASE_URL;
                // Strip trailing /api/ to get the host root
                base = base.replace("/api/", "").replace("/api", "");
                fullUrl = base + (fullUrl.startsWith("/") ? fullUrl : "/" + fullUrl);
            }
            Log.i(TAG, "Loading image: " + fullUrl);
            com.bumptech.glide.Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_camera)
                    .error(R.drawable.ic_camera)
                    .centerCrop()
                    .into(ivHeroImage);
        } else {
            ivHeroImage.setImageResource(R.drawable.ic_camera);
            ivHeroImage.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Build timeline
        buildTimeline(c);

        // Show feedback prompt if resolved and not yet rated
        View feedbackCard = findViewById(R.id.cardFeedback);
        if (feedbackCard != null) {
            if ("RESOLVED".equalsIgnoreCase(c.status) && (c.citizenRating == null || c.citizenRating == 0)) {
                feedbackCard.setVisibility(View.VISIBLE);
                setupFeedbackButtons(c);
            } else {
                feedbackCard.setVisibility(View.GONE);
            }
        }
    }

    // ”€”€ Timeline ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    /**
     * Builds the 4-step vertical timeline based on the complaint's current status.
     *
     * Step colours:
     *   completed / current ’ mint_green circle
     *   future              ’ gray circle, dimmed text
     */
    private void buildTimeline(ComplaintDto c) {
        llTimeline.removeAllViews();
        int current = c.getStatusStep();
        if (current == -1) {
            addStep("Rejected", c.adminNotes != null ? c.adminNotes : "Complaint was rejected by admin", formatTimestamp(c.timestamp), true, true);
            return;
        }
        String reportedTime   = formatTimestamp(c.timestamp);
        String acceptedTime   = current >= STEP_ACCEPTED    ? reportedTime : "Pending";
        String forwardedTime  = current >= STEP_FORWARDED   ? reportedTime : "Pending";
        String inProgressTime = current >= STEP_IN_PROGRESS ? reportedTime : "Pending";
        String resolvedTime   = current >= STEP_RESOLVED    ? formatTimestamp(c.resolvedAt) : "Pending";
        String forwardedSub   = (c.department != null && !c.department.isEmpty()) ? "Routed to " + c.department : "Awaiting department assignment";
        String inProgressSub  = (c.assignedOfficer != null && !c.assignedOfficer.isEmpty()) ? "Officer: " + c.assignedOfficer : "Department is working on it";
        addStep("Reported",     "Complaint submitted successfully", reportedTime,   current >= STEP_REPORTED,    false);
        addStep("Accepted",     "Admin verified the complaint",     acceptedTime,   current >= STEP_ACCEPTED,    false);
        addStep("Forwarded",    forwardedSub,                       forwardedTime,  current >= STEP_FORWARDED,   false);
        addStep("In Progress",  inProgressSub,                      inProgressTime, current >= STEP_IN_PROGRESS, false);
        addStep("Resolved",     "Road repaired and issue closed",    resolvedTime,   current >= STEP_RESOLVED,    true);
    }

    /**
     * Inflates one timeline row and adds it to llTimeline.
     *
     * @param title    Step heading
     * @param subtitle Descriptive text
     * @param time     Timestamp string
     * @param active   Whether this step has been reached
     * @param last     Whether to hide the connector line below the dot
     */
    private void addStep(String title, String subtitle, String time,
                         boolean active, boolean last) {

        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_timeline_step, llTimeline, false);

        View     vDot      = row.findViewById(R.id.vDot);
        View     vLine     = row.findViewById(R.id.vLine);
        TextView tvTitle   = row.findViewById(R.id.tvStepTitle);
        TextView tvSub     = row.findViewById(R.id.tvStepSubtitle);
        TextView tvTime    = row.findViewById(R.id.tvStepTime);

        tvTitle.setText(title);
        tvSub.setText(subtitle);
        tvTime.setText(time);

        // Dot colour
        vDot.setBackgroundResource(
                active ? R.drawable.timeline_circle_active
                       : R.drawable.timeline_circle_inactive);

        // Hide connector on last step
        if (last) vLine.setVisibility(View.GONE);

        // Dim future steps
        if (!active) {
            tvTitle.setAlpha(0.45f);
            tvSub.setAlpha(0.45f);
            tvTime.setAlpha(0.45f);
        }

        llTimeline.addView(row);
    }

    // ”€”€ Helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private String formatTimestamp(String iso) {
        if (iso == null || iso.isEmpty()) return "N/A";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso);
            if (d == null) return iso;
            return new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.US).format(d);
        } catch (ParseException e) {
            return iso;
        }
    }

    private void setupFeedbackButtons(ComplaintDto c) {
        // Simple 5-star rating using 5 TextViews styled as star buttons
        int[] starIds = {R.id.star1, R.id.star2, R.id.star3, R.id.star4, R.id.star5};
        for (int i = 0; i < starIds.length; i++) {
            View star = findViewById(starIds[i]);
            if (star == null) continue;
            final int rating = i + 1;
            star.setOnClickListener(v -> submitFeedback(c.id, rating));
        }
    }

    private void submitFeedback(Long complaintId, int rating) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("rating", rating);
        ApiClient.api(this).submitFeedback(complaintId, body)
                .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call,
                                           retrofit2.Response<okhttp3.ResponseBody> response) {
                        android.widget.Toast.makeText(ComplaintDetailActivity.this,
                                "Thank you for your feedback! " + rating,
                                android.widget.Toast.LENGTH_SHORT).show();
                        View card = findViewById(R.id.cardFeedback);
                        if (card != null) card.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                        android.widget.Toast.makeText(ComplaintDetailActivity.this,
                                "Could not submit feedback", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int severityColor(String sev) {
        if (sev == null) return ContextCompat.getColor(this, android.R.color.white);
        switch (sev.toUpperCase()) {
            case "HIGH":   return ContextCompat.getColor(this, android.R.color.holo_red_light);
            case "MEDIUM": return ContextCompat.getColor(this, android.R.color.holo_orange_light);
            case "LOW":    return ContextCompat.getColor(this, android.R.color.holo_blue_light);
            default:       return ContextCompat.getColor(this, android.R.color.white);
        }
    }
}


