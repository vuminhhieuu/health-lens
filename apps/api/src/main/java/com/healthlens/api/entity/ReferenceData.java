package com.healthlens.api.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceData {
    private String id;
    private String type;
    private String name;
    private String minValue;
    private String maxValue;
    private String unit;
    private String descriptionVi;
}
