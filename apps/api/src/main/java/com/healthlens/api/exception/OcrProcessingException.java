package com.healthlens.api.exception;

/**
 * Exception thrown when OCR processing fails.
 *
 * <p>Thrown by {@link com.healthlens.api.service.OcrService} when:
 * <ul>
 *   <li>EasyOCR service is unavailable or returns an error</li>
 *   <li>AWS Textract fallback also fails</li>
 *   <li>All OCR providers have been exhausted</li>
 * </ul>
 */
public class OcrProcessingException extends RuntimeException {

    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
