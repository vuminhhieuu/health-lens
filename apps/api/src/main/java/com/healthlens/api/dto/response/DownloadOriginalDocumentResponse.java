package com.healthlens.api.dto.response;

public record DownloadOriginalDocumentResponse(
        byte[] bytes,
        String contentType,
        String filename
) {
}
