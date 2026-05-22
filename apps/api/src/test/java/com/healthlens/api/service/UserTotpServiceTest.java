package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.UserTotpDisableRequest;
import com.healthlens.api.dto.response.UserTotpSetupResponse;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.entity.UserTotpSecret;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.repository.UserTotpSecretRepository;
import com.healthlens.api.security.UserTotpRateLimiter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTotpServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserTotpSecretRepository totpSecretRepository;
    @Mock private UserTotpSecretCryptoService cryptoService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserTotpRateLimiter rateLimiter;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private GoogleAuthenticator googleAuth;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;

    private UserTotpService userTotpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userTotpService = new UserTotpService(
                userRepository,
                totpSecretRepository,
                cryptoService,
                passwordEncoder,
                rateLimiter,
                redisTemplate,
                googleAuth,
                auditEventRecorder,
                objectMapper
        );
    }

    @Test
    @DisplayName("setup rejects admin accounts")
    void setup_rejectsAdmin() {
        UUID userId = UUID.randomUUID();
        User admin = user(userId);
        admin.setRole(UserRole.ROLE_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userTotpService.setup(userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("quản trị");
    }

    @Test
    @DisplayName("setup rejects when 2FA already verified")
    void setup_alreadyVerified() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        UserTotpSecret existing = new UserTotpSecret();
        existing.setUserId(userId);
        existing.setVerified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpSecretRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userTotpService.setup(userId))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("đã được bật");
    }

    @Test
    @DisplayName("setup returns secret, otpauth URI and backup codes")
    void setup_returnsSetupPayload() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder("JBSWY3DPEHPK3PXP").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(googleAuth.createCredentials()).thenReturn(key);
        when(cryptoService.encrypt(anyString())).thenReturn("enc");
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hash");
        when(totpSecretRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(totpSecretRepository.save(any(UserTotpSecret.class))).thenAnswer(inv -> inv.getArgument(0));

        UserTotpSetupResponse response = userTotpService.setup(userId);

        assertThat(response.secret()).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(response.otpauthUri()).contains("otpauth://totp/");
        assertThat(response.backupCodes()).hasSize(10);
    }

    @Test
    @DisplayName("verifySetup enables TOTP and records audit")
    void verifySetup_enablesTotp() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        UserTotpSecret secret = new UserTotpSecret();
        secret.setUserId(userId);
        secret.setEncryptedSecret("enc");
        secret.setVerified(false);
        secret.setBackupCodesHash("[]");

        doNothing().when(rateLimiter).checkLocked(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpSecretRepository.findByUserId(userId)).thenReturn(Optional.of(secret));
        when(cryptoService.decrypt("enc")).thenReturn("JBSWY3DPEHPK3PXP");
        when(googleAuth.authorize("JBSWY3DPEHPK3PXP", 123456)).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        userTotpService.verifySetup(userId, "123456");

        assertThat(secret.isVerified()).isTrue();
        verify(auditEventRecorder).recordEvent(
                eq(userId),
                eq(AuditActions.USER_TOTP_ENABLED),
                eq(AuditResourceTypes.AUTH),
                eq(userId),
                any()
        );
    }

    @Test
    @DisplayName("verifySetup wrong code records failure audit")
    void verifySetup_wrongCode() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        UserTotpSecret secret = new UserTotpSecret();
        secret.setUserId(userId);
        secret.setEncryptedSecret("enc");
        secret.setVerified(false);

        doNothing().when(rateLimiter).checkLocked(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(totpSecretRepository.findByUserId(userId)).thenReturn(Optional.of(secret));
        when(cryptoService.decrypt("enc")).thenReturn("JBSWY3DPEHPK3PXP");
        when(googleAuth.authorize(anyString(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> userTotpService.verifySetup(userId, "000000"))
                .isInstanceOf(BadCredentialsException.class);

        verify(auditEventRecorder).recordEvent(
                eq(userId),
                eq(AuditActions.USER_TOTP_VERIFY_FAILED),
                eq(AuditResourceTypes.AUTH),
                eq(userId),
                any()
        );
    }

    @Test
    @DisplayName("disable removes secret when password and code valid")
    void disable_success() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        user.setPasswordHash("hash");
        UserTotpSecret secret = new UserTotpSecret();
        secret.setUserId(userId);
        secret.setEncryptedSecret("enc");
        secret.setVerified(true);

        doNothing().when(rateLimiter).checkLocked(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
        when(totpSecretRepository.findByUserId(userId)).thenReturn(Optional.of(secret));
        when(cryptoService.decrypt("enc")).thenReturn("JBSWY3DPEHPK3PXP");
        when(googleAuth.authorize("JBSWY3DPEHPK3PXP", 654321)).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        userTotpService.disable(userId, new UserTotpDisableRequest("pass", "654321"));

        verify(totpSecretRepository).delete(secret);
        verify(auditEventRecorder).recordEvent(
                eq(userId),
                eq(AuditActions.USER_TOTP_DISABLED),
                eq(AuditResourceTypes.AUTH),
                eq(userId),
                any()
        );
    }

    private static User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setRole(UserRole.ROLE_USER);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
