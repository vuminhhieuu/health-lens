package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.HealthRecordService;
import com.healthlens.api.service.ProfileService;
import com.healthlens.api.service.ProfileShareService;
import com.healthlens.api.support.SecurityFilterTestSupport;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfileController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private HealthRecordService healthRecordService;

    @MockitoBean
    private ProfileShareService profileShareService;

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
    @DisplayName("DELETE profile gọi service và trả 204")
    void deleteProfile_returnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/profiles/{profileId}", profileId)
                        .with(SecurityMockMvcRequestPostProcessors.user(userId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(profileService).deleteProfile(userId, profileId);
    }
}
