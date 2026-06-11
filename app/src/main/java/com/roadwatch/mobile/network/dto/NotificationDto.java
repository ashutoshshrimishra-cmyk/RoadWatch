package com.roadwatch.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Mirrors the backend Notification entity returned by
 * GET /api/citizen/me/notifications.
 */
public class NotificationDto {

    public Long id;

    public String title;

    public String body;

    /** AI_ANALYSIS | ASSIGNED | IN_PROGRESS | RESOLVED | REJECTED | SYSTEM */
    public String category;

    @SerializedName("read")
    public boolean read;

    @SerializedName("fcmSent")
    public boolean fcmSent;

    @SerializedName("complaintId")
    public Long complaintId;

    /** ISO-8601 string: "yyyy-MM-dd'T'HH:mm:ss" */
    @SerializedName("createdAt")
    public String createdAt;
}
