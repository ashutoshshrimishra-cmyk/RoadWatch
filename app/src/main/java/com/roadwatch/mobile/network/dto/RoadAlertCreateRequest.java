package com.roadwatch.mobile.network.dto;

/** Body of POST /api/alerts. The "Quick Report" FAB sends this. */
public class RoadAlertCreateRequest {
    public String type;          // AlertType.wireValue
    public String description;   // optional one-liner
    public Double latitude;
    public Double longitude;

    public RoadAlertCreateRequest(AlertType type, String description,
                                  double latitude, double longitude) {
        this.type = type.wireValue;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
