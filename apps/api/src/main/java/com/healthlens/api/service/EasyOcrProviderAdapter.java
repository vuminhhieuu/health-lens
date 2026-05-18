package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import com.healthlens.api.service.ocr.OcrCapability;
import com.healthlens.api.service.ocr.OcrJob;
import com.healthlens.api.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class EasyOcrProviderAdapter implements OcrProvider {

    private final RestTemplate ocrRestTemplate;
    private final String ocrServiceUrl;
    private final int pdfRenderedImagesMaxPages;
    private final int pdfRenderedImagesDpi;
    private final int pdfRenderedImagesMaxTotalBytes;
    private final int pdfRenderedImagesMinTextCharsBeforeStop;

    public EasyOcrProviderAdapter(
            @Qualifier("ocrRestTemplate") RestTemplate ocrRestTemplate,
            @Value("${app.ocr.service.url:http://localhost:8001}") String ocrServiceUrl,
            @Value("${app.ocr.pdf.rendered-images.max-pages:10}") int pdfRenderedImagesMaxPages,
            @Value("${app.ocr.pdf.rendered-images.dpi:150}") int pdfRenderedImagesDpi,
            @Value("${app.ocr.pdf.rendered-images.max-total-bytes:31457280}") int pdfRenderedImagesMaxTotalBytes,
            @Value("${app.ocr.pdf.rendered-images.min-text-chars-before-stop:120}") int pdfRenderedImagesMinTextCharsBeforeStop
    ) {
        this.ocrRestTemplate = ocrRestTemplate;
        this.ocrServiceUrl = ocrServiceUrl;
        this.pdfRenderedImagesMaxPages = pdfRenderedImagesMaxPages;
        this.pdfRenderedImagesDpi = pdfRenderedImagesDpi;
        this.pdfRenderedImagesMaxTotalBytes = pdfRenderedImagesMaxTotalBytes;
        this.pdfRenderedImagesMinTextCharsBeforeStop = pdfRenderedImagesMinTextCharsBeforeStop;
    }

    @Override
    public String name() {
        return "easyocr";
    }

    @Override
    public Set<OcrCapability> capabilities() {
        return Set.of(OcrCapability.IMAGE_OCR, OcrCapability.PDF_SCAN);
    }

    @Override
    public OcrResult extract(OcrJob job) {
        if ("application/pdf".equals(job.mimeType())) {
            if (!job.hasDocumentBytes()) {
                throw new OcrProcessingException("EasyOCR PDF scan requires document bytes");
            }
            try {
                return extractScannedPdf(job.documentBytes(), job.mimeType());
            } catch (IOException ex) {
                throw new OcrProcessingException("EasyOCR PDF scan failed", ex);
            }
        }
        if (job.hasBase64Image()) {
            return callEasyOcrRequest(OcrService.EasyOcrRequest.fromBase64(job.imageBase64()));
        }
        return callEasyOcrRequest(OcrService.EasyOcrRequest.fromUrl(job.documentUrl()));
    }

    private OcrResult extractScannedPdf(byte[] pdfBytes, String mimeType) throws IOException {
        long started = System.nanoTime();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder combinedText = new StringBuilder();
            List<OcrResult.OcrPage> pages = new ArrayList<>();
            List<OcrResult.OcrSegment> lines = new ArrayList<>();
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
                    pageResult = callEasyOcrRequest(OcrService.EasyOcrRequest.fromBase64(Base64.getEncoder().encodeToString(pagePngBytes)));
                    pageText = pageResult.getText() == null ? "" : pageResult.getText().trim();
                } catch (OcrProcessingException ex) {
                    log.warn("PDF rendered page OCR failed. page={} error={}", pageIndex + 1, ex.getMessage());
                    pageResult = OcrResult.builder()
                            .text("")
                            .confidence(0.0f)
                            .provider(name())
                            .language("unknown")
                            .latencyMs(0)
                            .build();
                }
                float pageConfidence = pageText.isBlank() ? 0.0f : pageResult.getConfidence();
                if (!pageText.isBlank()) {
                    if (!combinedText.isEmpty()) {
                        combinedText.append("\n\n");
                    }
                    combinedText.append(pageText);
                    confidenceSum += pageConfidence;
                    recognizedPages++;
                    lines.add(OcrResult.OcrSegment.builder()
                            .pageNumber(pageIndex + 1)
                            .text(pageText)
                            .confidence(pageConfidence)
                            .kind("line")
                            .build());
                }
                pages.add(OcrResult.OcrPage.builder()
                        .pageNumber(pageIndex + 1)
                        .text(pageText)
                        .confidence(pageConfidence)
                        .build());
                if (combinedText.length() >= pdfRenderedImagesMinTextCharsBeforeStop) {
                    break;
                }
            }

            String text = combinedText.toString();
            float confidence = recognizedPages == 0 ? 0.0f : confidenceSum / recognizedPages;
            return OcrResult.builder()
                    .provider(text.isBlank() ? "pdf-rendered-images-empty" : name())
                    .modelVersion(name() + "-rendered-pdf")
                    .mimeType(mimeType)
                    .retentionMode("transient")
                    .latencyMs(elapsedMs(started))
                    .text(text)
                    .confidence(confidence)
                    .language("unknown")
                    .pages(pages)
                    .blocks(List.of())
                    .lines(lines)
                    .diagnostics(List.of())
                    .build();
        }
    }

    private byte[] renderPdfPageToPngBytes(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, pdfRenderedImagesDpi, ImageType.RGB);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }

    private OcrResult callEasyOcrRequest(OcrService.EasyOcrRequest request) {
        try {
            OcrService.EasyOcrResponse response = ocrRestTemplate.postForObject(
                    ocrServiceUrl + "/ocr",
                    request,
                    OcrService.EasyOcrResponse.class
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

            String text = response.text() == null ? "" : response.text();
            return OcrResult.builder()
                    .provider("easyocr")
                    .modelVersion("easyocr-default")
                    .mimeType("image/*")
                    .retentionMode("transient")
                    .providerRequestId(null)
                    .latencyMs(Math.max(0, response.processing_time_ms()))
                    .text(text)
                    .confidence(confidence)
                    .language(response.language_detected() != null ? response.language_detected() : "unknown")
                    .pages(List.of(OcrResult.OcrPage.builder()
                            .pageNumber(1)
                            .text(text)
                            .confidence(confidence)
                            .build()))
                    .lines(List.of(OcrResult.OcrSegment.builder()
                            .pageNumber(1)
                            .text(text)
                            .confidence(confidence)
                            .kind("line")
                            .build()))
                    .blocks(List.of())
                    .diagnostics(List.of())
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

    private long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
