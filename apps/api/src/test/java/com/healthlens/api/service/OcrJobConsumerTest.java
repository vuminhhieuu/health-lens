package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.entity.OcrJobState;
import com.healthlens.api.dto.OcrResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
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
        private OcrJobStateService ocrJobStateService;
        @Mock
        private StreamOperations<String, Object, Object> streamOperations;
        @Mock
        private ValueOperations<String, String> valueOperations;
        @Mock
        @SuppressWarnings("unchecked")
        private MapRecord<String, Object, Object> mapRecord;

        private OcrJobConsumer consumer;

        @BeforeEach
        void setUp() {
                when(redisTemplate.opsForStream()).thenReturn(streamOperations);
                when(redisTemplate.hasKey("ocr.events")).thenReturn(true);
                lenient().when(mapRecord.getId()).thenReturn(RecordId.of("1-0"));
                lenient().when(ocrJobStateService.startAttempt(any())).thenReturn(
                                new OcrJobStateService.AttemptDecision(false, 1, OcrJobState.PROCESSING));
                lenient().when(ocrJobStateService.markRetryableOrDeadLetter(anyString(), anyString())).thenReturn(
                                new OcrJobStateService.RetryDecision(false, 1, Instant.now().plusSeconds(30)));
                consumer = new OcrJobConsumer(
                                redisTemplate,
                                storageService,
                                ocrService,
                                healthRecordService,
                                ocrJobStateService,
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrJobStateService).completeSucceeded(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(true), anyString());
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrJobStateService).completeSucceeded(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(false), anyString());
        }

        @Test
        @DisplayName("all providers failed -> schedules retryable provider timeout")
        void handleRecord_allProvidersFailed_schedulesRetryableTimeout() {
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrJobStateService).markRetryableOrDeadLetter(anyString(), eq("provider_timeout"));
                verify(healthRecordService, never()).markOcrFailed(eq(recordId), anyString());
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(healthRecordService, never()).markOcrFailed(recordId, "low_confidence");
                verify(ocrJobStateService).completeSucceeded(eq(recordId), any(String.class),
                                any(OcrService.OcrExtractionResult.class), eq(true), anyString());
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrService).processDocument("https://example.com/img", "image/png");
                verify(ocrJobStateService).completeSucceeded(eq(recordId),
                                argThat(json -> json.contains("\"mimeType\":\"image/png\"")
                                                && json.contains("\"route\":\"image\"")
                                                && json.contains("\"provider\":\"easyocr\"")
                                                && json.contains("\"jobId\":\"job-1\"")
                                                && json.contains("\"correlationId\":\"corr-1\"")),
                                any(OcrService.OcrExtractionResult.class), eq(false), anyString());
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrService).processPdfBytes(any(byte[].class), eq("https://example.com/doc.pdf"),
                                eq("application/pdf"));
                verify(ocrJobStateService).completeSucceeded(eq(recordId),
                                argThat(json -> json.contains("\"mimeType\":\"application/pdf\"")
                                                && json.contains("\"route\":\"pdf-document\"")
                                                && json.contains("\"pages\"")),
                                any(OcrService.OcrExtractionResult.class), eq(false), anyString());
        }

        @Test
        @DisplayName("PDF provider failure schedules retry with PDF processing reason")
        void handleRecord_pdfProviderFailed_schedulesRetry() {
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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(ocrJobStateService).markRetryableOrDeadLetter(anyString(), eq("pdf_processing_failed"));
                verify(healthRecordService, never()).markOcrFailed(eq(recordId), anyString());
        }

        @Test
        @DisplayName("unsupported MIME marks structured OCR failure")
        void handleRecord_unsupportedMime_marksStructuredFailure() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "text/plain"));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

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

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verify(healthRecordService).markOcrFailed(recordId, "missing_mime_type");
                verify(storageService, never()).generateInternalDownloadUrl(any(), any(Duration.class));
                verifyNoInteractions(ocrService);
        }

        @Test
        @DisplayName("DB failure after provider success is persisted retryable before ack")
        void consume_dbFailureAfterProviderSuccess_schedulesRetryAndAcks() {
                UUID recordId = UUID.randomUUID();
                when(streamOperations.read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                                .thenReturn(List.of(mapRecord));
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class)))
                                .thenReturn("https://example.com/img");
                when(ocrService.processDocument("https://example.com/img", "image/jpeg")).thenReturn(
                                new OcrService.OcrProcessingResult(
                                                OcrResult.builder().text("x").confidence(0.90f).provider("easyocr")
                                                                .language("vi").latencyMs(100).build(),
                                                "image",
                                                "easyocr",
                                                "image/jpeg",
                                                java.util.List.of()));
                when(ocrService.parseMetrics("x", 0.90f)).thenReturn(
                                new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));
                doThrow(new RuntimeException("db down")).when(ocrJobStateService)
                                .completeSucceeded(eq(recordId), anyString(), any(OcrService.OcrExtractionResult.class),
                                                eq(false), anyString());

                consumer.consume();

                verify(ocrJobStateService).markRetryableOrDeadLetter(anyString(), eq("persistence_error"));
                verify(streamOperations).acknowledge(eq("ocr.events"), eq("ocr-consumers"), any(RecordId.class));
        }

        @Test
        @DisplayName("duplicate succeeded idempotency key skips OCR side effects")
        void handleRecord_duplicateSucceeded_skipsSideEffects() {
                UUID recordId = UUID.randomUUID();
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "jobId", "job-1", "fileKey", "k", "mimeType",
                                                "image/jpeg"));
                when(ocrJobStateService.startAttempt(any())).thenReturn(
                                new OcrJobStateService.AttemptDecision(true, 1, OcrJobState.SUCCEEDED));

                ReflectionTestUtils.invokeMethod(consumer, "handleRecordWithCorrelation", mapRecord);

                verifyNoInteractions(storageService);
                verifyNoInteractions(ocrService);
                verify(healthRecordService, never()).markOcrCompleted(any(), anyString(), any(), anyBoolean());
        }

        @Test
        @DisplayName("unexpected repeated consumer failure moves stream entry to DLQ and acks")
        void consume_repeatedUnexpectedFailure_deadLettersAndAcks() {
                UUID recordId = UUID.randomUUID();
                when(streamOperations.read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                                .thenReturn(List.of(mapRecord));
                when(mapRecord.getValue()).thenReturn(
                                Map.of("recordId", recordId.toString(), "fileKey", "k", "mimeType", "image/jpeg"));
                when(ocrJobStateService.startAttempt(any())).thenThrow(new RuntimeException("db down"));
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.increment("ocr:consumer-failures:1-0")).thenReturn(3L);

                consumer.consume();

                verify(ocrJobStateService).deadLetterPayload(argThat(payload ->
                                recordId.toString().equals(payload.get("recordId"))
                                                && "1-0".equals(payload.get("streamRecordId"))
                                                && "3".equals(payload.get("consumerFailureCount"))),
                                eq("consumer_processing_error"),
                                eq(3));
                verify(redisTemplate).delete("ocr:consumer-failures:1-0");
                verify(streamOperations).acknowledge(eq("ocr.events"), eq("ocr-consumers"), any(RecordId.class));
        }
}
