package com.healthlens.api.controller.admin;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.service.admin.AdminAuditLogService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping(ApiRoutes.ADMIN_AUDIT_LOGS)
@RequiredArgsConstructor
@Validated
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuditLogPageDto> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit
    ) {
        Instant fromInstant = from != null ? AdminAuditLogService.startOfUtcDay(from) : null;
        Instant toInstant = to != null ? AdminAuditLogService.endOfUtcDayInclusive(to) : null;

        return ResponseEntity.ok(
                adminAuditLogService.query(
                        resourceType,
                        resourceId,
                        actorEmail,
                        action,
                        fromInstant,
                        toInstant,
                        page,
                        limit
                )
        );
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10000") @Min(1) @Max(50_000) int maxRows
    ) {
        Instant fromInstant = from != null ? AdminAuditLogService.startOfUtcDay(from) : null;
        Instant toInstant = to != null ? AdminAuditLogService.endOfUtcDayInclusive(to) : null;

        StreamingResponseBody body = os -> {
            try (OutputStreamWriter writer =
                    new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write('\uFEFF');
                adminAuditLogService.writeCsv(
                        resourceType,
                        resourceId,
                        actorEmail,
                        action,
                        fromInstant,
                        toInstant,
                        writer,
                        maxRows
                );
            }
        };

        ContentDisposition disposition = ContentDisposition.attachment().filename(
                "system-audit-logs.csv",
                StandardCharsets.UTF_8
        ).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
