package com.healthlens.api.security;

import com.healthlens.api.exception.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicEndpointRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private PublicEndpointRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        rateLimiter = new PublicEndpointRateLimiter(redisTemplate, false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("register increments IP and email buckets")
    void consumeRegister_incrementsBuckets() {
        when(valueOperations.increment("public_endpoint_rate:register:ip:203.0.113.10")).thenReturn(1L);
        when(valueOperations.increment("public_endpoint_rate:register:email:user@example.com")).thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:register:ip:203.0.113.10", Duration.ofHours(1))).thenReturn(true);
        when(redisTemplate.expire("public_endpoint_rate:register:email:user@example.com", Duration.ofHours(1))).thenReturn(true);

        rateLimiter.consumeRegister("203.0.113.10", "User@Example.com");

        verify(redisTemplate).expire("public_endpoint_rate:register:ip:203.0.113.10", Duration.ofHours(1));
        verify(redisTemplate).expire("public_endpoint_rate:register:email:user@example.com", Duration.ofHours(1));
    }

    @Test
    @DisplayName("cancel deletion blocks exhausted token bucket without raw token in Redis key")
    void consumeCancelDeletion_blocksTokenBucket() {
        when(valueOperations.increment("public_endpoint_rate:cancel-deletion:ip:203.0.113.10")).thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:cancel-deletion:ip:203.0.113.10", Duration.ofHours(1))).thenReturn(true);
        when(valueOperations.increment(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:cancel-deletion:token:")))
                .thenReturn(6L);
        when(redisTemplate.getExpire(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:cancel-deletion:token:")))
                .thenReturn(120L);

        assertThatThrownBy(() -> rateLimiter.consumeCancelDeletion("203.0.113.10", "raw-secret-token"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("quá nhanh");
    }

    @Test
    @DisplayName("cancel deletion missing token only consumes IP bucket")
    void consumeCancelDeletion_missingTokenOnlyConsumesIpBucket() {
        when(valueOperations.increment("public_endpoint_rate:cancel-deletion:ip:203.0.113.10")).thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:cancel-deletion:ip:203.0.113.10", Duration.ofHours(1))).thenReturn(true);

        rateLimiter.consumeCancelDeletion("203.0.113.10", " ");

        verify(valueOperations).increment("public_endpoint_rate:cancel-deletion:ip:203.0.113.10");
        verify(valueOperations, never()).increment(startsWith("public_endpoint_rate:cancel-deletion:token:"));
    }

    @Test
    @DisplayName("profile invitation accept increments IP and token buckets")
    void consumeProfileInvitationAccept_incrementsBuckets() {
        when(valueOperations.increment("public_endpoint_rate:profile-invitation-accept:ip:203.0.113.10")).thenReturn(1L);
        when(valueOperations.increment(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:profile-invitation-accept:token:")))
                .thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:profile-invitation-accept:ip:203.0.113.10", Duration.ofHours(1)))
                .thenReturn(true);
        when(redisTemplate.expire(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:profile-invitation-accept:token:"),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)))).thenReturn(true);

        rateLimiter.consumeProfileInvitationAccept("203.0.113.10", "profile-token");

        verify(redisTemplate).expire("public_endpoint_rate:profile-invitation-accept:ip:203.0.113.10", Duration.ofHours(1));
        verify(redisTemplate).expire(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:profile-invitation-accept:token:"),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("health record invitation accept increments IP and token buckets")
    void consumeHealthRecordInvitationAccept_incrementsBuckets() {
        when(valueOperations.increment("public_endpoint_rate:health-record-invitation-accept:ip:203.0.113.10")).thenReturn(1L);
        when(valueOperations.increment(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:health-record-invitation-accept:token:")))
                .thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:health-record-invitation-accept:ip:203.0.113.10", Duration.ofHours(1)))
                .thenReturn(true);
        when(redisTemplate.expire(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:health-record-invitation-accept:token:"),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)))).thenReturn(true);

        rateLimiter.consumeHealthRecordInvitationAccept("203.0.113.10", "record-token");

        verify(redisTemplate).expire("public_endpoint_rate:health-record-invitation-accept:ip:203.0.113.10", Duration.ofHours(1));
        verify(redisTemplate).expire(org.mockito.ArgumentMatchers.startsWith("public_endpoint_rate:health-record-invitation-accept:token:"),
                org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("exhausted bucket still blocks when retry-after TTL lookup fails")
    void consumeRegister_ttlLookupFailureStillBlocks() {
        when(valueOperations.increment("public_endpoint_rate:register:ip:203.0.113.10")).thenReturn(6L);
        when(redisTemplate.getExpire("public_endpoint_rate:register:ip:203.0.113.10"))
                .thenThrow(new IllegalStateException("redis ttl down"));

        assertThatThrownBy(() -> rateLimiter.consumeRegister("203.0.113.10", "user@example.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("quá nhanh");
    }

    @Test
    @DisplayName("exhausted bucket repairs missing TTL before blocking")
    void consumeRegister_repairsMissingTtl() {
        when(valueOperations.increment("public_endpoint_rate:register:ip:203.0.113.10")).thenReturn(6L);
        when(redisTemplate.getExpire("public_endpoint_rate:register:ip:203.0.113.10")).thenReturn(-1L);

        assertThatThrownBy(() -> rateLimiter.consumeRegister("203.0.113.10", "user@example.com"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(redisTemplate).expire("public_endpoint_rate:register:ip:203.0.113.10", Duration.ofHours(1));
    }

    @Test
    @DisplayName("OCR trigger uses short record retry window")
    void consumeOcrTrigger_usesShortRecordWindow() {
        when(valueOperations.increment("public_endpoint_rate:ocr-trigger:user:user-1")).thenReturn(1L);
        when(valueOperations.increment("public_endpoint_rate:ocr-trigger:record:user-1:record-1")).thenReturn(1L);
        when(redisTemplate.expire("public_endpoint_rate:ocr-trigger:user:user-1", Duration.ofHours(1))).thenReturn(true);
        when(redisTemplate.expire("public_endpoint_rate:ocr-trigger:record:user-1:record-1", Duration.ofMinutes(1))).thenReturn(true);

        rateLimiter.consumeOcrTrigger("user-1", "record-1");

        verify(redisTemplate).expire("public_endpoint_rate:ocr-trigger:user:user-1", Duration.ofHours(1));
        verify(redisTemplate).expire("public_endpoint_rate:ocr-trigger:record:user-1:record-1", Duration.ofMinutes(1));
    }
}
