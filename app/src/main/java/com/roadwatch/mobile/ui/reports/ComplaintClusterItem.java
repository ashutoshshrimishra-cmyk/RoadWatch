package com.roadwatch.mobile.ui.reports;

import org.osmdroid.util.GeoPoint;
import com.roadwatch.mobile.network.dto.ReportDto;

public class ComplaintClusterItem {

    public static final int BUCKET_PENDING     = 0;
    public static final int BUCKET_IN_PROGRESS = 1;
    public static final int BUCKET_RESOLVED    = 2;

    private final ReportDto report;
    private final GeoPoint  position;
    private final int       bucket;

    public ComplaintClusterItem(ReportDto report) {
        this.report   = report;
        this.position = new GeoPoint(report.location.latitude, report.location.longitude);
        this.bucket   = computeBucket(report.status);
    }

    public ReportDto getReport()   { return report;   }
    public GeoPoint  getPosition() { return position; }
    public int       getBucket()   { return bucket;   }

    public String getTitle() {
        return report.roadType != null ? report.roadType : "Report";
    }

    public String getSnippet() {
        return report.status != null ? report.status : "";
    }

    public float getZIndex() {
        return (float) (3 - bucket);
    }

    private static int computeBucket(String status) {
        if (status == null) return BUCKET_PENDING;
        switch (status.toUpperCase()) {
            case "RESOLVED": case "FIXED": case "CLOSED":
                return BUCKET_RESOLVED;
            case "IN_PROGRESS": case "ASSIGNED": case "UNDER_REPAIR":
                return BUCKET_IN_PROGRESS;
            default:
                return BUCKET_PENDING;
        }
    }
}