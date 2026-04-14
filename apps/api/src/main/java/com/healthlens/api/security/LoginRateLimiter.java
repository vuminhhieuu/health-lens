package com.healthlens.api.security;

import com.healthlens.api.exception.AccountLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final String KEY_PREFIX = "login_attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public LoginRateLimiter(
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
                        "Tai khoan bi khoa tam thoi. Thu lai sau " + retryAfterSeconds + " giay.",
                        retryAfterSeconds
                );
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when checking account lock. Fail-closed: {}. Email: {}", failClosed, email, e);
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
            log.error("Redis unavailable when recording login failure. Email: {}", email, e);
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
            log.warn("Redis unavailable when resetting login attempts. Email: {}", email, e);
        }
    }

    public long getRetryAfterSeconds(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            Long ttl = redisTemplate.getExpire(key);
            return (ttl != null && ttl > 0) ? ttl : LOCK_DURATION.toSeconds();
        } catch (Exception e) {
            return LOCK_DURATION.toSeconds();
        }
    }
}
