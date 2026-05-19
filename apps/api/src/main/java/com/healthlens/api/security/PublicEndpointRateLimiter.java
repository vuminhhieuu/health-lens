package com.healthlens.api.security;

import com.healthlens.api.exception.RateLimitExceededException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublicEndpointRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PublicEndpointRateLimiter.class);
    private static final String KEY_PREFIX = "public_endpoint_rate:";
    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public PublicEndpointRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void consumeRegister(String clientIp, String email) {
        consume("register:ip:" + normalize(clientIp), 5, HOUR);
        if (email != null && !email.isBlank()) {
            consume("register:email:" + normalizeEmail(email), 3, HOUR);
        }
    }

    public void consumeProfileInvitationAccept(String clientIp, String token) {
        consume("profile-invitation-accept:ip:" + normalize(clientIp), 20, HOUR);
        consume("profile-invitation-accept:token:" + tokenFingerprint(token), 5, HOUR);
    }

    public void consumeHealthRecordInvitationAccept(String clientIp, String token) {
        consume("health-record-invitation-accept:ip:" + normalize(clientIp), 20, HOUR);
        consume("health-record-invitation-accept:token:" + tokenFingerprint(token), 5, HOUR);
    }

    public void consumeCancelDeletion(String clientIp, String token) {
        consume("cancel-deletion:ip:" + normalize(clientIp), 20, HOUR);
        if (token != null && !token.isBlank()) {
            consume("cancel-deletion:token:" + tokenFingerprint(token), 5, HOUR);
        }
    }

    public void consumeOcrTrigger(String userId, String recordId) {
        consume("ocr-trigger:user:" + normalize(userId), 30, HOUR);
        consume("ocr-trigger:record:" + normalize(userId) + ":" + normalize(recordId), 10, MINUTE);
    }

    private void consume(String keySuffix, int maxAttempts, Duration window) {
        String key = KEY_PREFIX + keySuffix;
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1L) {
                Boolean expirySet = redisTemplate.expire(key, window);
                if (!Boolean.TRUE.equals(expirySet)) {
                    redisTemplate.delete(key);
                    if (failClosed) {
                        throw new RateLimitExceededException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", window.toSeconds());
                    }
                    return;
                }
            }
            if (attempts != null && attempts > maxAttempts) {
                throw rateLimitExceeded(key, window);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when consuming public endpoint rate limit. keySuffix={} failClosed={}",
                    keySuffix, failClosed, e);
            if (failClosed) {
                throw new RateLimitExceededException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", window.toSeconds());
            }
        }
    }

    private RateLimitExceededException rateLimitExceeded(String key, Duration fallbackWindow) {
        Long ttl;
        try {
            ttl = redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.warn("Unable to read retry-after TTL for public endpoint rate limit key={}", key, e);
            ttl = fallbackWindow.toSeconds();
        }
        if (ttl == null || ttl <= 0) {
            try {
                redisTemplate.expire(key, fallbackWindow);
            } catch (Exception e) {
                log.warn("Unable to repair missing TTL for public endpoint rate limit key={}", key, e);
            }
        }
        long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : fallbackWindow.toSeconds();
        return new RateLimitExceededException(
                "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                retryAfterSeconds
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeEmail(String value) {
        return normalize(value);
    }

    private static String tokenFingerprint(String token) {
        String normalizedToken = token == null || token.isBlank() ? "missing" : token.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalizedToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required for token rate-limit fingerprints", e);
        }
    }
}
