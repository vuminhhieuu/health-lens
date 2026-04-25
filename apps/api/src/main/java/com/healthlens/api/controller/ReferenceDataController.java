package com.healthlens.api.controller;

import com.healthlens.api.dto.ReferenceDataSearchResult;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.ReferenceData;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.service.ReferenceDataService;
import com.healthlens.api.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final VectorStoreService vectorStoreService;
    private final ReferenceDataService referenceDataService;

    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexToVectorStore(@PathVariable String id) {
        ReferenceData data = referenceDataService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reference data not found"));
        
        String content = formatForEmbedding(data);
        Map<String, Object> metadata = Map.of(
            "type", data.getType() != null ? data.getType() : "unknown",
            "name", data.getName() != null ? data.getName() : "unknown",
            "id", id
        );
        
        vectorStoreService.upsertReferenceData(id, content, metadata);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReferenceDataSearchResult>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        
        List<Document> docs = vectorStoreService.semanticSearch(query, topK);
        
        List<ReferenceDataSearchResult> results = docs.stream()
            .map(doc -> new ReferenceDataSearchResult(
                doc.getId(),
                doc.getText(),
                doc.getMetadata()
            ))
            .toList();
        
        return ResponseEntity.ok(results);
    }

    @GetMapping("/ranges")
    public ResponseEntity<Map<String, Object>> getReferenceRange(
            @RequestParam String metricName,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) String gender
    ) {
        Optional<ReferenceRangeDto> range = referenceDataService.findReferenceRange(metricName, age, gender);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("metricName", metricName);
        response.put("age", age);
        response.put("gender", gender);
        response.put("referenceRange", range.orElse(null));
        return ResponseEntity.ok(response);
    }

    private String formatForEmbedding(ReferenceData data) {
        return String.format("""
            Tên chỉ số: %s
            Giá trị bình thường: %s - %s %s
            Ý nghĩa: %s
            """,
            data.getName(),
            data.getMinValue(), data.getMaxValue(), data.getUnit(),
            data.getDescriptionVi()
        );
    }
}
