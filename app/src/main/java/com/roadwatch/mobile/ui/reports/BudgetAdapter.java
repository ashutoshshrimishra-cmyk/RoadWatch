package com.roadwatch.mobile.ui.reports;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.BudgetDto;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Road Budget list.
 *
 * Colour rules:
 *   spend < 90 % ’ progress bar green  (#10B981), spent text green
 *   spend  90 % ’ progress bar red    (#EF4444), spent text red
 *                  card gets a subtle red left-border tint
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.VH> {

    // Threshold above which the card turns red
    private static final int OVER_BUDGET_THRESHOLD = 90;

    private static final int COLOR_GREEN  = Color.parseColor("#10B981");
    private static final int COLOR_RED    = Color.parseColor("#EF4444");
    private static final int COLOR_ORANGE = Color.parseColor("#F59E0B");

    private List<BudgetDto> items = new ArrayList<>();

    public void submitList(List<BudgetDto> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(items.get(pos));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ”€”€ ViewHolder ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    static class VH extends RecyclerView.ViewHolder {

        final TextView    tvRoadName, tvRoadType, tvContractor;
        final TextView    tvSanctioned, tvSpent, tvPercent, tvLastRelaying;
        final ProgressBar progressBar;

        VH(@NonNull View v) {
            super(v);
            tvRoadName     = v.findViewById(R.id.tvRoadName);
            tvRoadType     = v.findViewById(R.id.tvRoadType);
            tvContractor   = v.findViewById(R.id.tvContractor);
            tvSanctioned   = v.findViewById(R.id.tvSanctioned);
            tvSpent        = v.findViewById(R.id.tvSpent);
            tvPercent      = v.findViewById(R.id.tvPercent);
            tvLastRelaying = v.findViewById(R.id.tvLastRelaying);
            progressBar    = v.findViewById(R.id.progressBar);
        }

        void bind(BudgetDto b) {
            // ”€”€ Text fields ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€
            tvRoadName.setText(
                    b.roadName != null && !b.roadName.isEmpty() ? b.roadName : "Unknown Road");

            tvRoadType.setText(
                    b.roadType != null && !b.roadType.isEmpty() ? b.roadType : "N/A");

            tvContractor.setText(
                    b.contractorName != null && !b.contractorName.isEmpty()
                            ? b.contractorName : "Contractor not assigned");

            tvSanctioned.setText(BudgetDto.formatAmount(b.amountSanctioned));

            tvLastRelaying.setText(
                    b.lastRelayingDate != null && !b.lastRelayingDate.isEmpty()
                            ? b.lastRelayingDate : "N/A");

            // ”€”€ Progress calculation ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€
            int pct = b.spendPercent();
            tvPercent.setText(pct + "%");

            // ”€”€ Colour coding ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€
            int barColor, spentColor;
            if (pct >= OVER_BUDGET_THRESHOLD) {
                barColor   = COLOR_RED;
                spentColor = COLOR_RED;
            } else if (pct >= 70) {
                barColor   = COLOR_ORANGE;
                spentColor = COLOR_ORANGE;
            } else {
                barColor   = COLOR_GREEN;
                spentColor = COLOR_GREEN;
            }

            tvSpent.setText(BudgetDto.formatAmount(b.amountSpent));
            tvSpent.setTextColor(spentColor);
            tvPercent.setTextColor(barColor);

            // Tint the progress bar's filled layer
            tintProgressBar(progressBar, barColor);

            // Animate progress from 0 ’ pct
            progressBar.setProgress(0);
            progressBar.setProgress(pct);
        }

        /**
         * Tints the "progress" layer of a LayerDrawable progress bar.
         * Works with our custom progress_bar_budget.xml drawable.
         */
        private void tintProgressBar(ProgressBar bar, int color) {
            Drawable d = bar.getProgressDrawable();
            if (d instanceof LayerDrawable) {
                LayerDrawable ld = (LayerDrawable) d;
                Drawable progress = ld.findDrawableByLayerId(android.R.id.progress);
                if (progress != null) {
                    progress.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
            } else if (d != null) {
                d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }
}
