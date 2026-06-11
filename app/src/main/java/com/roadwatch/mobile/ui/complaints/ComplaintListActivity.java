package com.roadwatch.mobile.ui.complaints;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.ComplaintDto;
import com.roadwatch.mobile.network.dto.PagedComplaintsDto;
import com.roadwatch.mobile.network.dto.ReportDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * My Complaints screen.
 *
 * Features:
 *  - RecyclerView of complaint cards (thumbnail, location, date, status badge)
 *  - Shimmer skeleton while loading
 *  - Swipe-to-refresh
 *  - Empty state
 *  - Error state with Retry button
 *  - Tap a card ’ opens ComplaintDetailActivity
 */
public class ComplaintListActivity extends BaseActivity {

    private static final String TAG = "ComplaintListActivity";

    // Views
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView       rvComplaints;
    private LinearLayout       llShimmer;
    private LinearLayout       llEmpty;
    private LinearLayout       llError;
    private TextView           tvErrorMsg;

    private ComplaintListAdapter adapter;

    // ”€”€ Lifecycle ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_list);
        setupToolbar("My Complaints");

        bindViews();
        setupRecyclerView();
        setupSwipeRefresh();

        // Initial load
        loadComplaints();
    }

    // ”€”€ Setup ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvComplaints = findViewById(R.id.rvComplaints);
        llShimmer    = findViewById(R.id.llShimmer);
        llEmpty      = findViewById(R.id.llEmpty);
        llError      = findViewById(R.id.llError);
        tvErrorMsg   = findViewById(R.id.tvErrorMsg);

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> loadComplaints());
    }

    private void setupRecyclerView() {
        adapter = new ComplaintListAdapter(this::openDetail);
        rvComplaints.setLayoutManager(new LinearLayoutManager(this));
        rvComplaints.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.mint_green);
        swipeRefresh.setProgressBackgroundColorSchemeColor(
                getResources().getColor(R.color.bg_dark_slate, getTheme()));
        swipeRefresh.setOnRefreshListener(this::loadComplaints);
    }

    // ”€”€ Data loading ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void loadComplaints() {
        Log.i(TAG, "Fetching MY complaints from server€");
        showShimmer();

        ApiClient.api(this)
                .getMyComplaints(0, 100)
                .enqueue(new Callback<PagedComplaintsDto>() {

                    @Override
                    public void onResponse(Call<PagedComplaintsDto> call,
                                           Response<PagedComplaintsDto> response) {
                        swipeRefresh.setRefreshing(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "HTTP " + response.code());
                            showError("Server returned HTTP " + response.code());
                            return;
                        }

                        List<ReportDto> raw = response.body().content;
                        if (raw == null || raw.isEmpty()) {
                            Log.i(TAG, "No complaints on server");
                            showEmpty();
                            return;
                        }

                        Log.i(TAG, "Loaded " + raw.size() + " complaints");
                        adapter.submitList(toComplaintDtoList(raw));
                        showList();
                    }

                    @Override
                    public void onFailure(Call<PagedComplaintsDto> call, Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Log.e(TAG, "Network failure", t);
                        showError("Network error: " + t.getMessage());
                    }
                });
    }

    /** Convert the existing ReportDto (used by the paged endpoint) to ComplaintDto. */
    private List<ComplaintDto> toComplaintDtoList(List<ReportDto> raw) {
        List<ComplaintDto> out = new ArrayList<>(raw.size());
        for (ReportDto r : raw) {
            ComplaintDto c = new ComplaintDto();
            c.id          = r.id;
            c.description = r.description;
            c.severity    = r.severity;
            c.status      = r.status;
            c.roadType    = r.roadType;
            c.imageUrl    = r.imageUrl;
            c.location    = r.location;
            c.timestamp   = r.timestamp;
            c.department  = r.department;
            out.add(c);
        }
        return out;
    }

    // ”€”€ Navigation ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void openDetail(ComplaintDto complaint) {
        Log.i(TAG, "Opening detail for complaint id=" + complaint.id);
        Intent intent = new Intent(this, ComplaintDetailActivity.class);
        intent.putExtra(ComplaintDetailActivity.EXTRA_ID, complaint.id);
        startActivity(intent);
    }

    // ”€”€ State helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void showShimmer() {
        llShimmer.setVisibility(View.VISIBLE);
        rvComplaints.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.GONE);
        animateShimmer();
    }

    private void showList() {
        llShimmer.setVisibility(View.GONE);
        rvComplaints.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        llShimmer.setVisibility(View.GONE);
        rvComplaints.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
        llError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        llShimmer.setVisibility(View.GONE);
        rvComplaints.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.VISIBLE);
        tvErrorMsg.setText(msg);
    }

    /** Apply a repeating alpha pulse to every child of the shimmer container. */
    private void animateShimmer() {
        for (int i = 0; i < llShimmer.getChildCount(); i++) {
            View child = llShimmer.getChildAt(i);
            ObjectAnimator anim = ObjectAnimator.ofFloat(child, "alpha", 1f, 0.4f);
            anim.setDuration(800);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setStartDelay(i * 100L); // stagger each card
            anim.start();
        }
    }
}
