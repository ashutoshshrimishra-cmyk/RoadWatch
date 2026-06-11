package com.roadwatch.mobile.ui.alerts;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.AlertType;
import com.roadwatch.mobile.network.dto.RoadAlertDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoadAlertAdapter extends RecyclerView.Adapter<RoadAlertAdapter.VH> {

    public interface OnHelpfulClickListener {
        void onHelpfulClicked(RoadAlertDto alert, int adapterPosition);
    }

    private final List<RoadAlertDto> items = new ArrayList<>();
    private final OnHelpfulClickListener helpfulListener;
    private Double userLat;
    private Double userLng;

    public RoadAlertAdapter(OnHelpfulClickListener listener) {
        this.helpfulListener = listener;
    }

    public void submit(List<RoadAlertDto> next, Double userLat, Double userLng) {
        items.clear();
        if (next != null) items.addAll(next);
        this.userLat = userLat;
        this.userLng = userLng;
        notifyDataSetChanged();
    }

    /** Replaces a single row with the latest server-confirmed copy. */
    public void update(int position, RoadAlertDto updated) {
        if (position < 0 || position >= items.size()) return;
        items.set(position, updated);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_road_alert, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position), position, userLat, userLng, helpfulListener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView ivAlertIcon;
        final TextView tvAlertType, tvLocationLabel, tvDistance,
                tvDescription, tvReportedTime, tvUpvoteCount;
        final MaterialButton btnHelpful;

        VH(@NonNull View itemView) {
            super(itemView);
            ivAlertIcon = itemView.findViewById(R.id.ivAlertIcon);
            tvAlertType = itemView.findViewById(R.id.tvAlertType);
            tvLocationLabel = itemView.findViewById(R.id.tvLocationLabel);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvReportedTime = itemView.findViewById(R.id.tvReportedTime);
            tvUpvoteCount = itemView.findViewById(R.id.tvUpvoteCount);
            btnHelpful = itemView.findViewById(R.id.btnHelpful);
        }

        void bind(RoadAlertDto alert, int position,
                  Double userLat, Double userLng,
                  OnHelpfulClickListener listener) {
            AlertType type = alert.resolveType();
            ivAlertIcon.setImageResource(type.iconRes);
            tvAlertType.setText(type.displayLabel);

            if (TextUtils.isEmpty(alert.locationLabel)) {
                tvLocationLabel.setVisibility(View.GONE);
            } else {
                tvLocationLabel.setVisibility(View.VISIBLE);
                tvLocationLabel.setText(alert.locationLabel);
            }

            if (TextUtils.isEmpty(alert.description)) {
                tvDescription.setVisibility(View.GONE);
            } else {
                tvDescription.setVisibility(View.VISIBLE);
                tvDescription.setText(alert.description);
            }

            // Reported X mins ago
            tvReportedTime.setText("Reported "
                    + DateUtils.getRelativeTimeSpanString(
                        alert.resolveCreatedAt(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS));

            int upvotes = alert.resolveUpvotes();
            tvUpvoteCount.setText(upvotes > 0 ? String.valueOf(upvotes) : "");

            // Helpful button state
            boolean alreadyUpvoted = alert.resolveUpvotedByMe();
            btnHelpful.setActivated(alreadyUpvoted);
            btnHelpful.setText(alreadyUpvoted ? "Confirmed" : "Helpful");
            btnHelpful.setOnClickListener(v -> {
                if (listener != null && !alreadyUpvoted) {
                    listener.onHelpfulClicked(alert, position);
                }
            });

            // Distance from user (if we have a fix)
            if (userLat != null && userLng != null
                    && alert.latitude != null && alert.longitude != null) {
                double km = AlertGeo.distanceKm(userLat, userLng,
                        alert.latitude, alert.longitude);
                tvDistance.setVisibility(View.VISIBLE);
                tvDistance.setText(String.format(Locale.US, "%.1f km", km));
            } else {
                tvDistance.setVisibility(View.GONE);
            }
        }
    }
}
