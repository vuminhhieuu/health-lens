package com.healthlens.api.service;

import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.dto.response.RefreshResponse;
import com.healthlens.api.dto.request.ForgotPasswordRequest;
import com.healthlens.api.dto.request.ResetPasswordRequest;
import com.healthlens.api.dto.event.EmailEvent;
import com.healthlens.api.entity.EmailVerificationToken;
import com.healthlens.api.entity.PasswordResetToken;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.WeakPasswordException;
import com.healthlens.api.exception.AccountPendingDeletionException;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.constants.ConsentConstants;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.ForgotPasswordRateLimiter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService {

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter rateLimiter;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final ConsentService consentService;
    private final String emailEventStream;

    public AuthService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtUtil jwtUtil,
            LoginRateLimiter rateLimiter,
            ForgotPasswordRateLimiter forgotPasswordRateLimiter,
            StringRedisTemplate redisTemplate,
            ConsentService consentService,
            @Value("${app.stream.email-events:email.events}") String emailEventStream
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.forgotPasswordRateLimiter = forgotPasswordRateLimiter;
        this.redisTemplate = redisTemplate;
        this.consentService = consentService;
        this.emailEventStream = emailEventStream;
    }

    @Transactional
    public UUID register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email nay da duoc dang ky");
        }

        validatePasswordPolicy(request.password());

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setBirthDate(request.birthDate());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        user.setRole(UserRole.ROLE_USER);

        User savedUser;
        try {
            // Flush ngay để bắt race condition unique email trong transaction hiện tại.
            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException("Email nay da duoc dang ky");
        }

        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(savedUser);
        token.setToken(tokenValue);
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(token);

        publishVerificationEmailEvent(savedUser, tokenValue);

        return savedUser.getId();
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token xác thực không hợp lệ"));

        if (verificationToken.getUsedAt() != null || verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token xác thực đã hết hạn hoặc đã được sử dụng");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);
    }

    /**
     * Authenticate user and generate token pair.
     * AC #1: email/password → access token (15m) + refresh token (7d, HttpOnly
     * cookie)
     * AC #5: generic error message, does not leak email existence
     * AC #6: rate limit check before authentication
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // AC #6: Rate limiting check
        rateLimiter.checkLocked(normalizedEmail);

        // Find user — generic error if not found (AC #5)
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        if (user != null && user.getAccountStatus() == AccountStatus.PENDING_DELETION) {
            throw new AccountPendingDeletionException();
        }

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(normalizedEmail);
            throw new BadCredentialsException("Email hoac mat khau khong dung");
        }

        // Check email verified (AC #1: "đã xác thực email")
        if (!user.isEmailVerified()) {
            throw new BadCredentialsException("Vui long xac thuc email truoc khi dang nhap");
        }

        // Reset rate limiter on success
        rateLimiter.resetAttempts(normalizedEmail);

        // Generate access token
        String accessToken = jwtUtil.generateAccessToken(user);

        // Generate refresh token and store hash in DB
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTtl()));
        refreshTokenRepository.save(refreshToken);

        LoginResponse response = new LoginResponse(
                accessToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name()));

        return new LoginResult(response, rawRefreshToken);
    }

    /**
     * Refresh access token using the raw refresh token from HttpOnly cookie.
     * AC #2: expired access token → use refresh token to get new pair.
     * Implements refresh token rotation (invalidate old, issue new).
     * Returns refresh response including consent information.
     */
    @Transactional
    public RefreshResult refreshWithConsent(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token khong hop le"));

        if (storedToken.isExpired()) {
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("Refresh token da het han");
        }

        // Revoke old refresh token (rotation)
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        // Find user and generate new tokens
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User khong ton tai"));

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRawRefreshToken = jwtUtil.generateRefreshToken();
        String newTokenHash = sha256(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setTokenHash(newTokenHash);
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTtl()));
        refreshTokenRepository.save(newRefreshToken);

        // Fetch current consent status
        com.healthlens.api.dto.response.ConsentResponse consentStatus =
            consentService.getConsentStatus(user.getId(), ConsentConstants.ACTIVE_VERSION);

        RefreshResponse response = new RefreshResponse(
                newAccessToken,
                new RefreshResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name()),
                consentStatus.isConsentGiven(),
                consentStatus.getConsentVersion()
        );

        return new RefreshResult(response, newRawRefreshToken);
    }

    /**
     * Refresh access token using the raw refresh token from HttpOnly cookie.
     * AC #2: expired access token → use refresh token to get new pair.
     * Implements refresh token rotation (invalidate old, issue new).
     * @deprecated Use {@link #refreshWithConsent(String)} instead for new endpoints
     */
    @Transactional
    @Deprecated
    public LoginResult refresh(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token khong hop le"));

        if (storedToken.isExpired()) {
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
            throw new BadCredentialsException("Refresh token da het han");
        }

        // Revoke old refresh token (rotation)
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        // Find user and generate new tokens
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User khong ton tai"));

        String newAccessToken = jwtUtil.generateAccessToken(user);
        String newRawRefreshToken = jwtUtil.generateRefreshToken();
        String newTokenHash = sha256(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setTokenHash(newTokenHash);
        newRefreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTtl()));
        refreshTokenRepository.save(newRefreshToken);

        LoginResponse response = new LoginResponse(
                newAccessToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name()));

        return new LoginResult(response, newRawRefreshToken);
    }

    /**
     * Logout: blacklist access token in Redis, revoke all refresh tokens.
     * AC #4: access token blacklisted (TTL = remaining expiry), refresh token
     * cookie cleared.
     */
    @Transactional
    public void logout(String accessToken) {
        try {
            String jti = jwtUtil.extractJti(accessToken);
            long remainingMs = jwtUtil.getRemainingExpiry(accessToken);

            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_KEY_PREFIX + jti,
                        "1",
                        remainingMs,
                        TimeUnit.MILLISECONDS);
            }

            // Revoke all refresh tokens for this user
            String userId = jwtUtil.extractSubject(accessToken);
            refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId), Instant.now());
        } catch (Exception e) {
            // If Redis is unavailable, still revoke refresh tokens
            try {
                String userId = jwtUtil.extractSubject(accessToken);
                refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId), Instant.now());
            } catch (Exception ignored) {
                // Token may be expired/invalid at logout — acceptable
            }
        }
    }

    /**
     * Forgot password: check rate limit, generate token, send email. (AC #1, #2)
     * Always returns success to prevent user enumeration.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.info("[AuthService] Processing forgot password request for email: {}", normalizedEmail);

        // AC #2: Rate limiting
        forgotPasswordRateLimiter.checkRateLimit(normalizedEmail);

        try {
            User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
            
            if (user != null) {
                log.info("[AuthService] User found: {}. Generating reset token.", user.getEmail());
                // AC #1: Generate reset token
                String tokenValue = UUID.randomUUID().toString();
                PasswordResetToken token = new PasswordResetToken();
                token.setUser(user);
                token.setToken(tokenValue);
                token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
                passwordResetTokenRepository.save(token);

                // Send email
                emailService.sendPasswordResetEmail(user, tokenValue);
            } else {
                log.info("[AuthService] User NOT found for email: {}. Skipping email for security reasons.", normalizedEmail);
            }
        } catch (Exception e) {
            // AC #5: generic success response to keep responses indistinguishable even if SMTP/DB fails
            log.error("[AuthService] Error during forgot password processing for {}: {}", normalizedEmail, e.getMessage());
        } finally {
            // Record request for rate limiting (even if user not found or error occurred)
            forgotPasswordRateLimiter.recordRequest(normalizedEmail);
        }
    }

    /**
     * Reset password: validate token, update password, revoke tokens. (AC #3, #4, #5, #6)
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BadCredentialsException("Token khong hop le hoac da het han"));

        if (token.isUsed() || token.isExpired()) {
            throw new BadCredentialsException("Token khong hop le hoac da het han");
        }

        // AC #3: Update password
        validatePasswordPolicy(request.newPassword());
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // AC #3: Mark token as used
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        // AC #6: Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Mat khau phai co it nhat 8 ky tu, gom 1 chu hoa va 1 chu so");
        }
    }

    private void publishVerificationEmailEvent(User user, String token) {
        EmailEvent emailEvent = new EmailEvent(
                "verification",
                user.getId(),
                user.getEmail(),
                Map.of("token", token));

        // Publish after transaction commit so consumer never reads uncommitted user/token data.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishEmailEvent(emailEvent);
                }
            });
            return;
        }

        publishEmailEvent(emailEvent);
    }

    private void publishEmailEvent(EmailEvent emailEvent) {
        try {
            redisTemplate.opsForStream().add(emailEventStream, emailEvent.toStreamMap());
        } catch (Exception ex) {
            // Registration should stay fast and resilient even when Redis is unavailable.
            log.warn("[AuthService] Cannot publish verification email event for user {}", emailEvent.userId(), ex);
        }
    }

    /**
     * Hash a refresh token using SHA-256 (never store raw tokens in DB).
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Result record pairing the API response with the raw refresh token (for
     * HttpOnly cookie).
     */
    public record LoginResult(LoginResponse response, String rawRefreshToken) {
    }

    /**
     * Result record for refresh endpoint, including consent information.
     */
    public record RefreshResult(RefreshResponse response, String rawRefreshToken) {
    }
}
