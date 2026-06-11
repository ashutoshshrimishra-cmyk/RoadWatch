package com.roadwatch.mobile.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Inbox row for a push notification we received via FCM (or generated locally).
 *
 *  - {@code complaintId} is null for non-complaint alerts (e.g., budget updates).
 *  - {@code channel} mirrors {@link com.roadwatch.mobile.notifications.NotificationChannels}.
 *  - {@code read} drives the unread-dot badge on the dashboard bell.
 */
@Entity(tableName = "notifications")
public class NotificationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String body;
    public String channel;
    public Long complaintId;
    public long receivedAt;
    public boolean read;

    /** Server-side notification ID for dedup during sync. NULL for FCM-pushed notifications. */
    public Long serverId;
}
