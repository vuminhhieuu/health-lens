package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.AcceptInvitationResultResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.support.SecurityFilterTestSupport;
import com.healthlens.api.service.ProfileShareService;
import com.healthlens.api.util.JwtUtil;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvitationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileShareService profileShareService;

    @MockitoBean
    private PublicEndpointRateLimiter publicEndpointRateLimiter;

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

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
    }

    @Test
    @DisplayName("POST profile invitation accept consumes public rate limit")
    void acceptInvitation_consumesRateLimit() throws Exception {
        UUID profileId = UUID.randomUUID();
        when(profileShareService.acceptInvitation(eq("token-1"), eq(null)))
                .thenReturn(new AcceptInvitationResultResponse("require-login", "/login", profileId));

        mockMvc.perform(post("/api/v1/invitations/accept")
                        .queryParam("token", "token-1")
                        .header("X-Forwarded-For", "198.51.100.99, 10.0.0.10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("require-login"));

        verify(publicEndpointRateLimiter).consumeProfileInvitationAccept(eq("198.51.100.99"), eq("token-1"));
    }

    @Test
    @DisplayName("POST profile invitation accept -> 429 khi bi rate limit")
    void acceptInvitation_rateLimited() throws Exception {
        doThrow(new RateLimitExceededException("Bạn đã gửi yêu cầu quá nhanh.", 120))
                .when(publicEndpointRateLimiter).consumeProfileInvitationAccept(any(), eq("limited"));

        mockMvc.perform(post("/api/v1/invitations/accept")
                        .queryParam("token", "limited")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120));
    }
}
