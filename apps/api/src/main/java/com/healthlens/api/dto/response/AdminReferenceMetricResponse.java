package com.healthlens.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminReferenceMetricResponse(
        UUID id,
        String name,
        String displayNameVi,
        String unit,
        String status,
        int rangesCount,
        List<AdminReferenceRangeResponse> ranges,
        AdminPendingChangeSetSummary pendingChangeSet
) {
    /** Backward-compatible constructor without pendingChangeSet. */
    public AdminReferenceMetricResponse(
            UUID id,
            String name,
            String displayNameVi,
            String unit,
            String status,
            int rangesCount,
            List<AdminReferenceRangeResponse> ranges
    ) {
        this(id, name, displayNameVi, unit, status, rangesCount, ranges, null);
    }
}
