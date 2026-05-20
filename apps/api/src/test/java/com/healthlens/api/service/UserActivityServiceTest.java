package com.healthlens.api.service;

import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.repository.UserActivityEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(userActivityEventRepository.insertAuthEventIfAbsent(
                eq(userId),
                eq(UserActivityEventType.AUTHENTICATED_API_CALL)))
                .thenReturn(1);

        userActivityService.recordAuthIfAbsent(userId);

        verify(userActivityEventRepository).insertAuthEventIfAbsent(
                eq(userId),
                eq(UserActivityEventType.AUTHENTICATED_API_CALL));
    }

    @Test
    @DisplayName("recordUploadConfirmed persists upload event with retry flag")
    void recordUploadConfirmed_persistsEvent() {
        UUID userId = UUID.randomUUID();
        userActivityService.recordUploadConfirmed(userId, true);
        verify(userActivityEventRepository).save(any());
    }
}
