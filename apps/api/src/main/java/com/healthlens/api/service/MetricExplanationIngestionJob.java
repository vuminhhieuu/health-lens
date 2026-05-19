package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class MetricExplanationIngestionJob implements CommandLineRunner {

    private final MetricExplanationIngestionService ingestionService;

    @Value("${app.ai.explanation.ingestion.enabled:false}")
    private boolean ingestionEnabled;

    @Value("${app.ai.explanation.ingestion.resource:classpath:ai/metric-explanations.vi.json}")
    private Resource ingestionResource;

    @Value("${app.ai.explanation.retrieval-version:v1}")
    private String retrievalVersion;

    @Override
    public void run(String... args) {
        if (!ingestionEnabled) {
            return;
        }
        MetricExplanationIngestionService.IngestionReport report =
                ingestionService.ingestWithReport(ingestionResource, retrievalVersion, "system-ingestion");
        if (report.hasErrors()) {
            log.warn(
                    "metric_explanation_ingestion failed sourceVersion={} chunks={} embeddingModel={} embeddingDimension={} errors={}",
                    report.sourceVersion(),
                    report.chunkCount(),
                    report.embeddingModel(),
                    report.embeddingDimension(),
                    report.errors()
            );
            throw new IllegalStateException("Metric explanation ingestion failed: " + report.errors());
        }
        log.info(
                "metric_explanation_ingestion completed sourceVersion={} chunks={} embeddingModel={} embeddingDimension={}",
                report.sourceVersion(),
                report.chunkCount(),
                report.embeddingModel(),
                report.embeddingDimension()
        );
    }
}
