package com.healthlens.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsentResponse {
    private boolean consentGiven;
    private String consentVersion;
    private Instant consentedAt;
}
