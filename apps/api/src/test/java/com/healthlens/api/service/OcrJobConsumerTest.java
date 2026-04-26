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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
                "test-consumer"
        );
    }

    @Test
    @DisplayName("confidence 0.49 -> markOcrFailed low_confidence")
    void handleRecord_confidenceBelowFailureThreshold_marksFailed() {
        UUID recordId = UUID.randomUUID();
        when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey", "k"));
        when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class))).thenReturn("https://example.com/img");
        when(ocrService.processImage("https://example.com/img")).thenReturn(
                OcrResult.builder().text("x").confidence(0.49f).source("easyocr").language("vi").processingTimeMs(100).build()
        );

        ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

        verify(healthRecordService).markOcrFailed(recordId, "low_confidence");
        verify(healthRecordService, never()).markOcrCompleted(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("confidence 0.50 -> review_required with hasLowConfidenceMetrics=true")
    void handleRecord_confidenceAtMediumLowerBoundary_marksCompletedPartial() {
        UUID recordId = UUID.randomUUID();
        when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey", "k"));
        when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class))).thenReturn("https://example.com/img");
        when(ocrService.processImage("https://example.com/img")).thenReturn(
                OcrResult.builder().text("x").confidence(0.50f).source("easyocr").language("vi").processingTimeMs(100).build()
        );
        when(ocrService.parseMetrics("x", 0.50f)).thenReturn(new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

        ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

        verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class), any(OcrService.OcrExtractionResult.class), eq(true));
    }

    @Test
    @DisplayName("confidence 0.85 -> review_required with hasLowConfidenceMetrics=false")
    void handleRecord_confidenceAtHighBoundary_marksCompletedHigh() {
        UUID recordId = UUID.randomUUID();
        when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey", "k"));
        when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class))).thenReturn("https://example.com/img");
        when(ocrService.processImage("https://example.com/img")).thenReturn(
                OcrResult.builder().text("x").confidence(0.85f).source("easyocr").language("vi").processingTimeMs(100).build()
        );
        when(ocrService.parseMetrics("x", 0.85f)).thenReturn(new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));

        ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

        verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class), any(OcrService.OcrExtractionResult.class), eq(false));
    }

    @Test
    @DisplayName("all providers failed -> markOcrFailed timeout reason")
    void handleRecord_allProvidersFailed_marksTimeoutReason() {
        UUID recordId = UUID.randomUUID();
        when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey", "k"));
        when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class))).thenReturn("https://example.com/img");
        when(ocrService.processImage("https://example.com/img")).thenReturn(
                OcrResult.builder().text("").confidence(0.0f).source("all-providers-failed").language("unknown").processingTimeMs(0).build()
        );

        ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

        verify(healthRecordService).markOcrFailed(recordId, "timeout");
    }

    @Test
    @DisplayName("misordered thresholds are normalized before branching")
    void handleRecord_misorderedThresholds_normalizesBoundaries() {
        UUID recordId = UUID.randomUUID();
        when(mapRecord.getValue()).thenReturn(Map.of("recordId", recordId.toString(), "fileKey", "k"));
        when(storageService.generateInternalDownloadUrl(eq("k"), any(Duration.class))).thenReturn("https://example.com/img");
        when(ocrService.processImage("https://example.com/img")).thenReturn(
                OcrResult.builder().text("x").confidence(0.60f).source("easyocr").language("vi").processingTimeMs(100).build()
        );
        when(ocrService.parseMetrics("x", 0.60f)).thenReturn(new OcrService.OcrExtractionResult(null, null, null, null, java.util.List.of()));
        ReflectionTestUtils.setField(consumer, "ocrFailureThreshold", 0.85f);
        ReflectionTestUtils.setField(consumer, "ocrReviewRequiredThreshold", 0.50f);

        ReflectionTestUtils.invokeMethod(consumer, "handleRecord", mapRecord);

        verify(healthRecordService, never()).markOcrFailed(recordId, "low_confidence");
        verify(healthRecordService).markOcrCompleted(eq(recordId), any(String.class), any(OcrService.OcrExtractionResult.class), eq(true));
    }
}
