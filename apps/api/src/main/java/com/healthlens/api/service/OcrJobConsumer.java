package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.OcrResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OcrJobConsumer {
    private static final Logger log = LoggerFactory.getLogger(OcrJobConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final HealthRecordService healthRecordService;
    private final ObjectMapper objectMapper;
    private final String ocrStream;
    private final String consumerGroup;
    private final String consumerName;
    @Value("${app.ocr.confidence.medium-threshold:0.50}")
    private float ocrFailureThreshold = 0.50f;
    @Value("${app.ocr.confidence.high-threshold:0.85}")
    private float ocrReviewRequiredThreshold = 0.85f;

    public OcrJobConsumer(
            StringRedisTemplate redisTemplate,
            StorageService storageService,
            OcrService ocrService,
            HealthRecordService healthRecordService,
            ObjectMapper objectMapper,
            @Value("${app.stream.ocr-events:ocr.events}") String ocrStream,
            @Value("${app.stream.ocr-consumer-group:ocr-consumers}") String consumerGroup,
            @Value("${app.stream.ocr-consumer-name:api-ocr-consumer}") String consumerName
    ) {
        this.redisTemplate = redisTemplate;
        this.storageService = storageService;
        this.ocrService = ocrService;
        this.healthRecordService = healthRecordService;
        this.objectMapper = objectMapper;
        this.ocrStream = ocrStream;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
        ensureConsumerGroup();
    }

    @Scheduled(fixedDelayString = "${app.stream.ocr-poll-delay-ms:1000}")
    @SuppressWarnings("unchecked")
    public void consume() {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        List<MapRecord<String, Object, Object>> records;
        try {
            records = streamOps.read(
                    Consumer.from(consumerGroup, consumerName),
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                            .count(10)
                            .block(Duration.ofMillis(500)),
                    StreamOffset.create(ocrStream, ReadOffset.lastConsumed())
            );
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("NOGROUP")) {
                ensureConsumerGroup();
            }
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handleRecord(record);
                streamOps.acknowledge(ocrStream, consumerGroup, record.getId());
            } catch (Exception ex) {
                log.error("[OcrJobConsumer] Failed processing record {}", record.getId(), ex);
                markFailedSafely(record);
                streamOps.acknowledge(ocrStream, consumerGroup, record.getId());
            }
        }
    }

    private void handleRecord(MapRecord<String, Object, Object> record) {
        String recordIdRaw = valueAsString(record.getValue().get("recordId"));
        if (recordIdRaw.isBlank()) {
            // Ignore bootstrap/legacy stream records without OCR payload.
            return;
        }
        UUID recordId = UUID.fromString(recordIdRaw);
        String fileKey = valueAsString(record.getValue().get("fileKey"));
        if (fileKey.isBlank()) {
            healthRecordService.markOcrFailed(recordId);
            return;
        }
        String downloadUrl = storageService.generateInternalDownloadUrl(fileKey, Duration.ofMinutes(5));

        try {
            OcrResult result = ocrService.processImage(downloadUrl);
            float failureThreshold = normalizedFailureThreshold();
            float reviewThreshold = normalizedReviewThreshold();
            if ("all-providers-failed".equals(result.getSource()) || result.getConfidence() < failureThreshold) {
                String reason = "all-providers-failed".equals(result.getSource()) ? "timeout" : "low_confidence";
                healthRecordService.markOcrFailed(recordId, reason);
                return;
            }
            OcrService.OcrExtractionResult parsedData = ocrService.parseMetrics(result.getText(), result.getConfidence());
            boolean hasLowConfidenceMetrics = result.getConfidence() < reviewThreshold;

            String rawOcrJson = objectMapper.writeValueAsString(Map.of(
                    "text", result.getText(),
                    "confidence", result.getConfidence(),
                    "source", result.getSource(),
                    "language", result.getLanguage(),
                    "processingTimeMs", result.getProcessingTimeMs(),
                    "hasLowConfidenceMetrics", hasLowConfidenceMetrics
            ));
            healthRecordService.markOcrCompleted(recordId, rawOcrJson, parsedData, hasLowConfidenceMetrics);
        } catch (Exception ex) {
            healthRecordService.markOcrFailed(recordId);
        }
    }

    private void ensureConsumerGroup() {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(ocrStream))) {
                redisTemplate.opsForStream().add(ocrStream, Map.of("_init", "1"));
            }
            redisTemplate.opsForStream().createGroup(ocrStream, ReadOffset.latest(), consumerGroup);
            log.info("[OcrJobConsumer] Created consumer group={} for stream={}", consumerGroup, ocrStream);
        } catch (Exception ex) {
            if (containsAnyMessage(ex, "BUSYGROUP")) {
                log.info("[OcrJobConsumer] Consumer group already exists. group={} stream={}", consumerGroup, ocrStream);
                return;
            }
            log.warn("[OcrJobConsumer] ensureConsumerGroup failed for stream={}", ocrStream, ex);
        }
    }

    private boolean containsAnyMessage(Throwable throwable, String keyword) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(keyword)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String valueAsString(Object value) {
        return value == null ? "" : value.toString();
    }

    private float normalizedFailureThreshold() {
        return Math.min(ocrFailureThreshold, ocrReviewRequiredThreshold);
    }

    private float normalizedReviewThreshold() {
        return Math.max(ocrFailureThreshold, ocrReviewRequiredThreshold);
    }

    private void markFailedSafely(MapRecord<String, Object, Object> record) {
        String recordIdRaw = valueAsString(record.getValue().get("recordId"));
        if (recordIdRaw.isBlank()) {
            return;
        }
        try {
            healthRecordService.markOcrFailed(UUID.fromString(recordIdRaw));
        } catch (Exception ex) {
            log.warn("[OcrJobConsumer] Cannot mark OCR failed for recordId={}", recordIdRaw, ex);
        }
    }
}
