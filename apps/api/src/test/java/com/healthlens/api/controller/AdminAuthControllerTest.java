package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.response.AdminLoginResponse;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.AdminAuthService;
import com.healthlens.api.support.SecurityFilterTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@TestPropertySource(properties = "app.cookie.secure=false")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserActivityRecordingFilter userActivityRecordingFilter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @BeforeEach
    void setUp() throws Exception {
        SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
    }

    @Test
    void login_setsCookieAndOmitsAccessTokenFromResponseBody() throws Exception {
        when(adminAuthService.login(any())).thenReturn(new AdminLoginResponse(
                "secret-admin-token",
                false,
                false,
                "admin@healthlens.vn"
        ));

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@healthlens.vn\",\"password\":\"StrongPass1\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("admin_access_token"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.email").value("admin@healthlens.vn"))
                .andExpect(jsonPath("$.data.totpRequired").value(false))
                .andExpect(jsonPath("$.data.totpSetupRequired").value(false));
    }

    @Test
    void logout_rejectsMissingCsrfEvenWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .with(authentication(adminAuthentication(true)))
                        .header("Authorization", "Bearer bearer-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_blacklistsBearerTokenWhenNoCookiePresent() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .with(csrf())
                        .with(authentication(adminAuthentication(true)))
                        .header("Authorization", "Bearer bearer-token"))
                .andExpect(status().isNoContent());

        verify(adminAuthService).logout("bearer-token");
    }

    @Test
    void session_returnsAdminSessionDetails() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/session")
                        .with(authentication(adminAuthentication(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.data.email").value("admin@healthlens.vn"))
                .andExpect(jsonPath("$.data.totpVerified").value(true))
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    @Test
    void session_rejectsNonAdminPrincipal() throws Exception {
        UsernamePasswordAuthenticationToken userAuthentication = new UsernamePasswordAuthenticationToken(
                "22222222-2222-2222-2222-222222222222",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        userAuthentication.setDetails(Map.of(
                "email", "user@healthlens.vn",
                "totpVerified", true
        ));

        mockMvc.perform(get("/api/v1/admin/auth/session")
                        .with(authentication(userAuthentication)))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken adminAuthentication(boolean totpVerified) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "11111111-1111-1111-1111-111111111111",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        authentication.setDetails(Map.of(
                "email", "admin@healthlens.vn",
                "totpVerified", totpVerified
        ));
        return authentication;
    }
}
