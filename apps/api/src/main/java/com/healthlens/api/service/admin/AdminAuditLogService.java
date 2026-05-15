package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogEntryDto;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.repository.AuditLogRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ReferenceMetricRepository referenceMetricRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public AuditLogPageDto query(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            Instant from,
            Instant to,
            int page,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int safePage = Math.max(page, 0);
        Specification<AuditLog> spec = buildSpec(resourceType, resourceId, actorEmail, action, from, to);

        Page<AuditLog> result = auditLogRepository.findAll(
                spec,
                PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new AuditLogPageDto(
                result.stream().map(this::toDto).toList(),
                result.getTotalElements(),
                safePage,
                safeLimit
        );
    }

    /** Export all rows matching filters (bounded by maxRows). */
    @Transactional(readOnly = true)
    public void writeCsv(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            Instant from,
            Instant to,
            Writer writer,
            int maxRows
    ) throws IOException {
        Specification<AuditLog> spec = buildSpec(resourceType, resourceId, actorEmail, action, from, to);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "id",
                        "actorEmail",
                        "action",
                        "resourceType",
                        "resourceId",
                        "entityLabel",
                        "oldValueJson",
                        "newValueJson",
                        "ipAddress",
                        "createdAt"
                )
                .get();

        int cap = Math.min(Math.max(maxRows, 1), 50_000);
        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
        int pageSize = 500;
        int written = 0;
        int pageNum = 0;
        while (written < cap) {
                Page<AuditLog> page = auditLogRepository.findAll(
                        spec,
                        PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
                if (page.isEmpty()) {
                    break;
                }
                for (AuditLog row : page.getContent()) {
                    if (written >= cap) {
                        break;
                    }
                    AuditLogEntryDto dto = toDto(row);
                    printer.printRecord(
                            dto.id(),
                            dto.actorEmail(),
                            dto.action(),
                            dto.resourceType(),
                            dto.resourceId() != null ? dto.resourceId().toString() : "",
                            dto.entityLabel(),
                            dto.oldValueJson() != null ? dto.oldValueJson() : "",
                            dto.newValueJson() != null ? dto.newValueJson() : "",
                            dto.ipAddress() != null ? dto.ipAddress() : "",
                            dto.createdAt().toString()
                    );
                    written++;
                }
                if (!page.hasNext()) {
                    break;
                }
                pageNum++;
            }
        }

        writer.flush();
    }

    /**
     * Parse inclusive date boundaries as UTC start/end of day when only a date is provided from the UI.
     */
    public static Instant startOfUtcDay(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant endOfUtcDayInclusive(LocalDate date) {
        return date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);
    }

    private AuditLogEntryDto toDto(AuditLog log) {
        String email = resolveActorEmail(log);
        String label = buildEntityLabel(log);
        return new AuditLogEntryDto(
                log.getId(),
                email,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                label,
                log.getOldValueJson(),
                log.getNewValueJson(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }

    private String resolveActorEmail(AuditLog log) {
        User actor = log.getActor();
        if (actor != null && actor.getEmail() != null && !actor.getEmail().isBlank()) {
            return actor.getEmail();
        }
        return extractJsonString(log.getNewValueJson(), "email")
                .or(() -> extractJsonString(log.getOldValueJson(), "email"))
                .orElse("");
    }

    private Optional<String> extractJsonString(String json, String field) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return Optional.of(value.asText());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Optional.empty();
    }

    private String buildEntityLabel(AuditLog log) {
        if (AuditResourceTypes.AUTH.equals(log.getResourceType())) {
            return extractJsonString(log.getNewValueJson(), "email")
                    .map(email -> "Xác thực · " + email)
                    .orElse("Xác thực");
        }
        if (AuditResourceTypes.USER.equals(log.getResourceType())) {
            return extractJsonString(log.getNewValueJson(), "email")
                    .map(email -> "Tài khoản · " + email)
                    .orElse(log.getResourceId() != null ? "user:" + log.getResourceId() : "Tài khoản");
        }
        if (AuditResourceTypes.CONSENT.equals(log.getResourceType())) {
            return extractJsonString(log.getNewValueJson(), "version")
                    .map(version -> "Đồng ý điều khoản v" + version)
                    .orElse("Đồng ý điều khoản");
        }
        if (AuditResourceTypes.PROFILE.equals(log.getResourceType()) && log.getResourceId() != null) {
            return extractJsonString(log.getNewValueJson(), "displayName")
                    .map(name -> "Hồ sơ · " + name)
                    .orElse("profile:" + log.getResourceId());
        }
        if (AuditResourceTypes.REFERENCE_DATA.equals(log.getResourceType()) && log.getResourceId() != null) {
            Optional<ReferenceMetric> metric = referenceMetricRepository.findById(log.getResourceId());
            if (metric.isPresent()) {
                ReferenceMetric m = metric.get();
                return m.getDisplayNameVi() + " (" + m.getName() + ")";
            }
            return "reference_metric:" + log.getResourceId();
        }
        if (AuditResourceTypes.HEALTH_RECORD.equals(log.getResourceType()) && log.getResourceId() != null) {
            return "health_record:" + log.getResourceId();
        }
        return log.getResourceType() + (log.getResourceId() != null ? "/" + log.getResourceId() : "");
    }

    private Specification<AuditLog> buildSpec(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            Instant from,
            Instant to
    ) {
        var parts = new ArrayList<Specification<AuditLog>>();

        if (StringUtils.hasText(resourceType)) {
            parts.add((root, q, cb) -> cb.equal(root.get("resourceType"), resourceType.trim()));
        }
        if (resourceId != null) {
            parts.add((root, q, cb) -> cb.equal(root.get("resourceId"), resourceId));
        }
        if (StringUtils.hasText(action)) {
            parts.add((root, q, cb) -> cb.equal(root.get("action"), action.trim()));
        }
        if (from != null) {
            parts.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            parts.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (StringUtils.hasText(actorEmail)) {
            String normalized = actorEmail.trim().toLowerCase();
            parts.add((root, q, cb) -> {
                var join = root.join("actor", jakarta.persistence.criteria.JoinType.LEFT);
                return cb.equal(cb.lower(join.get("email")), normalized);
            });
        }

        return parts.stream().reduce(Specification::and).orElse((root, q, cb) -> cb.conjunction());
    }
}
