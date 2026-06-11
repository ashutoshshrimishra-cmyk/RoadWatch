package com.roadwatch.mobile.ui.reports;

import android.content.Context;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.roadwatch.mobile.R;
import com.roadwatch.mobile.network.dto.ReportDto;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class ComplaintInfoWindowAdapter extends InfoWindow {

    private final Context   context;
    private ReportDto       currentReport;
    private final ImageView ivThumb;
    private final TextView  tvRoadType;
    private final TextView  tvStatus;

    public ComplaintInfoWindowAdapter(MapView mapView) {
        super(R.layout.map_info_window, mapView);
        this.context    = mapView.getContext().getApplicationContext();
        this.ivThumb    = mView.findViewById(R.id.ivInfoThumb);
        this.tvRoadType = mView.findViewById(R.id.tvInfoRoadType);
        this.tvStatus   = mView.findViewById(R.id.tvInfoStatus);
    }

    public void setReport(ReportDto report) {
        this.currentReport = report;
    }

    @Override
    public void onOpen(Object item) {
        if (currentReport == null) return;
        tvRoadType.setText(TextUtils.isEmpty(currentReport.roadType) ? "Road" : currentReport.roadType);
        String status = TextUtils.isEmpty(currentReport.status) ? "PENDING" : currentReport.status;
        tvStatus.setText(status);
        tvStatus.setTextColor(ContextCompat.getColor(context, statusColorRes(status)));
        ivThumb.setImageResource(R.drawable.ic_camera);
        if (!TextUtils.isEmpty(currentReport.imageUrl)) {
            Glide.with(context).load(currentReport.imageUrl)
                    .apply(new RequestOptions().override(192, 192).centerCrop())
                    .into(ivThumb);
        }
    }

    @Override
    public void onClose() {
        Glide.with(context).clear(ivThumb);
    }

    private int statusColorRes(String status) {
        String s = status.toUpperCase();
        if (s.equals("RESOLVED") || s.equals("FIXED") || s.equals("CLOSED")) return R.color.mint_green;
        if (s.equals("IN_PROGRESS") || s.equals("ASSIGNED") || s.equals("UNDER_REPAIR")) return android.R.color.holo_orange_light;
        return R.color.coral_red;
    }
}