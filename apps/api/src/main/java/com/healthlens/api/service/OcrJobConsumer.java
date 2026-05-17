package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.OcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class OcrJobConsumer {

    static final String DEFAULT_FILE_VERSION = "unversioned";

    private final StringRedisTemplate redisTemplate;
    private final StorageService storageService;
    private final OcrService ocrService;
    private final HealthRecordService healthRecordService;
    private final OcrJobStateService ocrJobStateService;
    private final ObjectMapper objectMapper;
    private final String ocrStream;
    private final String consumerGroup;
    private final String consumerName;
    @Value("${app.ocr.confidence.medium-threshold:0.50}")
    private float ocrFailureThreshold = 0.50f;
    @Value("${app.ocr.confidence.high-threshold:0.85}")
    private float ocrReviewRequiredThreshold = 0.85f;
    @Value("${app.ocr.consumer.max-failures:3}")
    private int maxConsumerFailures = 3;
    @Value("${app.ocr.consumer.failure-counter-ttl:PT1H}")
    private Duration consumerFailureCounterTtl = Duration.ofHours(1);

    public OcrJobConsumer(
            StringRedisTemplate redisTemplate,
            StorageService storageService,
            OcrService ocrService,
            HealthRecordService healthRecordService,
            OcrJobStateService ocrJobStateService,
            ObjectMapper objectMapper,
            @Value("${app.stream.ocr-events:ocr.events}") String ocrStream,
            @Value("${app.stream.ocr-consumer-group:ocr-consumers}") String consumerGroup,
            @Value("${app.stream.ocr-consumer-name:api-ocr-consumer}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.storageService = storageService;
        this.ocrService = ocrService;
        this.healthRecordService = healthRecordService;
        this.ocrJobStateService = ocrJobStateService;
        this.objectMapper = objectMapper;
        this.ocrStream = ocrStream;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
        ensureConsumerGroup();
    }

    @Scheduled(fixedDelayString = "${app.stream.ocr-poll-delay-ms:1000}")
    public void consume() {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        List<MapRecord<String, Object, Object>> records;
        try {
            records = readPendingThenNew(streamOps);
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("NOGROUP")) {
                ensureConsumerGroup();
            }
            return;
        }

        if (records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                if (handleRecord(record)) {
                    streamOps.acknowledge(ocrStream, consumerGroup, record.getId());
                }
            } catch (Exception ex) {
                if (deadLetterUnexpectedFailure(record, ex)) {
                    streamOps.acknowledge(ocrStream, consumerGroup, record.getId());
                }
            }
        }
    }

    private List<MapRecord<String, Object, Object>> readPendingThenNew(
            StreamOperations<String, Object, Object> streamOps) {
        List<MapRecord<String, Object, Object>> records = new ArrayList<>();
        List<MapRecord<String, Object, Object>> pendingRecords = streamOps.read(
                Consumer.from(consumerGroup, consumerName),
                StreamReadOptions.empty().count(10),
                StreamOffset.create(ocrStream, ReadOffset.from("0")));
        if (pendingRecords != null && !pendingRecords.isEmpty()) {
            records.addAll(pendingRecords);
            return records;
        }

        List<MapRecord<String, Object, Object>> newRecords = streamOps.read(
                Consumer.from(consumerGroup, consumerName),
                StreamReadOptions.empty()
                        .count(10)
                        .block(Duration.ofMillis(500)),
                StreamOffset.create(ocrStream, ReadOffset.lastConsumed()));
        if (newRecords != null) {
            records.addAll(newRecords);
        }
        return records;
    }

    private boolean handleRecord(MapRecord<String, Object, Object> record) {
        Map<String, Object> payload = normalizePayload(record.getValue());
        String recordIdRaw = valueAsString(payload.get("recordId"));
        if (recordIdRaw.isBlank()) {
            // Ignore bootstrap stream records without OCR payload.
            return true;
        }

        UUID recordId;
        try {
            recordId = UUID.fromString(recordIdRaw);
        } catch (IllegalArgumentException ex) {
            ocrJobStateService.deadLetterMalformedPayload(payload, "invalid_record_id");
            return true;
        }

        String fileKey = valueAsString(payload.get("fileKey"));
        String jobId = boundedValue(payload, "jobId", record.getId().getValue(), 120);
        payload.put("jobId", jobId);
        String correlationId = boundedValue(payload, "correlationId", jobId, 120);
        payload.put("correlationId", correlationId);
        String fileVersion = boundedValue(payload, "fileVersion", DEFAULT_FILE_VERSION, 120);
        payload.put("fileVersion", fileVersion);
        String idempotencyKey = buildIdempotencyKey(recordId, jobId, fileKey, fileVersion);
        payload.put("idempotencyKey", idempotencyKey);

        if (fileKey.isBlank()) {
            ocrJobStateService.deadLetterMalformedPayload(payload, "missing_file_key");
            healthRecordService.markOcrFailed(recordId, "missing_file_key");
            return true;
        }
        if (fileKey.length() > 500) {
            ocrJobStateService.deadLetterMalformedPayload(payload, "file_key_too_long");
            healthRecordService.markOcrFailed(recordId, "file_key_too_long");
            return true;
        }

        OcrJobStateService.AttemptDecision attempt = ocrJobStateService
                .startAttempt(new OcrJobStateService.OcrJobMetadata(
                        recordId,
                        jobId,
                        fileKey,
                        idempotencyKey,
                        correlationId,
                        payload));
        if (attempt.shouldSkipProcessing()) {
            log.info(
                    "[OcrJobConsumer] Duplicate OCR delivery skipped. recordId={} jobId={} state={} correlationId={}",
                    recordId, jobId, attempt.state(), correlationId);
            return true;
        }

        String mimeType = resolveMimeType(valueAsString(payload.get("mimeType")));
        if (!isSupportedMimeType(mimeType)) {
            log.warn("[OcrJobConsumer] Unsupported OCR MIME type. recordId={} fileKey={} mimeType={}",
                    recordId,
                    fileKey,
                    mimeType);
            return persistTerminalFailure(recordId, idempotencyKey,
                    mimeType.isBlank() ? "missing_mime_type" : "unsupported_mime_type");
        }

        String downloadUrl = storageService.generateInternalDownloadUrl(fileKey, Duration.ofMinutes(5));
        OcrService.OcrProcessingResult processingResult;
        try {
            if ("application/pdf".equals(mimeType)) {
                processingResult = ocrService.processPdfBytes(
                        storageService.downloadObjectBytes(fileKey),
                        downloadUrl,
                        mimeType);
            } else {
                processingResult = ocrService.processDocument(downloadUrl, mimeType);
            }
        } catch (ResourceAccessException ex) {
            return persistRetryableFailure(recordId, idempotencyKey, "provider_timeout");
        } catch (Exception ex) {
            return persistRetryableFailure(recordId, idempotencyKey, "provider_transient_error");
        }

        try {
            return persistSuccessfulOcr(recordId, jobId, correlationId, idempotencyKey, attempt, processingResult);
        } catch (Exception ex) {
            log.warn("[OcrJobConsumer] Post-provider OCR persistence failed. recordId={} jobId={} correlationId={}",
                    recordId, jobId, correlationId, ex);
            return persistRetryableFailure(recordId, idempotencyKey, "persistence_error");
        }
    }

    private boolean persistSuccessfulOcr(
            UUID recordId,
            String jobId,
            String correlationId,
            String idempotencyKey,
            OcrJobStateService.AttemptDecision attempt,
            OcrService.OcrProcessingResult processingResult) {
        OcrResult result = processingResult.result();
        log.info(
                "[OcrJobConsumer] OCR route selected. recordId={} jobId={} correlationId={} mimeType={} route={} provider={} attempt={}",
                recordId,
                jobId,
                correlationId,
                processingResult.mimeType(),
                processingResult.route(),
                processingResult.provider(),
                attempt.attempt());

        float failureThreshold = normalizedFailureThreshold();
        float reviewThreshold = normalizedReviewThreshold();
        if ("all-providers-failed".equals(result.getSource())) {
            return persistRetryableFailure(recordId, idempotencyKey, resolveFailureReason(result, processingResult));
        }
        if (result.getConfidence() < failureThreshold) {
            return persistTerminalFailure(recordId, idempotencyKey, "low_confidence");
        }

        OcrService.OcrExtractionResult parsedData = ocrService.parseMetrics(result.getOrderedTextForParser(),
                result.getConfidence());
        boolean hasLowConfidenceMetrics = result.getConfidence() < reviewThreshold;

        Map<String, Object> rawOcrPayload = new LinkedHashMap<>();
        rawOcrPayload.put("text", result.getText());
        rawOcrPayload.put("confidence", result.getConfidence());
        rawOcrPayload.put("source", result.getSource());
        rawOcrPayload.put("language", result.getLanguage());
        rawOcrPayload.put("processingTimeMs", result.getProcessingTimeMs());
        rawOcrPayload.put("hasLowConfidenceMetrics", hasLowConfidenceMetrics);
        rawOcrPayload.put("mimeType", processingResult.mimeType());
        rawOcrPayload.put("route", processingResult.route());
        rawOcrPayload.put("provider", processingResult.provider());
        rawOcrPayload.put("recordId", recordId.toString());
        rawOcrPayload.put("jobId", jobId);
        rawOcrPayload.put("correlationId", correlationId);
        rawOcrPayload.put("idempotencyKey", idempotencyKey);
        rawOcrPayload.put("attempt", attempt.attempt());
        rawOcrPayload.put("pages", processingResult.pages());
        String rawOcrJson;
        try {
            rawOcrJson = objectMapper.writeValueAsString(rawOcrPayload);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize OCR payload", ex);
        }
        ocrJobStateService.completeSucceeded(recordId, rawOcrJson, parsedData, hasLowConfidenceMetrics, idempotencyKey);
        return true;
    }

    private boolean persistRetryableFailure(UUID recordId, String idempotencyKey, String reason) {
        OcrJobStateService.RetryDecision retryDecision = ocrJobStateService.markRetryableOrDeadLetter(idempotencyKey,
                reason);
        if (retryDecision.deadLettered()) {
            try {
                healthRecordService.markOcrFailed(recordId, reason);
            } catch (Exception ex) {
                log.warn(
                        "[OcrJobConsumer] OCR job is DLQ'd but health record failure reason could not be updated. recordId={}",
                        recordId, ex);
            }
        }
        return true;
    }

    private boolean persistTerminalFailure(UUID recordId, String idempotencyKey, String reason) {
        healthRecordService.markOcrFailed(recordId, reason);
        ocrJobStateService.markTerminal(idempotencyKey, reason);
        return true;
    }

    private boolean deadLetterUnexpectedFailure(
            MapRecord<String, Object, Object> record,
            Exception failure) {
        long failures = incrementConsumerFailureCount(record);
        if (failures < Math.max(1, maxConsumerFailures)) {
            log.error("[OcrJobConsumer] Failed processing record {}; leaving message pending. failureCount={}",
                    record.getId(), failures, failure);
            return false;
        }

        try {
            Map<String, Object> payload = normalizePayload(record.getValue());
            payload.put("streamRecordId", record.getId().getValue());
            payload.put("consumerFailureCount", Long.toString(failures));
            ocrJobStateService.deadLetterPayload(payload, "consumer_processing_error", (int) failures);
            redisTemplate.delete(consumerFailureKey(record));
            log.error("[OcrJobConsumer] Moved repeatedly failing OCR stream entry to DLQ. recordId={} failures={}",
                    record.getId(), failures, failure);
            return true;
        } catch (Exception dlqFailure) {
            log.error("[OcrJobConsumer] Failed processing record {} and could not persist consumer DLQ; leaving pending",
                    record.getId(), dlqFailure);
            return false;
        }
    }

    private long incrementConsumerFailureCount(MapRecord<String, Object, Object> record) {
        try {
            String key = consumerFailureKey(record);
            Long failures = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, consumerFailureCounterTtl);
            return failures == null ? 1L : failures;
        } catch (Exception counterFailure) {
            log.warn("[OcrJobConsumer] Could not increment OCR consumer failure counter for record={}", record.getId(),
                    counterFailure);
            return 1L;
        }
    }

    private String consumerFailureKey(MapRecord<String, Object, Object> record) {
        return "ocr:consumer-failures:" + record.getId().getValue();
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
                log.info("[OcrJobConsumer] Consumer group already exists. group={} stream={}", consumerGroup,
                        ocrStream);
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

    private Map<String, Object> normalizePayload(Map<Object, Object> rawPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        rawPayload.forEach((key, value) -> payload.put(valueAsString(key), value));
        return payload;
    }

    private String boundedValue(Map<String, Object> payload, String key, String fallback, int maxLength) {
        String value = valueAsString(payload.get(key));
        if (value.isBlank()) {
            value = fallback;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String valueAsString(Object value) {
        return value == null ? "" : value.toString();
    }

    private String buildIdempotencyKey(UUID recordId, String jobId, String fileKey, String fileVersion) {
        return recordId + ":" + jobId + ":" + fileKey + ":" + fileVersion;
    }

    private String resolveMimeType(String payloadMimeType) {
        String normalizedPayloadMimeType = normalizeMimeType(payloadMimeType);
        if (!normalizedPayloadMimeType.isBlank()) {
            return normalizedPayloadMimeType;
        }
        return "";
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        int parametersIndex = mimeType.indexOf(';');
        String baseType = parametersIndex >= 0 ? mimeType.substring(0, parametersIndex) : mimeType;
        return baseType.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isSupportedMimeType(String mimeType) {
        return "image/jpeg".equals(mimeType)
                || "image/png".equals(mimeType)
                || "application/pdf".equals(mimeType);
    }

    private String resolveFailureReason(OcrResult result, OcrService.OcrProcessingResult processingResult) {
        if ("all-providers-failed".equals(result.getSource())) {
            return processingResult.route().startsWith("pdf")
                    ? "pdf_processing_failed"
                    : "provider_timeout";
        }
        return "low_confidence";
    }

    private float normalizedFailureThreshold() {
        return Math.min(ocrFailureThreshold, ocrReviewRequiredThreshold);
    }

    private float normalizedReviewThreshold() {
        return Math.max(ocrFailureThreshold, ocrReviewRequiredThreshold);
    }
}
