package com.healthlens.api.service;

import com.healthlens.api.activity.FailureReasonNormalizer;
import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.repository.UserActivityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserActivityEventRepository userActivityEventRepository;

    public UserActivityService(UserActivityEventRepository userActivityEventRepository) {
        this.userActivityEventRepository = userActivityEventRepository;
    }

    /**
     * Idempotent daily AUTH marker for WAU.
     */
    @Transactional
    public void recordAuthIfAbsent(UUID userId) {
        if (userId == null) {
            return;
        }
        try {
            userActivityEventRepository.insertAuthEventIfAbsent(UUID.randomUUID(), userId);
        } catch (Exception e) {
            log.warn("Failed to record authenticated API activity for userId={}", userId, e);
        }
    }

    /**
     * Commits with {@code AuthService.register} so the user row and event share one transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUserRegistered(UUID userId) {
        if (userId == null) {
            return;
        }
        UserActivityEvent event = baseEvent(userId, UserActivityEventType.USER_REGISTERED);
        persistSafely(event, "userRegistered userId=" + userId);
    }

    /**
     * Isolated from read-only upload URL transaction; uses profile owner for analytics consistency.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUploadStarted(UUID profileOwnerId, UUID profileId, UUID recordId, String fileType) {
        if (profileOwnerId == null || profileId == null || recordId == null) {
            return;
        }
        UserActivityEvent event = baseEvent(profileOwnerId, UserActivityEventType.UPLOAD_STARTED);
        event.setProfileId(profileId);
        event.setRecordId(recordId);
        event.setFileType(normalizeFileType(fileType));
        persistSafely(
                event,
                "uploadStarted profileOwnerId=%s recordId=%s".formatted(profileOwnerId, recordId));
    }

    /**
     * Joins the caller transaction (e.g. {@code confirmUpload}) so the event commits with the health record.
     * {@code profileOwnerId} is the health record owner (not the shared-editor actor).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUploadConfirmed(
            UUID profileOwnerId,
            UUID profileId,
            UUID recordId,
            String fileType,
            boolean isRetry) {
        if (profileOwnerId == null || profileId == null || recordId == null) {
            return;
        }
        UserActivityEvent event = baseEvent(profileOwnerId, UserActivityEventType.UPLOAD_CONFIRMED);
        event.setRetry(isRetry);
        event.setProfileId(profileId);
        event.setRecordId(recordId);
        event.setFileType(normalizeFileType(fileType));
        persistSafely(
                event,
                "uploadConfirmed profileOwnerId=%s profileId=%s recordId=%s isRetry=%s"
                        .formatted(profileOwnerId, profileId, recordId, isRetry));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOcrCompleted(
            UUID userId,
            UUID profileId,
            UUID recordId,
            String provider,
            float confidence,
            boolean hasLowConfidenceMetrics) {
        if (userId == null || recordId == null || provider == null || provider.isBlank()) {
            return;
        }
        UserActivityEvent event = baseEvent(userId, UserActivityEventType.OCR_COMPLETED);
        event.setProfileId(profileId);
        event.setRecordId(recordId);
        event.setProvider(normalizeProvider(provider));
        event.setConfidence((double) confidence);
        event.setHasLowConfidenceMetrics(hasLowConfidenceMetrics);
        persistSafely(
                event,
                "ocrCompleted userId=%s recordId=%s provider=%s".formatted(userId, recordId, provider));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOcrFailed(
            UUID userId,
            UUID profileId,
            UUID recordId,
            String failureReason,
            String providerIfKnown) {
        if (userId == null || recordId == null) {
            return;
        }
        UserActivityEvent event = baseEvent(userId, UserActivityEventType.OCR_FAILED);
        event.setProfileId(profileId);
        event.setRecordId(recordId);
        event.setFailureReason(FailureReasonNormalizer.normalizeForStorage(failureReason));
        event.setProvider(normalizeProvider(providerIfKnown));
        persistSafely(
                event,
                "ocrFailed userId=%s recordId=%s reason=%s".formatted(userId, recordId, failureReason));
    }

    static Instant startOfUtcWeek(LocalDate reference) {
        return reference.with(java.time.DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    static Instant startOfNextUtcWeek(LocalDate reference) {
        return reference.with(java.time.DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /**
     * Expands {@code [from, toExclusive)} to full UTC ISO weeks (Monday 00:00 boundaries) so weekly
     * analytics buckets (WAU and upload volume with {@code granularity=week}) match
     * {@code DATE_TRUNC('week', ...)} and are not partial-week counts mislabeled as a week start.
     */
    static Instant wauQueryFrom(Instant from) {
        return startOfUtcWeek(from.atZone(ZoneOffset.UTC).toLocalDate());
    }

    static Instant wauQueryToExclusive(Instant toExclusive) {
        LocalDate lastIncluded = toExclusive.minusNanos(1).atZone(ZoneOffset.UTC).toLocalDate();
        return startOfNextUtcWeek(lastIncluded);
    }

    static String normalizeFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return null;
        }
        return fileType.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        String normalized = provider.trim();
        return normalized.length() > 50 ? normalized.substring(0, 50) : normalized;
    }

    static String fileTypeFromFileKey(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }
        int dot = fileKey.lastIndexOf('.');
        if (dot < 0 || dot == fileKey.length() - 1) {
            return null;
        }
        return fileKey.substring(dot + 1);
    }

    private static UserActivityEvent baseEvent(UUID userId, String eventType) {
        UserActivityEvent event = new UserActivityEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        return event;
    }

    /**
     * Best-effort write: native INSERT runs immediately (same pattern as
     * {@link UserActivityEventRepository#insertAuthEventIfAbsent}) so DB errors surface here,
     * not at caller commit flush.
     */
    private void persistSafely(UserActivityEvent event, String telemetryContext) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID());
        }
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(Instant.now());
        }
        try {
            userActivityEventRepository.insertProductEvent(
                    event.getId(),
                    event.getUserId(),
                    event.getEventType(),
                    event.isRetry(),
                    event.getProfileId(),
                    event.getRecordId(),
                    event.getFileType(),
                    event.getProvider(),
                    event.getConfidence(),
                    event.getHasLowConfidenceMetrics(),
                    event.getFailureReason(),
                    event.getCreatedAt());
        } catch (Exception e) {
            log.warn("Failed to record product activity event. context={}", telemetryContext, e);
        }
    }
}
