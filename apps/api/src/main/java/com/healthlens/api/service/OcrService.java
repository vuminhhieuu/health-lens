package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;



/**
 * OCR Service — Xử lý OCR với EasyOCR primary và AWS Textract fallback
 *
 * <p>Service này gọi EasyOCR microservice (FastAPI) qua REST API.
 * Khi EasyOCR fail (connection error, timeout, HTTP error), service
 * tự động fallback sang AWS Textract.
 *
 * <p>Architecture (Option B+):
 * <pre>
 *   Upload → Spring API → EasyOCR Service (primary)
 *                       ↘ AWS Textract     (fallback)
 * </pre>
 *
 * <p>Timeout: Configurable via {@code app.ocr.service.timeout-ms} (default: 10s)
 *
 * @see com.healthlens.api.config.OcrServiceConfig
 */
@Slf4j
@Service
public class OcrService {

    private final RestTemplate ocrRestTemplate;
    private final AwsTextractClient textractClient;
    private final String ocrServiceUrl;

    // =========================================
    // EasyOCR Response DTO (inner class)
    // =========================================
    /**
     * Response DTO matching the EasyOCR FastAPI /ocr response.
     */
    record EasyOcrResponse(
            String text,
            float confidence,
            String language_detected,
            int processing_time_ms,
            int block_count
    ) {}

    /**
     * Request DTO for EasyOCR FastAPI /ocr endpoint.
     */
    record EasyOcrRequest(String image_url) {}

    public OcrService(
            @Qualifier("ocrRestTemplate") RestTemplate ocrRestTemplate,
            AwsTextractClient textractClient,
            @Value("${app.ocr.service.url:http://localhost:8001}") String ocrServiceUrl) {
        this.ocrRestTemplate = ocrRestTemplate;
        this.textractClient = textractClient;
        this.ocrServiceUrl = ocrServiceUrl;
    }

    /**
     * Process image URL qua OCR pipeline.
     *
     * <p>Flow:
     * <ol>
     *   <li>Gọi EasyOCR service (primary)</li>
     *   <li>Nếu fail → log warning và fallback sang AwsTextractClient</li>
     *   <li>Nếu cả 2 fail → throw {@link OcrProcessingException}</li>
     * </ol>
     *
     * @param imageUrl URL của image cần OCR (http/https hoặc presigned S3/MinIO URL)
     * @return {@link OcrResult} chứa extracted text, confidence, source, language
     * @throws OcrProcessingException nếu tất cả OCR providers đều fail
     */
    public OcrResult processImage(String imageUrl) {
        try {
            log.info("Processing OCR for image: {}",
                    imageUrl.length() > 80 ? imageUrl.substring(0, 80) + "..." : imageUrl);
            return callEasyOcr(imageUrl);
        } catch (OcrProcessingException e) {
            log.warn("EasyOCR failed, attempting fallback: {}", e.getMessage());
            return callTextractFallback(imageUrl);
        }
    }

    /**
     * Gọi EasyOCR microservice qua REST API.
     *
     * @param imageUrl URL của image
     * @return {@link OcrResult} từ EasyOCR
     * @throws OcrProcessingException nếu EasyOCR service fail
     */
    OcrResult callEasyOcr(String imageUrl) {
        try {
            EasyOcrResponse response = ocrRestTemplate.postForObject(
                    ocrServiceUrl + "/ocr",
                    new EasyOcrRequest(imageUrl),
                    EasyOcrResponse.class
            );

            if (response == null) {
                throw new OcrProcessingException("EasyOCR returned null response");
            }

            float confidence = response.confidence();
            if (Float.isNaN(confidence) || Float.isInfinite(confidence) || confidence < 0f || confidence > 1f) {
                log.warn("EasyOCR returned invalid confidence: {}, clamping to 0.0", confidence);
                confidence = 0.0f;
            }

            int blockCount = response.block_count();
            if (blockCount < 0) {
                log.warn("EasyOCR returned negative block_count: {}, setting to 0", blockCount);
                blockCount = 0;
            }

            log.info("EasyOCR success: {} blocks, confidence: {}, time: {}ms",
                    blockCount, confidence, response.processing_time_ms());

            return OcrResult.builder()
                    .text(response.text() != null ? response.text() : "")
                    .confidence(confidence)
                    .source("easyocr")
                    .language(response.language_detected() != null ? response.language_detected() : "unknown")
                    .processingTimeMs(response.processing_time_ms())
                    .build();

        } catch (ResourceAccessException e) {
            log.error("EasyOCR service unreachable: {}", e.getMessage());
            throw new OcrProcessingException("EasyOCR service unavailable", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("EasyOCR service returned error: {} {}", e.getStatusCode(), e.getStatusText());
            throw new OcrProcessingException("EasyOCR service error: " + e.getStatusCode(), e);
        } catch (IllegalArgumentException e) {
            log.error("EasyOCR malformed response: {}", e.getMessage());
            throw new OcrProcessingException("EasyOCR malformed response", e);
        }
    }

    /**
     * AWS Textract fallback — delegates to {@link AwsTextractClient}.
     *
     * <p>Khi EasyOCR fail, method này delegate sang AwsTextractClient.
     * Nếu Textract cũng fail (stub mode hoặc actual error), catch exception
     * và trả về empty fallback result.
     *
     * @param imageUrl URL của image
     * @return {@link OcrResult} từ Textract hoặc empty fallback
     */
    OcrResult callTextractFallback(String imageUrl) {
        try {
            OcrResult result = textractClient.extract(imageUrl);
            if ("textract-stub".equals(result.getSource())) {
                log.warn("Textract fallback returned stub result — OCR will return empty text");
            }
            return result;
        } catch (Exception e) {
            log.error("Textract fallback failed: {}. All OCR providers exhausted.", e.getMessage());
            return OcrResult.builder()
                    .text("")
                    .confidence(0.0f)
                    .source("all-providers-failed")
                    .language("unknown")
                    .processingTimeMs(0)
                    .build();
        }
    }
}
