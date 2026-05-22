package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.UserTotpDisableRequest;
import com.healthlens.api.dto.response.UserTotpSetupResponse;
import com.healthlens.api.dto.response.UserTotpStatusResponse;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.entity.UserTotpSecret;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.repository.UserTotpSecretRepository;
import com.healthlens.api.security.UserTotpRateLimiter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserTotpService {

    private static final Logger log = LoggerFactory.getLogger(UserTotpService.class);
    private static final String ISSUER = "HealthLens";
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 10;

    private final UserRepository userRepository;
    private final UserTotpSecretRepository totpSecretRepository;
    private final UserTotpSecretCryptoService cryptoService;
    private final PasswordEncoder passwordEncoder;
    private final UserTotpRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final GoogleAuthenticator googleAuth;
    private final AuditEventRecorder auditEventRecorder;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserTotpService(
            UserRepository userRepository,
            UserTotpSecretRepository totpSecretRepository,
            UserTotpSecretCryptoService cryptoService,
            PasswordEncoder passwordEncoder,
            UserTotpRateLimiter rateLimiter,
            StringRedisTemplate redisTemplate,
            AuditEventRecorder auditEventRecorder,
            ObjectMapper objectMapper) {
        this(userRepository, totpSecretRepository, cryptoService, passwordEncoder, rateLimiter, redisTemplate,
                new GoogleAuthenticator(), auditEventRecorder, objectMapper);
    }

    UserTotpService(
            UserRepository userRepository,
            UserTotpSecretRepository totpSecretRepository,
            UserTotpSecretCryptoService cryptoService,
            PasswordEncoder passwordEncoder,
            UserTotpRateLimiter rateLimiter,
            StringRedisTemplate redisTemplate,
            GoogleAuthenticator googleAuth,
            AuditEventRecorder auditEventRecorder,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.totpSecretRepository = totpSecretRepository;
        this.cryptoService = cryptoService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.redisTemplate = redisTemplate;
        this.googleAuth = googleAuth;
        this.auditEventRecorder = auditEventRecorder;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public UserTotpStatusResponse getStatus(UUID userId) {
        Optional<UserTotpSecret> totpOpt = totpSecretRepository.findByUserId(userId);
        if (totpOpt.isEmpty()) {
            return new UserTotpStatusResponse(false, false);
        }
        UserTotpSecret secret = totpOpt.get();
        return new UserTotpStatusResponse(secret.isVerified(), !secret.isVerified());
    }

    public boolean isVerifiedEnabled(UUID userId) {
        return totpSecretRepository.findByUserId(userId)
                .map(UserTotpSecret::isVerified)
                .orElse(false);
    }

    @Transactional
    public UserTotpSetupResponse setup(UUID userId) {
        User user = requireNonAdminUser(userId);

        totpSecretRepository.findByUserId(userId)
                .filter(UserTotpSecret::isVerified)
                .ifPresent(secret -> {
                    throw new BadCredentialsException(
                            "Xác thực hai yếu tố đã được bật. Vui lòng tắt trước khi thiết lập lại.");
                });

        GoogleAuthenticatorKey key = googleAuth.createCredentials();
        String plainSecret = key.getKey();

        List<String> backupCodesPlain = generateBackupCodes();
        String backupHashesJson = hashBackupCodes(backupCodesPlain);

        UserTotpSecret totpSecret = totpSecretRepository.findByUserId(userId).orElse(new UserTotpSecret());
        totpSecret.setUserId(userId);
        totpSecret.setEncryptedSecret(cryptoService.encrypt(plainSecret));
        totpSecret.setBackupCodesHash(backupHashesJson);
        totpSecret.setVerified(false);
        totpSecret.setCreatedAt(totpSecret.getCreatedAt() != null ? totpSecret.getCreatedAt() : Instant.now());
        totpSecret.setUpdatedAt(Instant.now());
        totpSecretRepository.save(totpSecret);

        String otpauthUri = buildOtpauthUri(user.getEmail(), plainSecret);

        log.info("User TOTP setup initiated for user: {}", userId);
        return new UserTotpSetupResponse(plainSecret, otpauthUri, backupCodesPlain);
    }

    @Transactional
    public void verifySetup(UUID userId, String code) {
        User user = requireNonAdminUser(userId);
        rateLimiter.checkLocked(userId);

        UserTotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new BadCredentialsException("Chưa thiết lập xác thực hai yếu tố."));

        if (totpSecret.isVerified()) {
            throw new BadCredentialsException("Xác thực hai yếu tố đã được bật.");
        }

        if (!validateSubmittedCode(totpSecret, userId, code)) {
            rateLimiter.recordFailure(userId);
            auditEventRecorder.recordEvent(
                    userId,
                    AuditActions.USER_TOTP_VERIFY_FAILED,
                    AuditResourceTypes.AUTH,
                    userId,
                    Map.of("email", user.getEmail(), "context", "setup")
            );
            throw new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn.");
        }

        totpSecret.setVerified(true);
        totpSecret.setUpdatedAt(Instant.now());
        totpSecretRepository.save(totpSecret);
        rateLimiter.resetAttempts(userId);

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.USER_TOTP_ENABLED,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", user.getEmail())
        );
        log.info("User TOTP enabled for user: {}", userId);
    }

    @Transactional
    public void disable(UUID userId, UserTotpDisableRequest request) {
        User user = requireNonAdminUser(userId);
        rateLimiter.checkLocked(userId);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu không đúng.");
        }

        UserTotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .filter(UserTotpSecret::isVerified)
                .orElseThrow(() -> new BadCredentialsException("Xác thực hai yếu tố chưa được bật."));

        if (!validateSubmittedCode(totpSecret, userId, request.code())) {
            rateLimiter.recordFailure(userId);
            auditEventRecorder.recordEvent(
                    userId,
                    AuditActions.USER_TOTP_VERIFY_FAILED,
                    AuditResourceTypes.AUTH,
                    userId,
                    Map.of("email", user.getEmail(), "context", "disable")
            );
            throw new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn.");
        }

        totpSecretRepository.delete(totpSecret);
        rateLimiter.resetAttempts(userId);

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.USER_TOTP_DISABLED,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", user.getEmail())
        );
        log.info("User TOTP disabled for user: {}", userId);
    }

    /**
     * Validates TOTP or backup code during login second step.
     */
    @Transactional
    public User validateLoginTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại."));

        rateLimiter.checkLocked(userId);

        UserTotpSecret totpSecret = totpSecretRepository.findByUserId(userId)
                .filter(UserTotpSecret::isVerified)
                .orElseThrow(() -> new BadCredentialsException("Xác thực hai yếu tố chưa được bật."));

        if (!validateSubmittedCode(totpSecret, userId, code)) {
            rateLimiter.recordFailure(userId);
            auditEventRecorder.recordEvent(
                    userId,
                    AuditActions.USER_TOTP_VERIFY_FAILED,
                    AuditResourceTypes.AUTH,
                    userId,
                    Map.of("email", user.getEmail(), "context", "login")
            );
            throw new BadCredentialsException("Mã xác thực không hợp lệ hoặc đã hết hạn.");
        }

        rateLimiter.resetAttempts(userId);
        return user;
    }

    private boolean validateSubmittedCode(UserTotpSecret totpSecret, UUID userId, String code) {
        String submitted = code.trim().toUpperCase();

        if (submitted.length() > 6) {
            if (consumeBackupCode(totpSecret, submitted)) {
                totpSecret.setUpdatedAt(Instant.now());
                totpSecretRepository.save(totpSecret);
                log.info("Backup code used for user: {}", userId);
                return true;
            }
        }

        String secret = cryptoService.decrypt(totpSecret.getEncryptedSecret());
        int totpCode;
        try {
            totpCode = Integer.parseInt(submitted);
        } catch (NumberFormatException e) {
            return false;
        }

        if (!googleAuth.authorize(secret, totpCode)) {
            return false;
        }

        String replayKey = "user_totp_used:" + userId + ":" + totpCode;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(replayKey))) {
            return false;
        }
        redisTemplate.opsForValue().set(replayKey, "1", Duration.ofMinutes(2));
        return true;
    }

    private boolean consumeBackupCode(UserTotpSecret totpSecret, String submittedCode) {
        List<String> hashes = parseBackupHashes(totpSecret.getBackupCodesHash());
        if (hashes.isEmpty()) {
            return false;
        }

        int matchedIndex = -1;
        for (int i = 0; i < hashes.size(); i++) {
            if (passwordEncoder.matches(submittedCode, hashes.get(i))) {
                matchedIndex = i;
                break;
            }
        }
        if (matchedIndex < 0) {
            return false;
        }

        hashes.remove(matchedIndex);
        totpSecret.setBackupCodesHash(serializeBackupHashes(hashes));
        return true;
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    private String hashBackupCodes(List<String> plainCodes) {
        List<String> hashes = plainCodes.stream().map(passwordEncoder::encode).toList();
        return serializeBackupHashes(hashes);
    }

    private String serializeBackupHashes(List<String> hashes) {
        try {
            return objectMapper.writeValueAsString(hashes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize backup code hashes", e);
        }
    }

    private List<String> parseBackupHashes(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String buildOtpauthUri(String email, String secret) {
        String encodedIssuer = encodeOtpauthComponent(ISSUER);
        String encodedEmail = encodeOtpauthComponent(email);
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodedIssuer,
                encodedEmail,
                secret,
                encodedIssuer);
    }

    private static String encodeOtpauthComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private User requireNonAdminUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            throw new AccessDeniedException(
                    "Tài khoản quản trị dùng xác thực hai yếu tố riêng khi đăng nhập khu vực Admin.");
        }
        return user;
    }
}
