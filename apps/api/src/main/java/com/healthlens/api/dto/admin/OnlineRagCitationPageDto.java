package com.healthlens.api.dto.admin;

import java.util.List;

public record OnlineRagCitationPageDto(
        List<OnlineRagCitationEntryDto> content,
        long totalElements,
        int page,
        int limit
) {
}
