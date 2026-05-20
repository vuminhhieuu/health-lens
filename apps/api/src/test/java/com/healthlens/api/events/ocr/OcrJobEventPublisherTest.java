package com.healthlens.api.events.ocr;

import com.healthlens.api.events.ApplicationStreamPublisher;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OcrJobEventPublisherTest {

    @Test
    void ocrJobEventSerializesExpectedStreamPayload() {
        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        OcrJobEvent event = new OcrJobEvent(
                "job-1",
                "corr-1",
                recordId,
                "health-records/original.pdf",
                "unversioned",
                "application/pdf",
                profileId,
                null
        );

        assertThat(event.toStreamMap())
                .containsEntry("jobId", "job-1")
                .containsEntry("correlationId", "corr-1")
                .containsEntry("recordId", recordId.toString())
                .containsEntry("fileKey", "health-records/original.pdf")
                .containsEntry("fileVersion", "unversioned")
                .containsEntry("mimeType", "application/pdf")
                .containsEntry("profileId", profileId.toString())
                .doesNotContainKey("attempt");
    }

    @Test
    void redisPublisherUsesConfiguredOcrStreamBoundary() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        RedisOcrJobEventPublisher publisher = new RedisOcrJobEventPublisher(streamPublisher, "ocr.events");
        UUID recordId = UUID.randomUUID();

        publisher.publishAfterCommit(new OcrJobEvent(
                "job-1",
                "corr-1",
                recordId,
                "file-key",
                "unversioned",
                "image/jpeg",
                UUID.randomUUID(),
                null
        ));

        verify(streamPublisher).publishAfterCommit(eq("ocr.events"), argThat(payload ->
                "job-1".equals(payload.get("jobId"))
                        && recordId.toString().equals(payload.get("recordId"))
                        && "image/jpeg".equals(payload.get("mimeType"))));
    }

    @Test
    void redisPublisherCanPublishRetryImmediately() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        RedisOcrJobEventPublisher publisher = new RedisOcrJobEventPublisher(streamPublisher, "ocr.events");
        UUID recordId = UUID.randomUUID();

        publisher.publish(new OcrJobEvent(
                "job-1",
                "corr-1",
                recordId,
                "file-key",
                "unversioned",
                "image/png",
                UUID.randomUUID(),
                2
        ));

        verify(streamPublisher).publish(eq("ocr.events"), argThat(payload ->
                "2".equals(payload.get("attempt"))
                        && recordId.toString().equals(payload.get("recordId"))));
    }
}
