package com.healthlens.api.service;

import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record HealthRecordPdfContext(
        HealthRecord record,
        Profile profile,
        List<MetricDto> metrics,
        Map<String, String> explanations,
        List<String> recommendations,
        String recommendationsDisclaimer,
        Instant generatedAt
) {
}
