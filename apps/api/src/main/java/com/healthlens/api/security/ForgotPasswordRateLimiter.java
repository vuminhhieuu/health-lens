package com.healthlens.api.security;

import com.healthlens.api.exception.AccountLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ForgotPasswordRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordRateLimiter.class);
    private static final String KEY_PREFIX = "forgot_password_attempts:";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW_DURATION = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public ForgotPasswordRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void checkRateLimit(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= MAX_ATTEMPTS) {
                Long ttl = redisTemplate.getExpire(key);
                long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : WINDOW_DURATION.toSeconds();
                
                String timeMsg = retryAfterSeconds >= 60 
                    ? (retryAfterSeconds / 60) + " phut" 
                    : retryAfterSeconds + " giay";
                    
                throw new AccountLockedException(
                        "Ban da gui yeu cau qua nhanh. Vui long thu lai sau " + timeMsg + ".",
                        retryAfterSeconds
                );
            }
        } catch (AccountLockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Redis unavailable when checking forgot password rate limit. Fail-closed: {}. Email: {}", failClosed, email, e);
            if (failClosed) {
                throw new AccountLockedException("He thong tam ngung. Thu lai sau.", WINDOW_DURATION.toSeconds());
            }
        }
    }

    public void recordRequest(String email) {
        try {
            String key = KEY_PREFIX + email.toLowerCase();
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, WINDOW_DURATION);
            }
        } catch (Exception e) {
            log.error("Redis unavailable when recording forgot password request. Email: {}", email, e);
            if (failClosed) {
                throw new AccountLockedException("He thong tam ngung. Thu lai sau.", WINDOW_DURATION.toSeconds());
            }
        }
    }
}
