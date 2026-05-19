package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.UserAnalyticsResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.service.AnalyticsService;
import com.healthlens.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAnalyticsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

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
    @DisplayName("GET /admin/analytics/users tra ve du lieu khi admin hop le")
    void getUserAnalytics_returnsDataForAdmin() throws Exception {
        when(analyticsService.getUserAnalytics(any(Instant.class), any(Instant.class)))
                .thenReturn(new UserAnalyticsResponse(
                        42,
                        List.of(new UserAnalyticsResponse.MonthlyGrowthPoint("2026-03", 10))));

        mockMvc.perform(get("/api/v1/admin/analytics/users")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(42))
                .andExpect(jsonPath("$.data.monthlyGrowth[0].month").value("2026-03"))
                .andExpect(jsonPath("$.data.monthlyGrowth[0].newUsers").value(10));
    }

    @Test
    @DisplayName("GET /admin/analytics/users tra 400 khi khoang trong tuong lai")
    void getUserAnalytics_returnsBadRequestForFutureRange() throws Exception {
        YearMonth future = YearMonth.now(ZoneOffset.UTC).plusMonths(2);
        String from = future.atDay(1).toString();
        String to = future.atEndOfMonth().toString();

        mockMvc.perform(get("/api/v1/admin/analytics/users")
                        .param("from", from)
                        .param("to", to)
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /admin/analytics/users tra 400 khi khoang vuot gioi han")
    void getUserAnalytics_returnsBadRequestForInvalidRange() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/users")
                        .param("from", "2020-01-01")
                        .param("to", "2026-12-31")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
