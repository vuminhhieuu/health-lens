package com.healthlens.api.controller;

import com.healthlens.api.annotation.RequiresConsent;
import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.dto.request.OcrExtractRequest;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.OcrService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * OCR Controller — REST endpoint cho OCR processing
 *
 * <p>Cung cấp endpoint để test OCR pipeline end-to-end.
 * Trong Epic 3, endpoint này sẽ được tích hợp với upload flow.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/ocr/extract} — Extract text từ image URL</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * Extract text từ image URL qua OCR pipeline.
     *
     * <p>Gọi EasyOCR service (primary), fallback theo provider đã cấu hình khi fail.
     * 
     * <p>Requires user consent before processing health data.
     *
     * @param request body chứa imageUrl
     * @return {@link OcrResult} với extracted text, confidence, source
     */
    @RequiresConsent
    @PostMapping("/extract")
    public ResponseEntity<OcrResult> extractText(@Valid @RequestBody OcrExtractRequest request) {
        String imageUrl = request.imageUrl();

        if (!isAllowedScheme(imageUrl)) {
            log.warn("OCR request rejected: invalid URL scheme — {}", imageUrl);
            return ResponseEntity.badRequest().build();
        }

        log.info("OCR extract request for: {}",
                imageUrl.length() > 80 ? imageUrl.substring(0, 80) + "..." : imageUrl);

        try {
            OcrResult result = ocrService.processImage(imageUrl);
            return ResponseEntity.ok(result);
        } catch (OcrProcessingException e) {
            log.error("OCR processing failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(OcrResult.builder()
                            .provider("error")
                            .modelVersion("n/a")
                            .mimeType("image/*")
                            .retentionMode("transient")
                            .latencyMs(0)
                            .text("")
                            .confidence(0.0f)
                            .language("unknown")
                            .pages(java.util.List.of())
                            .blocks(java.util.List.of())
                            .lines(java.util.List.of())
                            .diagnostics(java.util.List.of(OcrResult.OcrDiagnostic.builder()
                                    .provider("error")
                                    .category("provider_failure")
                                    .code("OCR_UNHANDLED_EXCEPTION")
                                    .message("OCR processing failed")
                                    .build()))
                            .build());
        }
    }

    // Pattern để detect private/internal IP ranges (SSRF prevention)
    private static final Pattern PRIVATE_HOST_PATTERN = Pattern.compile(
            "^(localhost|127\\..*|10\\..*|172\\.(1[6-9]|2[0-9]|3[01])\\..*|192\\.168\\..*|0\\.0\\.0\\.0|\\[::1\\])$",
            Pattern.CASE_INSENSITIVE
    );

    private boolean isAllowedScheme(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        // Block SSRF: private IPs, localhost
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && PRIVATE_HOST_PATTERN.matcher(host).matches()) {
                log.warn("SSRF blocked: private/internal host — {}", host);
                return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
