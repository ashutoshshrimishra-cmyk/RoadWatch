package com.roadwatch.mobile.ui.roads;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.RoadDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Road Intelligence €” lists all roads with budget utilization bars.
 * Tap a road to see full detail (contractor, budget, status).
 */
public class RoadListActivity extends BaseActivity {

    private RecyclerView rvRoads;
    private ProgressBar progressBar;
    private RoadListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_list);
        setupToolbar("Road Intelligence");

        rvRoads = findViewById(R.id.rvRoads);
        progressBar = findViewById(R.id.progressBar);

        adapter = new RoadListAdapter();
        adapter.setOnRoadClickListener(road -> {
            Intent intent = new Intent(this, RoadDetailActivity.class);
            intent.putExtra(RoadDetailActivity.EXTRA_ROAD_ID, road.id);
            startActivity(intent);
        });
        rvRoads.setLayoutManager(new LinearLayoutManager(this));
        rvRoads.setAdapter(adapter);

        loadRoads();
    }

    private void loadRoads() {
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.api(this).getRoads().enqueue(new Callback<List<RoadDto>>() {
            @Override
            public void onResponse(Call<List<RoadDto>> call, Response<List<RoadDto>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setRoads(response.body());
                } else {
                    Toast.makeText(RoadListActivity.this,
                            "Failed to load roads (HTTP " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RoadDto>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RoadListActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
