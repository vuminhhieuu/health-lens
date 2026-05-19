package com.healthlens.api.controller.admin;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.admin.OnlineRagCitationPageDto;
import com.healthlens.api.service.admin.AdminOnlineRagCitationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.ADMIN_ONLINE_RAG_CITATIONS)
@RequiredArgsConstructor
@Validated
public class AdminOnlineRagCitationController {

    private final AdminOnlineRagCitationService citationService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OnlineRagCitationPageDto> list(
            @RequestParam(required = false) UUID healthRecordId,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String answerHash,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String snapshotHash,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit
    ) {
        return ResponseEntity.ok(citationService.query(
                healthRecordId,
                metricName,
                answerHash,
                sourceUrl,
                publisher,
                snapshotHash,
                reviewStatus,
                page,
                limit
        ));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false) UUID healthRecordId,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) String answerHash,
            @RequestParam(required = false) String sourceUrl,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String snapshotHash,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(defaultValue = "10000") @Min(1) @Max(50_000) int maxRows
    ) {
        StreamingResponseBody body = os -> {
            try (OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                citationService.writeCsv(
                        healthRecordId,
                        metricName,
                        answerHash,
                        sourceUrl,
                        publisher,
                        snapshotHash,
                        reviewStatus,
                        writer,
                        maxRows
                );
            }
        };
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("online-rag-citations.csv", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
