package com.healthlens.api.service;

import com.healthlens.api.entity.User;
import com.healthlens.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EmailConsumer {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final String emailEventStream;
    private final String consumerGroup;
    private final String consumerName;

    public EmailConsumer(
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            UserRepository userRepository,
            @Value("${app.stream.email-events:email.events}") String emailEventStream,
            @Value("${app.stream.email-consumer-group:email-consumers}") String consumerGroup,
            @Value("${app.stream.email-consumer-name:api-email-consumer}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.emailEventStream = emailEventStream;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
    }

    @PostConstruct
    public void initConsumerGroup() {
        ensureConsumerGroup();
    }

    private void ensureConsumerGroup() {
        try {
            // Ensure stream exists before creating the consumer group.
            redisTemplate.opsForStream().add(emailEventStream, Map.of("_init", "1"));
            redisTemplate.opsForStream().createGroup(emailEventStream, ReadOffset.latest(), consumerGroup);
        } catch (Exception ex) {
            log.debug("[EmailConsumer] Stream/group initialization skipped: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.stream.email-poll-delay-ms:1000}")
    @SuppressWarnings("unchecked")
    public void consumeEmailEvents() {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        List<MapRecord<String, Object, Object>> records;
        try {
            records = streamOps.read(
                    Consumer.from(consumerGroup, consumerName),
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty()
                            .count(10)
                            .block(Duration.ofMillis(500)),
                    StreamOffset.create(emailEventStream, ReadOffset.lastConsumed()));
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("NOGROUP")) {
                ensureConsumerGroup();
            }
            log.warn("[EmailConsumer] Failed reading stream {}", emailEventStream, ex);
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handleRecord(record);
                streamOps.acknowledge(emailEventStream, consumerGroup, record.getId());
            } catch (Exception ex) {
                log.error("[EmailConsumer] Failed processing record {}", record.getId(), ex);
            }
        }
    }

    private void handleRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        String eventType = stringVal(value.get("eventType"));
        if (!"verification".equals(eventType)) {
            return;
        }

        UUID userId = UUID.fromString(stringVal(value.get("userId")));
        String token = stringVal(value.get("token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        emailService.sendVerificationEmail(user, token);
    }

    private String stringVal(Object value) {
        return value == null ? "" : value.toString();
    }
}
