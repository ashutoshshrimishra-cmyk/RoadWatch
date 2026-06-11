package com.roadwatch.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Full complaint DTO €” mirrors the backend Complaint entity.
 *
 * Fields: id, description, imageUrl, location, timestamp,
 *         severity, status, roadType, department,
 *         aiLabel, aiConfidence, aiDetectionsJson, adminNotes.
 */
public class ComplaintDto {

    public Long   id;
    public String description;

    @SerializedName("imageUrl")
    public String imageUrl;

    public GeoPointDto location;

    /** ISO-8601 string: "yyyy-MM-dd'T'HH:mm:ss" */
    public String timestamp;

    /** HIGH | MEDIUM | LOW */
    public String severity;

    /** PENDING | ASSIGNED | IN_PROGRESS | RESOLVED */
    public String status;

    @SerializedName("roadType")
    public String roadType;

    public String department;

    // ”€”€ AI / Admin fields populated by the backend after YOLO + admin review ”€

    /** YOLO top-level label, e.g. "pothole" or "broken_divider". */
    @SerializedName("aiLabel")
    public String aiLabel;

    /** YOLO confidence score in 0..1. */
    @SerializedName("aiConfidence")
    public Double aiConfidence;

    /** Raw JSON of all AI detections (bounding boxes etc.) €” keep as String. */
    @SerializedName("aiDetectionsJson")
    public String aiDetectionsJson;

    /** Notes added by the admin moderator on the website. */
    @SerializedName("adminNotes")
    public String adminNotes;

    // ”€”€ Department workflow fields ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    /** Department's response (e.g. "Team dispatched to site") */
    @SerializedName("deptResponse")
    public String deptResponse;

    /** Officer/team assigned by the department */
    @SerializedName("assignedOfficer")
    public String assignedOfficer;

    /** Expected completion date (ISO string) */
    @SerializedName("expectedCompletion")
    public String expectedCompletion;

    /** URL of the repair proof image (before/after) */
    @SerializedName("resolutionImageUrl")
    public String resolutionImageUrl;

    /** When complaint was resolved (ISO string) */
    @SerializedName("resolvedAt")
    public String resolvedAt;

    /** Citizen's satisfaction rating (1-5) */
    @SerializedName("citizenRating")
    public Integer citizenRating;

    // ”€”€ Helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    /**
     * Maps status string to a 0-based step index for the timeline.
     *   0 = PENDING   (Reported)
     *   1 = ASSIGNED  (Verification)
     *   2 = IN_PROGRESS (Action)
     *   3 = RESOLVED  (Resolved)
     */
    public int getStatusStep() {
        if (status == null) return 0;
        switch (status.toUpperCase()) {
            case "ACCEPTED":    return 1;
            case "FORWARDED":   return 2;
            case "ASSIGNED":    return 2;
            case "IN_PROGRESS": return 3;
            case "RESOLVED":    return 4;
            case "REJECTED":    return -1;
            default:            return 0;
        }
    }

    /** Human-readable status label. */
    public String getStatusLabel() {
        if (status == null) return "Reported";
        switch (status.toUpperCase()) {
            case "ACCEPTED":    return "Accepted";
            case "FORWARDED":   return "Forwarded to Dept";
            case "ASSIGNED":    return "Dept Assigned";
            case "IN_PROGRESS": return "In Progress";
            case "RESOLVED":    return "Resolved";
            default:            return "Reported";
        }
    }
}



