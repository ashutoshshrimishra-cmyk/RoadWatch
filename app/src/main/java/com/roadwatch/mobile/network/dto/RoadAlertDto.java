package com.roadwatch.mobile.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Server-side view of a temporary road hazard.
 *
 * Lifecycle: an alert auto-expires after 4 hours from {@code createdAt}
 * unless other users keep upvoting it. {@link #isExpired(long)} captures
 * the "still active?" rule so the feed and map agree on what to show.
 */
public class RoadAlertDto {

    public static final long DEFAULT_TTL_MS = 4L * 60L * 60L * 1000L; // 4 hours
    public static final long UPVOTE_EXTENSION_MS = 30L * 60L * 1000L; // each upvote keeps it alive +30 min

    public Long id;

    /** "ACCIDENT" | "WATER_LOGGING" | "HEAVY_TRAFFIC" */
    public String type;

    public String description;

    /** Optional human-readable location label (e.g., "NH-24, Ghaziabad"). */
    public String locationLabel;

    public Double latitude;
    public Double longitude;

    /** Epoch millis when the alert was first reported. */
    public Long createdAt;

    /** Epoch millis of the most recent upvote €” used to extend validity. */
    @SerializedName(value = "lastUpvoteAt", alternate = {"updatedAt", "lastConfirmedAt"})
    public Long lastUpvoteAt;

    /** Total upvotes / "still here" confirmations. */
    public Integer upvotes;

    /** Whether the current user has already upvoted this alert. */
    public Boolean upvotedByMe;

    /** Reporter display name, optional. */
    public String reporterName;

    // ”€”€ Helpers ”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€”€

    public AlertType resolveType() {
        return AlertType.fromWire(type);
    }

    public long resolveCreatedAt() {
        return createdAt != null ? createdAt : System.currentTimeMillis();
    }

    public long resolveLastUpvoteAt() {
        return lastUpvoteAt != null ? lastUpvoteAt : resolveCreatedAt();
    }

    public int resolveUpvotes() {
        return upvotes != null ? upvotes : 0;
    }

    public boolean resolveUpvotedByMe() {
        return upvotedByMe != null && upvotedByMe;
    }

    /**
     * An alert is considered active for 4 hours after creation, plus
     * {@code 30 minutes — upvoteCount} of community-driven extension.
     */
    public boolean isExpired(long now) {
        long aliveUntil = resolveCreatedAt()
                + DEFAULT_TTL_MS
                + UPVOTE_EXTENSION_MS * Math.min(resolveUpvotes(), 16); // cap at +8h extension
        // Recent upvotes also keep things alive on their own.
        long upvoteAliveUntil = resolveLastUpvoteAt() + DEFAULT_TTL_MS;
        return now > Math.max(aliveUntil, upvoteAliveUntil);
    }
}
