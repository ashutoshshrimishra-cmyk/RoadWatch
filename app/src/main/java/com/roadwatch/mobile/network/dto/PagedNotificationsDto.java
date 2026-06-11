package com.roadwatch.mobile.network.dto;

import java.util.List;

/**
 * Spring Page wrapper for GET /api/citizen/me/notifications.
 */
public class PagedNotificationsDto {
    public List<NotificationDto> content;
    public int totalPages;
    public long totalElements;
    public boolean last;
}
