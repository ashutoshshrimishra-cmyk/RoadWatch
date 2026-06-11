package com.roadwatch.mobile.ui.roads;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.RoadDto;

import java.util.ArrayList;
import java.util.List;

public class RoadListAdapter extends RecyclerView.Adapter<RoadListAdapter.VH> {

    public interface OnRoadClickListener {
        void onRoadClick(RoadDto road);
    }

    private List<RoadDto> roads = new ArrayList<>();
    private OnRoadClickListener listener;

    public void setRoads(List<RoadDto> roads) {
        this.roads = roads != null ? roads : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnRoadClickListener(OnRoadClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_road, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RoadDto road = roads.get(position);
        holder.tvName.setText(road.name != null ? road.name : "Unnamed Road");
        holder.tvType.setText(road.roadType != null ? road.roadType : "N/A");
        holder.tvContractor.setText(road.contractorName != null
                ? "Contractor: " + road.contractorName : "Contractor: Not assigned");
        holder.pbBudget.setProgress(road.getBudgetUtilizationPercent());
        holder.tvBudget.setText(road.getBudgetDisplay());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRoadClick(road);
        });
    }

    @Override
    public int getItemCount() { return roads.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvContractor, tvBudget;
        ProgressBar pbBudget;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvRoadName);
            tvType = v.findViewById(R.id.tvRoadType);
            tvContractor = v.findViewById(R.id.tvContractor);
            pbBudget = v.findViewById(R.id.pbBudget);
            tvBudget = v.findViewById(R.id.tvBudget);
        }
    }
}
