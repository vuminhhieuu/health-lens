package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho OcrService
 *
 * <p>Sử dụng Mockito để mock RestTemplate và AwsTextractClient,
 * tránh phụ thuộc vào EasyOCR microservice và AWS thật.
 *
 * <p>Test covers:
 * <ul>
 *   <li>Primary OCR (EasyOCR) thành công</li>
 *   <li>Fallback khi EasyOCR fail (connection error, timeout)</li>
 *   <li>Fallback khi EasyOCR trả về HTTP error</li>
 *   <li>Textract fallback behavior (stub mode)</li>
 *   <li>Null response handling</li>
 *   <li>Last-resort empty fallback khi cả Textract fail</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock
    private RestTemplate ocrRestTemplate;

    @Mock
    private AwsTextractClient textractClient;

    private OcrService ocrService;

    private static final String TEST_IMAGE_URL = "https://example.com/test-medical-report.jpg";
    private static final String OCR_SERVICE_URL = "http://localhost:8001";

    @BeforeEach
    void setUp() {
        ocrService = new OcrService(ocrRestTemplate, textractClient, OCR_SERVICE_URL);
    }

    // =========================================================
    // processImage() — Primary EasyOCR Success Cases
    // =========================================================
    @Nested
    @DisplayName("EasyOCR Primary — Success Cases")
    class EasyOcrSuccessTests {

        @Test
        @DisplayName("EasyOCR trả về kết quả Vietnamese text thành công")
        void processImage_easyOcrSuccess_returnsVietnameseResult() {
            // Arrange
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Kết quả xét nghiệm máu: Glucose 5.4 mmol/L",
                    0.92f,
                    "vi",
                    3500,
                    5
            );

            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getText()).contains("Glucose");
            assertThat(result.getConfidence()).isEqualTo(0.92f);
            assertThat(result.getSource()).isEqualTo("easyocr");
            assertThat(result.getLanguage()).isEqualTo("vi");
            assertThat(result.getProcessingTimeMs()).isEqualTo(3500);

            verify(ocrRestTemplate).postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            );
            // Should NOT call Textract when EasyOCR succeeds
            verifyNoInteractions(textractClient);
        }

        @Test
        @DisplayName("EasyOCR trả về kết quả English text thành công")
        void processImage_easyOcrSuccess_returnsEnglishResult() {
            // Arrange
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Blood Test Results: Glucose 5.4 mmol/L",
                    0.95f,
                    "en",
                    2800,
                    3
            );

            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("easyocr");
            assertThat(result.getLanguage()).isEqualTo("en");
            assertThat(result.getConfidence()).isGreaterThan(0.9f);
        }

        @Test
        @DisplayName("EasyOCR trả về empty text (no text in image)")
        void processImage_easyOcrEmptyResult_returnsEmptyText() {
            // Arrange
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "",
                    0.0f,
                    "unknown",
                    1500,
                    0
            );

            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getText()).isEmpty();
            assertThat(result.getSource()).isEqualTo("easyocr");
        }
    }

    // =========================================================
    // processImage() — Fallback Cases
    // =========================================================
    @Nested
    @DisplayName("Fallback — EasyOCR Failure Cases")
    class FallbackTests {

        /**
         * Cấu hình mock AwsTextractClient trả về stub result (default behavior).
         */
        private void mockTextractStub() {
            when(textractClient.extract(any())).thenReturn(
                    OcrResult.builder()
                            .text("")
                            .confidence(0.0f)
                            .source("textract-stub")
                            .language("unknown")
                            .processingTimeMs(0)
                            .build()
            );
        }

        @Test
        @DisplayName("EasyOCR connection refused → fallback sang Textract")
        void processImage_easyOcrConnectionRefused_fallsBackToTextract() {
            // Arrange: EasyOCR unreachable
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));
            mockTextractStub();

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert: Should get Textract stub result
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("textract-stub");
            assertThat(result.getText()).isEmpty();
            verify(textractClient).extract(TEST_IMAGE_URL);
        }

        @Test
        @DisplayName("EasyOCR timeout → fallback sang Textract")
        void processImage_easyOcrTimeout_fallsBackToTextract() {
            // Arrange: EasyOCR timeout (10s)
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenThrow(new ResourceAccessException("Read timed out"));
            mockTextractStub();

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("textract-stub");
            verify(textractClient).extract(TEST_IMAGE_URL);
        }

        @Test
        @DisplayName("EasyOCR trả về HTTP 500 → fallback sang Textract")
        void processImage_easyOcrServerError_fallsBackToTextract() {
            // Arrange: EasyOCR 500 error
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenThrow(HttpServerErrorException.create(
                    org.springframework.http.HttpStatusCode.valueOf(500),
                    "Internal Server Error",
                    org.springframework.http.HttpHeaders.EMPTY,
                    new byte[0],
                    null
            ));
            mockTextractStub();

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("textract-stub");
        }

        @Test
        @DisplayName("EasyOCR trả về null → fallback sang Textract")
        void processImage_easyOcrNullResponse_fallsBackToTextract() {
            // Arrange
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(null);
            mockTextractStub();

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("textract-stub");
        }

        @Test
        @DisplayName("EasyOCR fail + Textract fail → trả về fallback-empty")
        void processImage_bothFail_returnsEmptyFallback() {
            // Arrange: EasyOCR fails
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenThrow(new ResourceAccessException("Connection refused"));
            // Textract also fails
            when(textractClient.extract(any())).thenThrow(
                    new OcrProcessingException("Textract not configured")
            );

            // Act
            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            // Assert: Last-resort empty result
            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("all-providers-failed");
            assertThat(result.getText()).isEmpty();
        }
    }

    // =========================================================
    // callEasyOcr() — Direct Tests
    // =========================================================
    @Nested
    @DisplayName("callEasyOcr() — Direct Method Tests")
    class CallEasyOcrDirectTests {

        @Test
        @DisplayName("callEasyOcr gửi đúng URL endpoint")
        void callEasyOcr_sendsCorrectEndpoint() {
            // Arrange
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test text", 0.9f, "en", 1000, 1
            );

            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            // Act
            ocrService.callEasyOcr(TEST_IMAGE_URL);

            // Assert
            verify(ocrRestTemplate).postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(OcrService.EasyOcrRequest.class),
                    eq(OcrService.EasyOcrResponse.class)
            );
        }
    }

    // =========================================================
    // callTextractFallback() — Direct Tests
    // =========================================================
    @Nested
    @DisplayName("callTextractFallback() — Delegation Tests")
    class TextractFallbackTests {

        @Test
        @DisplayName("Textract stub trả về result từ AwsTextractClient")
        void callTextractFallback_delegatesToTextractClient() {
            // Arrange
            OcrResult textractResult = OcrResult.builder()
                    .text("")
                    .confidence(0.0f)
                    .source("textract-stub")
                    .language("unknown")
                    .processingTimeMs(0)
                    .build();
            when(textractClient.extract(TEST_IMAGE_URL)).thenReturn(textractResult);

            // Act
            OcrResult result = ocrService.callTextractFallback(TEST_IMAGE_URL);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getText()).isEmpty();
            assertThat(result.getConfidence()).isEqualTo(0.0f);
            assertThat(result.getSource()).isEqualTo("textract-stub");
            verify(textractClient).extract(TEST_IMAGE_URL);
        }

        @Test
        @DisplayName("Textract exception → trả về all-providers-failed")
        void callTextractFallback_onException_returnsAllProvidersFailed() {
            when(textractClient.extract(any())).thenThrow(
                    new OcrProcessingException("AWS not configured")
            );

            OcrResult result = ocrService.callTextractFallback(TEST_IMAGE_URL);

            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("all-providers-failed");
            assertThat(result.getText()).isEmpty();
        }
    }

    // =========================================================
    // Edge Case Tests — Validation & Boundary Values
    // =========================================================
    @Nested
    @DisplayName("Edge Cases — Response Validation")
    class ResponseValidationTests {

        @Test
        @DisplayName("NaN confidence → clamped to 0.0")
        void callEasyOcr_NaNConfidence_returnsClampedConfidence() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", Float.NaN, "en", 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getConfidence()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Infinity confidence → clamped to 0.0")
        void callEasyOcr_InfinityConfidence_returnsClampedConfidence() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", Float.POSITIVE_INFINITY, "en", 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getConfidence()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Negative confidence → clamped to 0.0")
        void callEasyOcr_negativeConfidence_returnsClampedConfidence() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", -0.5f, "en", 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getConfidence()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Confidence > 1.0 → clamped to 0.0")
        void callEasyOcr_confidenceAboveOne_returnsClampedConfidence() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", 1.5f, "en", 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getConfidence()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Negative block_count → set to 0")
        void callEasyOcr_negativeBlockCount_handledGracefully() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", 0.9f, "en", 1000, -5
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getText()).isEqualTo("Test");
        }

        @Test
        @DisplayName("Null text → returns empty string")
        void callEasyOcr_nullText_handledGracefully() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    null, 0.9f, "en", 1000, 0
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getText()).isEmpty();
        }

        @Test
        @DisplayName("Null language → returns unknown")
        void callEasyOcr_nullLanguage_handledGracefully() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Test", 0.9f, null, 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getLanguage()).isEqualTo("unknown");
        }
    }
}
