package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.service.HealthRecordService;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthRecordController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class HealthRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthRecordService healthRecordService;

    @MockitoBean
    private HealthRecordShareService healthRecordShareService;

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
    @DisplayName("GET explanation endpoint trả dữ liệu an toàn khi retrieval fallback")
    void getMetricExplanation_returnsFallbackPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        String metricName = "ALT";
        String explanation = """
                Chỉ số này là gì: ALT là men gan phản ánh mức tổn thương tế bào gan.
                Chỉ số này liên quan đến: Chức năng gan và nguy cơ viêm gan.
                Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: ALT tăng kéo dài có thể gợi ý tổn thương gan.
                """.trim();

        when(healthRecordService.getMetricExplanation(eq(userId), eq(recordId), eq(metricName)))
                .thenReturn(new MetricExplanationResponse(explanation, "fallback"));

        mockMvc.perform(get("/api/v1/health-records/{recordId}/metrics/explanation", recordId)
                        .queryParam("metricName", metricName)
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("fallback"))
                .andExpect(jsonPath("$.data.explanation").value(explanation))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @DisplayName("DELETE health record gọi service và trả 200")
    void deleteHealthRecord_returnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/health-records/{recordId}", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Deleted successfully"));

        verify(healthRecordService).deleteHealthRecord(userId, recordId);
    }
}
