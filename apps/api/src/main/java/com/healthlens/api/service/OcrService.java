package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



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
    private final GoogleCloudVisionClient googleCloudVisionClient;
    private final MeterRegistry meterRegistry;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String ocrServiceUrl;
    private final String primaryProvider;
    private final String fallbackProviders;
    private final String openRouterApiKey;
    private final String openRouterBaseUrl;
    private final String openRouterModel;
    private final String openRouterReferer;
    @Value("${app.ocr.confidence.high-threshold:0.85}")
    private float highConfidenceThreshold = 0.85f;
    @Value("${app.ocr.confidence.medium-threshold:0.50}")
    private float mediumConfidenceThreshold = 0.50f;
    @Value("${app.ocr.pdf.rendered-images.max-pages:10}")
    private int pdfRenderedImagesMaxPages = 10;
    @Value("${app.ocr.pdf.rendered-images.dpi:150}")
    private int pdfRenderedImagesDpi = 150;
    @Value("${app.ocr.pdf.rendered-images.max-total-bytes:31457280}")
    private int pdfRenderedImagesMaxTotalBytes = 30 * 1024 * 1024;
    @Value("${app.ocr.pdf.rendered-images.min-text-chars-before-stop:120}")
    private int pdfRenderedImagesMinTextCharsBeforeStop = 120;

    // =========================================
    // EasyOCR Response DTO (inner class)
    // =========================================
    public record OcrExtractionResult(
            String examDate,
            String recordType,
            String hospitalName,
            String diagnosis,
            List<MetricDto> metrics
    ) {}

    public record OcrPageResult(
            int pageNumber,
            String provider,
            float confidence
    ) {}

    public record OcrProcessingResult(
            OcrResult result,
            String route,
            String provider,
            String mimeType,
            List<OcrPageResult> pages
    ) {}

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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EasyOcrRequest(
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("image_base64") String imageBase64
    ) {
        static EasyOcrRequest fromUrl(String imageUrl) {
            return new EasyOcrRequest(imageUrl, null);
        }

        static EasyOcrRequest fromBase64(String imageBase64) {
            return new EasyOcrRequest(null, imageBase64);
        }
    }

    public OcrService(
            @Qualifier("ocrRestTemplate") RestTemplate ocrRestTemplate,
            AwsTextractClient textractClient,
            GoogleCloudVisionClient googleCloudVisionClient,
            MeterRegistry meterRegistry,
            ChatClient chatClient,
            ObjectMapper objectMapper,
            @Value("${app.ocr.service.url:http://localhost:8001}") String ocrServiceUrl,
            @Value("${app.ocr.providers.primary:easyocr}") String primaryProvider,
            @Value("${app.ocr.providers.fallback-order:textract}") String fallbackProviders,
            @Value("${OPENROUTER_API_KEY:}") String openRouterApiKey,
            @Value("${OPENROUTER_BASE_URL:https://openrouter.ai/api/v1}") String openRouterBaseUrl,
            @Value("${OPENROUTER_MODEL:meta-llama/llama-3.3-70b-instruct}") String openRouterModel,
            @Value("${OPENROUTER_HTTP_REFERER:}") String openRouterReferer) {
        this.ocrRestTemplate = ocrRestTemplate;
        this.textractClient = textractClient;
        this.googleCloudVisionClient = googleCloudVisionClient;
        this.meterRegistry = meterRegistry;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.ocrServiceUrl = ocrServiceUrl;
        this.primaryProvider = primaryProvider;
        this.fallbackProviders = fallbackProviders;
        this.openRouterApiKey = openRouterApiKey;
        this.openRouterBaseUrl = openRouterBaseUrl;
        this.openRouterModel = openRouterModel;
        this.openRouterReferer = openRouterReferer;
    }

    /**
     * Process image URL qua OCR pipeline.
     *
     * <p>Flow:
     * <ol>
     *   <li>Chọn provider theo thứ tự cấu hình (`primary` rồi `fallback-order`)</li>
     *   <li>Nếu provider hiện tại fail → log warning rồi thử provider tiếp theo</li>
     *   <li>Nếu tất cả provider fail → trả về fallback result `source=all-providers-failed`</li>
     * </ol>
     *
     * @param imageUrl URL của image cần OCR (http/https hoặc presigned S3/MinIO URL)
     * @return {@link OcrResult} chứa extracted text, confidence, source, language
     */
    public OcrResult processImage(String imageUrl) {
        long pipelineStarted = System.nanoTime();
        log.info("Processing OCR for image: {}",
                imageUrl.length() > 80 ? imageUrl.substring(0, 80) + "..." : imageUrl);

        List<String> providerOrder = resolveProviderOrder();
        if (providerOrder.isEmpty()) {
            log.error("OCR provider order is empty. Check OCR_PROVIDER_PRIMARY/OCR_PROVIDER_FALLBACK_ORDER");
            return buildAllProvidersFailedResult();
        }
        for (String provider : providerOrder) {
            long attemptStarted = System.nanoTime();
            try {
                OcrResult result = switch (provider) {
                    case "easyocr" -> callEasyOcr(imageUrl);
                    case "gcv" -> callGoogleCloudVision(imageUrl);
                    case "textract" -> textractClient.extract(imageUrl);
                    default -> throw new OcrProcessingException("Unsupported OCR provider: " + provider);
                };
                recordProviderMetrics(provider, true, attemptStarted);
                log.info("ocr_provider_selected provider={} success=true source={} providerLatencyMs={} totalLatencyMs={}",
                        provider,
                        result.getSource(),
                        elapsedMs(attemptStarted),
                        elapsedMs(pipelineStarted));
                return result;
            } catch (OcrProcessingException ex) {
                recordProviderMetrics(provider, false, attemptStarted);
                log.warn("OCR provider '{}' failed: {}", provider, ex.getMessage());
            } catch (Exception ex) {
                recordProviderMetrics(provider, false, attemptStarted);
                log.warn("OCR provider '{}' failed with unexpected error: {}", provider, ex.getMessage());
            }
        }

        recordProviderMetrics("all-providers-failed", false, pipelineStarted);
        log.warn("ocr_provider_selected provider=all-providers-failed success=false source=all-providers-failed totalLatencyMs={}",
                elapsedMs(pipelineStarted));
        return buildAllProvidersFailedResult();
    }

    public OcrProcessingResult processDocument(String documentUrl, String mimeTypeRaw) {
        String mimeType = normalizeMimeType(mimeTypeRaw);
        if (mimeType.startsWith("image/")) {
            OcrResult result = processImage(documentUrl);
            return new OcrProcessingResult(
                    result,
                    "image",
                    result.getSource(),
                    mimeType,
                    List.of()
            );
        }
        if ("application/pdf".equals(mimeType)) {
            return processPdf(documentUrl, mimeType);
        }
        throw new OcrProcessingException("Unsupported OCR MIME type: " + mimeType);
    }

    public OcrProcessingResult processPdfBytes(byte[] pdfBytes, String documentUrl, String mimeTypeRaw) {
        String mimeType = normalizeMimeType(mimeTypeRaw);
        if (!"application/pdf".equals(mimeType)) {
            throw new OcrProcessingException("Unsupported OCR MIME type: " + mimeType);
        }
        return processPdfBytes(pdfBytes, documentUrl, mimeType, System.nanoTime());
    }

    private OcrProcessingResult processPdf(String documentUrl, String mimeType) {
        long started = System.nanoTime();
        try {
            byte[] pdfBytes = downloadDocumentBytes(documentUrl);
            return processPdfBytes(pdfBytes, documentUrl, mimeType, started);
        } catch (Exception ex) {
            log.warn("ocr_route_selected route=pdf-document provider=pdf-text-layer mimeType={} success=false errorType={}",
                    mimeType,
                    ex.getClass().getSimpleName());
            return failedPdfResult(mimeType, List.of());
        }
    }

    private OcrProcessingResult processPdfBytes(byte[] pdfBytes, String documentUrl, String mimeType, long started) {
        OcrProcessingResult textLayerResult = emptyPdfStageResult("pdf-text-layer", "pdfbox", mimeType);
        try {
            textLayerResult = extractPdfTextLayer(pdfBytes, mimeType, started);
            if (!textLayerResult.result().getText().isBlank()) {
                return textLayerResult;
            }
        } catch (Exception ex) {
            log.warn("ocr_route_selected route=pdf-text-layer provider=pdfbox mimeType={} success=false errorType={}",
                    mimeType,
                    ex.getClass().getSimpleName());
        }

        OcrProcessingResult renderedImageResult = emptyPdfStageResult("pdf-rendered-images", "easyocr", mimeType);
        try {
            renderedImageResult = extractScannedPdfWithEasyOcr(pdfBytes, mimeType, started);
            if (!renderedImageResult.result().getText().isBlank()) {
                return renderedImageResult;
            }
        } catch (Exception ex) {
            log.warn("ocr_route_selected route=pdf-rendered-images provider=easyocr mimeType={} success=false errorType={}",
                    mimeType,
                    ex.getClass().getSimpleName());
        }

        try {
            OcrResult result = textractClient.extract(documentUrl);
            if ("textract-stub".equals(result.getSource())) {
                log.warn("ocr_route_selected route=pdf-document provider=textract-stub mimeType={} success=false reason=provider-disabled",
                        mimeType);
                return failedPdfResult(mimeType, renderedImageResult.pages());
            }
            String provider = result.getSource() == null || result.getSource().isBlank()
                    ? "document-ocr"
                    : result.getSource();
            log.info("ocr_route_selected route=pdf-document provider={} mimeType={} success=true totalLatencyMs={}",
                    provider,
                    mimeType,
                    elapsedMs(started));
            return new OcrProcessingResult(
                    result,
                    "pdf-document",
                    provider,
                    mimeType,
                    buildProviderPages(
                            renderedImageResult.pages().isEmpty() ? textLayerResult.pages() : renderedImageResult.pages(),
                            provider,
                            result.getConfidence()
                    )
            );
        } catch (Exception ex) {
            log.warn("ocr_route_selected route=pdf-document provider=textract mimeType={} success=false errorType={}",
                    mimeType,
                    ex.getClass().getSimpleName());
            return failedPdfResult(
                    mimeType,
                    renderedImageResult.pages().isEmpty() ? textLayerResult.pages() : renderedImageResult.pages()
            );
        }
    }

    private byte[] downloadDocumentBytes(String documentUrl) {
        byte[] bytes = ocrRestTemplate.getForObject(documentUrl, byte[].class);
        if (bytes == null || bytes.length == 0) {
            throw new OcrProcessingException("PDF download returned empty body");
        }
        return bytes;
    }

    private OcrProcessingResult extractPdfTextLayer(byte[] pdfBytes, String mimeType, long started) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder combinedText = new StringBuilder();
            List<OcrPageResult> pages = new ArrayList<>();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isBlank()) {
                    if (!combinedText.isEmpty()) {
                        combinedText.append("\n\n");
                    }
                    combinedText.append(pageText);
                }
                pages.add(new OcrPageResult(page, "pdf-text-layer", pageText.isBlank() ? 0.0f : 1.0f));
            }
            String text = combinedText.toString();
            float confidence = text.isBlank() ? 0.0f : 1.0f;
            log.info("ocr_route_selected route=pdf-text-layer provider=pdfbox mimeType={} success={} pages={} totalLatencyMs={}",
                    mimeType,
                    !text.isBlank(),
                    pageCount,
                    elapsedMs(started));
            return new OcrProcessingResult(
                    OcrResult.builder()
                            .text(text)
                            .confidence(confidence)
                            .source(text.isBlank() ? "pdf-text-layer-empty" : "pdf-text-layer")
                            .language("unknown")
                            .processingTimeMs((int) elapsedMs(started))
                            .build(),
                    "pdf-text-layer",
                    "pdfbox",
                    mimeType,
                    pages
            );
        }
    }

    private OcrProcessingResult extractScannedPdfWithEasyOcr(byte[] pdfBytes, String mimeType, long started) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder combinedText = new StringBuilder();
            List<OcrPageResult> pages = new ArrayList<>();
            float confidenceSum = 0.0f;
            int recognizedPages = 0;
            int pageCount = document.getNumberOfPages();
            int maxPages = Math.max(1, Math.min(pageCount, pdfRenderedImagesMaxPages));
            long renderedBytes = 0L;

            for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
                OcrResult pageResult;
                String pageText = "";
                try {
                    byte[] pagePngBytes = renderPdfPageToPngBytes(renderer, pageIndex);
                    renderedBytes += pagePngBytes.length;
                    if (renderedBytes > pdfRenderedImagesMaxTotalBytes) {
                        log.warn("PDF rendered page OCR aborted due to render byte budget. page={} renderedBytes={} budget={}",
                                pageIndex + 1,
                                renderedBytes,
                                pdfRenderedImagesMaxTotalBytes);
                        break;
                    }
                    pageResult = callEasyOcrBase64(Base64.getEncoder().encodeToString(pagePngBytes));
                    pageText = pageResult.getText() == null ? "" : pageResult.getText().trim();
                } catch (OcrProcessingException ex) {
                    log.warn("PDF rendered page OCR failed. page={} error={}", pageIndex + 1, ex.getMessage());
                    pageResult = OcrResult.builder()
                            .text("")
                            .confidence(0.0f)
                            .source("easyocr")
                            .language("unknown")
                            .processingTimeMs(0)
                            .build();
                }
                if (!pageText.isBlank()) {
                    if (!combinedText.isEmpty()) {
                        combinedText.append("\n\n");
                    }
                    combinedText.append(pageText);
                    confidenceSum += pageResult.getConfidence();
                    recognizedPages++;
                }
                pages.add(new OcrPageResult(pageIndex + 1, "easyocr", pageText.isBlank() ? 0.0f : pageResult.getConfidence()));
                if (combinedText.length() >= pdfRenderedImagesMinTextCharsBeforeStop) {
                    break;
                }
            }

            String text = combinedText.toString();
            float confidence = recognizedPages == 0 ? 0.0f : confidenceSum / recognizedPages;
            log.info("ocr_route_selected route=pdf-rendered-images provider=easyocr mimeType={} success={} pages={} totalLatencyMs={}",
                    mimeType,
                    !text.isBlank(),
                    pages.size(),
                    elapsedMs(started));
            return new OcrProcessingResult(
                    OcrResult.builder()
                            .text(text)
                            .confidence(confidence)
                            .source(text.isBlank() ? "pdf-rendered-images-empty" : "easyocr")
                            .language("unknown")
                            .processingTimeMs((int) elapsedMs(started))
                            .build(),
                    "pdf-rendered-images",
                    "easyocr",
                    mimeType,
                    pages
            );
        }
    }

    private byte[] renderPdfPageToPngBytes(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, pdfRenderedImagesDpi, ImageType.RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    private OcrProcessingResult emptyPdfStageResult(String route, String provider, String mimeType) {
        return new OcrProcessingResult(
                OcrResult.builder()
                        .text("")
                        .confidence(0.0f)
                        .source(provider + "-empty")
                        .language("unknown")
                        .processingTimeMs(0)
                        .build(),
                route,
                provider,
                mimeType,
                List.of()
        );
    }

    private List<OcrPageResult> buildProviderPages(List<OcrPageResult> sourcePages, String provider, float confidence) {
        if (sourcePages == null || sourcePages.isEmpty()) {
            return List.of(new OcrPageResult(1, provider, confidence));
        }
        return sourcePages.stream()
                .map(page -> new OcrPageResult(page.pageNumber(), provider, confidence))
                .toList();
    }

    private OcrProcessingResult failedPdfResult(String mimeType, List<OcrPageResult> pages) {
        return new OcrProcessingResult(
                buildAllProvidersFailedResult(),
                "pdf-document",
                "textract",
                mimeType,
                buildProviderPages(pages, "textract", 0.0f)
        );
    }

    /**
     * Gọi EasyOCR microservice qua REST API.
     *
     * @param imageUrl URL của image
     * @return {@link OcrResult} từ EasyOCR
     * @throws OcrProcessingException nếu EasyOCR service fail
     */
    OcrResult callEasyOcr(String imageUrl) {
        return callEasyOcrRequest(EasyOcrRequest.fromUrl(imageUrl));
    }

    OcrResult callEasyOcrBase64(String imageBase64) {
        return callEasyOcrRequest(EasyOcrRequest.fromBase64(imageBase64));
    }

    private OcrResult callEasyOcrRequest(EasyOcrRequest request) {
        try {
            EasyOcrResponse response = ocrRestTemplate.postForObject(
                    ocrServiceUrl + "/ocr",
                    request,
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
            log.error("EasyOCR service returned error: {} {} body={}",
                    e.getStatusCode(),
                    e.getStatusText(),
                    summarizeOcrErrorBody(e.getResponseBodyAsString()));
            throw new OcrProcessingException("EasyOCR service error: " + e.getStatusCode(), e);
        } catch (IllegalArgumentException e) {
            log.error("EasyOCR malformed response: {}", e.getMessage());
            throw new OcrProcessingException("EasyOCR malformed response", e);
        }
    }

    private String summarizeOcrErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String sanitized = body.replaceAll("\"image_base64\"\\s*:\\s*\"[^\"]+\"", "\"image_base64\":\"<redacted>\"");
        int maxLength = 1_000;
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength) + "...<truncated>";
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
            return buildAllProvidersFailedResult();
        }
    }

    OcrResult callGoogleCloudVision(String imageUrl) {
        try {
            return googleCloudVisionClient.extract(imageUrl);
        } catch (Exception ex) {
            throw new OcrProcessingException("Google Cloud Vision failed", ex);
        }
    }

    private List<String> resolveProviderOrder() {
        List<String> providers = new ArrayList<>();
        addProviderIfValid(providers, normalizeProvider(primaryProvider), "primary");
        Arrays.stream(fallbackProviders.split(","))
                .map(this::normalizeProvider)
                .forEach(provider -> addProviderIfValid(providers, provider, "fallback"));
        return providers;
    }

    private void addProviderIfValid(List<String> providers, String provider, String source) {
        if (provider.isBlank()) {
            return;
        }
        if (!isSupportedProvider(provider)) {
            log.warn("Ignoring unsupported OCR provider '{}' from {}", provider, source);
            return;
        }
        if (!providers.contains(provider)) {
            providers.add(provider);
        }
    }

    private boolean isSupportedProvider(String provider) {
        return "easyocr".equals(provider) || "gcv".equals(provider) || "textract".equals(provider);
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        int parametersIndex = mimeType.indexOf(';');
        String baseType = parametersIndex >= 0 ? mimeType.substring(0, parametersIndex) : mimeType;
        return baseType.trim().toLowerCase(Locale.ROOT);
    }

    private OcrResult buildAllProvidersFailedResult() {
        return OcrResult.builder()
                .text("")
                .confidence(0.0f)
                .source("all-providers-failed")
                .language("unknown")
                .processingTimeMs(0)
                .build();
    }

    private void recordProviderMetrics(String provider, boolean success, long startedNanos) {
        if (meterRegistry == null) {
            return;
        }
        try {
            meterRegistry.counter("ocr.provider.selection.count",
                    "provider", provider,
                    "success", Boolean.toString(success))
                    .increment();
            Timer.builder("ocr.provider.selection.latency")
                    .tag("provider", provider)
                    .register(meterRegistry)
                    .record(System.nanoTime() - startedNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
        } catch (Exception ex) {
            log.debug("Failed to record OCR provider metrics: {}", ex.getMessage());
        }
    }

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    /**
     * Parse OCR raw text thành danh sách metrics bằng LLM.
     */
    public OcrExtractionResult parseMetrics(String ocrText, float overallConfidence) {
        if (ocrText == null || ocrText.isBlank()) {
            return new OcrExtractionResult(null, null, null, null, new ArrayList<>());
        }

        String prompt = """
            You are a medical data extraction expert. Extract and NORMALIZE information from the following OCR text of a Vietnamese medical record (Phiếu kết quả xét nghiệm/khám bệnh).
            
            RULES:
            1. Language: Vietnamese (Tiếng Việt).
            2. Normalization: If a metric name is misread or abbreviated, map it to the standard medical term.
               Standard terms reference:
               - Blood Sugar: Glucose, HbA1c.
               - Liver: AST (GOT), ALT (GPT), GGT, Bilirubin (Toàn phần/Trực tiếp/Gián tiếp), Albumin, Protein toàn phần.
               - Kidney: Urea (Urê), Creatinine (Creatinin), Acid Uric (Gout).
               - Lipids: Cholesterol toàn phần, Triglyceride, HDL-C, LDL-C.
               - Blood Count: WBC (Bạch cầu), RBC (Hồng cầu), HGB (Huyết sắc tố), HCT, PLT (Tiểu cầu), Neutrophil, Lymphocyte.
            
            3. Fields to extract:
               - "examDate": Date of examination (YYYY-MM-DD). Look for "Ngày khám", "Ngày chỉ định".
               - "recordType": Type of document (e.g., "Xét nghiệm máu", "Siêu âm").
               - "hospitalName": Medical facility name (top left/header).
               - "diagnosis": Doctor's diagnosis or conclusion ("Chẩn đoán", "Kết luận").
               - "metrics": List of laboratory results. Each has "name", "value", "unit".
                 If available in document, also extract:
                 - "flag": one of H/L/N/critical
                 - "referenceRange": {"min": number, "max": number, "attentionMin": number, "attentionMax": number, "unit": "..."}
            
            CRITICAL: 
            - Clean noisy numeric values (e.g., "5.4H" -> "5.4").
            - Ensure "name" is the full standard Vietnamese name if possible.
            - Return null for missing fields.
            
            Return ONLY a raw JSON object. NO markdown, NO preamble.
            {
              "examDate": "YYYY-MM-DD",
              "recordType": "...",
              "hospitalName": "...",
              "diagnosis": "...",
              "metrics": [
                {"name": "...", "value": "...", "unit": "...", "flag": "H|L|N|critical|null", "referenceRange": null}
              ]
            }
            
            OCR Text:
            """ + ocrText;

        String jsonResponse = null;
        try {
            log.info("[LLM] Sending OCR text to LLM (length: {})", ocrText.length());
            jsonResponse = callPrimaryLlm(prompt);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                log.warn("[LLM] Primary provider returned empty content. Attempting OpenRouter fallback once.");
                jsonResponse = callOpenRouterFallback(prompt);
            }
            log.debug("[LLM] Received response from model (length: {})",
                    jsonResponse != null ? jsonResponse.length() : 0);

            if (jsonResponse == null || jsonResponse.isBlank()) {
                log.warn("[LLM] All providers returned empty content. Falling back to regex parser.");
                return parseMetricsByRegex(ocrText, overallConfidence);
            }

            // Clean markdown and any preamble
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.contains("```json")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```json") + 7);
                if (jsonResponse.contains("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
                }
            } else if (jsonResponse.contains("```")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```") + 3);
                if (jsonResponse.contains("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
                }
            }
            jsonResponse = jsonResponse.trim();

            // Simple attempt to fix truncated JSON if it looks like it ended early in an array
            if (!jsonResponse.endsWith("}") && !jsonResponse.endsWith("]")) {
                log.warn("[LLM] Detected truncated JSON, attempting to close blocks");
                if (jsonResponse.contains("[") && !jsonResponse.contains("]")) {
                    jsonResponse += "]}";
                } else {
                    jsonResponse += "}";
                }
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            String examDate = rootNode.has("examDate") && !rootNode.get("examDate").isNull() ? rootNode.get("examDate").asText() : null;
            String recordType = rootNode.has("recordType") && !rootNode.get("recordType").isNull() ? rootNode.get("recordType").asText() : null;
            String hospitalName = rootNode.has("hospitalName") && !rootNode.get("hospitalName").isNull() ? rootNode.get("hospitalName").asText() : null;
            String diagnosis = rootNode.has("diagnosis") && !rootNode.get("diagnosis").isNull() ? rootNode.get("diagnosis").asText() : null;

            JsonNode metricsNode = rootNode.get("metrics");
            List<MetricDto> metrics = new ArrayList<>();
            if (metricsNode != null && metricsNode.isArray()) {
                for (JsonNode node : metricsNode) {
                    MetricDto metric = new MetricDto();
                    String rawName = node.has("name") ? node.get("name").asText() : "";
                    metric.setName(rawName);
                    metric.setRawName(rawName);
                    metric.setNormalizedName(normalizeText(rawName));
                    // Value can be numeric or string, handle gracefully
                    String value = "";
                    if (node.has("value")) {
                        JsonNode vNode = node.get("value");
                        value = vNode.isNull() ? "" : vNode.asText();
                    }
                    metric.setRawValue(value);
                    metric.setInterpretation(extractInterpretation(value));
                    metric.setInterpretationSource("document");
                    metric.setValue(stripFlagSuffix(value));
                    metric.setNormalizedValue(normalizeNumeric(value));
                    String unit = node.has("unit") && !node.get("unit").isNull() ? node.get("unit").asText() : "";
                    metric.setUnit(unit);
                    metric.setRawUnit(unit);
                    metric.setNormalizedUnit(normalizeText(unit));
                    metric.setConfidence(overallConfidence);
                    metric.setSource("ocr");
                    metric.setConfidenceLevel(classifyConfidence(overallConfidence));
                    metric.setReferenceRangeSource("none");
                    metric.setStatusSource("none");
                    metric.setCritical(false);
                    if (node.has("flag") && !node.get("flag").isNull()) {
                        String flag = node.get("flag").asText();
                        applyDocumentFlag(metric, flag);
                    }
                    metric.setReferenceRange(parseDocumentReferenceRange(node.get("referenceRange")));
                    if (metric.getReferenceRange() != null) {
                        metric.setReferenceRangeSource("document");
                    }
                    metrics.add(metric);
                }
            }
            log.info("[LLM] Parsed: date={}, type={}, hospital={}, diagnosis={}, metricsCount={}", 
                    examDate, recordType, hospitalName, diagnosis, metrics.size());
            return new OcrExtractionResult(examDate, recordType, hospitalName, diagnosis, metrics);
        } catch (Exception e) {
            log.error("[LLM] Failed to parse metrics using LLM. responseLength={}",
                    jsonResponse != null ? jsonResponse.length() : 0, e);
            return parseMetricsByRegex(ocrText, overallConfidence);
        }
    }

    private String callPrimaryLlm(String prompt) {
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception ex) {
            log.warn("[LLM] Primary provider failed: {}", ex.getMessage());
            return null;
        }
    }

    private String callOpenRouterFallback(String prompt) {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            log.warn("[LLM] OpenRouter API key missing. Skip provider fallback.");
            return null;
        }
        try {
            String url = openRouterBaseUrl + "/chat/completions";
            Map<String, Object> payload = Map.of(
                    "model", openRouterModel,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a medical OCR extraction assistant."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.2
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            if (openRouterReferer != null && !openRouterReferer.isBlank()) {
                headers.set("HTTP-Referer", openRouterReferer);
            }
            headers.set("X-Title", "HealthLens");

            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(payload, headers);
            org.springframework.http.ResponseEntity<String> response = ocrRestTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                return null;
            }
            JsonNode content = choices.get(0).path("message").path("content");
            return content.isMissingNode() || content.isNull() ? null : content.asText();
        } catch (Exception ex) {
            log.warn("[LLM] OpenRouter fallback failed: {}", ex.getMessage());
            return null;
        }
    }

    private OcrExtractionResult parseMetricsByRegex(String ocrText, float overallConfidence) {
        List<MetricDto> metrics = new ArrayList<>();
        String[] lines = ocrText.split("\\R");

        Pattern metricWithUnitPattern = Pattern.compile(
                "(?i)^\\s*([\\p{L}A-Za-z0-9\\-\\(\\)\\./%\\s]{2,}?)\\s+([<>]?[0-9]+(?:[\\.,][0-9]+)?\\s*[HL]?)\\s+([%a-zA-Z\\^0-9/]+)\\b.*$"
        );
        Pattern metricNoUnitPattern = Pattern.compile(
                "(?i)^\\s*([\\p{L}A-Za-z0-9\\-\\(\\)\\./%\\s]{2,}?)\\s+([<>]?[0-9]+(?:[\\.,][0-9]+)?\\s*[HL]?)\\s*$"
        );
        Pattern datePattern = Pattern.compile("(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{4})");

        String examDate = null;
        String recordType = null;
        String hospitalName = null;

        for (String lineRaw : lines) {
            String line = lineRaw == null ? "" : lineRaw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (examDate == null) {
                Matcher d = datePattern.matcher(line);
                if (d.find()) {
                    examDate = normalizeDate(d.group(1));
                }
            }
            if (recordType == null && line.toLowerCase(Locale.ROOT).contains("xét nghiệm")) {
                recordType = "Xét nghiệm máu";
            }
            if (hospitalName == null && line.length() > 8 && line.equals(line.toUpperCase(Locale.ROOT)) && line.matches(".*[\\p{L}].*")) {
                hospitalName = line;
            }

            String[] parsed = parseMetricLine(line, metricWithUnitPattern, metricNoUnitPattern);
            if (parsed == null) {
                continue;
            }
            String name = parsed[0];
            String rawValue = parsed[1];
            String unit = parsed[2];
            if (name.isBlank() || rawValue.isBlank() || name.matches(".*\\d{3,}.*")) {
                continue;
            }

            MetricDto metric = new MetricDto();
            metric.setName(name);
            metric.setRawName(name);
            metric.setNormalizedName(normalizeText(name));
            metric.setRawValue(rawValue);
            metric.setValue(stripFlagSuffix(rawValue));
            metric.setNormalizedValue(normalizeNumeric(rawValue));
            metric.setInterpretation(extractInterpretation(rawValue));
            metric.setInterpretationSource("document");
            metric.setUnit(unit);
            metric.setRawUnit(unit);
            metric.setNormalizedUnit(normalizeText(unit));
            metric.setConfidence(overallConfidence);
            metric.setConfidenceLevel(classifyConfidence(overallConfidence));
            metric.setSource("ocr_regex_fallback");
            metric.setStatusSource("none");
            metric.setReferenceRangeSource("none");
            metric.setCritical(false);
            metrics.add(metric);
        }

        log.warn("[LLM] Using regex fallback parser. extractedMetrics={}", metrics.size());
        return new OcrExtractionResult(examDate, recordType, hospitalName, null, metrics);
    }

    private String[] parseMetricLine(String line, Pattern metricWithUnitPattern, Pattern metricNoUnitPattern) {
        Matcher withUnit = metricWithUnitPattern.matcher(line);
        if (withUnit.matches()) {
            String name = withUnit.group(1) != null ? withUnit.group(1).trim() : "";
            String value = withUnit.group(2) != null ? withUnit.group(2).trim() : "";
            String unit = withUnit.group(3) != null ? withUnit.group(3).trim() : "";
            return new String[] {name, value, unit};
        }

        Matcher noUnit = metricNoUnitPattern.matcher(line);
        if (noUnit.matches()) {
            String name = noUnit.group(1) != null ? noUnit.group(1).trim() : "";
            String value = noUnit.group(2) != null ? noUnit.group(2).trim() : "";
            return new String[] {name, value, ""};
        }

        // Heuristic fallback for noisy OCR rows: pick the first numeric token as value.
        String[] tokens = line.split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        int valueIdx = -1;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches("(?i)[<>]?[0-9]+(?:[\\.,][0-9]+)?[HL]?")) {
                valueIdx = i;
                break;
            }
        }
        if (valueIdx <= 0) {
            return null;
        }
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < valueIdx; i++) {
            String token = tokens[i];
            if (token.matches("\\d+")) {
                continue;
            }
            if (!nameBuilder.isEmpty()) {
                nameBuilder.append(' ');
            }
            nameBuilder.append(token);
        }
        String name = nameBuilder.toString().trim();
        String value = tokens[valueIdx];
        String unit = (valueIdx + 1 < tokens.length && tokens[valueIdx + 1].matches("(?i)[%a-z\\^0-9/]+")) ? tokens[valueIdx + 1] : "";
        if (name.length() < 2) {
            return null;
        }
        return new String[] {name, value, unit};
    }

    private String normalizeDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        String normalized = rawDate.replace('/', '-');
        try {
            if (normalized.matches("\\d{1,2}-\\d{1,2}-\\d{4}")) {
                String[] parts = normalized.split("-");
                return "%s-%02d-%02d".formatted(parts[2], Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            if (normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] parts = normalized.split("-");
                return "%s-%02d-%02d".formatted(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    /**
     * Phân loại confidence level dựa trên rules từ PRD.
     */
    public String classifyConfidence(float confidence) {
        float highThreshold = Math.max(highConfidenceThreshold, mediumConfidenceThreshold);
        float mediumThreshold = Math.min(highConfidenceThreshold, mediumConfidenceThreshold);
        if (confidence >= highThreshold) {
            return "high";
        } else if (confidence >= mediumThreshold) {
            return "medium";
        } else {
            return "low";
        }
    }

    private String normalizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }

    private String stripFlagSuffix(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        return rawValue.trim().replaceAll("(?i)\\s*[HL]$", "").trim();
    }

    private String normalizeNumeric(String rawValue) {
        String stripped = stripFlagSuffix(rawValue);
        if (stripped == null) {
            return null;
        }
        return stripped.replace(",", ".").replaceAll("\\s+", "");
    }

    private String extractInterpretation(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "unknown";
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.endsWith("H")) {
            return "high";
        }
        if (normalized.endsWith("L")) {
            return "low";
        }
        return "unknown";
    }

    private void applyDocumentFlag(MetricDto metric, String flagRaw) {
        if (flagRaw == null || flagRaw.isBlank()) {
            return;
        }
        String flag = flagRaw.trim().toUpperCase(Locale.ROOT);
        switch (flag) {
            case "H" -> metric.setInterpretation("high");
            case "L" -> metric.setInterpretation("low");
            case "N", "NORMAL" -> metric.setInterpretation("normal");
            case "CRITICAL", "PANIC" -> {
                metric.setInterpretation("critical");
                metric.setCritical(true);
            }
            default -> metric.setInterpretation("unknown");
        }
    }

    private ReferenceRangeDto parseDocumentReferenceRange(JsonNode rangeNode) {
        if (rangeNode == null || rangeNode.isNull() || !rangeNode.isObject()) {
            return null;
        }
        try {
            BigDecimal min = readDecimal(rangeNode.get("min"));
            BigDecimal max = readDecimal(rangeNode.get("max"));
            BigDecimal attentionMin = readDecimal(rangeNode.get("attentionMin"));
            BigDecimal attentionMax = readDecimal(rangeNode.get("attentionMax"));
            if (min == null || max == null) {
                return null;
            }
            if (attentionMin == null) {
                attentionMin = min;
            }
            if (attentionMax == null) {
                attentionMax = max;
            }
            String unit = rangeNode.has("unit") && !rangeNode.get("unit").isNull() ? rangeNode.get("unit").asText() : null;
            return new ReferenceRangeDto(min, max, attentionMin, attentionMax, unit);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal readDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String raw = node.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return new BigDecimal(raw.trim().replace(",", "."));
    }

}
