package com.healthlens.api.security;

import com.healthlens.api.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerifyEmailRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private VerifyEmailRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        rateLimiter = new VerifyEmailRateLimiter(redisTemplate, false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("checkRateLimit blocks when IP bucket is exhausted")
    void checkRateLimit_blocksIpBucket() {
        when(valueOperations.get("verify_email_attempts:ip:203.0.113.10")).thenReturn("10");
        when(redisTemplate.getExpire("verify_email_attempts:ip:203.0.113.10")).thenReturn(120L);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("203.0.113.10", null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("quá nhanh");
    }

    @Test
    @DisplayName("consumeAttempt increments IP and email buckets")
    void consumeAttempt_incrementsIpAndEmailBuckets() {
        when(valueOperations.increment("verify_email_attempts:ip:203.0.113.10")).thenReturn(1L);
        when(valueOperations.increment("verify_email_attempts:email:user@example.com")).thenReturn(1L);

        rateLimiter.consumeAttempt("203.0.113.10", "User@Example.com");

        verify(redisTemplate).expire("verify_email_attempts:ip:203.0.113.10", Duration.ofHours(1));
        verify(redisTemplate).expire("verify_email_attempts:email:user@example.com", Duration.ofHours(1));
    }

    @Test
    @DisplayName("checkRateLimit blocks when email bucket is exhausted")
    void checkRateLimit_blocksEmailBucket() {
        when(valueOperations.get("verify_email_attempts:ip:203.0.113.10")).thenReturn("1");
        when(valueOperations.get("verify_email_attempts:email:user@example.com")).thenReturn("5");
        when(redisTemplate.getExpire("verify_email_attempts:email:user@example.com")).thenReturn(240L);

        assertThatThrownBy(() -> rateLimiter.checkRateLimit("203.0.113.10", "User@Example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("quá nhanh");
    }

    @Test
    @DisplayName("consumeAttempt blocks atomically when email bucket exceeds limit")
    void consumeAttempt_blocksEmailBucketAfterIncrement() {
        when(valueOperations.increment("verify_email_attempts:ip:203.0.113.10")).thenReturn(2L);
        when(valueOperations.increment("verify_email_attempts:email:user@example.com")).thenReturn(6L);
        when(redisTemplate.getExpire("verify_email_attempts:email:user@example.com")).thenReturn(240L);

        assertThatThrownBy(() -> rateLimiter.consumeAttempt("203.0.113.10", "User@Example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("quá nhanh");
    }
}
