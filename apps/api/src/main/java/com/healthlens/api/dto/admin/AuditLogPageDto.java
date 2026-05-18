package com.healthlens.api.dto.admin;

import java.util.List;

public record AuditLogPageDto(
        List<AuditLogEntryDto> content,
        long totalElements,
        int page,
        int limit
) {}
