package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.AdminLoginRequest;
import com.healthlens.api.dto.response.AdminLoginResponse;
import com.healthlens.api.dto.response.AdminTotpSetupResponse;
import com.healthlens.api.entity.AdminTotpSecret;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.AdminTotpSecretRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.AdminAuthRateLimiter;
import com.healthlens.api.util.JwtUtil;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";
    private static final String ISSUER = "HealthLens";

    private final UserRepository userRepository;
    private final AdminTotpSecretRepository totpSecretRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TotpSecretCryptoService cryptoService;
    private final AdminAuthRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final GoogleAuthenticator googleAuth;
    private final AuditEventRecorder auditEventRecorder;

    @Autowired
    public AdminAuthService(
            UserRepository userRepository,
            AdminTotpSecretRepository totpSecretRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TotpSecretCryptoService cryptoService,
            AdminAuthRateLimiter rateLimiter,
            StringRedisTemplate redisTemplate,
            AuditEventRecorder auditEventRecorder) {
        this(userRepository, totpSecretRepository, passwordEncoder, jwtUtil, cryptoService, rateLimiter, redisTemplate, new GoogleAuthenticator(), auditEventRecorder);
    }

    // Constructor for testing
    AdminAuthService(
            UserRepository userRepository,
            AdminTotpSecretRepository totpSecretRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TotpSecretCryptoService cryptoService,
            AdminAuthRateLimiter rateLimiter,
            StringRedisTemplate redisTemplate,
            GoogleAuthenticator googleAuth,
            AuditEventRecorder auditEventRecorder) {
        this.userRepository = userRepository;
        this.totpSecretRepository = totpSecretRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.cryptoService = cryptoService;
        this.rateLimiter = rateLimiter;
        this.redisTemplate = redisTemplate;
        this.googleAuth = googleAuth;
        this.auditEventRecorder = auditEventRecorder;
    }

    /**
     * Admin login: validate password + optionally TOTP.
     * Returns a response indicating what step is needed next.
     */
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        rateLimiter.checkLocked(request.email());

        // Verify admin credentials
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(request.email());
                    return new BadCredentialsException("Email hoặc mật khẩu không đúng.");
                });

        if (user.getRole() != UserRole.ROLE_ADMIN) {
            rateLimiter.recordFailure(request.email());
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(request.email());
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng.");
        }

        // Check TOTP status
        Optional<AdminTotpSecret> totpOpt = totpSecretRepository.findByUserId(user.getId());

        if (totpOpt.isEmpty()) {
            // No TOTP set up — issue a temporary token (totpVerified=false) for setup flow
            String tempToken = jwtUtil.generateAdminAccessToken(user, false);
            rateLimiter.resetAttempts(request.email());
            return new AdminLoginResponse(tempToken, false, true, user.getEmail());
        }

        AdminTotpSecret totpSecret = totpOpt.get();

        if (!totpSecret.isVerified()) {
            // TOTP created but not yet verified — need setup completion
            String tempToken = jwtUtil.generateAdminAccessToken(user, false);
            rateLimiter.resetAttempts(request.email());
            return new AdminLoginResponse(tempToken, false, true, user.getEmail());
        }

        // TOTP is set up and verified — require TOTP code
        if (request.totpCode() == null || request.totpCode().isBlank()) {
            // Password correct, but TOTP code not provided yet
            String tempToken = jwtUtil.generateAdminAccessToken(user, false);
            rateLimiter.resetAttempts(request.email());
            return new AdminLoginResponse(tempToken, true, false, user.getEmail());
        }

        // Validate TOTP code or Backup code
        boolean isValidCode = false;
        String submittedCode = request.totpCode().trim().toUpperCase();

        if (submittedCode.length() > 6 && totpSecret.getBackupCodes() != null && !totpSecret.getBackupCodes().isEmpty()) {
            java.util.List<String> backupCodesList = new java.util.ArrayList<>(java.util.Arrays.asList(totpSecret.getBackupCodes().split(",")));
            if (backupCodesList.contains(submittedCode)) {
                isValidCode = true;
                backupCodesList.remove(submittedCode);
                totpSecret.setBackupCodes(backupCodesList.isEmpty() ? null : String.join(",", backupCodesList));
                totpSecretRepository.save(totpSecret);
                log.info("Backup code used for admin user: {}", user.getId());
            }
        }

        if (!isValidCode) {
            String secret = cryptoService.decrypt(totpSecret.getEncryptedSecret());
            int code;
            try {
                code = Integer.parseInt(submittedCode);
            } catch (NumberFormatException e) {
                rateLimiter.recordFailure(request.email());
                throw new BadCredentialsException("Mã xác thực không hợp lệ");
            }

            if (!googleAuth.authorize(secret, code)) {
                rateLimiter.recordFailure(request.email());
                throw new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn");
            }

            // Prevent replay attacks for TOTP
            String replayKey = "totp_used:" + user.getId() + ":" + code;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(replayKey))) {
                rateLimiter.recordFailure(request.email());
                throw new BadCredentialsException("Mã xác thực đã được sử dụng, vui lòng đợi mã mới");
            }
            redisTemplate.opsForValue().set(replayKey, "1", Duration.ofMinutes(2));
        }

        // Full authentication successful
        String accessToken = jwtUtil.generateAdminAccessToken(user, true);
        rateLimiter.resetAttempts(request.email());
        log.info("Admin login successful for user: {}", user.getId());
        auditEventRecorder.recordEvent(
                user.getId(),
                AuditActions.ADMIN_LOGIN,
                AuditResourceTypes.AUTH,
                user.getId(),
                Map.of("email", user.getEmail())
        );
        return new AdminLoginResponse(accessToken, false, false, user.getEmail());
    }

    /**
     * Generate TOTP setup: create a new TOTP secret + QR code URL.
     * Requires a valid admin session (password already verified).
     */
    @Transactional
    public AdminTotpSetupResponse setupTotp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadCredentialsException("Không có quyền admin");
        }

        // Generate new TOTP credentials
        GoogleAuthenticatorKey key = googleAuth.createCredentials();
        String secret = key.getKey();

        // Generate 16 Backup Codes
        java.util.List<String> generatedCodes = new java.util.ArrayList<>();
        for (int i = 0; i < 16; i++) {
            String backupCode = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            generatedCodes.add(backupCode);
        }
        String backupCodesStr = String.join(",", generatedCodes);

        // Store encrypted secret and backup codes
        AdminTotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .orElse(new AdminTotpSecret());
        totpSecret.setUserId(userId);
        totpSecret.setEncryptedSecret(cryptoService.encrypt(secret));
        totpSecret.setBackupCodes(backupCodesStr);
        totpSecret.setVerified(false);
        totpSecret.setCreatedAt(Instant.now());
        totpSecretRepository.save(totpSecret);

        // Build QR code URL (otpauth:// URI format)
        String encodedIssuer = encodeOtpauthComponent(ISSUER);
        String encodedEmail = encodeOtpauthComponent(user.getEmail());
        String qrCodeUrl = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedIssuer,
                encodedEmail,
                secret,
                encodedIssuer);

        log.info("TOTP setup initiated for admin user: {}", userId);
        auditEventRecorder.recordEvent(
                userId,
                AuditActions.ADMIN_TOTP_SETUP,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", user.getEmail())
        );
        return new AdminTotpSetupResponse(secret, qrCodeUrl, generatedCodes);
    }

    /**
     * Verify TOTP code during setup. Marks TOTP as verified and issues full admin token.
     */
    @Transactional
    public AdminLoginResponse verifyTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        rateLimiter.checkLocked(user.getEmail());

        AdminTotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(user.getEmail());
                    return new BadCredentialsException("Chưa thiết lập TOTP");
                });

        boolean isValidCode = false;
        String submittedCode = code.trim().toUpperCase();

        if (submittedCode.length() > 6 && totpSecret.getBackupCodes() != null && !totpSecret.getBackupCodes().isEmpty()) {
            java.util.List<String> backupCodesList = new java.util.ArrayList<>(java.util.Arrays.asList(totpSecret.getBackupCodes().split(",")));
            if (backupCodesList.contains(submittedCode)) {
                isValidCode = true;
                backupCodesList.remove(submittedCode);
                totpSecret.setBackupCodes(backupCodesList.isEmpty() ? null : String.join(",", backupCodesList));
            }
        }

        if (!isValidCode) {
            String secret = cryptoService.decrypt(totpSecret.getEncryptedSecret());
            int totpCode;
            try {
                totpCode = Integer.parseInt(submittedCode);
            } catch (NumberFormatException e) {
                rateLimiter.recordFailure(user.getEmail());
                throw new BadCredentialsException("Mã xác thực không hợp lệ");
            }

            if (!googleAuth.authorize(secret, totpCode)) {
                rateLimiter.recordFailure(user.getEmail());
                throw new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn");
            }

            // Prevent replay attacks
            String replayKey = "totp_used:" + user.getId() + ":" + code;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(replayKey))) {
                rateLimiter.recordFailure(user.getEmail());
                throw new BadCredentialsException("Mã xác thực đã được sử dụng, vui lòng đợi mã mới");
            }
            redisTemplate.opsForValue().set(replayKey, "1", Duration.ofMinutes(2));
        }

        // Mark TOTP as verified
        totpSecret.setVerified(true);
        totpSecretRepository.save(totpSecret);

        // Issue full admin token
        String accessToken = jwtUtil.generateAdminAccessToken(user, true);
        rateLimiter.resetAttempts(user.getEmail());
        log.info("TOTP verified successfully for admin user: {}", userId);
        auditEventRecorder.recordEvent(
                userId,
                AuditActions.ADMIN_TOTP_VERIFY,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", user.getEmail())
        );
        auditEventRecorder.recordEvent(
                userId,
                AuditActions.ADMIN_LOGIN,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", user.getEmail(), "via", "totp_setup")
        );
        return new AdminLoginResponse(accessToken, false, false, user.getEmail());
    }

    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            String jti = jwtUtil.extractJti(accessToken);
            Instant expiry = jwtUtil.extractClaims(accessToken).getExpiration().toInstant();
            long ttlSeconds = Math.max(1, Instant.now().until(expiry, ChronoUnit.SECONDS));
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to blacklist admin access token on logout", e);
        }
    }

    private static String encodeOtpauthComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
