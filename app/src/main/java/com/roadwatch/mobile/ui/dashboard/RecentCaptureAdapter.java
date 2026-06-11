package com.roadwatch.mobile.ui.dashboard;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.data.ComplaintEntity;

import java.util.ArrayList;
import java.util.List;

public class RecentCaptureAdapter extends RecyclerView.Adapter<RecentCaptureAdapter.ViewHolder> {

    private List<ComplaintEntity> complaints = new ArrayList<>();

    public void setComplaints(List<ComplaintEntity> newComplaints) {
        this.complaints = newComplaints;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_capture, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ComplaintEntity entity = complaints.get(position);
        
        // Setup title
        String title = entity.roadType != null ? entity.roadType + " Defect" : "Road Defect";
        holder.tvTitle.setText(title);
        
        // Setup subtitle (timestamp + sync status)
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                entity.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        String status = entity.isSynced ? "Synced" : "Pending";
        String loc = "";
        if (entity.latitude != null && entity.longitude != null) {
            loc = String.format(" - %.4f, %.4f", entity.latitude, entity.longitude);
        }
        holder.tvSubtitle.setText(timeAgo + " - " + status + loc);
        
        // Icon color based on sync
        if (entity.isSynced) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_map);
            holder.iconContainer.setCardBackgroundColor(android.graphics.Color.parseColor("#33FFFFFF"));
            holder.ivIcon.setColorFilter(android.graphics.Color.parseColor("#25C19A"));
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_camera);
            holder.iconContainer.setCardBackgroundColor(android.graphics.Color.parseColor("#25C19A"));
            holder.ivIcon.setColorFilter(android.graphics.Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return complaints == null ? 0 : complaints.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.card.MaterialCardView iconContainer;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}
