package com.healthlens.api.repository;

import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import com.healthlens.api.support.PostgresTestContainerBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@EnabledIf("com.healthlens.api.support.PostgresTestContainerBase#isDockerAvailable")
class UserActivityEventRepositoryIntegrationTest extends PostgresTestContainerBase {

    @Autowired
    private UserActivityEventRepository userActivityEventRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;

    @BeforeEach
    void seedUser() {
        User user = new User();
        user.setEmail("analytics-event-" + UUID.randomUUID() + "@example.com");
        user.setFullName("Analytics Event User");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPasswordHash("hashed");
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        userId = userRepository.saveAndFlush(user).getId();
    }

    @Test
    @DisplayName("product events are queryable by type and time range without log scan")
    void countProductEvents_filtersByTypeAndRange() {
        Instant now = Instant.now();

        UserActivityEvent registered = new UserActivityEvent();
        registered.setUserId(userId);
        registered.setEventType(UserActivityEventType.USER_REGISTERED);
        registered.setCreatedAt(now.minus(2, ChronoUnit.HOURS));
        userActivityEventRepository.save(registered);

        UserActivityEvent ocrFailed = new UserActivityEvent();
        ocrFailed.setUserId(userId);
        ocrFailed.setEventType(UserActivityEventType.OCR_FAILED);
        ocrFailed.setRecordId(UUID.randomUUID());
        ocrFailed.setFailureReason("api_error");
        ocrFailed.setCreatedAt(now.minus(1, ChronoUnit.HOURS));
        userActivityEventRepository.save(ocrFailed);

        long registeredCount = userActivityEventRepository.countProductEvents(
                UserActivityEventType.USER_REGISTERED,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.HOURS));
        long ocrFailedCount = userActivityEventRepository.countProductEvents(
                UserActivityEventType.OCR_FAILED,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.HOURS));

        assertThat(registeredCount).isEqualTo(1);
        assertThat(ocrFailedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("upload quality buckets aggregate OCR_COMPLETED and OCR_FAILED by day")
    void findUploadQualityBuckets_aggregatesTerminalOcrEvents() {
        Instant bucketDay = Instant.parse("2026-03-15T12:00:00Z");
        Instant from = Instant.parse("2026-03-15T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-16T00:00:00Z");

        UserActivityEvent completed = new UserActivityEvent();
        completed.setUserId(userId);
        completed.setEventType(UserActivityEventType.OCR_COMPLETED);
        completed.setRecordId(UUID.randomUUID());
        completed.setCreatedAt(bucketDay);
        userActivityEventRepository.save(completed);

        UserActivityEvent failed = new UserActivityEvent();
        failed.setUserId(userId);
        failed.setEventType(UserActivityEventType.OCR_FAILED);
        failed.setRecordId(UUID.randomUUID());
        failed.setFailureReason("timeout");
        failed.setCreatedAt(bucketDay.plus(1, ChronoUnit.HOURS));
        userActivityEventRepository.save(failed);

        List<UploadQualityBucketProjection> buckets =
                userActivityEventRepository.findUploadQualityBuckets(from, toExclusive, "day");

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0).getBucketDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(buckets.get(0).getSuccessCount()).isEqualTo(1);
        assertThat(buckets.get(0).getFailedCount()).isEqualTo(1);

        List<UploadFailureBreakdownProjection> breakdown =
                userActivityEventRepository.findUploadFailureBreakdown(from, toExclusive, "day");

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).getFailureReason()).isEqualTo("timeout");
        assertThat(breakdown.get(0).getFailureCount()).isEqualTo(1);
    }
}
