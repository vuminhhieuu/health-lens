package com.healthlens.api.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OcrResult {

    /** Provider-agnostic contract fields */
    private String provider;
    private String modelVersion;
    private String mimeType;
    @Builder.Default
    private List<OcrPage> pages = new ArrayList<>();
    @Builder.Default
    private List<OcrSegment> blocks = new ArrayList<>();
    @Builder.Default
    private List<OcrSegment> lines = new ArrayList<>();
    private String text;
    private float confidence;
    @Builder.Default
    private List<OcrDiagnostic> diagnostics = new ArrayList<>();
    private String providerRequestId;
    private long latencyMs;
    private String retentionMode;
    private String language;

    /**
     * Backward-compatible alias for old API.
     */
    @JsonProperty("source")
    public String getSource() {
        return provider;
    }

    @JsonProperty("source")
    public void setSource(String source) {
        this.provider = source;
    }

    /**
     * Backward-compatible alias for old API.
     */
    @JsonProperty("processingTimeMs")
    public int getProcessingTimeMs() {
        return Math.toIntExact(Math.max(0, Math.min(Integer.MAX_VALUE, latencyMs)));
    }

    @JsonProperty("processingTimeMs")
    public void setProcessingTimeMs(int processingTimeMs) {
        this.latencyMs = Math.max(0, processingTimeMs);
    }

    /**
     * Parser helper to consume line text in display order when available.
     */
    @JsonIgnore
    public String getOrderedTextForParser() {
        if (lines == null || lines.isEmpty()) {
            return text == null ? "" : text;
        }
        return lines.stream()
                .map(OcrSegment::getText)
                .filter(segmentText -> segmentText != null && !segmentText.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse(text == null ? "" : text);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrPage {
        private Integer pageNumber;
        private String text;
        private Float confidence;
        private BoundingBox boundingBox;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrSegment {
        private Integer pageNumber;
        private String text;
        private Float confidence;
        private BoundingBox boundingBox;
        private String kind;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private Float left;
        private Float top;
        private Float width;
        private Float height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OcrDiagnostic {
        private String code;
        private String category;
        private String provider;
        private String message;
        @JsonAlias("metadata")
        private Map<String, String> context;
    }
}
