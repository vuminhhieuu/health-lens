package com.healthlens.api.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RedisStreamConsumerSupport {

    private final StringRedisTemplate redisTemplate;
    private final ApplicationStreamPublisher streamPublisher;

    public RedisStreamConsumerSupport(StringRedisTemplate redisTemplate, ApplicationStreamPublisher streamPublisher) {
        this.redisTemplate = redisTemplate;
        this.streamPublisher = streamPublisher;
    }

    public void ensureConsumerGroup(String streamName, String consumerGroup, String logPrefix) {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        try {
            boolean streamExists = Boolean.TRUE.equals(redisTemplate.hasKey(streamName));
            boolean groupExists = streamExists && hasConsumerGroup(streamOps, streamName, consumerGroup);
            if (groupExists) {
                return;
            }
            if (!streamExists) {
                streamPublisher.publish(streamName, Map.of("_init", "1"));
            }
            streamOps.createGroup(streamName, ReadOffset.latest(), consumerGroup);
        } catch (Exception ex) {
            if (containsMessage(ex, "BUSYGROUP")) {
                log.debug("[{}] Consumer group {} already exists for stream {}", logPrefix, consumerGroup, streamName);
                return;
            }
            log.debug("[{}] Stream/group initialization skipped for stream={}: {}",
                    logPrefix, streamName, ex.getMessage());
        }
    }

    public List<MapRecord<String, Object, Object>> readPendingThenNew(
            StreamOperations<String, Object, Object> streamOps,
            String streamName,
            String consumerGroup,
            String consumerName,
            int count,
            Duration block
    ) {
        List<MapRecord<String, Object, Object>> records = new ArrayList<>();
        List<MapRecord<String, Object, Object>> pendingRecords = streamOps.read(
                Consumer.from(consumerGroup, consumerName),
                StreamReadOptions.empty().count(count),
                StreamOffset.create(streamName, ReadOffset.from("0")));
        if (pendingRecords != null && !pendingRecords.isEmpty()) {
            records.addAll(pendingRecords);
        }

        int remainingCount = Math.max(0, count - records.size());
        if (remainingCount == 0) {
            return records;
        }
        StreamReadOptions readOptions = StreamReadOptions.empty().count(remainingCount);
        if (records.isEmpty()) {
            readOptions = readOptions.block(block);
        }
        List<MapRecord<String, Object, Object>> newRecords = streamOps.read(
                Consumer.from(consumerGroup, consumerName),
                readOptions,
                StreamOffset.create(streamName, ReadOffset.lastConsumed()));
        if (newRecords != null) {
            records.addAll(newRecords);
        }
        return records;
    }

    private boolean hasConsumerGroup(
            StreamOperations<String, Object, Object> streamOps,
            String streamName,
            String consumerGroup
    ) {
        StreamInfo.XInfoGroups groups = streamOps.groups(streamName);
        if (groups == null) {
            return false;
        }
        for (StreamInfo.XInfoGroup group : groups) {
            if (consumerGroup.equals(group.groupName())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMessage(Throwable throwable, String keyword) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(keyword)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
