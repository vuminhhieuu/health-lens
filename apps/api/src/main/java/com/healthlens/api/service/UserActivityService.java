package com.healthlens.api.service;

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
            userActivityEventRepository.insertAuthEventIfAbsent(
                    userId,
                    UserActivityEventType.AUTHENTICATED_API_CALL);
        } catch (Exception e) {
            log.warn("Failed to record authenticated API activity for userId={}", userId, e);
        }
    }

    /**
     * Joins the caller transaction (e.g. {@code confirmUpload}) so the event commits with the health record.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordUploadConfirmed(UUID userId, boolean isRetry) {
        if (userId == null) {
            return;
        }
        try {
            UserActivityEvent event = new UserActivityEvent();
            event.setUserId(userId);
            event.setEventType(UserActivityEventType.UPLOAD_CONFIRMED);
            event.setRetry(isRetry);
            userActivityEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record upload activity for userId={}", userId, e);
        }
    }

    static Instant startOfUtcWeek(LocalDate reference) {
        return reference.with(java.time.DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    static Instant startOfNextUtcWeek(LocalDate reference) {
        return reference.with(java.time.DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
