package com.healthlens.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.healthlens.api.dto.MetricDto;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthRecordDetailResponse(
        UUID id,
        UUID profileId,
        String profileName,
        String status,
        List<MetricDto> metrics,
        String overallStatus,
        List<MetricDto> keyMetrics,
        List<MetricDto> allMetrics,
        String summary,
        String examDate,
        String recordType,
        String hospitalName,
        String diagnosis,
        String analyzerModel,
        String testMethod,
        String labSite,
        String fileUrl
) {
}
