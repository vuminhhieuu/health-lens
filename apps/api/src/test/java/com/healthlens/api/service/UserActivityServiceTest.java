package com.healthlens.api.service;

import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.repository.UserActivityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock private UserActivityEventRepository userActivityEventRepository;

    @InjectMocks private UserActivityService userActivityService;

    @Test
    @DisplayName("recordAuthIfAbsent uses atomic idempotent insert")
    void recordAuthIfAbsent_usesAtomicInsert() {
        UUID userId = UUID.randomUUID();
        when(userActivityEventRepository.insertAuthEventIfAbsent(any(UUID.class), eq(userId)))
                .thenReturn(1);

        userActivityService.recordAuthIfAbsent(userId);

        verify(userActivityEventRepository).insertAuthEventIfAbsent(any(UUID.class), eq(userId));
    }

    @Test
    @DisplayName("recordUserRegistered persists USER_REGISTERED")
    void recordUserRegistered_persistsEvent() {
        UUID userId = UUID.randomUUID();
        userActivityService.recordUserRegistered(userId);

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(UUID.class),
                        eq(userId),
                        eq(UserActivityEventType.USER_REGISTERED),
                        eq(false),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Instant.class));
    }

    @Test
    @DisplayName("recordUploadStarted persists dimensions for profile owner")
    void recordUploadStarted_persistsDimensions() {
        UUID profileOwnerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordUploadStarted(profileOwnerId, profileId, recordId, "PDF");

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(UUID.class),
                        eq(profileOwnerId),
                        eq(UserActivityEventType.UPLOAD_STARTED),
                        eq(false),
                        eq(profileId),
                        eq(recordId),
                        eq("pdf"),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Instant.class));
    }

    @Test
    @DisplayName("recordUploadConfirmed persists upload event with retry flag and dimensions")
    void recordUploadConfirmed_persistsEvent() {
        UUID profileOwnerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordUploadConfirmed(profileOwnerId, profileId, recordId, "jpg", true);

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(UUID.class),
                        eq(profileOwnerId),
                        eq(UserActivityEventType.UPLOAD_CONFIRMED),
                        eq(true),
                        eq(profileId),
                        eq(recordId),
                        eq("jpg"),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Instant.class));
    }

    @Test
    @DisplayName("recordUploadConfirmed skips persist when required dimensions are missing")
    void recordUploadConfirmed_skipsWhenDimensionsMissing() {
        UUID profileOwnerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordUploadConfirmed(null, profileId, recordId, "jpg", false);
        userActivityService.recordUploadConfirmed(profileOwnerId, null, recordId, "jpg", false);
        userActivityService.recordUploadConfirmed(profileOwnerId, profileId, null, "jpg", false);

        verify(userActivityEventRepository, never())
                .insertProductEvent(
                        any(),
                        any(),
                        any(),
                        anyBoolean(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }

    @Test
    @DisplayName("recordOcrCompleted persists provider and confidence")
    void recordOcrCompleted_persistsDimensions() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordOcrCompleted(userId, profileId, recordId, "easyocr", 0.92f, true);

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(UUID.class),
                        eq(userId),
                        eq(UserActivityEventType.OCR_COMPLETED),
                        eq(false),
                        eq(profileId),
                        eq(recordId),
                        isNull(),
                        eq("easyocr"),
                        org.mockito.ArgumentMatchers.argThat(
                                confidence ->
                                        confidence != null && Math.abs(confidence - 0.92) < 0.0001),
                        eq(true),
                        isNull(),
                        any(Instant.class));
    }

    @Test
    @DisplayName("recordOcrFailed normalizes failure reason")
    void recordOcrFailed_normalizesReason() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordOcrFailed(userId, profileId, recordId, "processing_error", "textract");

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(UUID.class),
                        eq(userId),
                        eq(UserActivityEventType.OCR_FAILED),
                        eq(false),
                        eq(profileId),
                        eq(recordId),
                        isNull(),
                        eq("textract"),
                        isNull(),
                        isNull(),
                        eq("api_error"),
                        any(Instant.class));
    }

    @Test
    @DisplayName("failed product event write does not propagate")
    void recordUserRegistered_swallowsPersistenceFailure() {
        doThrow(new RuntimeException("db down"))
                .when(userActivityEventRepository)
                .insertProductEvent(
                        any(),
                        any(),
                        any(),
                        anyBoolean(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());

        userActivityService.recordUserRegistered(UUID.randomUUID());

        verify(userActivityEventRepository)
                .insertProductEvent(
                        any(),
                        any(),
                        any(),
                        anyBoolean(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }

    @Test
    @DisplayName("fileTypeFromFileKey extracts extension")
    void fileTypeFromFileKey_extractsExtension() {
        assertThat(UserActivityService.fileTypeFromFileKey("health-records/u/p/r/original.pdf"))
                .isEqualTo("pdf");
    }
}
