package com.healthlens.api.security;

import com.healthlens.api.exception.AccountLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Rate limiter for user TOTP verification (setup, login step, disable).
 * 5 failed attempts per 15 minutes per user.
 */
@Component
public class UserTotpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(UserTotpRateLimiter.class);
    private static final String KEY_PREFIX = "user_totp_verify:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public UserTotpRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void checkLocked(UUID userId) {
        try {
            String key = KEY_PREFIX + userId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= MAX_ATTEMPTS) {
                Long ttl = redisTemplate.getExpire(key);
                long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : LOCK_DURATION.toSeconds();
                throw new AccountLockedException(
                        "Bạn đã nhập sai mã xác thực quá nhiều lần. Vui lòng thử lại sau "
                                + retryAfterSeconds + " giây.",
                        retryAfterSeconds
                );
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when checking user TOTP lock. Fail-closed: {}. User: {}", failClosed, userId, e);
            if (failClosed) {
                throw new AccountLockedException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", LOCK_DURATION.toSeconds());
            }
        }
    }

    public void recordFailure(UUID userId) {
        try {
            String key = KEY_PREFIX + userId;
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, LOCK_DURATION);
            }
        } catch (Exception e) {
            log.error("Redis unavailable when recording user TOTP failure. User: {}", userId, e);
            if (failClosed) {
                throw new AccountLockedException("Hệ thống tạm ngưng. Vui lòng thử lại sau.", LOCK_DURATION.toSeconds());
            }
        }
    }

    public void resetAttempts(UUID userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Redis unavailable when resetting user TOTP attempts. User: {}", userId, e);
        }
    }
}
