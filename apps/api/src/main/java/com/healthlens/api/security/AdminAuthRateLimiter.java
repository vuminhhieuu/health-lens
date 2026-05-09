package com.healthlens.api.security;

import com.healthlens.api.exception.AccountLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Rate limiter for admin authentication attempts.
 * More restrictive than regular login: 3 max attempts, 30 min lock.
 */
@Component
public class AdminAuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthRateLimiter.class);
    private static final String KEY_PREFIX = "admin_login_attempts:";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public AdminAuthRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void checkLocked(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= MAX_ATTEMPTS) {
                Long ttl = redisTemplate.getExpire(key);
                long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : LOCK_DURATION.toSeconds();
                throw new AccountLockedException(
                        "Tai khoan admin bi khoa tam thoi. Thu lai sau " + retryAfterSeconds + " giay.",
                        retryAfterSeconds
                );
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when checking admin lock. Fail-closed: {}. Email: {}", failClosed, email, e);
            if (failClosed) {
                throw new AccountLockedException("He thong tam ngung. Thu lai sau.", LOCK_DURATION.toSeconds());
            }
        }
    }

    public void recordFailure(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, LOCK_DURATION);
            }
        } catch (Exception e) {
            log.error("Redis unavailable when recording admin login failure. Email: {}", email, e);
            if (failClosed) {
                throw new AccountLockedException("He thong tam ngung. Thu lai sau.", LOCK_DURATION.toSeconds());
            }
        }
    }

    public void resetAttempts(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis unavailable when resetting admin login attempts. Email: {}", email, e);
        }
    }
}
