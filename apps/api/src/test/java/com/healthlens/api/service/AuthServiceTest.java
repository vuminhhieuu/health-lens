package com.healthlens.api.service;

import com.healthlens.api.dto.request.ForgotPasswordRequest;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.ResetPasswordRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.entity.EmailVerificationToken;
import com.healthlens.api.entity.PasswordResetToken;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.exception.AccountLockedException;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.ForgotPasswordRateLimiter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.VerifyEmailRateLimiter;
import com.healthlens.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private LoginRateLimiter rateLimiter;
    @Mock private ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    @Mock private VerifyEmailRateLimiter verifyEmailRateLimiter;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ConsentService consentService;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private StreamOperations<String, Object, Object> streamOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, tokenRepository, passwordResetTokenRepository,
                refreshTokenRepository, passwordEncoder, emailService,
                jwtUtil, rateLimiter, forgotPasswordRateLimiter, verifyEmailRateLimiter,
                redisTemplate, consentService, auditEventRecorder, "email.events"
        );
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("register publish event vao Redis stream va khong gui email dong bo")
    void register_publishEvent() {
        com.healthlens.api.dto.request.RegisterRequest request =
                new com.healthlens.api.dto.request.RegisterRequest(
                        "Nguyen Van A", "user@example.com", java.time.LocalDate.of(1999, 1, 1), "StrongPass1");
        User user = createUnverifiedUser();

        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass1")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        authService.register(request);

        verify(streamOperations).add(eq("email.events"), any(java.util.Map.class));
        verify(emailService, org.mockito.Mockito.never()).sendVerificationEmail(any(), anyString());
    }

    @Test
    @DisplayName("verifyEmail thanh cong set emailVerified=true")
    void verifyEmail_success() {
        User user = createUnverifiedUser();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("valid-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        authService.verifyEmail("valid-token", "203.0.113.10");

        assertThat(user.isEmailVerified()).isTrue();
        verify(verifyEmailRateLimiter).consumeAttempt("203.0.113.10", "user@example.com");
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyEmail voi token khong hop le tra loi chung va audit khong ghi raw token")
    void verifyEmail_invalidToken() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("invalid-token", "203.0.113.10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không thể xác thực email bằng liên kết này.");

        verify(verifyEmailRateLimiter).consumeAttempt("203.0.113.10", null);
        verify(auditEventRecorder).recordAnonymous(
                eq("VERIFY_EMAIL_FAILED"),
                eq("AUTH"),
                eq(null),
                argThat(details -> details.containsKey("tokenSupplied")
                        && Boolean.TRUE.equals(details.get("tokenSupplied"))
                        && !details.containsValue("invalid-token"))
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("verifyEmail voi token het han tra loi chung va audit khong ghi raw token")
    void verifyEmail_expiredToken() {
        User user = createUnverifiedUser();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("expired-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token", "203.0.113.10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không thể xác thực email bằng liên kết này.");

        verify(verifyEmailRateLimiter).consumeAttempt("203.0.113.10", "user@example.com");
        verify(auditEventRecorder).recordAnonymous(
                eq("VERIFY_EMAIL_FAILED"),
                eq("AUTH"),
                eq(user.getId()),
                argThat(details -> "failure".equals(details.get("outcome"))
                        && !details.containsValue("expired-token"))
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("verifyEmail voi token da dung tra loi chung va audit khong ghi raw token")
    void verifyEmail_usedToken() {
        User user = createUnverifiedUser();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("used-token");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        token.setUsedAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail("used-token", "203.0.113.10"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không thể xác thực email bằng liên kết này.");

        verify(verifyEmailRateLimiter).consumeAttempt("203.0.113.10", "user@example.com");
        verify(auditEventRecorder).recordAnonymous(
                eq("VERIFY_EMAIL_FAILED"),
                eq("AUTH"),
                eq(user.getId()),
                argThat(details -> "failure".equals(details.get("outcome"))
                        && "invalid".equals(details.get("reason"))
                        && !details.containsValue("used-token"))
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("verifyEmail khi bi rate limit van audit khong ghi raw token")
    void verifyEmail_rateLimitedAudited() {
        when(tokenRepository.findByToken("limited-token")).thenReturn(Optional.empty());
        doThrow(new RateLimitExceededException("Bạn đã gửi yêu cầu quá nhanh.", 120))
                .when(verifyEmailRateLimiter).consumeAttempt("203.0.113.10", null);

        assertThatThrownBy(() -> authService.verifyEmail("limited-token", "203.0.113.10"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(auditEventRecorder).recordAnonymous(
                eq("VERIFY_EMAIL_FAILED"),
                eq("AUTH"),
                eq(null),
                argThat(details -> "failure".equals(details.get("outcome"))
                        && "rate_limited".equals(details.get("reason"))
                        && !details.containsValue("limited-token"))
        );
        verify(tokenRepository).findByToken("limited-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login thanh cong tra ve token pair (AC #1)")
    void login_success() {
        User user = createVerifiedUser();
        LoginRequest request = new LoginRequest("user@example.com", "StrongPass1");

        doNothing().when(rateLimiter).checkLocked(anyString());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateAccessToken(user)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);

        AuthService.LoginResult result = authService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().user().email()).isEqualTo("user@example.com");
        assertThat(result.rawRefreshToken()).isEqualTo("refresh-token");

        verify(rateLimiter).resetAttempts("user@example.com");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login sai password throw BadCredentialsException (AC #5)")
    void login_wrongPassword() {
        User user = createVerifiedUser();
        LoginRequest request = new LoginRequest("user@example.com", "WrongPass1");

        doNothing().when(rateLimiter).checkLocked(anyString());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email hoặc mật khẩu không đúng.");

        verify(rateLimiter).recordFailure("user@example.com");
    }

    @Test
    @DisplayName("login email khong ton tai cung throw BadCredentialsException (AC #5)")
    void login_emailNotFound() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "AnyPass1");

        doNothing().when(rateLimiter).checkLocked(anyString());
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email hoặc mật khẩu không đúng.");

        verify(rateLimiter).recordFailure("nonexistent@example.com");
    }

    @Test
    @DisplayName("login khi tai khoan bi lock throw AccountLockedException (AC #6)")
    void login_accountLocked() {
        LoginRequest request = new LoginRequest("locked@example.com", "AnyPass1");

        doThrow(new AccountLockedException("Tài khoản bị khóa tạm thời. Vui lòng thử lại sau 900 giây."))
                .when(rateLimiter).checkLocked("locked@example.com");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("login khi email chua xac thuc throw BadCredentialsException")
    void login_emailNotVerified() {
        User user = createUnverifiedUser();
        LoginRequest request = new LoginRequest("user@example.com", "StrongPass1");

        doNothing().when(rateLimiter).checkLocked(anyString());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Vui lòng xác thực email trước khi đăng nhập.");
    }

    // ========== REFRESH TESTS ==========

    @Test
    @DisplayName("refreshWithConsent thanh cong tra ve token va consent info (AC #2)")
    void refreshWithConsent_success() {
        String rawRefreshToken = "old-refresh-token";
        RefreshToken storedToken = createValidRefreshToken();
        User user = createVerifiedUser();
        ConsentResponse consentResponse = ConsentResponse.builder()
                .consentGiven(true)
                .consentVersion("1.0")
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.rotateActiveToken(eq(storedToken.getId()), any(Instant.class)))
                .thenReturn(1);
        when(userRepository.findById(storedToken.getUserId())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);
        when(consentService.getConsentStatus(eq(user.getId()), anyString()))
                .thenReturn(consentResponse);

        AuthService.RefreshResult result = authService.refreshWithConsent(rawRefreshToken);

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        assertThat(result.response().consentGiven()).isTrue();
        assertThat(result.response().consentVersion()).isEqualTo("1.0");
        assertThat(result.rawRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).rotateActiveToken(eq(storedToken.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("refreshWithConsent khi user khong co consent")
    void refreshWithConsent_noConsent() {
        String rawRefreshToken = "old-refresh-token";
        RefreshToken storedToken = createValidRefreshToken();
        User user = createVerifiedUser();
        ConsentResponse consentResponse = ConsentResponse.builder()
                .consentGiven(false)
                .consentVersion(null)
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.rotateActiveToken(eq(storedToken.getId()), any(Instant.class)))
                .thenReturn(1);
        when(userRepository.findById(storedToken.getUserId())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);
        when(consentService.getConsentStatus(eq(user.getId()), anyString()))
                .thenReturn(consentResponse);

        AuthService.RefreshResult result = authService.refreshWithConsent(rawRefreshToken);

        assertThat(result.response().consentGiven()).isFalse();
        assertThat(result.response().consentVersion()).isNull();
    }

    @Test
    @DisplayName("refresh thanh cong tra ve token pair moi (AC #2)")
    void refresh_success() {
        String rawRefreshToken = "old-refresh-token";
        RefreshToken storedToken = createValidRefreshToken();
        User user = createVerifiedUser();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.rotateActiveToken(eq(storedToken.getId()), any(Instant.class)))
                .thenReturn(1);
        when(userRepository.findById(storedToken.getUserId())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);

        AuthService.LoginResult result = authService.refresh(rawRefreshToken);

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        assertThat(result.rawRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).rotateActiveToken(eq(storedToken.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("refreshWithConsent replay token da rotate revoke session family va audit khong ghi raw token")
    void refreshWithConsent_reusedRotatedTokenInvalidatesFamily() {
        String rawRefreshToken = "stolen-refresh-token";
        RefreshToken rotatedToken = createValidRefreshToken();
        rotatedToken.setRevokedAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(rotatedToken));

        assertThatThrownBy(() -> authService.refreshWithConsent(rawRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token không hợp lệ");

        verify(refreshTokenRepository).revokeAllBySessionFamilyId(eq(rotatedToken.getSessionFamilyId()), any(Instant.class));
        verify(auditEventRecorder).recordEvent(
                eq(rotatedToken.getUserId()),
                eq(AuditActions.REFRESH_TOKEN_REUSE_FAILED),
                eq(AuditResourceTypes.AUTH),
                eq(rotatedToken.getUserId()),
                argThat(details -> "reused_refresh_token".equals(details.get("reason"))
                        && !details.containsValue(rawRefreshToken))
        );
        verify(jwtUtil, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("refreshWithConsent concurrent loser khong mint token moi va khong revoke session family")
    void refreshWithConsent_atomicRotationRaceLoserDoesNotInvalidateFamily() {
        String rawRefreshToken = "old-refresh-token";
        RefreshToken storedToken = createValidRefreshToken();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.rotateActiveToken(eq(storedToken.getId()), any(Instant.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> authService.refreshWithConsent(rawRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token không hợp lệ");

        verify(refreshTokenRepository, never()).revokeAllBySessionFamilyId(any(UUID.class), any(Instant.class));
        verify(auditEventRecorder, never()).recordEvent(
                any(UUID.class),
                eq(AuditActions.REFRESH_TOKEN_REUSE_FAILED),
                eq(AuditResourceTypes.AUTH),
                any(UUID.class),
                any()
        );
        verify(jwtUtil, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("refresh voi token het han throw BadCredentialsException")
    void refresh_expired() {
        String rawRefreshToken = "expired-refresh-token";
        RefreshToken expiredToken = createExpiredRefreshToken();

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(rawRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token đã hết hạn");
    }

    @Test
    @DisplayName("refresh voi token bi revoke throw BadCredentialsException")
    void refresh_revoked() {
        String rawRefreshToken = "revoked-refresh-token";

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(rawRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token không hợp lệ");
    }

    // ========== LOGOUT TESTS ==========

    @Test
    @DisplayName("logout blacklist access token va revoke refresh tokens (AC #4)")
    void logout_success() {
        String accessToken = "test-access-token";
        UUID userId = UUID.randomUUID();

        when(jwtUtil.extractJti(accessToken)).thenReturn("test-jti");
        when(jwtUtil.getRemainingExpiry(accessToken)).thenReturn(300000L);
        when(jwtUtil.extractSubject(accessToken)).thenReturn(userId.toString());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout(accessToken);

        verify(valueOperations).set(eq("blacklist:token:test-jti"), eq("1"), eq(300000L), any());
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(java.time.Instant.class));
    }

    // ========== PASSWORD RESET TESTS ==========

    @Test
    @DisplayName("forgotPassword gui email neu user ton tai (AC #1)")
    void forgotPassword_userExists() {
        User user = createVerifiedUser();
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(user), anyString());
        verify(forgotPasswordRateLimiter).recordRequest("user@example.com");
    }

    @Test
    @DisplayName("forgotPassword khong gui email neu user khong ton tai nhung van thanh cong (AC #2)")
    void forgotPassword_userNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(forgotPasswordRateLimiter).recordRequest("nonexistent@example.com");
        verify(emailService, org.mockito.Mockito.never()).sendPasswordResetEmail(any(), anyString());
    }

    @Test
    @DisplayName("forgotPassword throw RATE_LIMITED khi bi rate limit (AC #2)")
    void forgotPassword_rateLimited() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("limited@example.com");

        doThrow(new RateLimitExceededException("Rate limit exceeded", 3600))
                .when(forgotPasswordRateLimiter).checkRateLimit("limited@example.com");

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("forgotPassword ghi telemetry khi email provider loi nhung van tra thanh cong")
    void forgotPassword_emailProviderFailureAudited() {
        User user = createVerifiedUser();
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("smtp down"))
                .when(emailService).sendPasswordResetEmail(eq(user), anyString());

        authService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(auditEventRecorder).recordEvent(
                eq(user.getId()),
                eq(AuditActions.EMAIL_PROVIDER_FAILURE),
                eq(AuditResourceTypes.AUTH),
                eq(user.getId()),
                argThat(details -> "forgot_password".equals(details.get("flow"))
                        && "IllegalStateException".equals(details.get("failureClass"))
                        && !details.containsValue("smtp down"))
        );
        verify(forgotPasswordRateLimiter).recordRequest("user@example.com");
    }

    @Test
    @DisplayName("resetPassword thanh cong voi token hop le (AC #3, #6)")
    void resetPassword_success() {
        User user = createVerifiedUser();
        PasswordResetToken token = createValidResetToken(user);
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "NewStrongPass1");

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewStrongPass1")).thenReturn("new-hashed-pass");

        authService.resetPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-pass");
        assertThat(token.getUsedAt()).isNotNull();
        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("resetPassword throw exception voi token het han (AC #4)")
    void resetPassword_expired() {
        User user = createVerifiedUser();
        PasswordResetToken token = createExpiredResetToken(user);
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "NewStrongPass1");

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("resetPassword throw exception voi token da duoc su dung (AC #4, #5)")
    void resetPassword_usedToken() {
        User user = createVerifiedUser();
        PasswordResetToken token = createValidResetToken(user);
        token.setUsedAt(Instant.now());
        ResetPasswordRequest request = new ResetPasswordRequest("used-token", "NewStrongPass1");

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");
    }

    // ========== HELPERS ==========

    private User createVerifiedUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Nguyen Van A");
        user.setPasswordHash("$2a$12$hashedpassword");
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        return user;
    }

    private User createUnverifiedUser() {
        User user = createVerifiedUser();
        user.setEmailVerified(false);
        return user;
    }

    private RefreshToken createValidRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserId(UUID.randomUUID());
        token.setSessionFamilyId(UUID.randomUUID());
        token.setTokenHash("hashed-token-value");
        token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        token.setCreatedAt(Instant.now());
        return token;
    }

    private RefreshToken createExpiredRefreshToken() {
        RefreshToken token = createValidRefreshToken();
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return token;
    }

    private PasswordResetToken createValidResetToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("valid-token");
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        return token;
    }

    private PasswordResetToken createExpiredResetToken(User user) {
        PasswordResetToken token = createValidResetToken(user);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        return token;
    }
}
