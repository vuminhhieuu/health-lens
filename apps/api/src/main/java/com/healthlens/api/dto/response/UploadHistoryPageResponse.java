package com.healthlens.api.dto.response;

import java.util.List;

public record UploadHistoryPageResponse(
        List<UploadHistoryItemResponse> items,
        int page,
        int limit,
        long total,
        int totalPages
) {
}
