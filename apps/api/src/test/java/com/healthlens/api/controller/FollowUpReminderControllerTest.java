package com.healthlens.api.controller;

import com.healthlens.api.dto.response.FollowUpReminderResponse;
import com.healthlens.api.service.FollowUpReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowUpReminderControllerTest {

    private final FollowUpReminderService followUpReminderService = mock(FollowUpReminderService.class);
    private final FollowUpReminderController controller = new FollowUpReminderController(followUpReminderService);

    @Test
    @DisplayName("GET follow-up reminders lists data without dispatching reminder emails")
    void list_doesNotDispatchReminderEmails() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        FollowUpReminderResponse reminder = new FollowUpReminderResponse(
                UUID.randomUUID(),
                profileId,
                LocalDate.of(2026, 6, 2),
                "Tái khám",
                "Mang toa cũ",
                null,
                null,
                Instant.parse("2026-06-02T00:00:00Z"),
                Instant.parse("2026-06-02T00:00:00Z")
        );
        when(followUpReminderService.list(userId, profileId)).thenReturn(List.of(reminder));

        Map<String, Object> body = controller
                .list(new TestingAuthenticationToken(userId.toString(), null), profileId)
                .getBody();

        verify(followUpReminderService).list(userId, profileId);
        verify(followUpReminderService, never()).dispatchDueReminderEmailsIfEnabled(userId);
        assertThat(body).isNotNull();
        assertThat(body.get("data")).isEqualTo(List.of(reminder));
    }
}
