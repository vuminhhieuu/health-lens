package com.healthlens.api.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDataSearchResult {
    private String id;
    private String text;
    private Map<String, Object> metadata;
}
