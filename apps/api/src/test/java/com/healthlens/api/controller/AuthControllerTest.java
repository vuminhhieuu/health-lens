package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.exception.AccountLockedException;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.security.CustomUserDetailsService;
import com.healthlens.api.security.JwtAuthenticationFilter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.security.UserActivityRecordingFilter;
import com.healthlens.api.service.AuthService;
import com.healthlens.api.support.SecurityFilterTestSupport;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        private PublicEndpointRateLimiter publicEndpointRateLimiter;

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
        private ConsentService consentService;

        @org.junit.jupiter.api.BeforeEach
        void setUp() throws Exception {
                SecurityFilterTestSupport.stubPassthroughFilters(jwtAuthenticationFilter, userActivityRecordingFilter);
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
                                .thenThrow(new EmailAlreadyExistsException("Email này đã được đăng ký"));

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

        @Test
        @DisplayName("POST /api/v1/auth/register -> 429 khi bi rate limit")
        void register_rateLimited() throws Exception {
                org.mockito.Mockito.doThrow(new RateLimitExceededException(
                                "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau 60 giây.",
                                60
                        )).when(publicEndpointRateLimiter)
                                .consumeRegister(org.mockito.Mockito.anyString(), org.mockito.Mockito.eq("limited@example.com"));

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toRegisterJson("Nguyen Van A", "limited@example.com", "1999-01-01",
                                                "StrongPass1")))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
        }

        // ========== LOGIN TESTS ==========

        @Test
        @DisplayName("GET /api/v1/auth/csrf -> 200 va materialize CSRF token")
        void csrf_success() throws Exception {
                mockMvc.perform(get("/api/v1/auth/csrf"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                                .andExpect(jsonPath("$.data.parameterName").value("_csrf"));
        }

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
                                .with(csrf())
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
                                .thenThrow(new BadCredentialsException("Email hoặc mật khẩu không đúng."));

                mockMvc.perform(post("/api/v1/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"WrongPass1\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/unauthorized"))
                                .andExpect(jsonPath("$.status").value(401))
                                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                                .andExpect(jsonPath("$.detail").value("Email hoặc mật khẩu không đúng."));
        }

        @Test
        @DisplayName("POST /api/v1/auth/login -> 429 khi tai khoan bi lock (AC #6)")
        void login_accountLocked() throws Exception {
                when(authService.login(any(LoginRequest.class)))
                                .thenThrow(new AccountLockedException(
                                                "Tài khoản bị khóa tạm thời. Vui lòng thử lại sau 900 giây."));

                mockMvc.perform(post("/api/v1/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"user@example.com\",\"password\":\"AnyPass1\"}"))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/account-locked"))
                                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"))
                                .andExpect(jsonPath("$.retryAfterSeconds").value(900));
        }

        // ========== REFRESH TESTS ==========

        @Test
        @DisplayName("POST /api/v1/auth/verify-email -> 200 khi token hop le")
        void verifyEmail_success() throws Exception {
                doNothing().when(authService).verifyEmail(org.mockito.Mockito.eq("valid-token"), org.mockito.Mockito.anyString());

                mockMvc.perform(post("/api/v1/auth/verify-email")
                                .header("X-Forwarded-For", "198.51.100.99")
                                .with(request -> {
                                        request.setRemoteAddr("203.0.113.44");
                                        return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"valid-token\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.message").value("Email đã được xác thực thành công"));

                verify(authService).verifyEmail("valid-token", "198.51.100.99");
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify-email -> 400 voi thong diep chung khi token khong hop le")
        void verifyEmail_invalidToken() throws Exception {
                org.mockito.Mockito.doThrow(new IllegalArgumentException("Không thể xác thực email bằng liên kết này."))
                                .when(authService).verifyEmail(org.mockito.Mockito.eq("invalid-token"), org.mockito.Mockito.anyString());

                mockMvc.perform(post("/api/v1/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"invalid-token\"}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("Không thể xác thực email bằng liên kết này."));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify-email -> 429 khi bi rate limit")
        void verifyEmail_rateLimited() throws Exception {
                org.mockito.Mockito.doThrow(new RateLimitExceededException(
                                "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau 120 giây.",
                                120
                        )).when(authService).verifyEmail(org.mockito.Mockito.eq("limited-token"), org.mockito.Mockito.anyString());

                mockMvc.perform(post("/api/v1/auth/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"limited-token\"}"))
                        .andExpect(status().isTooManyRequests())
                        .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                        .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                        .andExpect(jsonPath("$.retryAfterSeconds").value(120))
                        .andExpect(jsonPath("$.detail").value("Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau 120 giây."));
        }

        @Test
        @DisplayName("POST /api/v1/auth/forgot-password -> 429 voi RATE_LIMITED")
        void forgotPassword_rateLimited() throws Exception {
                org.mockito.Mockito.doThrow(new RateLimitExceededException(
                                "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau 3600 giây.",
                                3600
                        )).when(authService).forgotPassword(any());

                mockMvc.perform(post("/api/v1/auth/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"limited@example.com\"}"))
                                .andExpect(status().isTooManyRequests())
                                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/rate-limited"))
                                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                                .andExpect(jsonPath("$.retryAfterSeconds").value(3600));
        }

        @Test
        @DisplayName("POST /api/v1/auth/refresh -> 401 khi khong co cookie (AC #2)")
        void refresh_noCookie() throws Exception {
                mockMvc.perform(post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.detail").value("Refresh token không tồn tại"))
                                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
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
                                .with(csrf())
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
                                .with(csrf())
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
                                .with(csrf())
                                .header("Authorization", "Bearer some-access-token"))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("POST /api/v1/auth/logout -> 204 khi khong co Bearer header")
        void logout_noAuthHeader() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout")
                                .with(csrf()))
                                .andExpect(status().isNoContent());
        }

        // ========== HELPERS ==========

        private String toRegisterJson(String fullName, String email, String birthDate, String password) {
                return "{\"fullName\":\"" + fullName + "\",\"email\":\"" + email + "\",\"birthDate\":\"" + birthDate
                                + "\",\"password\":\"" + password + "\"}";
        }
}
