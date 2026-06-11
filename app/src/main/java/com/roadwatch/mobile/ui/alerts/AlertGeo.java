package com.roadwatch.mobile.ui.alerts;

/** Tiny haversine distance helper used to filter alerts within a radius. */
public final class AlertGeo {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private AlertGeo() {}

    public static double distanceKm(double lat1, double lng1,
                                    double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
