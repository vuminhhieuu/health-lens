package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.exception.AccountLockedException;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.service.AuthService;
import com.healthlens.api.service.ConsentService;
import com.healthlens.api.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private LoginRateLimiter rateLimiter;

        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockitoBean
        private JwtUtil jwtUtil;

        @MockitoBean
        private CustomUserDetailsService customUserDetailsService;

        @MockitoBean
        private StringRedisTemplate stringRedisTemplate;

        @MockitoBean
        private ConsentService consentService;

        @org.junit.jupiter.api.BeforeEach
        void setUp() throws Exception {
                // Make the mocked JwtAuthenticationFilter delegate to the filter chain
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        }

        // ========== REGISTER TESTS ==========

        @Test
        @DisplayName("POST /api/v1/auth/register -> 201 khi dang ky thanh cong")
        void register_success() throws Exception {
                when(authService.register(any(RegisterRequest.class))).thenReturn(UUID.randomUUID());

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toRegisterJson("Nguyen Van A", "user@example.com", "1999-01-01",
                                                "StrongPass1")))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.message").exists())
                                .andExpect(jsonPath("$.meta.timestamp").exists());
        }

        @Test
        @DisplayName("POST /api/v1/auth/register -> 409 khi email da ton tai")
        void register_duplicateEmail() throws Exception {
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new EmailAlreadyExistsException("Email nay da duoc dang ky"));

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toRegisterJson("Nguyen Van A", "existing@example.com", "1999-01-01",
                                                "StrongPass1")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.type")
                                                .value("https://healthlens.vn/errors/email-already-exists"))
                                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register -> 400 va co field-level details khi password khong dat")
        void register_weakPasswordValidation() throws Exception {
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toRegisterJson("Nguyen Van A", "user@example.com", "1999-01-01", "weak")))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/validation-error"))
                                .andExpect(jsonPath("$.errors[0].field").value("password"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register -> 409 khi DB unique constraint bi vi pham")
        void register_dbIntegrityViolation() throws Exception {
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new DataIntegrityViolationException("users_uk_email"));

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toRegisterJson("Nguyen Van A", "existing@example.com", "1999-01-01",
                                                "StrongPass1")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.type")
                                                .value("https://healthlens.vn/errors/email-already-exists"))
                                .andExpect(jsonPath("$.status").value(409));
        }

        // ========== LOGIN TESTS ==========

        @Test
        @DisplayName("POST /api/v1/auth/login -> 200 khi dang nhap thanh cong (AC #1)")
        void login_success() throws Exception {
                UUID userId = UUID.randomUUID();
                LoginResponse response = new LoginResponse(
                                "eyJhbGciOiJIUzI1NiJ9.test",
                                new LoginResponse.UserInfo(userId, "user@example.com", "ROLE_USER", "Test User"));
                AuthService.LoginResult result = new AuthService.LoginResult(response, "refresh-token-value");

                when(authService.login(any(LoginRequest.class))).thenReturn(result);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"StrongPass1\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").value("eyJhbGciOiJIUzI1NiJ9.test"))
                                .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                                .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"))
                                .andExpect(jsonPath("$.meta.timestamp").exists());
        }

        @Test
        @DisplayName("POST /api/v1/auth/login -> 401 khi sai password (AC #5, khong lo email)")
        void login_wrongPassword() throws Exception {
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new BadCredentialsException("Email hoac mat khau khong dung"));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"WrongPass1\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/unauthorized"))
                                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("POST /api/v1/auth/login -> 429 khi tai khoan bi lock (AC #6)")
        void login_accountLocked() throws Exception {
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new AccountLockedException(
                                                "Tai khoan bi khoa tam thoi. Thu lai sau 900 giay."));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"AnyPass1\"}"))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/account-locked"))
                                .andExpect(jsonPath("$.retryAfterSeconds").value(900));
        }

        // ========== REFRESH TESTS ==========

        @Test
        @DisplayName("POST /api/v1/auth/verify-email -> 200 khi token hop le")
        void verifyEmail_success() throws Exception {
                doNothing().when(authService).verifyEmail("valid-token");

                mockMvc.perform(post("/api/v1/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"valid-token\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.message").value("Email đã được xác thực thành công"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify-email -> 400 khi token khong hop le")
        void verifyEmail_invalidToken() throws Exception {
                org.mockito.Mockito.doThrow(new IllegalArgumentException("Token xác thực không hợp lệ"))
                                .when(authService).verifyEmail("invalid-token");

                mockMvc.perform(post("/api/v1/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"invalid-token\"}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("Token xác thực không hợp lệ"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh -> 401 khi khong co cookie (AC #2)")
        void refresh_noCookie() throws Exception {
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.detail").value("Refresh token khong ton tai"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh -> 200 khi refresh thanh cong (AC #2) with consent")
        void refresh_success() throws Exception {
                UUID userId = UUID.randomUUID();
                com.healthlens.api.dto.response.RefreshResponse response = new com.healthlens.api.dto.response.RefreshResponse(
                        "new-access-token",
                        new com.healthlens.api.dto.response.RefreshResponse.UserInfo(userId, "user@example.com", "ROLE_USER", "Test User"),
                        true,
                        "1.0"
                );
                AuthService.RefreshResult result = new AuthService.RefreshResult(response, "new-refresh-token");

                when(authService.refreshWithConsent("old-refresh-token")).thenReturn(result);

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                        .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                        .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                        .andExpect(jsonPath("$.data.user.role").value("ROLE_USER"))
                        .andExpect(jsonPath("$.data.consentGiven").value(true))
                        .andExpect(jsonPath("$.data.consentVersion").value("1.0"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh -> 200 khi consentVersion null")
        void refresh_success_withNullConsentVersion() throws Exception {
                UUID userId = UUID.randomUUID();
                com.healthlens.api.dto.response.RefreshResponse response = new com.healthlens.api.dto.response.RefreshResponse(
                        "new-access-token",
                        new com.healthlens.api.dto.response.RefreshResponse.UserInfo(userId, "user@example.com", "ROLE_USER", "Test User"),
                        false,
                        null
                );
                AuthService.RefreshResult result = new AuthService.RefreshResult(response, "new-refresh-token");

                when(authService.refreshWithConsent("old-refresh-token")).thenReturn(result);

                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-refresh-token")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                        .andExpect(jsonPath("$.data.user.id").value(userId.toString()))
                        .andExpect(jsonPath("$.data.consentGiven").value(false))
                        .andExpect(jsonPath("$.data.consentVersion").value(nullValue()));
        }

        // ========== LOGOUT TESTS ==========

        @Test
        @DisplayName("POST /api/v1/auth/logout -> 204 khi logout thanh cong (AC #4)")
        void logout_success() throws Exception {
                doNothing().when(authService).logout(any());

                mockMvc.perform(post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer some-access-token"))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /api/v1/auth/logout -> 204 khi khong co Bearer header")
        void logout_noAuthHeader() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isNoContent());
        }

        // ========== HELPERS ==========

        private String toRegisterJson(String fullName, String email, String birthDate, String password) {
                return "{\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\",\"birthDate\":\"" + birthDate
                                + "\",\"password\":\"" + password + "\"}";
        }
}
