package com.healthlens.api.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Slf4j
@Component
public class RedisApplicationStreamPublisher implements ApplicationStreamPublisher {

    private final StringRedisTemplate redisTemplate;

    public RedisApplicationStreamPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publish(String streamName, Map<String, String> payload) {
        redisTemplate.opsForStream().add(streamName, payload);
    }

    @Override
    public void publishAfterCommit(String streamName, Map<String, String> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(streamName, payload);
                }
            });
            return;
        }
        log.warn("Publishing stream event outside an active transaction. stream={}", streamName);
        publish(streamName, payload);
    }
}
