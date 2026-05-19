package com.healthlens.api.security;

import com.healthlens.api.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

@Component
public class VerifyEmailRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(VerifyEmailRateLimiter.class);
    private static final String KEY_PREFIX = "verify_email_attempts:";
    private static final int MAX_IP_ATTEMPTS = 10;
    private static final int MAX_EMAIL_ATTEMPTS = 5;
    private static final Duration WINDOW_DURATION = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public VerifyEmailRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void consumeAttempt(String clientIp, String email) {
        consumeBucket(ipKey(clientIp), MAX_IP_ATTEMPTS);
        if (email != null && !email.isBlank()) {
            consumeBucket(emailKey(email), MAX_EMAIL_ATTEMPTS);
        }
    }

    public void checkRateLimit(String clientIp, String email) {
        checkBucket(ipKey(clientIp), MAX_IP_ATTEMPTS);
        if (email != null && !email.isBlank()) {
            checkBucket(emailKey(email), MAX_EMAIL_ATTEMPTS);
        }
    }

    private void checkBucket(String key, int maxAttempts) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= maxAttempts) {
                throw rateLimitExceeded(key);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when checking verify email rate limit. Fail-closed: {}", failClosed, e);
            if (failClosed) {
                throw new RateLimitExceededException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", WINDOW_DURATION.toSeconds());
            }
        }
    }

    private void consumeBucket(String key, int maxAttempts) {
        try {
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, WINDOW_DURATION);
            }
            if (attempts != null && attempts > maxAttempts) {
                throw rateLimitExceeded(key);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when consuming verify email rate limit bucket. Fail-closed: {}", failClosed, e);
            if (failClosed) {
                throw new RateLimitExceededException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", WINDOW_DURATION.toSeconds());
            }
        }
    }

    private RateLimitExceededException rateLimitExceeded(String key) {
        Long ttl = redisTemplate.getExpire(key);
        long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : WINDOW_DURATION.toSeconds();
        return new RateLimitExceededException(
                "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                retryAfterSeconds
        );
    }

    private static String ipKey(String clientIp) {
        String normalizedIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        return KEY_PREFIX + "ip:" + normalizedIp;
    }

    private static String emailKey(String email) {
        return KEY_PREFIX + "email:" + email.trim().toLowerCase(Locale.ROOT);
    }
}
