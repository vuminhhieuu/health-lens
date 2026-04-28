package com.healthlens.api.dto.response;

public record PaginationResponse(
        int page,
        int limit,
        long total,
        int totalPages
) {
}
