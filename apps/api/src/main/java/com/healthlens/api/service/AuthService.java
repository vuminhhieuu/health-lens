package com.healthlens.api.service;

import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.dto.response.RefreshResponse;
import com.healthlens.api.dto.request.ForgotPasswordRequest;
import com.healthlens.api.dto.request.ResetPasswordRequest;
import com.healthlens.api.entity.EmailVerificationToken;
import com.healthlens.api.entity.PasswordResetToken;
import com.healthlens.api.entity.RefreshToken;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.WeakPasswordException;
import com.healthlens.api.exception.RateLimitExceededException;
import com.healthlens.api.exception.AccountPendingDeletionException;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.constants.ConsentConstants;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.ForgotPasswordRateLimiter;
import com.healthlens.api.security.LoginRateLimiter;
import com.healthlens.api.security.VerifyEmailRateLimiter;
import com.healthlens.api.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailEventPublisher emailEventPublisher;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter rateLimiter;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;
    private final VerifyEmailRateLimiter verifyEmailRateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final ConsentService consentService;
    private final AuditEventRecorder auditEventRecorder;

    public AuthService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailEventPublisher emailEventPublisher,
            JwtUtil jwtUtil,
            LoginRateLimiter rateLimiter,
            ForgotPasswordRateLimiter forgotPasswordRateLimiter,
            VerifyEmailRateLimiter verifyEmailRateLimiter,
            StringRedisTemplate redisTemplate,
            ConsentService consentService,
            AuditEventRecorder auditEventRecorder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailEventPublisher = emailEventPublisher;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.forgotPasswordRateLimiter = forgotPasswordRateLimiter;
        this.verifyEmailRateLimiter = verifyEmailRateLimiter;
        this.redisTemplate = redisTemplate;
        this.consentService = consentService;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional
    public UUID register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email này đã được đăng ký");
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
            throw new EmailAlreadyExistsException("Email này đã được đăng ký");
        }

        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(savedUser);
        token.setToken(tokenValue);
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        tokenRepository.save(token);

        emailEventPublisher.publishVerification(savedUser, tokenValue);

        auditEventRecorder.recordEvent(
                savedUser.getId(),
                AuditActions.REGISTER,
                AuditResourceTypes.AUTH,
                savedUser.getId(),
                Map.of("email", normalizedEmail)
        );

        return savedUser.getId();
    }

    @Transactional
    public void verifyEmail(String token, String clientIp) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token).orElse(null);

        if (verificationToken == null) {
            try {
                verifyEmailRateLimiter.consumeAttempt(clientIp, null);
            } catch (RateLimitExceededException e) {
                auditVerifyEmailFailure(null, "rate_limited");
                throw e;
            }
            auditVerifyEmailFailure(null, "invalid");
            throw new IllegalArgumentException("Không thể xác thực email bằng liên kết này.");
        }

        User user = verificationToken.getUser();
        String email = user.getEmail();
        try {
            verifyEmailRateLimiter.consumeAttempt(clientIp, email);
        } catch (RateLimitExceededException e) {
            auditVerifyEmailFailure(user.getId(), "rate_limited");
            throw e;
        }

        if (verificationToken.getUsedAt() != null || verificationToken.getExpiresAt().isBefore(Instant.now())) {
            auditVerifyEmailFailure(user.getId(), "invalid");
            throw new IllegalArgumentException("Không thể xác thực email bằng liên kết này.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsedAt(Instant.now());
        tokenRepository.save(verificationToken);

        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.VERIFY_EMAIL,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail())
        );
    }

    private void auditVerifyEmailFailure(UUID userId, String reason) {
        auditEventRecorder.recordAnonymous(
                AuditActions.VERIFY_EMAIL_FAILED,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("outcome", "failure", "reason", reason, "tokenSupplied", true)
        );
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
            if (user != null) {
                rateLimiter.recordFailure(normalizedEmail);
            } else {
                // Also record failure for non-existent emails to prevent timing attacks
                rateLimiter.recordFailure(normalizedEmail);
            }
            auditEventRecorder.recordAnonymous(
                    AuditActions.LOGIN_FAILED,
                    AuditResourceTypes.AUTH,
                    null,
                    Map.of("email", normalizedEmail, "reason", "bad_credentials")
            );
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng.");
        }

        // Check email verified (AC #1: "đã xác thực email")
        if (!user.isEmailVerified()) {
            auditEventRecorder.recordEvent(
                    user.getId(),
                    AuditActions.LOGIN_FAILED,
                    AuditResourceTypes.AUTH,
                    user.getId(),
                    Map.of("email", normalizedEmail, "reason", "email_not_verified")
            );
            throw new BadCredentialsException("Vui lòng xác thực email trước khi đăng nhập.");
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
        refreshToken.setSessionFamilyId(UUID.randomUUID());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtUtil.getRefreshTtl()));
        refreshTokenRepository.save(refreshToken);

        LoginResponse response = new LoginResponse(
                accessToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()));

        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.LOGIN,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail(), "role", user.getRole().name())
        );

        return new LoginResult(response, rawRefreshToken);
    }

    /**
     * Refresh access token using the raw refresh token from HttpOnly cookie.
     * AC #2: expired access token → use refresh token to get new pair.
     * Implements refresh token rotation (invalidate old, issue new).
     * Returns refresh response including consent information.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public RefreshResult refreshWithConsent(String rawRefreshToken) {
        RotatedRefreshToken rotated = rotateRefreshToken(rawRefreshToken);
        User user = rotated.user();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        // Fetch current consent status
        com.healthlens.api.dto.response.ConsentResponse consentStatus =
            consentService.getConsentStatus(user.getId(), ConsentConstants.ACTIVE_VERSION);

        RefreshResponse response = new RefreshResponse(
                newAccessToken,
                new RefreshResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()),
                consentStatus.isConsentGiven(),
                consentStatus.getConsentVersion()
        );

        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.REFRESH_TOKEN,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail())
        );

        return new RefreshResult(response, rotated.rawRefreshToken());
    }

    /**
     * Refresh access token using the raw refresh token from HttpOnly cookie.
     * AC #2: expired access token → use refresh token to get new pair.
     * Implements refresh token rotation (invalidate old, issue new).
     * @deprecated Use {@link #refreshWithConsent(String)} instead for new endpoints
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    @Deprecated
    public LoginResult refresh(String rawRefreshToken) {
        RotatedRefreshToken rotated = rotateRefreshToken(rawRefreshToken);
        User user = rotated.user();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        LoginResponse response = new LoginResponse(
                newAccessToken,
                new LoginResponse.UserInfo(user.getId(), user.getEmail(), user.getRole().name(), user.getFullName()));

        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.REFRESH_TOKEN,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail())
        );

        return new LoginResult(response, rotated.rawRefreshToken());
    }

    private RotatedRefreshToken rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        Instant now = Instant.now();

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseGet(() -> handleMissingActiveRefreshToken(tokenHash, now));

        if (storedToken.isExpired()) {
            refreshTokenRepository.revokeByIdIfActive(storedToken.getId(), now);
            throw new BadCredentialsException("Refresh token đã hết hạn");
        }

        int rotatedRows = refreshTokenRepository.rotateActiveToken(storedToken.getId(), now);
        if (rotatedRows != 1) {
            throw new BadCredentialsException("Refresh token không hợp lệ");
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        String newRawRefreshToken = jwtUtil.generateRefreshToken();
        String newTokenHash = sha256(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUserId(user.getId());
        newRefreshToken.setSessionFamilyId(storedToken.getSessionFamilyId());
        newRefreshToken.setTokenHash(newTokenHash);
        newRefreshToken.setExpiresAt(now.plusMillis(jwtUtil.getRefreshTtl()));
        refreshTokenRepository.save(newRefreshToken);

        return new RotatedRefreshToken(user, newRawRefreshToken);
    }

    private RefreshToken handleMissingActiveRefreshToken(String tokenHash, Instant now) {
        RefreshToken knownToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token không hợp lệ"));

        if (knownToken.isRevoked()) {
            invalidateRefreshTokenFamily(knownToken, now, "reused_refresh_token");
        }

        if (knownToken.isExpired()) {
            refreshTokenRepository.revokeByIdIfActive(knownToken.getId(), now);
            throw new BadCredentialsException("Refresh token đã hết hạn");
        }

        throw new BadCredentialsException("Refresh token không hợp lệ");
    }

    private void invalidateRefreshTokenFamily(RefreshToken token, Instant now, String reason) {
        refreshTokenRepository.revokeAllBySessionFamilyId(token.getSessionFamilyId(), now);
        auditEventRecorder.recordEvent(
                token.getUserId(),
                AuditActions.REFRESH_TOKEN_REUSE_FAILED,
                AuditResourceTypes.AUTH,
                token.getUserId(),
                Map.of(
                        "reason", reason,
                        "sessionFamilyId", token.getSessionFamilyId().toString(),
                        "refreshTokenId", token.getId().toString()
                )
        );
        throw new BadCredentialsException("Refresh token không hợp lệ");
    }

    /**
     * Logout: blacklist access token in Redis, revoke all refresh tokens.
     * AC #4: access token blacklisted (TTL = remaining expiry), refresh token
     * cookie cleared.
     */
    @Transactional
    public void logout(String accessToken) {
        UUID userId = null;
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
            String userIdStr = jwtUtil.extractSubject(accessToken);
            userId = UUID.fromString(userIdStr);
            refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        } catch (Exception e) {
            // If Redis is unavailable, still revoke refresh tokens
            try {
                String userIdStr = jwtUtil.extractSubject(accessToken);
                userId = UUID.fromString(userIdStr);
                refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
            } catch (Exception ignored) {
                // Token may be expired/invalid at logout — acceptable
            }
        }

        if (userId != null) {
            auditEventRecorder.recordEvent(
                    userId,
                    AuditActions.LOGOUT,
                    AuditResourceTypes.AUTH,
                    userId,
                    Map.of()
            );
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

                emailEventPublisher.publishPasswordReset(user, tokenValue);
                auditEventRecorder.recordEvent(
                        user.getId(),
                        AuditActions.FORGOT_PASSWORD,
                        AuditResourceTypes.AUTH,
                        user.getId(),
                        Map.of("email", normalizedEmail)
                );
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
                .orElseThrow(() -> new BadCredentialsException("Token không hợp lệ hoặc đã hết hạn"));

        if (token.isUsed() || token.isExpired()) {
            throw new BadCredentialsException("Token không hợp lệ hoặc đã hết hạn");
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

        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.RESET_PASSWORD,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail())
        );
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[A-Z].*")
                || !password.matches(".*\\d.*")) {
            throw new WeakPasswordException("Mật khẩu phải có ít nhất 8 ký tự, gồm 1 chữ hoa và 1 chữ số");
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
    private record RotatedRefreshToken(User user, String rawRefreshToken) {
    }

    public record LoginResult(LoginResponse response, String rawRefreshToken) {
    }

    /**
     * Result record for refresh endpoint, including consent information.
     */
    public record RefreshResult(RefreshResponse response, String rawRefreshToken) {
    }
}
