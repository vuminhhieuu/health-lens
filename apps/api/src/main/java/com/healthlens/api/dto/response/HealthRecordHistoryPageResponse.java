package com.healthlens.api.dto.response;

import java.util.List;

public record HealthRecordHistoryPageResponse(
        List<HealthRecordHistoryItemResponse> data,
        PaginationResponse pagination
) {
}
