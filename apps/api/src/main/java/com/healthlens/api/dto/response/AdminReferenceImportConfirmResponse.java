package com.healthlens.api.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminReferenceImportConfirmResponse(
        UUID importId,
        int draftChangeSetCount,
        List<UUID> changeSetIds,
        String message
) {
}
