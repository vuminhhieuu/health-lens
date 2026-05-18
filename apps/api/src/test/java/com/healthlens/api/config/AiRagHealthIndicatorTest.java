package com.healthlens.api.config;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiRagHealthIndicatorTest {

    @Test
    void health_reportsUpWhenQdrantResponds() {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        when(qdrantClient.healthCheckAsync())
                .thenReturn(Futures.immediateFuture(QdrantOuterClass.HealthCheckReply.newBuilder()
                        .setTitle("qdrant")
                        .setVersion("1.13.0")
                        .build()));

        Health health = new AiRagHealthIndicator(qdrantClient).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("ragStatus", "available");
    }

    @Test
    void health_reportsDownAndUnavailableWhenQdrantFails() {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        when(qdrantClient.healthCheckAsync())
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("connection refused")));

        Health health = new AiRagHealthIndicator(qdrantClient).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("ragStatus", "unavailable");
    }

    @Test
    void health_reportsDownWhenQdrantHealthCheckTimesOut() throws Exception {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<QdrantOuterClass.HealthCheckReply> future = mock(ListenableFuture.class);
        when(qdrantClient.healthCheckAsync()).thenReturn(future);
        when(future.get(5, TimeUnit.SECONDS)).thenThrow(new TimeoutException("qdrant timeout"));

        Health health = new AiRagHealthIndicator(qdrantClient).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("ragStatus", "unavailable");
    }

    @Test
    void health_restoresInterruptWhenInterrupted() throws Exception {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        @SuppressWarnings("unchecked")
        ListenableFuture<QdrantOuterClass.HealthCheckReply> future = mock(ListenableFuture.class);
        when(qdrantClient.healthCheckAsync()).thenReturn(future);
        when(future.get(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        try {
            Health health = new AiRagHealthIndicator(qdrantClient).health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
