package com.roadwatch.mobile.ui.reports;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.ApiClient;
import com.roadwatch.mobile.network.dto.BudgetDto;
import com.roadwatch.mobile.ui.BaseActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Road Budget & Financial Dashboard.
 *
 * Features:
 *  - Mint-green summary card (total allotted, total spent, overall %)
 *  - RecyclerView of project cards with colour-coded progress bars
 *      green  ’ spend < 70 %
 *      orange ’ spend 70€“89 %
 *      red    ’ spend  90 %
 *  - Shimmer skeleton while loading
 *  - Swipe-to-refresh
 *  - Empty state  ("No budget data available for your region")
 *  - Error state  with Retry button
 *  - Back button via BaseActivity
 */
public class RoadFinancialStatusActivity extends BaseActivity {

    private static final String TAG = "RoadBudgetActivity";

    // ”€”€ Views ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout       llContent;
    private LinearLayout       llShimmer;
    private LinearLayout       llEmpty;
    private LinearLayout       llError;
    private TextView           tvErrorMsg;

    // Summary card views
    private TextView    tvTotalSanctioned;
    private TextView    tvTotalSpent;
    private TextView    tvTotalPercent;
    private ProgressBar progressOverall;

    private RecyclerView  rvBudgets;
    private BudgetAdapter adapter;

    // ”€”€ Lifecycle ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_road_financial_status);
        setupToolbar("Road Budget");

        bindViews();
        setupRecyclerView();
        setupSwipeRefresh();

        loadBudgets();
    }

    // ”€”€ Setup ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh);
        llContent    = findViewById(R.id.llContent);
        llShimmer    = findViewById(R.id.llShimmer);
        llEmpty      = findViewById(R.id.llEmpty);
        llError      = findViewById(R.id.llError);
        tvErrorMsg   = findViewById(R.id.tvErrorMsg);
        rvBudgets    = findViewById(R.id.rvBudgets);

        // Summary card (included layout)
        View summaryCard    = findViewById(R.id.summaryCard);
        tvTotalSanctioned   = summaryCard.findViewById(R.id.tvTotalSanctioned);
        tvTotalSpent        = summaryCard.findViewById(R.id.tvTotalSpent);
        tvTotalPercent      = summaryCard.findViewById(R.id.tvTotalPercent);
        progressOverall     = summaryCard.findViewById(R.id.progressOverall);

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> loadBudgets());
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter();
        rvBudgets.setLayoutManager(new LinearLayoutManager(this));
        rvBudgets.setAdapter(adapter);
        // Disable nested scrolling so the outer NestedScrollView (if any) handles it
        rvBudgets.setNestedScrollingEnabled(false);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.mint_green);
        swipeRefresh.setProgressBackgroundColorSchemeColor(
                getResources().getColor(R.color.bg_dark_slate, getTheme()));
        swipeRefresh.setOnRefreshListener(this::loadBudgets);
    }

    // ”€”€ Data loading ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void loadBudgets() {
        Log.i(TAG, "Fetching budgets from server€");
        showShimmer();

        ApiClient.api(this)
                .getBudgets()
                .enqueue(new Callback<List<BudgetDto>>() {

                    @Override
                    public void onResponse(Call<List<BudgetDto>> call,
                                           Response<List<BudgetDto>> response) {
                        swipeRefresh.setRefreshing(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "HTTP " + response.code());
                            showError("Server returned HTTP " + response.code());
                            return;
                        }

                        List<BudgetDto> list = response.body();
                        if (list.isEmpty()) {
                            Log.i(TAG, "No budget data");
                            showEmpty();
                            return;
                        }

                        Log.i(TAG, "Loaded " + list.size() + " budget entries");
                        populateSummary(list);
                        adapter.submitList(list);
                        showContent();
                    }

                    @Override
                    public void onFailure(Call<List<BudgetDto>> call, Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Log.e(TAG, "Network failure", t);
                        showError("Network error €” " + t.getMessage());
                    }
                });
    }

    // ”€”€ Summary card ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    /**
     * Aggregates all budget entries and populates the summary card.
     *
     * totalSanctioned =  amountSanctioned
     * totalSpent      =  amountSpent
     * overallPercent  = (totalSpent / totalSanctioned) * 100
     */
    private void populateSummary(List<BudgetDto> list) {
        double totalSanctioned = 0;
        double totalSpent      = 0;

        for (BudgetDto b : list) {
            totalSanctioned += b.sanctionedDouble();
            totalSpent      += b.spentDouble();
        }

        int overallPct = (totalSanctioned > 0)
                ? (int) Math.min(100, (totalSpent / totalSanctioned) * 100)
                : 0;

        tvTotalSanctioned.setText(BudgetDto.formatAmount(String.valueOf(totalSanctioned)));
        tvTotalSpent.setText(BudgetDto.formatAmount(String.valueOf(totalSpent)));
        tvTotalPercent.setText(overallPct + "%");

        // Animate the overall progress bar
        ObjectAnimator.ofInt(progressOverall, "progress", 0, overallPct)
                .setDuration(900)
                .start();
    }

    // ”€”€ State helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    private void showShimmer() {
        llShimmer.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.GONE);
        animateShimmer();
    }

    private void showContent() {
        llShimmer.setVisibility(View.GONE);
        llContent.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.GONE);
    }

    private void showEmpty() {
        llShimmer.setVisibility(View.GONE);
        llContent.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
        llError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        llShimmer.setVisibility(View.GONE);
        llContent.setVisibility(View.GONE);
        llEmpty.setVisibility(View.GONE);
        llError.setVisibility(View.VISIBLE);
        tvErrorMsg.setText(msg);
    }

    /** Staggered alpha pulse on each shimmer skeleton card. */
    private void animateShimmer() {
        for (int i = 0; i < llShimmer.getChildCount(); i++) {
            View child = llShimmer.getChildAt(i);
            ObjectAnimator anim = ObjectAnimator.ofFloat(child, "alpha", 1f, 0.4f);
            anim.setDuration(800);
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setStartDelay(i * 120L);
            anim.start();
        }
    }
}
