package com.healthlens.api.service;

import com.healthlens.api.activity.UserActivityEventType;
import com.healthlens.api.support.PostgresTestContainerBase;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserActivityEventRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.condition.EnabledIf;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@EnabledIf("com.healthlens.api.support.PostgresTestContainerBase#isDockerAvailable")
class AuthServiceIntegrationTest extends PostgresTestContainerBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserActivityEventRepository userActivityEventRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    @DisplayName("register luu user voi password da hash, email chưa verified, role ROLE_USER")
    void register_persistsUserWithExpectedState() {
        RegisterRequest request = new RegisterRequest("Integration User", "integration@example.com",
                LocalDate.of(2000, 2, 20), "StrongPass1");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        UUID userId = authService.register(request);

        User saved = userRepository.findById(userId).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("integration@example.com");
        assertThat(saved.getFullName()).isEqualTo("Integration User");
        assertThat(saved.getBirthDate()).isEqualTo(LocalDate.of(2000, 2, 20));
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("StrongPass1");
        assertThat(passwordEncoder.matches("StrongPass1", saved.getPasswordHash())).isTrue();

        assertThat(tokenRepository.findAll())
                .anySatisfy(token -> assertThat(token.getUser().getId()).isEqualTo(saved.getId()));
        verify(emailService, never()).sendVerificationEmail(any(User.class), any(String.class));

        Instant now = Instant.now();
        assertThat(userActivityEventRepository.countProductEvents(
                UserActivityEventType.USER_REGISTERED,
                now.minus(1, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.HOURS))).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("concurrent refresh khong the cung mint token moi tu token cu")
    void concurrentRefresh_onlyOneRotationSucceeds() throws Exception {
        User user = new User();
        user.setEmail("concurrent-refresh@example.com");
        user.setFullName("Concurrent Refresh");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPasswordHash(passwordEncoder.encode("StrongPass1"));
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        User savedUser = userRepository.saveAndFlush(user);

        String rawRefreshToken = "race-refresh-token";
        UUID familyId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(savedUser.getId());
        refreshToken.setSessionFamilyId(familyId);
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(refreshToken);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Boolean> refreshCall = () -> {
            ready.countDown();
            start.await();
            try {
                authService.refresh(rawRefreshToken);
                return true;
            } catch (Exception ex) {
                return false;
            }
        };

        Future<Boolean> first = executor.submit(refreshCall);
        Future<Boolean> second = executor.submit(refreshCall);
        ready.await();
        start.countDown();

        List<Boolean> outcomes = List.of(first.get(), second.get());
        executor.shutdownNow();

        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        refreshTokenRepository.deleteAll();
        userRepository.delete(savedUser);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("stolen-token replay sau rotation bi chan va revoke session family")
    void stolenRefreshReplayAfterRotation_revokesFamily() {
        User user = new User();
        user.setEmail("stolen-replay@example.com");
        user.setFullName("Stolen Replay");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPasswordHash(passwordEncoder.encode("StrongPass1"));
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        User savedUser = userRepository.saveAndFlush(user);

        String rawRefreshToken = "stolen-replay-refresh-token";
        UUID familyId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(savedUser.getId());
        refreshToken.setSessionFamilyId(familyId);
        refreshToken.setTokenHash(sha256(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        refreshTokenRepository.saveAndFlush(refreshToken);

        AuthService.LoginResult rotated = authService.refresh(rawRefreshToken);
        assertThat(rotated.rawRefreshToken()).isNotBlank();

        assertThatThrownBy(() -> authService.refresh(rawRefreshToken))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage("Refresh token không hợp lệ");

        List<RefreshToken> familyTokens = refreshTokenRepository.findAll().stream()
                .filter(token -> familyId.equals(token.getSessionFamilyId()))
                .toList();
        assertThat(familyTokens).hasSize(2);
        assertThat(familyTokens).allMatch(RefreshToken::isRevoked);

        refreshTokenRepository.deleteAll();
        userRepository.delete(savedUser);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
