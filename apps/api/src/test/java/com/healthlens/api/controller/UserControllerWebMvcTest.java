package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.exception.DeletionCancellationTokenException;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
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
import static org.mockito.Mockito.doAnswer;
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
}
