package com.healthlens.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho OcrResult — Kết quả OCR từ bất kỳ provider nào.
 *
 * <p>Đây là internal DTO cho Spring Boot API, chứa kết quả OCR
 * đã chuẩn hóa từ EasyOCR hoặc AWS Textract fallback.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {

    /** Extracted text content */
    private String text;

    /** Confidence score (0.0 — 1.0) */
    private float confidence;

    /** OCR provider source: "easyocr" or "textract" */
    private String source;

    /** Detected primary language: "vi" or "en" */
    private String language;

    /** Processing time in milliseconds */
    private int processingTimeMs;
}
