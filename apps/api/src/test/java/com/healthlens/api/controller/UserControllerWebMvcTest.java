package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.exception.DeletionCancellationTokenException;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.support.SecurityFilterTestSupport;
import com.healthlens.api.service.DataDeletionService;
import com.healthlens.api.service.UserService;
import com.healthlens.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private DataDeletionService dataDeletionService;

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
    @DisplayName("DELETE /api/v1/users/deletion-requests/cancel without token uses cancellation token semantics")
    void cancelDeletion_missingToken_returnsTokenError() throws Exception {
        when(dataDeletionService.cancelDeletionRequest(null))
                .thenThrow(new DeletionCancellationTokenException(
                        "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực."));

        mockMvc.perform(delete("/api/v1/users/deletion-requests/cancel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("https://healthlens.vn/errors/deletion-cancel-token-invalid"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("DELETION_CANCEL_TOKEN_INVALID"));

        verify(dataDeletionService).cancelDeletionRequest(null);
    }

    @Test
    @DisplayName("DELETE /api/v1/users/deletion-requests/cancel -> 429 khi bi rate limit")
    void cancelDeletion_rateLimited() throws Exception {
        doThrow(new RateLimitExceededException("Bạn đã gửi yêu cầu quá nhanh.", 120))
                .when(publicEndpointRateLimiter).consumeCancelDeletion(any(), org.mockito.Mockito.eq("limited"));

        mockMvc.perform(delete("/api/v1/users/deletion-requests/cancel")
                        .queryParam("token", "limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120));
    }
}
