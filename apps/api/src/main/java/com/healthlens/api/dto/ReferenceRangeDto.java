package com.healthlens.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * @JsonProperty trên từng record component giúp Jackson biết tên field
 * khi deserialize JSON → record (không cần -parameters compiler flag).
 */
public record ReferenceRangeDto(
        @JsonProperty("min") BigDecimal min,
        @JsonProperty("max") BigDecimal max,
        @JsonProperty("attentionMin") BigDecimal attentionMin,
        @JsonProperty("attentionMax") BigDecimal attentionMax,
        @JsonProperty("unit") String unit
) {
}
