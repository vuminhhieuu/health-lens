package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.service.HealthRecordShareService;
import com.healthlens.api.util.JwtUtil;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthRecordInvitationController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class HealthRecordInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthRecordShareService healthRecordShareService;

    @MockitoBean
    private PublicEndpointRateLimiter publicEndpointRateLimiter;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST health record invitation accept -> 429 khi bi rate limit")
    void acceptInvitation_rateLimited() throws Exception {
        doThrow(new RateLimitExceededException("Bạn đã gửi yêu cầu quá nhanh.", 120))
                .when(publicEndpointRateLimiter).consumeHealthRecordInvitationAccept(any(), eq("limited"));

        mockMvc.perform(post("/api/v1/health-record-invitations/accept")
                        .queryParam("token", "limited")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120));
    }
}
