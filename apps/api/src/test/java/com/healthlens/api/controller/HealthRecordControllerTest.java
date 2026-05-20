package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.DownloadHealthRecordPdfResponse;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.support.SecurityFilterTestSupport;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                .andExpect(jsonPath("$.data.message").value("Đã xóa kết quả khám thành công"));

        verify(healthRecordService).deleteHealthRecord(userId, recordId);
    }

    @Test
    @DisplayName("POST confirm-upload -> 429 khi OCR trigger bi rate limit")
    void confirmUpload_rateLimited() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(healthRecordService.confirmUpload(userId, recordId))
                .thenThrow(new RateLimitExceededException("Bạn đã gửi yêu cầu quá nhanh.", 30));

        mockMvc.perform(post("/api/v1/health-records/{recordId}/confirm-upload", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(30));
    }

    @Test
    @DisplayName("GET health record PDF trả binary PDF attachment")
    void downloadHealthRecordPdf_returnsPdfAttachment() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        byte[] pdfBytes = "%PDF-1.4\n% HealthLens\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        when(healthRecordService.downloadHealthRecordPdf(eq(userId), eq(recordId)))
                .thenReturn(new DownloadHealthRecordPdfResponse(
                        pdfBytes,
                        "healthlens-ket-qua-xet-nghiem-mau.pdf"
                ));

        mockMvc.perform(get("/api/v1/health-records/{recordId}/pdf", recordId)
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"healthlens-ket-qua-xet-nghiem-mau.pdf\""
                ))
                .andExpect(content().bytes(pdfBytes));

        verify(healthRecordService).downloadHealthRecordPdf(userId, recordId);
    }
}
