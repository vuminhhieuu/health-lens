package com.healthlens.api.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component("aiRag")
@RequiredArgsConstructor
public class AiRagHealthIndicator implements HealthIndicator {

    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(5);

    private final QdrantClient qdrantClient;

    @Override
    public Health health() {
        try {
            QdrantOuterClass.HealthCheckReply reply = qdrantClient.healthCheckAsync()
                    .get(HEALTH_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("ragStatus", "available")
                    .withDetail("vectorStore", "qdrant")
                    .withDetail("version", reply.getVersion())
                    .build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return unavailable(ex);
        } catch (Exception ex) {
            return unavailable(ex);
        }
    }

    private Health unavailable(Exception ex) {
        return Health.down()
                .withDetail("ragStatus", "unavailable")
                .withDetail("vectorStore", "qdrant")
                .withDetail("error", ex.getMessage())
                .build();
    }
}
