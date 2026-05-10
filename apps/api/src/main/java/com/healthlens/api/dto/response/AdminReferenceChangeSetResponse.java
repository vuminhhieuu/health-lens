package com.healthlens.api.dto.response;

import java.util.UUID;

public record AdminReferenceChangeSetResponse(
        UUID changeSetId,
        String status,
        String message,
        String resultType
) {
}
