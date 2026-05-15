package com.healthlens.api.dto.response;

public record AdminReferenceImportErrorRowResponse(
        int line,
        String error
) {
}
