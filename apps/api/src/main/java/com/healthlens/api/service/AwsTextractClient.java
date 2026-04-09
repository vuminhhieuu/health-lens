package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AWS Textract Client — Fallback OCR provider
 *
 * <p>Stub implementation cho MVP. Khi EasyOCR microservice fail,
 * {@link OcrService} sẽ delegate sang client này.
 *
 * <p>TODO: Implement actual AWS Textract integration khi có AWS credentials.
 * Production implementation cần:
 * <ul>
 *   <li>AWS SDK dependency: {@code software.amazon.awssdk:textract}</li>
 *   <li>S3 integration để upload image trước khi gọi Textract</li>
 *   <li>AWS credentials (IAM role hoặc access key)</li>
 * </ul>
 *
 * <p>Architecture (Option B+):
 * <pre>
 *   OcrService → callEasyOcr() [primary]
 *             ↘ AwsTextractClient.extract() [fallback]
 * </pre>
 *
 * @see OcrService#callTextractFallback(String)
 */
@Slf4j
@Component
public class AwsTextractClient {

    @Value("${app.ocr.textract.enabled:false}")
    private boolean textractEnabled;

    @Value("${app.ocr.textract.region:ap-southeast-1}")
    private String awsRegion;

    /**
     * Extract text từ image URL sử dụng AWS Textract.
     *
     * <p>Hiện tại là stub implementation — trả về empty result.
     * Khi {@code app.ocr.textract.enabled=true} và có AWS credentials,
     * sẽ gọi Textract API thực.
     *
     * @param imageUrl URL hoặc S3 key của image
     * @return {@link OcrResult} với source="textract" hoặc "textract-stub"
     * @throws OcrProcessingException nếu Textract cũng fail
     */
    public OcrResult extract(String imageUrl) {
        if (!textractEnabled) {
            log.warn("AWS Textract is disabled (stub mode). Set app.ocr.textract.enabled=true " +
                    "and configure AWS credentials to enable.");
            return OcrResult.builder()
                    .text("")
                    .confidence(0.0f)
                    .source("textract-stub")
                    .language("unknown")
                    .processingTimeMs(0)
                    .build();
        }

        // TODO: Implement actual AWS Textract call
        // Pseudo-code:
        // 1. Download image from imageUrl (or use S3 key directly)
        // 2. Create DetectDocumentTextRequest
        // 3. Call textract.detectDocumentText(request)
        // 4. Parse Block[] results, extract LINE blocks
        // 5. Build OcrResult with aggregated text and confidence
        log.info("AWS Textract extraction for: {}", 
                imageUrl.length() > 80 ? imageUrl.substring(0, 80) + "..." : imageUrl);

        throw new OcrProcessingException(
                "AWS Textract not yet implemented. Configure AWS SDK and credentials.");
    }
}
