package com.healthlens.api.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricExplanationIngestionJobTest {

    @Test
    @DisplayName("Ingestion job: report có lỗi phải fail runner thay vì log completed")
    void run_errorReport_throws() {
        MetricExplanationIngestionService ingestionService = mock(MetricExplanationIngestionService.class);
        ByteArrayResource resource = new ByteArrayResource("[]".getBytes(StandardCharsets.UTF_8));
        MetricExplanationIngestionJob job = new MetricExplanationIngestionJob(ingestionService);
        ReflectionTestUtils.setField(job, "ingestionEnabled", true);
        ReflectionTestUtils.setField(job, "ingestionResource", resource);
        ReflectionTestUtils.setField(job, "retrievalVersion", "v2026-05-19");

        when(ingestionService.ingestWithReport(any(), eq("v2026-05-19"), eq("system-ingestion")))
                .thenReturn(new MetricExplanationIngestionService.IngestionReport(
                        "v2026-05-19",
                        0,
                        "text-embedding-3-small",
                        1536,
                        List.of("qdrant unavailable"),
                        "system-ingestion",
                        Instant.parse("2026-05-19T03:00:00Z")
                ));

        assertThatThrownBy(job::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Metric explanation ingestion failed")
                .hasMessageContaining("qdrant unavailable");
    }
}
