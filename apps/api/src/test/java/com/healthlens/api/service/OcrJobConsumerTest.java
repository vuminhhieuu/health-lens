package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrJobConsumerTest {

        @Mock
        private StringRedisTemplate redisTemplate;
        @Mock
        private StorageService storageService;
        @Mock
        private OcrService ocrService;
        @Mock
        private HealthRecordService healthRecordService;
        @Mock
        private StreamOperations<String, Object, Object> streamOperations;
        @Mock
        @SuppressWarnings("unchecked")
        private MapRecord<String, Object, Object> mapRecord;

        private OcrJobConsumer consumer;

        @BeforeEach
        void setUp() {
                when(redisTemplate.opsForStream()).thenReturn(streamOperations);
                when(redisTemplate.hasKey("ocr.events")).thenReturn(true);
                consumer = new OcrJobConsumer(
                                redisTemplate,
                                storageService,
                                ocrService,
                                healthRecordService,
                                new ObjectMapper(),
                                "ocr.events",
                                "ocr-consumers",
                                "test-consumer");
        }

        @Test
        @DisplayName("confidence 0.49 -> markOcrFailed low_confidence")
        void handleRecord_confidenceBelowFailureThreshold_marksFailed() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("x").confidence(0.49f).provider("easyocr")
                                                                .language("vi").latencyMs(100).build(),
                                                "image",
                                                "easyocr",
                                                "image/jpeg",
                                                java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "low_confidence");
                verify(healthRecordService, never()).markOcrCompleted(any(), any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("confidence 0.50 -> review_required with hasLowConfidenceMetrics=true")
        void handleRecord_confidenceAtMediumLowerBoundary_marksCompletedPartial() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("x").confidence(0.50f).provider("easyocr")
                                                                .language("vi").latencyMs(100).build(),
                                                "image",
                                                "easyocr",
                                                "image/jpeg",
                                                java.util.List.of()));
                when(ocrService.parseMetrics("x", 0.50f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(true));
        }

        @Test
        @DisplayName("confidence 0.85 -> review_required with hasLowConfidenceMetrics=false")
        void handleRecord_confidenceAtHighBoundary_marksCompletedHigh() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("x").confidence(0.85f).provider("easyocr")
                                                                .language("vi").latencyMs(100).build(),
                                                "image",
                                                "easyocr",
                                                "image/jpeg",
                                                java.util.List.of()));
                when(ocrService.parseMetrics("x", 0.85f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(false));
        }

        @Test
        @DisplayName("all providers failed -> markOcrFailed timeout reason")
        void handleRecord_allProvidersFailed_marksTimeoutReason() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("").confidence(0.0f)
                                                                .provider("all-providers-failed").language("unknown")
                                                                .latencyMs(0).build(),
                                                "image",
                                                "all-providers-failed",
                                                "image/jpeg",
                                                java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "timeout");
        }

        @Test
        @DisplayName("misordered thresholds are normalized before branching")
        void handleRecord_misorderedThresholds_normalizesBoundaries() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("x").confidence(0.60f).provider("easyocr")
                                                                .language("vi").latencyMs(100).build(),
                                                "image",
                                                "easyocr",
                                                "image/jpeg",
                                                java.util.List.of()));
                when(ocrService.parseMetrics("x", 0.60f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));
                ReflectionTestUtils.setField(consumer, "ocrFailureThreshold", 0.85f);
                ReflectionTestUtils.setField(consumer, "ocrReviewRequiredThreshold", 0.50f);

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService, never()).markOcrFailed(recordId, "low_confidence");
                verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(true));
        }

        @Test
        @DisplayName("image MIME route records route diagnostics")
        void handleRecord_imageMime_usesImageRouteAndPersistsDiagnostics() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(Map.of(
                                "jobId", "job-1",
                                "correlationId", "corr-1",
                                "recordId", recordId.toString(),
                                "fileKey", "k",
                                "mimeType", "image/png"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/png")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("glucose 5.4").confidence(0.91f)
                                                                .provider("easyocr").language("vi").latencyMs(100)
                                                                .build(),
                                                "image",
                                                "easyocr",
                                                "image/png",
                                                java.util.List.of()));
                when(ocrService.parseMetrics("glucose 5.4", 0.91f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(ocrService).processDocument("https://example.com/img", "image/png");
                verify(healthRecordService).markOcrCompleted(eq(recordId),
                                argThat(json -> json.contains("\"mimeType\":\"image/png\"")
                                                && json.contains("\"route\":\"image\"")
                                                && json.contains("\"provider\":\"easyocr\"")
                                                && json.contains("\"jobId\":\"job-1\"")
                                                && json.contains("\"correlationId\":\"corr-1\"")),
                                any(OcrService.OcrExtractionResult.class), eq(false));
        }

        @Test
        @DisplayName("PDF MIME route preserves page diagnostics")
        void handleRecord_pdfMime_usesPdfRouteAndPersistsPages() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "application/pdf"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/doc.pdf");
                when(storageService.downloadObjectBytes("k")).thenReturn("%PDF".getBytes());
                when(ocrService.processPdfBytes(any(byte[].class), eq("https://example.com/doc.pdf"),
                                eq("application/pdf"))).thenReturn(
                                                new OcrService.OcrProcessingResult(
                                                                OcrResult.builder().text("HbA1c 5.6").confidence(0.90f)
                                                                                .provider("textract").language("vi")
                                                                                .latencyMs(140).build(),
                                                                "pdf-document",
                                                                "textract",
                                                                "application/pdf",
                                                                java.util.List.of(new OcrService.OcrPageResult(1,
                                                                                "textract", 0.90f))));
                when(ocrService.parseMetrics("HbA1c 5.6", 0.90f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(ocrService).processPdfBytes(any(byte[].class), eq("https://example.com/doc.pdf"),
                                eq("application/pdf"));
                verify(healthRecordService).markOcrCompleted(eq(recordId),
                                argThat(json -> json.contains("\"mimeType\":\"application/pdf\"")
                                                && json.contains("\"route\":\"pdf-document\"")
                                                && json.contains("\"pages\"")),
                                any(OcrService.OcrExtractionResult.class), eq(false));
        }

        @Test
        @DisplayName("PDF provider failure stores PDF processing failure reason")
        void handleRecord_pdfProviderFailed_marksPdfProcessingFailed() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "application/pdf"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/doc.pdf");
                when(storageService.downloadObjectBytes("k")).thenReturn("%PDF".getBytes());
                when(ocrService.processPdfBytes(any(byte[].class), eq("https://example.com/doc.pdf"),
                                eq("application/pdf"))).thenReturn(
                                                new OcrService.OcrProcessingResult(
                                                                OcrResult.builder().text("").confidence(0.0f)
                                                                                .provider("all-providers-failed")
                                                                                .language("unknown").latencyMs(0)
                                                                                .build(),
                                                                "pdf-document",
                                                                "textract",
                                                                "application/pdf",
                                                                java.util.List.of(new OcrService.OcrPageResult(1,
                                                                                "textract", 0.0f))));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "pdf_processing_failed");
        }

        @Test
        @DisplayName("unsupported MIME marks structured OCR failure")
        void handleRecord_unsupportedMime_marksStructuredFailure() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "text/plain"));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "unsupported_mime_type");
                verify(storageService, never()).generateInternalDownloadUrl(any(), any(Duration.class));
                verifyNoInteractions(ocrService);
        }

        @Test
        @DisplayName("unsupported image subtype marks structured OCR failure")
        void handleRecord_unsupportedImageSubtype_marksStructuredFailure() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/webp"));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "unsupported_mime_type");
                verify(storageService, never()).generateInternalDownloadUrl(any(), any(Duration.class));
                verifyNoInteractions(ocrService);
        }

        @Test
        @DisplayName("legacy payload without MIME fails terminally instead of trusting file extension")
        void handleRecord_legacyPayloadWithoutMime_marksMissingMimeType() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey",
                                "health-records/u/p/r/original.pdf"));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "missing_mime_type");
                verify(storageService, never()).generateInternalDownloadUrl(any(), any(Duration.class));
                verifyNoInteractions(ocrService);
        }
}
