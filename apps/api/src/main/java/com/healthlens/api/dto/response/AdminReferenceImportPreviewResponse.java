package com.healthlens.api.dto.response;

import java.util.List;
import java.util.UUID;

public record AdminReferenceImportPreviewResponse(
        UUID importId,
        List<AdminReferenceImportPreviewRowResponse> validRows,
        List<AdminReferenceImportErrorRowResponse> errorRows
) {
}
