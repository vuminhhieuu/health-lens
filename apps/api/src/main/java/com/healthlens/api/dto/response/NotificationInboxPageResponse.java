package com.healthlens.api.dto.response;

import java.util.List;

public record NotificationInboxPageResponse(
        List<NotificationInboxItemResponse> data,
        PaginationResponse pagination,
        long unreadCount
) {
}
