package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.ActivityAnalyticsResponse;
import com.healthlens.api.dto.response.UserAnalyticsResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.support.SecurityFilterTestSupport;
import com.healthlens.api.service.AnalyticsService;
import com.healthlens.api.service.UserActivityService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
    private UserActivityService userActivityService;

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
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

    @Test
    @DisplayName("GET /admin/analytics/activity tra ve du lieu khi admin hop le")
    void getActivity_returnsDataForAdmin() throws Exception {
        ActivityAnalyticsResponse.ActivitySummary summary = new ActivityAnalyticsResponse.ActivitySummary(
                10, 8, 25.0, 5, 3, 66.7, 2, 1, 100.0);
        ActivityAnalyticsResponse response = new ActivityAnalyticsResponse(
                summary,
                List.of(new ActivityAnalyticsResponse.WauBucket("2026-03-03", 12)),
                List.of(),
                List.of(new ActivityAnalyticsResponse.UploadBucket("2026-03-02", 5, 1)),
                List.of());

        when(analyticsService.getActivity(any(Instant.class), any(Instant.class), anyString(), anyBoolean()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/analytics/activity")
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-07")
                        .param("granularity", "day")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.wauCurrentWeek").value(10))
                .andExpect(jsonPath("$.data.wauBuckets[0].wau").value(12))
                .andExpect(jsonPath("$.data.uploadBuckets[0].count").value(5));
    }

    @Test
    @DisplayName("GET /admin/analytics/activity tra 400 khi khoang vuot 90 ngay")
    void getActivity_returnsBadRequestForRangeOverMaxDays() throws Exception {
        when(analyticsService.getActivity(any(Instant.class), any(Instant.class), anyString(), anyBoolean()))
                .thenAnswer(invocation -> {
                    AnalyticsService.validateActivityRange(
                            invocation.getArgument(0), invocation.getArgument(1));
                    return null;
                });

        mockMvc.perform(get("/api/v1/admin/analytics/activity")
                        .param("from", "2020-01-01")
                        .param("to", "2026-12-31")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
