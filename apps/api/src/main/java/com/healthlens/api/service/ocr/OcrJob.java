package com.healthlens.api.service.ocr;

public record OcrJob(
        String documentUrl,
        String mimeType,
        String imageBase64,
        byte[] documentBytes
) {
    public static OcrJob fromUrl(String documentUrl, String mimeType) {
        return new OcrJob(documentUrl, mimeType, null, null);
    }

    public static OcrJob fromBase64(String imageBase64, String mimeType) {
        return new OcrJob(null, mimeType, imageBase64, null);
    }

    public static OcrJob fromDocumentBytes(byte[] documentBytes, String documentUrl, String mimeType) {
        return new OcrJob(documentUrl, mimeType, null, documentBytes);
    }

    public boolean hasBase64Image() {
        return imageBase64 != null && !imageBase64.isBlank();
    }

    public boolean hasDocumentBytes() {
        return documentBytes != null && documentBytes.length > 0;
    }
}
