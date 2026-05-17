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
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.healthlens.api.dto.MetricDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private GoogleCloudVisionClient googleCloudVisionClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    private OcrService ocrService;

    private static final String TEST_IMAGE_URL = "https://example.com/test-medical-report.jpg";
    private static final String OCR_SERVICE_URL = "http://localhost:8001";

    @BeforeEach
    void setUp() {
        ocrService = new OcrService(
                ocrRestTemplate,
                textractClient,
                googleCloudVisionClient,
                meterRegistry,
                chatClient,
                objectMapper,
                OCR_SERVICE_URL,
                "easyocr",
                "textract",
                "",
                "https://openrouter.ai/api/v1",
                "meta-llama/llama-3.3-70b-instruct",
                ""
        );
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
            verifyNoInteractions(googleCloudVisionClient);
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

    @Nested
    @DisplayName("Provider Routing theo môi trường")
    class ProviderRoutingTests {

        @Test
        @DisplayName("Staging/prod: GCV là primary, không gọi EasyOCR khi GCV success")
        void processImage_gcvPrimarySuccess_skipsEasyOcr() {
            OcrService gcvPrimaryService = new OcrService(
                    ocrRestTemplate,
                    textractClient,
                    googleCloudVisionClient,
                    meterRegistry,
                    chatClient,
                    objectMapper,
                    OCR_SERVICE_URL,
                    "gcv",
                    "textract",
                    "",
                    "https://openrouter.ai/api/v1",
                    "meta-llama/llama-3.3-70b-instruct",
                    ""
            );

            when(googleCloudVisionClient.extract(TEST_IMAGE_URL)).thenReturn(
                    OcrResult.builder()
                            .text("GCV OCR text")
                            .confidence(0.88f)
                            .provider("gcv")
                            .language("vi")
                            .latencyMs(1200)
                            .build()
            );

            OcrResult result = gcvPrimaryService.processImage(TEST_IMAGE_URL);

            assertThat(result.getSource()).isEqualTo("gcv");
            verify(googleCloudVisionClient).extract(TEST_IMAGE_URL);
            verifyNoInteractions(ocrRestTemplate);
            verifyNoInteractions(textractClient);
        }

        @Test
        @DisplayName("Staging/prod: GCV fail thì fallback Textract")
        void processImage_gcvFail_fallbacksToTextract() {
            OcrService gcvPrimaryService = new OcrService(
                    ocrRestTemplate,
                    textractClient,
                    googleCloudVisionClient,
                    meterRegistry,
                    chatClient,
                    objectMapper,
                    OCR_SERVICE_URL,
                    "gcv",
                    "textract",
                    "",
                    "https://openrouter.ai/api/v1",
                    "meta-llama/llama-3.3-70b-instruct",
                    ""
            );

            when(googleCloudVisionClient.extract(TEST_IMAGE_URL))
                    .thenThrow(new OcrProcessingException("GCV unavailable"));
            when(textractClient.extract(TEST_IMAGE_URL)).thenReturn(
                    OcrResult.builder()
                            .text("")
                            .confidence(0.0f)
                            .provider("textract-stub")
                            .language("unknown")
                            .latencyMs(0)
                            .build()
            );

            OcrResult result = gcvPrimaryService.processImage(TEST_IMAGE_URL);

            assertThat(result.getSource()).isEqualTo("textract-stub");
            verify(googleCloudVisionClient).extract(TEST_IMAGE_URL);
            verify(textractClient).extract(TEST_IMAGE_URL);
            verifyNoInteractions(ocrRestTemplate);
        }
    }

    @Nested
    @DisplayName("MIME-aware document routing")
    class MimeAwareRoutingTests {

        @Test
        @DisplayName("image/* dùng image OCR pipeline hiện có")
        void processDocument_imageMime_usesImagePipeline() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Glucose 5.4 mmol/L",
                    0.92f,
                    "vi",
                    500,
                    2
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrService.OcrProcessingResult result = ocrService.processDocument(TEST_IMAGE_URL, "image/png");

            assertThat(result.route()).isEqualTo("image");
            assertThat(result.provider()).isEqualTo("easyocr");
            assertThat(result.mimeType()).isEqualTo("image/png");
            assertThat(result.result().getText()).contains("Glucose");
            assertThat(result.pages()).isEmpty();
        }

        @Test
        @DisplayName("application/pdf có text layer dùng PDFBox trước provider fallback")
        void processDocument_pdfTextLayer_usesPdfBoxBeforeProviderFallback() throws Exception {
            when(ocrRestTemplate.getForObject("https://example.com/report.pdf", byte[].class))
                    .thenReturn(buildPdf("HbA1c 5.6", "Glucose 5.4"));

            OcrService.OcrProcessingResult result = ocrService.processDocument("https://example.com/report.pdf", "application/pdf");

            assertThat(result.route()).isEqualTo("pdf-text-layer");
            assertThat(result.provider()).isEqualTo("pdfbox");
            assertThat(result.mimeType()).isEqualTo("application/pdf");
            assertThat(result.result().getSource()).isEqualTo("pdf-text-layer");
            assertThat(result.result().getMimeType()).isEqualTo("application/pdf");
            assertThat(result.result().getPages()).hasSize(2);
            assertThat(result.result().getPages().get(0).getText()).contains("HbA1c 5.6");
            assertThat(result.result().getLines()).hasSize(2);
            assertThat(result.result().getText()).contains("HbA1c 5.6", "Glucose 5.4");
            assertThat(result.pages()).containsExactly(
                    new OcrService.OcrPageResult(1, "pdf-text-layer", 1.0f, "HbA1c 5.6"),
                    new OcrService.OcrPageResult(2, "pdf-text-layer", 1.0f, "Glucose 5.4")
            );
            verifyNoInteractions(textractClient);
        }

        @Test
        @DisplayName("PDF bytes path dùng PDFBox trực tiếp, không tải lại presigned URL")
        void processPdfBytes_pdfTextLayer_usesProvidedBytes() throws Exception {
            OcrService.OcrProcessingResult result = ocrService.processPdfBytes(
                    buildPdf("HbA1c 5.6"),
                    "https://example.com/report.pdf",
                    "application/pdf"
            );

            assertThat(result.route()).isEqualTo("pdf-text-layer");
            assertThat(result.provider()).isEqualTo("pdfbox");
            assertThat(result.result().getText()).contains("HbA1c 5.6");
            verify(ocrRestTemplate, never()).getForObject(any(String.class), eq(byte[].class));
            verifyNoInteractions(textractClient);
        }

        @Test
        @DisplayName("application/pdf scan render từng trang qua EasyOCR khi không có text layer")
        void processDocument_scannedPdf_usesRenderedPageEasyOcrBeforeTextract() throws Exception {
            when(ocrRestTemplate.getForObject("https://example.com/scanned.pdf", byte[].class))
                    .thenReturn(buildBlankPdf(1));
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(new OcrService.EasyOcrResponse(
                    "HBsAg Negative",
                    0.88f,
                    "en",
                    600,
                    2
            ));

            OcrService.OcrProcessingResult result = ocrService.processDocument("https://example.com/scanned.pdf", "application/pdf");

            assertThat(result.route()).isEqualTo("pdf-rendered-images");
            assertThat(result.provider()).isEqualTo("easyocr");
            assertThat(result.result().getMimeType()).isEqualTo("application/pdf");
            assertThat(result.result().getPages()).hasSize(1);
            assertThat(result.result().getPages().get(0).getText()).contains("HBsAg Negative");
            assertThat(result.result().getLines()).hasSize(1);
            assertThat(result.result().getText()).contains("HBsAg Negative");
            assertThat(result.pages()).containsExactly(new OcrService.OcrPageResult(1, "easyocr", 0.88f, "HBsAg Negative"));
            verifyNoInteractions(textractClient);
        }

        @Test
        @DisplayName("application/pdf scan fallback dùng document provider khi không có text layer")
        void processDocument_scannedPdf_usesDocumentProviderFallback() throws Exception {
            when(ocrRestTemplate.getForObject("https://example.com/scanned.pdf", byte[].class))
                    .thenReturn(buildBlankPdf(2));
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(new OcrService.EasyOcrResponse(
                    "",
                    0.0f,
                    "unknown",
                    100,
                    0
            ));
            when(textractClient.extract("https://example.com/scanned.pdf")).thenReturn(
                    OcrResult.builder()
                            .text("HbA1c 5.6")
                            .confidence(0.89f)
                            .provider("textract")
                            .language("vi")
                            .latencyMs(900)
                            .build()
            );

            OcrService.OcrProcessingResult result = ocrService.processDocument("https://example.com/scanned.pdf", "application/pdf");

            assertThat(result.route()).isEqualTo("pdf-document");
            assertThat(result.provider()).isEqualTo("textract");
            assertThat(result.mimeType()).isEqualTo("application/pdf");
            assertThat(result.pages()).containsExactly(
                    new OcrService.OcrPageResult(1, "textract", 0.89f),
                    new OcrService.OcrPageResult(2, "textract", 0.89f)
            );
            verify(textractClient).extract("https://example.com/scanned.pdf");
        }

        @Test
        @DisplayName("application/pdf khi Textract disabled trả provider failure rõ ràng")
        void processDocument_textractStub_returnsAllProvidersFailed() throws Exception {
            when(ocrRestTemplate.getForObject("https://example.com/scanned.pdf", byte[].class))
                    .thenReturn(buildBlankPdf(1));
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"),
                    any(),
                    eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(new OcrService.EasyOcrResponse(
                    "",
                    0.0f,
                    "unknown",
                    100,
                    0
            ));
            when(textractClient.extract("https://example.com/scanned.pdf")).thenReturn(
                    OcrResult.builder()
                            .text("")
                            .confidence(0.0f)
                            .provider("textract-stub")
                            .language("unknown")
                            .latencyMs(0)
                            .build()
            );

            OcrService.OcrProcessingResult result = ocrService.processDocument("https://example.com/scanned.pdf", "application/pdf");

            assertThat(result.route()).isEqualTo("pdf-document");
            assertThat(result.result().getSource()).isEqualTo("all-providers-failed");
            assertThat(result.result().getMimeType()).isEqualTo("application/pdf");
            assertThat(result.result().getPages()).hasSize(1);
            assertThat(result.pages()).containsExactly(new OcrService.OcrPageResult(1, "textract", 0.0f, ""));
        }

        @Test
        @DisplayName("application/pdf khi PDFBox lỗi vẫn fallback sang document provider")
        void processPdfBytes_pdfBoxFailure_stillFallsBackToDocumentProvider() {
            when(textractClient.extract("https://example.com/scanned.pdf")).thenReturn(
                    OcrResult.builder()
                            .text("fallback text")
                            .confidence(0.82f)
                            .provider("textract")
                            .language("vi")
                            .latencyMs(200)
                            .build()
            );

            OcrService.OcrProcessingResult result = ocrService.processPdfBytes(
                    "not-a-valid-pdf".getBytes(StandardCharsets.UTF_8),
                    "https://example.com/scanned.pdf",
                    "application/pdf"
            );

            assertThat(result.route()).isEqualTo("pdf-document");
            assertThat(result.provider()).isEqualTo("textract");
            assertThat(result.result().getText()).isEqualTo("fallback text");
            verify(textractClient).extract("https://example.com/scanned.pdf");
        }

        @Test
        @DisplayName("unsupported MIME bị reject rõ ràng")
        void processDocument_unsupportedMime_throws() {
            assertThatThrownBy(() -> ocrService.processDocument(TEST_IMAGE_URL, "text/plain"))
                    .isInstanceOf(OcrProcessingException.class)
                    .hasMessageContaining("Unsupported OCR MIME type");
        }

        private byte[] buildPdf(String... pageTexts) throws IOException {
            try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (String pageText : pageTexts) {
                    PDPage page = new PDPage();
                    document.addPage(page);
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(72, 720);
                        content.showText(pageText);
                        content.endText();
                    }
                }
                document.save(out);
                return out.toByteArray();
            }
        }

        private byte[] buildBlankPdf(int pages) throws IOException {
            try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (int i = 0; i < pages; i++) {
                    document.addPage(new PDPage());
                }
                document.save(out);
                return out.toByteArray();
            }
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
                            .provider("textract-stub")
                            .language("unknown")
                            .latencyMs(0)
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
                    .provider("textract-stub")
                    .language("unknown")
                    .latencyMs(0)
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

    // =========================================================
    // parseMetrics() — Extraction and Classification Tests
    // =========================================================
    @Nested
    @DisplayName("parseMetrics() — LLM Extraction Tests")
    class ParseMetricsTests {

        @Test
        @DisplayName("Phân loại confidence >= 0.85 là high")
        void parseMetrics_confidenceHigh() throws Exception {
            // Act
            String level = ocrService.classifyConfidence(0.85f);
            assertThat(level).isEqualTo("high");
        }

        @Test
        @DisplayName("Phân loại confidence 0.50-0.84 là medium")
        void parseMetrics_confidenceMedium() throws Exception {
            // Act
            String level = ocrService.classifyConfidence(0.50f);
            assertThat(level).isEqualTo("medium");
        }

        @Test
        @DisplayName("Phan loai confidence 0.84 la medium (partial)")
        void parseMetrics_confidenceUpperMediumBoundary() {
            String level = ocrService.classifyConfidence(0.84f);
            assertThat(level).isEqualTo("medium");
        }

        @Test
        @DisplayName("Phân loại confidence < 0.50 là low")
        void parseMetrics_confidenceLow() throws Exception {
            // Act
            String level = ocrService.classifyConfidence(0.49f);
            assertThat(level).isEqualTo("low");
        }
    }

    @Nested
    @DisplayName("Normalized OCR Contract")
    class NormalizedContractTests {

        @Test
        @DisplayName("EasyOCR mapping populates normalized contract defaults")
        void callEasyOcr_mapsIntoNormalizedContract() {
            OcrService.EasyOcrResponse mockResponse = new OcrService.EasyOcrResponse(
                    "Page line", 0.9f, "vi", 1000, 1
            );
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenReturn(mockResponse);

            OcrResult result = ocrService.callEasyOcr(TEST_IMAGE_URL);

            assertThat(result.getProvider()).isEqualTo("easyocr");
            assertThat(result.getModelVersion()).isEqualTo("easyocr-default");
            assertThat(result.getRetentionMode()).isEqualTo("transient");
            assertThat(result.getPages()).hasSize(1);
            assertThat(result.getLines()).hasSize(1);
            assertThat(result.getSource()).isEqualTo("easyocr");
            assertThat(result.getProcessingTimeMs()).isEqualTo(1000);
        }

        @Test
        @DisplayName("provider failure diagnostics are redacted before persistence")
        void processImage_providerFailureDiagnostics_areRedacted() {
            when(ocrRestTemplate.postForObject(
                    eq(OCR_SERVICE_URL + "/ocr"), any(), eq(OcrService.EasyOcrResponse.class)
            )).thenThrow(new ResourceAccessException("Authorization=Bearer abc123 https://secret.example.com/file"));
            when(textractClient.extract(TEST_IMAGE_URL)).thenReturn(
                    OcrResult.builder().provider("textract-stub").text("").confidence(0f).language("unknown").latencyMs(0).build()
            );

            OcrResult result = ocrService.processImage(TEST_IMAGE_URL);

            assertThat(result.getDiagnostics()).isNotEmpty();
            String diagnosticMessage = result.getDiagnostics().stream()
                    .filter(d -> "OCR_PROVIDER_FAILED".equals(d.getCode()))
                    .findFirst()
                    .map(OcrResult.OcrDiagnostic::getMessage)
                    .orElse("");
            assertThat(diagnosticMessage).isNotBlank();
            assertThat(diagnosticMessage).doesNotContain("abc123");
            assertThat(diagnosticMessage).doesNotContain("https://secret.example.com/file");
        }
    }
}
