package com.healthlens.api.service;

import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.repository.UserActivityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(UserActivityEventType.USER_REGISTERED);
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("recordUploadStarted persists dimensions for profile owner")
    void recordUploadStarted_persistsDimensions() {
        UUID profileOwnerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordUploadStarted(profileOwnerId, profileId, recordId, "PDF");

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityEventRepository).save(captor.capture());
        UserActivityEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(UserActivityEventType.UPLOAD_STARTED);
        assertThat(event.getProfileId()).isEqualTo(profileId);
        assertThat(event.getRecordId()).isEqualTo(recordId);
        assertThat(event.getFileType()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("recordUploadConfirmed persists upload event with retry flag and dimensions")
    void recordUploadConfirmed_persistsEvent() {
        UUID profileOwnerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordUploadConfirmed(profileOwnerId, profileId, recordId, "jpg", true);

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityEventRepository).save(captor.capture());
        UserActivityEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(UserActivityEventType.UPLOAD_CONFIRMED);
        assertThat(event.isRetry()).isTrue();
        assertThat(event.getProfileId()).isEqualTo(profileId);
        assertThat(event.getRecordId()).isEqualTo(recordId);
        assertThat(event.getFileType()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("recordOcrCompleted persists provider and confidence")
    void recordOcrCompleted_persistsDimensions() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordOcrCompleted(userId, profileId, recordId, "easyocr", 0.92f, true);

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityEventRepository).save(captor.capture());
        UserActivityEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(UserActivityEventType.OCR_COMPLETED);
        assertThat(event.getProvider()).isEqualTo("easyocr");
        assertThat(event.getConfidence()).isCloseTo(0.92, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(event.getHasLowConfidenceMetrics()).isTrue();
    }

    @Test
    @DisplayName("recordOcrFailed normalizes failure reason")
    void recordOcrFailed_normalizesReason() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        userActivityService.recordOcrFailed(userId, profileId, recordId, "processing_error", "textract");

        ArgumentCaptor<UserActivityEvent> captor = ArgumentCaptor.forClass(UserActivityEvent.class);
        verify(userActivityEventRepository).save(captor.capture());
        UserActivityEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(UserActivityEventType.OCR_FAILED);
        assertThat(event.getFailureReason()).isEqualTo("api_error");
        assertThat(event.getProvider()).isEqualTo("textract");
    }

    @Test
    @DisplayName("failed product event write does not propagate")
    void recordUserRegistered_swallowsPersistenceFailure() {
        doThrow(new RuntimeException("db down")).when(userActivityEventRepository).save(any());

        userActivityService.recordUserRegistered(UUID.randomUUID());

        verify(userActivityEventRepository).save(any());
    }

    @Test
    @DisplayName("fileTypeFromFileKey extracts extension")
    void fileTypeFromFileKey_extractsExtension() {
        assertThat(UserActivityService.fileTypeFromFileKey("health-records/u/p/r/original.pdf"))
                .isEqualTo("pdf");
    }
}
