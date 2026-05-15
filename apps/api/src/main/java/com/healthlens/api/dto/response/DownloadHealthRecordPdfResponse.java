package com.healthlens.api.dto.response;

public record DownloadHealthRecordPdfResponse(
        byte[] bytes,
        String filename
) {
}
