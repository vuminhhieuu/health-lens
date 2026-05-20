package com.healthlens.api.events;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamConsumerSupportTest {

    @Test
    @SuppressWarnings("unchecked")
    void readPendingThenNewReadsNewRecordsWhenPendingRecordsExistAndCapacityRemains() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        MapRecord<String, Object, Object> pendingRecord = record("1-0", "pending");
        MapRecord<String, Object, Object> newRecord = record("2-0", "new");
        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), anyStreamOffset()))
                .thenReturn(List.of(pendingRecord))
                .thenReturn(List.of(newRecord));
        RedisStreamConsumerSupport support = new RedisStreamConsumerSupport(redisTemplate, streamPublisher);

        List<MapRecord<String, Object, Object>> records = support.readPendingThenNew(
                streamOps,
                "email.events",
                "email-consumers",
                "api-email-consumer",
                10,
                Duration.ofMillis(500)
        );

        assertThat(records).containsExactly(pendingRecord, newRecord);
        verify(streamOps, org.mockito.Mockito.times(2)).read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                anyStreamOffset()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void readPendingThenNewDoesNotReadNewRecordsWhenPendingFillsCapacity() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        MapRecord<String, Object, Object> pendingRecord = record("1-0", "pending");
        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), anyStreamOffset()))
                .thenReturn(List.of(pendingRecord));
        RedisStreamConsumerSupport support = new RedisStreamConsumerSupport(redisTemplate, streamPublisher);

        List<MapRecord<String, Object, Object>> records = support.readPendingThenNew(
                streamOps,
                "email.events",
                "email-consumers",
                "api-email-consumer",
                1,
                Duration.ofMillis(500)
        );

        assertThat(records).containsExactly(pendingRecord);
        verify(streamOps).read(any(Consumer.class), any(StreamReadOptions.class), anyStreamOffset());
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, Object, Object> record(String id, String type) {
        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getId()).thenReturn(RecordId.of(id));
        when(record.getValue()).thenReturn((Map<Object, Object>) (Map<?, ?>) Map.of("type", type));
        return record;
    }

    @SuppressWarnings("unchecked")
    private StreamOffset<String>[] anyStreamOffset() {
        return any(StreamOffset[].class);
    }
}
