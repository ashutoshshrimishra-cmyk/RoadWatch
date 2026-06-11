package com.roadwatch.mobile.ui.roads;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.RoadDto;
import com.roadwatch.mobile.ui.BaseActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Road Detail €” shows full intelligence for a single road:
 * code, type, status, contractor, last relaying date, budget utilization.
 */
public class RoadDetailActivity extends BaseActivity {

    public static final String EXTRA_ROAD_ID = "road_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_detail);
        setupToolbar("Road Details");

        long roadId = getIntent().getLongExtra(EXTRA_ROAD_ID, -1L);
        if (roadId == -1L) {
            Toast.makeText(this, "Invalid road", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchRoad(roadId);
    }

    private void fetchRoad(long id) {
        ApiClient.api(this).getRoadById(id).enqueue(new Callback<RoadDto>() {
            @Override
            public void onResponse(Call<RoadDto> call, Response<RoadDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populate(response.body());
                } else {
                    Toast.makeText(RoadDetailActivity.this,
                            "Road not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<RoadDto> call, Throwable t) {
                Toast.makeText(RoadDetailActivity.this,
                        "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populate(RoadDto road) {
        setupToolbar(road.name != null ? road.name : "Road Details");

        TextView tvName = findViewById(R.id.tvRoadName);
        TextView tvCode = findViewById(R.id.tvRoadCode);
        TextView tvType = findViewById(R.id.tvRoadType);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvContractor = findViewById(R.id.tvContractor);
        TextView tvLastRelaying = findViewById(R.id.tvLastRelaying);
        TextView tvBudgetDisplay = findViewById(R.id.tvBudgetDisplay);
        ProgressBar pbBudget = findViewById(R.id.pbBudget);
        TextView tvBudgetPercent = findViewById(R.id.tvBudgetPercent);

        tvName.setText(road.name != null ? road.name : "Unnamed Road");
        tvCode.setText(road.roadCode != null ? "Code: " + road.roadCode : "");
        tvType.setText(road.roadType != null ? road.roadType : "N/A");
        tvStatus.setText(road.status != null ? road.status : "Unknown");
        tvContractor.setText(road.contractorName != null ? road.contractorName : "Not assigned");
        tvLastRelaying.setText(road.lastRelayingDate != null
                ? "Last relaying: " + road.lastRelayingDate : "Last relaying: Unknown");

        int percent = road.getBudgetUtilizationPercent();
        tvBudgetDisplay.setText(road.getBudgetDisplay());
        pbBudget.setProgress(percent);
        tvBudgetPercent.setText(percent + "% utilized");
    }
}
