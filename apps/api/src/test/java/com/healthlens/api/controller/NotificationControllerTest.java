package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.NotificationInboxItemResponse;
import com.healthlens.api.dto.response.NotificationInboxItemType;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.NotificationInboxService;
import com.healthlens.api.support.SecurityFilterTestSupport;
import com.healthlens.api.util.JwtUtil;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationInboxService notificationInboxService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserActivityRecordingFilter userActivityRecordingFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @MockitoBean
    private PublicEndpointRateLimiter publicEndpointRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
    }

    @Test
    @DisplayName("GET /notifications/inbox requires authentication and returns items for current user")
    void listInbox_authenticated_returnsItems() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-03-01T08:00:00Z");
        when(notificationInboxService.listInbox(eq(userId)))
                .thenReturn(List.of(new NotificationInboxItemResponse(
                        "PROFILE_INVITATION:" + UUID.randomUUID(),
                        NotificationInboxItemType.PROFILE_INVITATION,
                        "Lời mời xem hồ sơ",
                        "Lan mời bạn xem hồ sơ \"Hồ sơ An\".",
                        createdAt,
                        "/invitations/accept?token=abc",
                        false
                )));

        mockMvc.perform(get("/api/v1/notifications/inbox")
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("PROFILE_INVITATION"))
                .andExpect(jsonPath("$.data[0].read").value(false))
                .andExpect(jsonPath("$.data[0].actionUrl").value("/invitations/accept?token=abc"));

        verify(notificationInboxService).listInbox(userId);
    }

    @Test
    @DisplayName("POST /notifications/inbox/read marks item read for current user")
    void markInboxRead_authenticated_marksItem() throws Exception {
        UUID userId = UUID.randomUUID();
        String itemId = "PROFILE_INVITATION:" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notifications/inbox/read")
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":\"" + itemId + "\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemId").value(itemId))
                .andExpect(jsonPath("$.data.read").value(true));

        verify(notificationInboxService).markAsRead(userId, itemId);
    }

    @Test
    @DisplayName("POST /notifications/inbox/read-all marks all items read for current user")
    void markAllInboxRead_authenticated_marksAll() throws Exception {
        UUID userId = UUID.randomUUID();
        when(notificationInboxService.markAllAsRead(userId)).thenReturn(2);

        mockMvc.perform(post("/api/v1/notifications/inbox/read-all")
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedCount").value(2));

        verify(notificationInboxService).markAllAsRead(userId);
    }

    @Test
    @DisplayName("GET /notifications/inbox without auth returns 401")
    void listInbox_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/inbox").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
