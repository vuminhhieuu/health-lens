package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.entity.OcrDeadLetter;
import com.healthlens.api.entity.OcrJobExecution;
import com.healthlens.api.entity.OcrJobState;
import com.healthlens.api.events.ocr.OcrJobEvent;
import com.healthlens.api.events.ocr.OcrJobEventPublisher;
import com.healthlens.api.repository.OcrDeadLetterRepository;
import com.healthlens.api.repository.OcrJobExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class OcrJobStateService {

    private static final Set<String> SAFE_PAYLOAD_FIELDS = Set.of(
            "recordId",
            "jobId",
            "fileKey",
            "fileVersion",
            "mimeType",
            "profileId",
            "correlationId",
            "attempt",
            "idempotencyKey",
            "streamRecordId",
            "consumerFailureCount"
    );

    private final OcrJobExecutionRepository jobRepository;
    private final OcrDeadLetterRepository deadLetterRepository;
    private final ObjectMapper objectMapper;
    private final HealthRecordService healthRecordService;
    private final Clock clock;
    private final OcrJobEventPublisher ocrJobEventPublisher;
    private final int maxAttempts;
    private final Duration initialBackoff;
    private AuditEventRecorder auditEventRecorder;

    public OcrJobStateService(
            OcrJobExecutionRepository jobRepository,
            OcrDeadLetterRepository deadLetterRepository,
            OcrJobEventPublisher ocrJobEventPublisher,
            ObjectMapper objectMapper,
            HealthRecordService healthRecordService,
            Clock clock,
            @org.springframework.beans.factory.annotation.Value("${app.ocr.retry.max-attempts:3}") int maxAttempts,
            @org.springframework.beans.factory.annotation.Value("${app.ocr.retry.initial-backoff-ms:30000}") long initialBackoffMs
    ) {
        this.jobRepository = jobRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.ocrJobEventPublisher = ocrJobEventPublisher;
        this.objectMapper = objectMapper;
        this.healthRecordService = healthRecordService;
        this.clock = clock;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoff = Duration.ofMillis(Math.max(1L, initialBackoffMs));
    }

    @Transactional
    public AttemptDecision startAttempt(OcrJobMetadata metadata) {
        OcrJobExecution job = jobRepository.findByIdempotencyKey(metadata.idempotencyKey()).orElseGet(() -> {
            OcrJobExecution created = new OcrJobExecution();
            created.setRecordId(metadata.recordId());
            created.setJobId(metadata.jobId());
            created.setFileKey(metadata.fileKey());
            created.setIdempotencyKey(metadata.idempotencyKey());
            return created;
        });

        if (isTerminalState(job.getState()) || isInFlightState(job.getState())) {
            return new AttemptDecision(true, job.getAttemptCount(), job.getState());
        }

        job.setRecordId(metadata.recordId());
        job.setJobId(metadata.jobId());
        job.setFileKey(metadata.fileKey());
        job.setCorrelationId(metadata.correlationId());
        job.setPayload(toJson(sanitizedPayload(metadata.payload())));
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setState(OcrJobState.PROCESSING);
        job.setNextRetryAt(null);
        jobRepository.save(job);
        return new AttemptDecision(false, job.getAttemptCount(), job.getState());
    }

    @Transactional
    public void completeSucceeded(
            UUID recordId,
            String rawOcrJson,
            OcrService.OcrExtractionResult parsedData,
            boolean hasLowConfidenceMetrics,
            String idempotencyKey
    ) {
        healthRecordService.markOcrCompleted(recordId, rawOcrJson, parsedData, hasLowConfidenceMetrics);
        OcrJobExecution job = loadJob(idempotencyKey);
        job.setState(OcrJobState.SUCCEEDED);
        job.setLastFailureReason(null);
        job.setNextRetryAt(null);
        jobRepository.save(job);
        log.info("[OcrJobState] OCR job succeeded. recordId={} jobId={} attempt={} correlationId={}",
                job.getRecordId(), job.getJobId(), job.getAttemptCount(), job.getCorrelationId());
        recordOcrAudit(AuditActions.OCR_JOB_SUCCEEDED, job, Map.of(
                "state", job.getState().name(),
                "attempt", job.getAttemptCount(),
                "hasLowConfidenceMetrics", hasLowConfidenceMetrics
        ));
    }

    @Transactional
    public RetryDecision markRetryableOrDeadLetter(String idempotencyKey, String failureReason) {
        OcrJobExecution job = loadJob(idempotencyKey);
        String sanitizedReason = sanitizeReason(failureReason);
        job.setLastFailureReason(sanitizedReason);
        if (job.getAttemptCount() >= maxAttempts) {
            job.setState(OcrJobState.DEAD_LETTERED);
            job.setNextRetryAt(null);
            jobRepository.save(job);
            persistDeadLetter(job, sanitizedReason);
            log.error("[OcrJobState] OCR job moved to DLQ. recordId={} jobId={} attempts={} reason={} correlationId={}",
                    job.getRecordId(), job.getJobId(), job.getAttemptCount(), sanitizedReason, job.getCorrelationId());
            recordOcrAudit(AuditActions.OCR_JOB_DEAD_LETTERED, job, Map.of(
                    "state", job.getState().name(),
                    "attempt", job.getAttemptCount(),
                    "reason", sanitizedReason
            ));
            return new RetryDecision(true, job.getAttemptCount(), null);
        }

        Instant nextRetryAt = Instant.now(clock).plus(backoffForAttempt(job.getAttemptCount()));
        job.setState(OcrJobState.FAILED_RETRYABLE);
        job.setNextRetryAt(nextRetryAt);
        jobRepository.save(job);
        log.warn("[OcrJobState] OCR job scheduled for retry. recordId={} jobId={} attempt={} nextRetryAt={} reason={} correlationId={}",
                job.getRecordId(), job.getJobId(), job.getAttemptCount(), nextRetryAt, sanitizedReason, job.getCorrelationId());
        recordOcrAudit(AuditActions.OCR_JOB_FAILED_RETRYABLE, job, Map.of(
                "state", job.getState().name(),
                "attempt", job.getAttemptCount(),
                "reason", sanitizedReason,
                "nextRetryAt", nextRetryAt.toString()
        ));
        return new RetryDecision(false, job.getAttemptCount(), nextRetryAt);
    }

    @Transactional
    public void markTerminal(String idempotencyKey, String failureReason) {
        OcrJobExecution job = loadJob(idempotencyKey);
        String sanitizedReason = sanitizeReason(failureReason);
        job.setState(OcrJobState.FAILED_TERMINAL);
        job.setLastFailureReason(sanitizedReason);
        job.setNextRetryAt(null);
        jobRepository.save(job);
        log.warn("[OcrJobState] OCR job terminally failed. recordId={} jobId={} attempt={} reason={} correlationId={}",
                job.getRecordId(), job.getJobId(), job.getAttemptCount(), sanitizedReason, job.getCorrelationId());
        recordOcrAudit(AuditActions.OCR_JOB_FAILED_TERMINAL, job, Map.of(
                "state", job.getState().name(),
                "attempt", job.getAttemptCount(),
                "reason", sanitizedReason
        ));
    }

    @Transactional
    public void deadLetterMalformedPayload(Map<String, Object> payload, String failureReason) {
        deadLetterPayload(payload, failureReason, 0);
    }

    @Transactional
    public void deadLetterPayload(Map<String, Object> payload, String failureReason, int attempts) {
        Map<String, String> sanitizedPayload = sanitizedPayload(payload);
        OcrDeadLetter deadLetter = new OcrDeadLetter();
        deadLetter.setRecordId(parseUuidOrNull(sanitizedPayload.get("recordId")));
        deadLetter.setJobId(truncate(sanitizedPayload.get("jobId"), 120));
        deadLetter.setIdempotencyKey(truncate(sanitizedPayload.get("idempotencyKey"), 800));
        deadLetter.setSanitizedPayload(toJson(sanitizedPayload));
        deadLetter.setFailureCategory(sanitizeReason(failureReason));
        deadLetter.setAttempts(Math.max(0, attempts));
        deadLetter.setCorrelationId(truncate(sanitizedPayload.get("correlationId"), 120));
        deadLetterRepository.save(deadLetter);
        if (auditEventRecorder != null) {
            auditEventRecorder.recordAnonymous(
                    AuditActions.OCR_JOB_DEAD_LETTERED,
                    AuditResourceTypes.OCR_JOB,
                    deadLetter.getRecordId(),
                    Map.of(
                            "jobId", String.valueOf(deadLetter.getJobId()),
                            "attempt", deadLetter.getAttempts(),
                            "reason", deadLetter.getFailureCategory(),
                            "correlationId", String.valueOf(deadLetter.getCorrelationId())
                    )
            );
        }
    }

    @Autowired(required = false)
    void setAuditEventRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    @Scheduled(fixedDelayString = "${app.ocr.retry.dispatch-delay-ms:10000}")
    public void enqueueDueRetries() {
        Instant now = Instant.now(clock);
        List<OcrJobExecution> dueJobs;
        try {
            dueJobs = jobRepository.findTop50ByStateAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                    OcrJobState.FAILED_RETRYABLE,
                    now
            );
        } catch (Exception ex) {
            log.debug("[OcrJobState] Retry scan skipped because storage is unavailable", ex);
            return;
        }
        for (OcrJobExecution job : dueJobs) {
            enqueueRetry(job, now);
        }
    }

    private void enqueueRetry(OcrJobExecution job, Instant now) {
        try {
            Instant claimUntil = now.plus(backoffForAttempt(job.getAttemptCount() + 1));
            int claimed = jobRepository.claimDueRetry(job.getId(), OcrJobState.FAILED_RETRYABLE, now, claimUntil);
            if (claimed == 0) {
                return;
            }

            OcrJobEvent event = retryEvent(job);
            ocrJobEventPublisher.publish(event);
            job.setState(OcrJobState.QUEUED);
            job.setNextRetryAt(null);
            jobRepository.save(job);
            log.info("[OcrJobState] Re-enqueued OCR retry. recordId={} jobId={} nextAttempt={} correlationId={}",
                    job.getRecordId(), job.getJobId(), job.getAttemptCount() + 1, job.getCorrelationId());
        } catch (Exception ex) {
            log.warn("[OcrJobState] Failed to re-enqueue OCR retry. recordId={} jobId={}",
                    job.getRecordId(), job.getJobId(), ex);
        }
    }

    private OcrJobExecution loadJob(String idempotencyKey) {
        return jobRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("OCR job state does not exist for idempotency key"));
    }

    private boolean isTerminalState(OcrJobState state) {
        return state == OcrJobState.SUCCEEDED
                || state == OcrJobState.FAILED_TERMINAL
                || state == OcrJobState.DEAD_LETTERED;
    }

    private Duration backoffForAttempt(int attemptCount) {
        long multiplier = 1L << Math.max(0, Math.min(attemptCount - 1, 5));
        return initialBackoff.multipliedBy(multiplier);
    }

    private void persistDeadLetter(OcrJobExecution job, String failureCategory) {
        OcrDeadLetter deadLetter = new OcrDeadLetter();
        deadLetter.setRecordId(job.getRecordId());
        deadLetter.setJobId(job.getJobId());
        deadLetter.setIdempotencyKey(job.getIdempotencyKey());
        deadLetter.setSanitizedPayload(job.getPayload() == null ? "{}" : job.getPayload());
        deadLetter.setFailureCategory(failureCategory);
        deadLetter.setAttempts(job.getAttemptCount());
        deadLetter.setCorrelationId(job.getCorrelationId());
        deadLetterRepository.save(deadLetter);
    }

    private void recordOcrAudit(String action, OcrJobExecution job, Map<String, ?> details) {
        if (auditEventRecorder == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", job.getJobId());
        payload.put("recordId", job.getRecordId() == null ? null : job.getRecordId().toString());
        payload.put("fileKey", job.getFileKey());
        payload.put("correlationId", job.getCorrelationId());
        payload.putAll(details);
        UUID actorId = resolveActorId(job.getFileKey());
        if (actorId != null) {
            auditEventRecorder.recordEvent(actorId, action, AuditResourceTypes.OCR_JOB, job.getRecordId(), payload);
            return;
        }
        auditEventRecorder.recordAnonymous(action, AuditResourceTypes.OCR_JOB, job.getRecordId(), payload);
    }

    private UUID resolveActorId(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }
        String[] parts = fileKey.split("/");
        if (parts.length < 2 || !"health-records".equals(parts[0])) {
            return null;
        }
        try {
            return UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private OcrJobEvent retryEvent(OcrJobExecution job) throws JsonProcessingException {
        Map<String, String> payload = objectMapper.readValue(job.getPayload(), new TypeReference<>() {});
        return new OcrJobEvent(
                job.getJobId(),
                job.getCorrelationId(),
                job.getRecordId(),
                job.getFileKey(),
                payload.getOrDefault("fileVersion", OcrJobEvent.DEFAULT_FILE_VERSION),
                payload.get("mimeType"),
                parseUuidOrNull(payload.get("profileId")),
                job.getAttemptCount() + 1
        );
    }

    private Map<String, String> sanitizedPayload(Map<String, Object> payload) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (key == null || value == null || !SAFE_PAYLOAD_FIELDS.contains(key)) {
                return;
            }
            sanitized.put(key, value.toString());
        });
        return sanitized;
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private UUID parseUuidOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "processing_error";
        }
        return reason.trim().toLowerCase().replaceAll("[^a-z0-9_\\-]", "_");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record OcrJobMetadata(
            UUID recordId,
            String jobId,
            String fileKey,
            String idempotencyKey,
            String correlationId,
            Map<String, Object> payload
    ) {
    }

    private boolean isInFlightState(OcrJobState state) {
        return state == OcrJobState.PROCESSING;
    }

    public record AttemptDecision(boolean duplicateTerminalState, int attempt, OcrJobState state) {
        public boolean shouldSkipProcessing() {
            return duplicateTerminalState;
        }
    }

    public record RetryDecision(boolean deadLettered, int attempts, Instant nextRetryAt) {
    }
}
