package com.roadwatch.mobile.ui.complaints;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.ComplaintDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the My Complaints list.
 * Each card shows: thumbnail, description, road type, date, status badge.
 */
public class ComplaintListAdapter
        extends RecyclerView.Adapter<ComplaintListAdapter.VH> {

    public interface OnItemClick {
        void onClick(ComplaintDto complaint);
    }

    private List<ComplaintDto> items = new ArrayList<>();
    private final OnItemClick listener;

    public ComplaintListAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    /** Replace the full dataset and refresh. */
    public void submitList(List<ComplaintDto> list) {
        items = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_complaint, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(items.get(pos));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────

    class VH extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView  tvDescription, tvRoadType, tvDate, tvSeverity, tvStatus;

        VH(@NonNull View v) {
            super(v);
            ivThumb       = v.findViewById(R.id.ivThumb);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvRoadType    = v.findViewById(R.id.tvRoadType);
            tvDate        = v.findViewById(R.id.tvDate);
            tvSeverity    = v.findViewById(R.id.tvSeverity);
            tvStatus      = v.findViewById(R.id.tvStatus);

            v.setOnClickListener(view -> {
                int p = getAdapterPosition();
                if (p != RecyclerView.NO_POSITION) listener.onClick(items.get(p));
            });
        }

        void bind(ComplaintDto c) {
            // Description
            tvDescription.setText(
                    c.description != null && !c.description.isEmpty()
                            ? c.description : "Road defect reported");

            // Road type
            tvRoadType.setText(c.roadType != null ? c.roadType : "—");

            // Date
            tvDate.setText(relativeTime(c.timestamp));

            // Severity chip
            if (c.severity != null && !c.severity.isEmpty()) {
                tvSeverity.setVisibility(View.VISIBLE);
                tvSeverity.setText(c.severity);
                tvSeverity.setBackgroundResource(severityBg(c.severity));
            } else {
                tvSeverity.setVisibility(View.GONE);
            }

            // Status badge
            tvStatus.setText(c.getStatusLabel());
            tvStatus.setBackgroundResource(statusBg(c.status));

            // Thumbnail - load from Cloudinary via Glide
            if (c.imageUrl != null && !c.imageUrl.isEmpty()) {
                ivThumb.clearColorFilter();
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(c.imageUrl)
                        .placeholder(R.drawable.ic_camera)
                        .error(R.drawable.ic_camera)
                        .centerCrop()
                        .into(ivThumb);
            } else {
                ivThumb.setImageResource(R.drawable.ic_camera);
            }
        }

        // ── helpers ──────────────────────────────────────────────────────

        private int statusBg(String status) {
            if (status == null) return R.drawable.badge_status_pending;
            switch (status.toUpperCase()) {
                case "IN_PROGRESS": return R.drawable.badge_status_in_progress;
                case "RESOLVED":    return R.drawable.badge_status_resolved;
                default:            return R.drawable.badge_status_pending;
            }
        }

        private int severityBg(String sev) {
            if (sev == null) return R.drawable.badge_severity_high;
            switch (sev.toUpperCase()) {
                case "MEDIUM": return R.drawable.badge_status_pending;     // orange
                case "LOW":    return R.drawable.badge_status_in_progress; // blue
                default:       return R.drawable.badge_severity_high;      // red
            }
        }

        /** Returns a human-readable relative time string. */
        private String relativeTime(String iso) {
            if (iso == null || iso.isEmpty()) return "Unknown date";
            try {
                Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso);
                if (d == null) return iso;
                long diff = System.currentTimeMillis() - d.getTime();
                long mins  = diff / 60_000;
                long hours = mins  / 60;
                long days  = hours / 24;
                if (days  > 7)  return new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(d);
                if (days  > 0)  return days  + " day"  + (days  > 1 ? "s" : "") + " ago";
                if (hours > 0)  return hours + " hr"   + (hours > 1 ? "s" : "") + " ago";
                if (mins  > 0)  return mins  + " min"  + (mins  > 1 ? "s" : "") + " ago";
                return "Just now";
            } catch (ParseException e) {
                return iso;
            }
        }
    }
}
