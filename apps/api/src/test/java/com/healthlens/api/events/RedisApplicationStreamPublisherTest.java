package com.healthlens.api.events;

import com.healthlens.api.correlation.CorrelationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisApplicationStreamPublisherTest {

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
        CorrelationContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishAfterCommit_publishesPayloadCapturedAtRegistration_notAtCallbackTime() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);

        RedisApplicationStreamPublisher publisher = new RedisApplicationStreamPublisher(redisTemplate);
        Map<String, String> payload = Map.of(
                "jobId", "job-1",
                "correlationId", "corr-at-register"
        );

        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publishAfterCommit("ocr.events", payload);
            CorrelationContext.clear();

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCommit());

            verify(streamOps).add(eq("ocr.events"), eq(payload));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
