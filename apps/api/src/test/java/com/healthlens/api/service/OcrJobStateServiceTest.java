package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.entity.OcrDeadLetter;
import com.healthlens.api.entity.OcrJobExecution;
import com.healthlens.api.entity.OcrJobState;
import com.healthlens.api.repository.OcrDeadLetterRepository;
import com.healthlens.api.repository.OcrJobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrJobStateServiceTest {

    @Mock private OcrJobExecutionRepository jobRepository;
    @Mock private OcrDeadLetterRepository deadLetterRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HealthRecordService healthRecordService;
    @Mock private StreamOperations<String, Object, Object> streamOperations;
    @Mock private AuditEventRecorder auditEventRecorder;

    private OcrJobStateService service;
    private final Instant now = Instant.parse("2026-05-17T05:00:00Z");

    @BeforeEach
    void setUp() {
        service = new OcrJobStateService(
                jobRepository,
                deadLetterRepository,
                redisTemplate,
                new ObjectMapper(),
                healthRecordService,
                Clock.fixed(now, ZoneOffset.UTC),
                "ocr.events",
                2,
                1000L
        );
        service.setAuditEventRecorder(auditEventRecorder);
        lenient().when(jobRepository.save(any(OcrJobExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("startAttempt persists processing state and increments attempt")
    void startAttempt_newJob_persistsProcessingState() {
        UUID recordId = UUID.randomUUID();
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.empty());

        OcrJobStateService.AttemptDecision decision = service.startAttempt(new OcrJobStateService.OcrJobMetadata(
                recordId,
                "job-1",
                "file-key",
                "idem",
                "corr-1",
                Map.of("recordId", recordId.toString(), "fileKey", "file-key", "downloadUrl", "https://signed")
        ));

        ArgumentCaptor<OcrJobExecution> captor = ArgumentCaptor.forClass(OcrJobExecution.class);
        verify(jobRepository).save(captor.capture());
        assertThat(decision.duplicateTerminalState()).isFalse();
        assertThat(decision.attempt()).isEqualTo(1);
        assertThat(captor.getValue().getState()).isEqualTo(OcrJobState.PROCESSING);
        assertThat(captor.getValue().getPayload()).contains("fileKey").doesNotContain("downloadUrl");
    }

    @Test
    @DisplayName("succeeded idempotency key short-circuits duplicate delivery")
    void startAttempt_succeededJob_returnsDuplicateDecision() {
        OcrJobExecution existing = job("idem", 1, OcrJobState.SUCCEEDED);
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.of(existing));

        OcrJobStateService.AttemptDecision decision = service.startAttempt(metadata("idem"));

        assertThat(decision.duplicateTerminalState()).isTrue();
        assertThat(decision.state()).isEqualTo(OcrJobState.SUCCEEDED);
    }

    @Test
    @DisplayName("processing idempotency key short-circuits duplicate retry delivery without increment")
    void startAttempt_processingJob_skipsDuplicateInFlightDelivery() {
        OcrJobExecution existing = job("idem", 1, OcrJobState.PROCESSING);
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.of(existing));

        OcrJobStateService.AttemptDecision decision = service.startAttempt(metadata("idem"));

        assertThat(decision.shouldSkipProcessing()).isTrue();
        assertThat(decision.attempt()).isEqualTo(1);
        assertThat(existing.getAttemptCount()).isEqualTo(1);
        verify(jobRepository, never()).save(any(OcrJobExecution.class));
    }

    @Test
    @DisplayName("retryable failure stores backoff when attempts remain")
    void markRetryableOrDeadLetter_attemptsRemain_setsBackoff() {
        OcrJobExecution existing = job("idem", 1, OcrJobState.PROCESSING);
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.of(existing));

        OcrJobStateService.RetryDecision decision = service.markRetryableOrDeadLetter("idem", "provider_timeout");

        assertThat(decision.deadLettered()).isFalse();
        assertThat(existing.getState()).isEqualTo(OcrJobState.FAILED_RETRYABLE);
        assertThat(existing.getNextRetryAt()).isEqualTo(now.plusSeconds(1));
        assertThat(existing.getLastFailureReason()).isEqualTo("provider_timeout");
        verify(auditEventRecorder).recordAnonymous(
                eq(AuditActions.OCR_JOB_FAILED_RETRYABLE),
                eq(AuditResourceTypes.OCR_JOB),
                eq(existing.getRecordId()),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("successful OCR audit uses actor parsed from health record file key")
    void completeSucceeded_withHealthRecordFileKey_recordsAttributedAudit() {
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        OcrJobExecution existing = job("idem", 1, OcrJobState.PROCESSING);
        existing.setRecordId(recordId);
        existing.setFileKey("health-records/%s/%s/%s/original.jpg".formatted(actorId, profileId, recordId));
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.of(existing));

        service.completeSucceeded(recordId, "{}", null, false, "idem");

        verify(auditEventRecorder).recordEvent(
                eq(actorId),
                eq(AuditActions.OCR_JOB_SUCCEEDED),
                eq(AuditResourceTypes.OCR_JOB),
                eq(recordId),
                any(Map.class)
        );
        verify(auditEventRecorder, never()).recordAnonymous(
                eq(AuditActions.OCR_JOB_SUCCEEDED),
                eq(AuditResourceTypes.OCR_JOB),
                eq(recordId),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("final retryable failure moves job to DLQ with attempts and correlation")
    void markRetryableOrDeadLetter_maxAttempts_movesToDlq() {
        OcrJobExecution existing = job("idem", 2, OcrJobState.PROCESSING);
        existing.setPayload("{\"recordId\":\"" + existing.getRecordId() + "\"}");
        when(jobRepository.findByIdempotencyKey("idem")).thenReturn(Optional.of(existing));

        OcrJobStateService.RetryDecision decision = service.markRetryableOrDeadLetter("idem", "provider_timeout");

        ArgumentCaptor<OcrDeadLetter> captor = ArgumentCaptor.forClass(OcrDeadLetter.class);
        verify(deadLetterRepository).save(captor.capture());
        assertThat(decision.deadLettered()).isTrue();
        assertThat(existing.getState()).isEqualTo(OcrJobState.DEAD_LETTERED);
        assertThat(captor.getValue().getAttempts()).isEqualTo(2);
        assertThat(captor.getValue().getFailureCategory()).isEqualTo("provider_timeout");
        assertThat(captor.getValue().getCorrelationId()).isEqualTo("corr-1");
        verify(auditEventRecorder, times(1)).recordAnonymous(
                eq(AuditActions.OCR_JOB_DEAD_LETTERED),
                eq(AuditResourceTypes.OCR_JOB),
                eq(existing.getRecordId()),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("due retry jobs are re-enqueued and reset to queued")
    void enqueueDueRetries_requeuesDueJobs() {
        OcrJobExecution existing = job("idem", 1, OcrJobState.FAILED_RETRYABLE);
        existing.setPayload("{\"recordId\":\"" + existing.getRecordId() + "\",\"fileKey\":\"file-key\",\"mimeType\":\"image/jpeg\"}");
        existing.setNextRetryAt(now.minusSeconds(1));
        when(jobRepository.findTop50ByStateAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                OcrJobState.FAILED_RETRYABLE,
                now
        )).thenReturn(List.of(existing));
        when(jobRepository.claimDueRetry(eq(existing.getId()), eq(OcrJobState.FAILED_RETRYABLE), eq(now), any(Instant.class)))
                .thenReturn(1);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        service.enqueueDueRetries();

        verify(streamOperations).add(eq("ocr.events"), any(Map.class));
        assertThat(existing.getState()).isEqualTo(OcrJobState.QUEUED);
        assertThat(existing.getNextRetryAt()).isNull();
    }


    @Test
    @DisplayName("due retry not enqueued when another scheduler already claimed it")
    void enqueueDueRetries_claimLost_skipsRedisAdd() {
        OcrJobExecution existing = job("idem", 1, OcrJobState.FAILED_RETRYABLE);
        existing.setNextRetryAt(now.minusSeconds(1));
        when(jobRepository.findTop50ByStateAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
                OcrJobState.FAILED_RETRYABLE,
                now
        )).thenReturn(List.of(existing));
        when(jobRepository.claimDueRetry(eq(existing.getId()), eq(OcrJobState.FAILED_RETRYABLE), eq(now), any(Instant.class)))
                .thenReturn(0);

        service.enqueueDueRetries();

        verify(redisTemplate, never()).opsForStream();
    }

    private OcrJobStateService.OcrJobMetadata metadata(String idempotencyKey) {
        UUID recordId = UUID.randomUUID();
        return new OcrJobStateService.OcrJobMetadata(
                recordId,
                "job-1",
                "file-key",
                idempotencyKey,
                "corr-1",
                Map.of("recordId", recordId.toString(), "fileKey", "file-key")
        );
    }

    private OcrJobExecution job(String idempotencyKey, int attempts, OcrJobState state) {
        OcrJobExecution job = new OcrJobExecution();
        job.setId(UUID.randomUUID());
        job.setRecordId(UUID.randomUUID());
        job.setJobId("job-1");
        job.setFileKey("file-key");
        job.setIdempotencyKey(idempotencyKey);
        job.setAttemptCount(attempts);
        job.setState(state);
        job.setCorrelationId("corr-1");
        job.setPayload("{\"recordId\":\"" + job.getRecordId() + "\",\"fileKey\":\"file-key\"}");
        return job;
    }
}
