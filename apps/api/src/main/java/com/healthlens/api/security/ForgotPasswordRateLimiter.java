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
public class ForgotPasswordRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordRateLimiter.class);
    private static final String ATTEMPTS_KEY_PREFIX = "forgot_password_attempts:";
    private static final String COOLDOWN_KEY_PREFIX = "forgot_password_cooldown:";
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW_DURATION = Duration.ofHours(1);
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final boolean failClosed;

    public ForgotPasswordRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.security.rate-limit-fail-closed:false}") boolean failClosed) {
        this.redisTemplate = redisTemplate;
        this.failClosed = failClosed;
    }

    public void checkRateLimit(String email) {
        String normalizedEmail = normalizeEmail(email);
        checkCooldown(normalizedEmail);
        checkHourlyLimit(normalizedEmail);
    }

    public void recordRequest(String email) {
        String normalizedEmail = normalizeEmail(email);
        setCooldown(normalizedEmail);
        recordHourlyAttempt(normalizedEmail);
    }

    private void checkCooldown(String normalizedEmail) {
        try {
            String key = cooldownKey(normalizedEmail);
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                return;
            }
            Long ttl = redisTemplate.getExpire(key);
            long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : MIN_INTERVAL.toSeconds();
            throw cooldownExceeded(retryAfterSeconds);
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Redis unavailable when checking forgot password cooldown. Fail-closed: {}. Email: {}",
                    failClosed,
                    normalizedEmail,
                    e);
            if (failClosed) {
                throw cooldownExceeded(MIN_INTERVAL.toSeconds());
            }
        }
    }

    private void checkHourlyLimit(String normalizedEmail) {
        try {
            String key = attemptsKey(normalizedEmail);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null && Integer.parseInt(value) >= MAX_ATTEMPTS) {
                Long ttl = redisTemplate.getExpire(key);
                long retryAfterSeconds = (ttl != null && ttl > 0) ? ttl : WINDOW_DURATION.toSeconds();

                String timeMsg = retryAfterSeconds >= 60
                        ? (long) Math.ceil(retryAfterSeconds / 60.0) + " phút"
                        : retryAfterSeconds + " giây";

                throw new RateLimitExceededException(
                        "Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau " + timeMsg + ".",
                        retryAfterSeconds);
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Redis unavailable when checking forgot password rate limit. Fail-closed: {}. Email: {}",
                    failClosed,
                    normalizedEmail,
                    e);
            if (failClosed) {
                throw new RateLimitExceededException(
                        "Hệ thống tạm ngưng. Vui lòng thử lại sau.",
                        WINDOW_DURATION.toSeconds());
            }
        }
    }

    private void setCooldown(String normalizedEmail) {
        try {
            redisTemplate.opsForValue().set(cooldownKey(normalizedEmail), "1", MIN_INTERVAL);
        } catch (Exception e) {
            log.error("Redis unavailable when setting forgot password cooldown. Email: {}", normalizedEmail, e);
            if (failClosed) {
                throw cooldownExceeded(MIN_INTERVAL.toSeconds());
            }
        }
    }

    private void recordHourlyAttempt(String normalizedEmail) {
        try {
            String key = attemptsKey(normalizedEmail);
            Long attempts = redisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1) {
                redisTemplate.expire(key, WINDOW_DURATION);
            }
        } catch (Exception e) {
            log.error("Redis unavailable when recording forgot password request. Email: {}", normalizedEmail, e);
            if (failClosed) {
                throw new RateLimitExceededException(
                        "Hệ thống tạm ngưng. Vui lòng thử lại sau.",
                        WINDOW_DURATION.toSeconds());
            }
        }
    }

    private static RateLimitExceededException cooldownExceeded(long retryAfterSeconds) {
        String timeMsg = retryAfterSeconds >= 60
                ? (long) Math.ceil(retryAfterSeconds / 60.0) + " phút"
                : retryAfterSeconds + " giây";
        return new RateLimitExceededException(
                "Bạn vừa gửi yêu cầu. Vui lòng thử lại sau " + timeMsg + ".",
                retryAfterSeconds);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String attemptsKey(String normalizedEmail) {
        return ATTEMPTS_KEY_PREFIX + normalizedEmail;
    }

    private static String cooldownKey(String normalizedEmail) {
        return COOLDOWN_KEY_PREFIX + normalizedEmail;
    }
}
