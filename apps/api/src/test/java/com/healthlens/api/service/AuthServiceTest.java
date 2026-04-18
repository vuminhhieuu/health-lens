package com.healthlens.api.service;

import com.healthlens.api.dto.request.ForgotPasswordRequest;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.ResetPasswordRequest;
import com.healthlens.api.dto.response.ConsentResponse;
import com.healthlens.api.entity.PasswordResetToken;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.exception.AccountLockedException;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.ForgotPasswordRateLimiter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ConsentService consentService;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, tokenRepository, passwordResetTokenRepository,
                refreshTokenRepository, passwordEncoder, emailService,
                jwtUtil, rateLimiter, forgotPasswordRateLimiter, redisTemplate, consentService
        );
    }

    // ========== LOGIN TESTS ==========

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
                .hasMessage("Email hoac mat khau khong dung");

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
                .hasMessage("Email hoac mat khau khong dung");

        verify(rateLimiter).recordFailure("nonexistent@example.com");
    }

    @Test
    @DisplayName("login khi tai khoan bi lock throw AccountLockedException (AC #6)")
    void login_accountLocked() {
        LoginRequest request = new LoginRequest("locked@example.com", "AnyPass1");

        doThrow(new AccountLockedException("Tai khoan bi khoa tam thoi. Thu lai sau 900 giay."))
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
                .hasMessage("Vui long xac thuc email truoc khi dang nhap");
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
        when(userRepository.findById(storedToken.getUserId())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);

        AuthService.LoginResult result = authService.refresh(rawRefreshToken);

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        assertThat(result.rawRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(storedToken.getRevokedAt()).isNotNull();
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
                .hasMessage("Refresh token da het han");
    }

    @Test
    @DisplayName("refresh voi token bi revoke throw BadCredentialsException")
    void refresh_revoked() {
        String rawRefreshToken = "revoked-refresh-token";

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(rawRefreshToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Refresh token khong hop le");
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
    @DisplayName("forgotPassword throw exception khi bi rate limit (AC #2)")
    void forgotPassword_rateLimited() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("limited@example.com");

        doThrow(new AccountLockedException("Rate limit exceeded")).when(forgotPasswordRateLimiter).checkRateLimit("limited@example.com");

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(AccountLockedException.class);
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
                .hasMessage("Token khong hop le hoac da het han");
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
                .hasMessage("Token khong hop le hoac da het han");
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
