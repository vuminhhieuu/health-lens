package com.healthlens.api.service;

import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.UserAuthTotpVerifyRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.ForgotPasswordRateLimiter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.VerifyEmailRateLimiter;
import com.healthlens.api.util.JwtUtil;
import com.healthlens.api.events.email.EmailEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class AuthServiceTotpTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private EmailEventPublisher emailEventPublisher;
    @Mock private JwtUtil jwtUtil;
    @Mock private LoginRateLimiter rateLimiter;
    @Mock private ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    @Mock private VerifyEmailRateLimiter verifyEmailRateLimiter;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ConsentService consentService;
    @Mock private UserNotificationPreferenceService notificationPreferenceService;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;
    @Mock private UserTotpService userTotpService;
    @Mock private UserActivityService userActivityService;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, tokenRepository, passwordResetTokenRepository,
                refreshTokenRepository, passwordEncoder, emailEventPublisher,
                jwtUtil, rateLimiter, forgotPasswordRateLimiter, verifyEmailRateLimiter,
                redisTemplate, consentService, notificationPreferenceService, auditEventRecorder,
                userTotpService,
                userActivityService
        );
    }

    @Test
    @DisplayName("login with verified TOTP returns preAuthToken without access token")
    void login_totpEnabled_returnsPreAuth() {
        User user = verifiedUser();
        LoginRequest request = new LoginRequest("user@example.com", "StrongPass1");

        doNothing().when(rateLimiter).checkLocked(anyString());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPasswordHash())).thenReturn(true);
        when(userTotpService.isVerifiedEnabled(user.getId())).thenReturn(true);
        when(jwtUtil.generatePreAuthToken(user)).thenReturn("pre-auth-jwt");
        when(jwtUtil.getPreAuthTtlSeconds()).thenReturn(300L);

        AuthService.LoginResult result = authService.login(request);

        assertThat(result.totpRequired()).isTrue();
        assertThat(result.preAuthToken()).isEqualTo("pre-auth-jwt");
        assertThat(result.expiresInSeconds()).isEqualTo(300);
        assertThat(result.response()).isNull();
        assertThat(result.rawRefreshToken()).isNull();
    }

    @Test
    @DisplayName("verifyLoginTotp issues full token pair")
    void verifyLoginTotp_success() {
        UUID userId = UUID.randomUUID();
        User user = verifiedUser();
        user.setId(userId);

        when(jwtUtil.extractPreAuthUserId("pre-auth")).thenReturn(userId);
        when(jwtUtil.extractPreAuthJti("pre-auth")).thenReturn("jti-1");
        when(jwtUtil.getRemainingExpiry("pre-auth")).thenReturn(300_000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("pre_auth_used:jti-1"), eq("1"), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);
        when(userTotpService.validateLoginTotp(userId, "123456")).thenReturn(user);
        when(jwtUtil.generateAccessToken(user)).thenReturn("access");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh");
        when(jwtUtil.getRefreshTtl()).thenReturn(604800000L);

        AuthService.LoginResult result = authService.verifyLoginTotp(
                new UserAuthTotpVerifyRequest("pre-auth", "123456"));

        assertThat(result.totpRequired()).isFalse();
        assertThat(result.response().accessToken()).isEqualTo("access");
        assertThat(result.rawRefreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    private static User verifiedUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.ROLE_USER);
        user.setEmailVerified(true);
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.now());
        return user;
    }
}
