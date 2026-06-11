package com.roadwatch.mobile.network.dto;

import androidx.annotation.DrawableRes;

import com.roadwatch.mobile.R;

/**
 * Three Step-9 hazard categories. The {@code wireValue} is what travels over
 * the API; {@link #fromWire(String)} is forgiving so backend variations
 * (lowercase, snake_case) still parse correctly.
 */
public enum AlertType {

    ACCIDENT(
            "ACCIDENT",
            " Accident",
            R.drawable.ic_alert_accident,
            R.drawable.marker_alert_accident),

    WATER_LOGGING(
            "WATER_LOGGING",
            " Water Logging",
            R.drawable.ic_alert_water,
            R.drawable.marker_alert_water),

    HEAVY_TRAFFIC(
            "HEAVY_TRAFFIC",
            " Heavy Traffic",
            R.drawable.ic_alert_traffic,
            R.drawable.marker_alert_traffic);

    public final String wireValue;
    public final String displayLabel;
    @DrawableRes public final int iconRes;
    @DrawableRes public final int markerRes;

    AlertType(String wireValue, String displayLabel,
              @DrawableRes int iconRes, @DrawableRes int markerRes) {
        this.wireValue = wireValue;
        this.displayLabel = displayLabel;
        this.iconRes = iconRes;
        this.markerRes = markerRes;
    }

    public static AlertType fromWire(String value) {
        if (value == null) return ACCIDENT;
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (AlertType t : values()) {
            if (t.wireValue.equals(normalized)) return t;
        }
        // Tolerate alternate names from older backends.
        if (normalized.contains("FLOOD") || normalized.contains("WATER")) return WATER_LOGGING;
        if (normalized.contains("TRAFFIC") || normalized.contains("JAM"))  return HEAVY_TRAFFIC;
        return ACCIDENT;
    }
}
