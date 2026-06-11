package com.roadwatch.mobile.ui.reports;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import com.roadwatch.mobile.R;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class ComplaintClusterRenderer {

    private final Context context;
    private final MapView mapView;

    public ComplaintClusterRenderer(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
    }

    public Marker createMarker(ComplaintClusterItem item) {
        Marker marker = new Marker(mapView);
        marker.setPosition(item.getPosition());
        marker.setTitle(item.getTitle());
        marker.setSnippet(item.getSnippet());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(iconFor(item.getBucket()));
        return marker;
    }

    private Drawable iconFor(int bucket) {
        @DrawableRes int resId;
        switch (bucket) {
            case ComplaintClusterItem.BUCKET_RESOLVED:    resId = R.drawable.marker_green;  break;
            case ComplaintClusterItem.BUCKET_IN_PROGRESS: resId = R.drawable.marker_yellow; break;
            default:                                      resId = R.drawable.marker_red;    break;
        }
        return ContextCompat.getDrawable(context, resId);
    }
}