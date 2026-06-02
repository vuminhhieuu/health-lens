package com.healthlens.api.config;

import com.healthlens.api.controller.AuthController;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.AuthService;
import com.healthlens.api.support.SecurityFilterTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class SecurityConfigHeadersIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserActivityRecordingFilter userActivityRecordingFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @MockitoBean
    private PublicEndpointRateLimiter publicEndpointRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
    }

    @Test
    void response_includesContentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    @Test
    void response_includesStrictTransportSecurity() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf").secure(true))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().string("Strict-Transport-Security",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("max-age=31536000"),
                                org.hamcrest.Matchers.containsString("includeSubDomains"))));
    }

    @Test
    void response_includesXFrameOptionsDeny() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void response_includesXContentTypeOptionsNosniff() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void response_includesReferrerPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void response_includesPermissionsPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(header().exists("Permissions-Policy"))
                .andExpect(header().string("Permissions-Policy",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("camera=()"),
                                org.hamcrest.Matchers.containsString("microphone=()"),
                                org.hamcrest.Matchers.containsString("geolocation=()"),
                                org.hamcrest.Matchers.containsString("payment=()"))));
    }
}
