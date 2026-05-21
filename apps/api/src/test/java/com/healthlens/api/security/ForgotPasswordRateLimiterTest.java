package com.healthlens.api.security;

import com.healthlens.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForgotPasswordRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ForgotPasswordRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        rateLimiter = new ForgotPasswordRateLimiter(redisTemplate, false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("checkRateLimit blocks when 60s cooldown key exists")
    void checkRateLimit_blocksCooldown() {
        when(redisTemplate.hasKey("forgot_password_cooldown:user@example.com")).thenReturn(true);
        when(redisTemplate.getExpire("forgot_password_cooldown:user@example.com")).thenReturn(42L);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("User@Example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Vui lòng thử lại sau 42 giây");
    }

    @Test
    @DisplayName("recordRequest sets 60s cooldown and increments hourly bucket")
    void recordRequest_setsCooldownAndHourlyAttempt() {
        when(valueOperations.increment("forgot_password_attempts:user@example.com")).thenReturn(1L);

        rateLimiter.recordRequest("user@example.com");

        verify(valueOperations).set(
                eq("forgot_password_cooldown:user@example.com"),
                eq("1"),
                eq(Duration.ofSeconds(60)));
        verify(redisTemplate).expire("forgot_password_attempts:user@example.com", Duration.ofHours(1));
    }

    @Test
    @DisplayName("checkRateLimit allows when cooldown absent and hourly bucket below limit")
    void checkRateLimit_allowsFreshRequest() {
        when(redisTemplate.hasKey("forgot_password_cooldown:user@example.com")).thenReturn(false);
        when(valueOperations.get("forgot_password_attempts:user@example.com")).thenReturn("1");

        assertThatCode(() -> rateLimiter.checkRateLimit("user@example.com"))
                .doesNotThrowAnyException();
    }
}
