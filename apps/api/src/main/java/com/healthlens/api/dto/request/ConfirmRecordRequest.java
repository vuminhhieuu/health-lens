package com.healthlens.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.healthlens.api.dto.MetricDto;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmRecordRequest {
    private LocalDate examDate;
    private String recordType;
    private String hospitalName;
    private String diagnosis;
    private List<MetricDto> metrics;
}
