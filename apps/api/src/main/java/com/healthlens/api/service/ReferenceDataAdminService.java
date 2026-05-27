package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.audit.UnifiedAuditLogWriter;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.request.AdminReferenceRangeRequest;
import com.healthlens.api.dto.response.AdminReferenceImportConfirmResponse;
import com.healthlens.api.dto.response.AdminReferenceImportErrorRowResponse;
import com.healthlens.api.dto.response.AdminReferenceImportPreviewResponse;
import com.healthlens.api.dto.response.AdminReferenceImportPreviewRowResponse;
import com.healthlens.api.dto.response.AdminChangeSetDetailResponse;
import com.healthlens.api.dto.response.AdminPendingChangeSetSummary;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.dto.response.AdminReferenceRangeResponse;
import com.healthlens.api.entity.ReferenceDataChangeSet;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceMetricAlias;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.entity.ReferenceRangeAuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.exception.BusinessException;
import com.healthlens.api.repository.ReferenceDataChangeSetRepository;
import com.healthlens.api.repository.ReferenceMetricAliasRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeAuditLogRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import com.healthlens.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ReferenceDataAdminService {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataAdminService.class);

    private static final List<String> NON_NEGATIVE_METRICS = List.of(
            "glucose", "hba1c", "cholesterol", "triglycerides", "hdl", "ldl"
    );
    private static final long IMPORT_FILE_MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> SUPPORTED_IMPORT_EXTENSIONS = Set.of("csv", "json");
    private static final Duration IMPORT_PREVIEW_SESSION_TTL = Duration.ofHours(1);

    private final ReferenceMetricRepository referenceMetricRepository;
    private final ReferenceMetricAliasRepository referenceMetricAliasRepository;
    private final ReferenceRangeRepository referenceRangeRepository;
    private final ReferenceDataChangeSetRepository referenceDataChangeSetRepository;
    private final ReferenceRangeAuditLogRepository referenceRangeAuditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<UUID, ImportPreviewSession> importPreviewSessions = new ConcurrentHashMap<>();
    private final UnifiedAuditLogWriter unifiedAuditLogWriter;

    public ReferenceDataAdminService(
            ReferenceMetricRepository referenceMetricRepository,
            ReferenceMetricAliasRepository referenceMetricAliasRepository,
            ReferenceRangeRepository referenceRangeRepository,
            ReferenceDataChangeSetRepository referenceDataChangeSetRepository,
            ReferenceRangeAuditLogRepository referenceRangeAuditLogRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            Clock clock,
            UnifiedAuditLogWriter unifiedAuditLogWriter
    ) {
        this.referenceMetricRepository = referenceMetricRepository;
        this.referenceMetricAliasRepository = referenceMetricAliasRepository;
        this.referenceRangeRepository = referenceRangeRepository;
        this.referenceDataChangeSetRepository = referenceDataChangeSetRepository;
        this.referenceRangeAuditLogRepository = referenceRangeAuditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.unifiedAuditLogWriter = unifiedAuditLogWriter;
    }

    @Transactional(readOnly = true)
    public List<AdminReferenceMetricResponse> listMetrics() {
        List<ReferenceMetric> allMetrics = referenceMetricRepository.findAllByOrderByNameAsc();
        Map<UUID, List<AdminReferenceRangeResponse>> rangesByMetricId = referenceRangeRepository
                .findAllOrderByMetricIdAscGenderAscMinAgeAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        range -> range.getMetric().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toRangeResponse, Collectors.toList())
                ));

        List<UUID> metricIds = allMetrics.stream().map(ReferenceMetric::getId).toList();
        Map<UUID, AdminPendingChangeSetSummary> pendingByMetricId = buildPendingChangeSetsMap(metricIds);

        return allMetrics.stream()
                .map(metric -> {
                    List<AdminReferenceRangeResponse> ranges = rangesByMetricId.getOrDefault(metric.getId(), List.of());
                    return new AdminReferenceMetricResponse(
                            metric.getId(),
                            metric.getName(),
                            metric.getDisplayNameVi(),
                            metric.getUnit(),
                            metric.getStatus(),
                            ranges.size(),
                            ranges,
                            pendingByMetricId.get(metric.getId())
                    );
                })
                .toList();
    }

    @Transactional
    public AdminReferenceMetricResponse createMetric(UUID adminId, AdminReferenceMetricRequest request) {
        validateMetricRequest(request);
        referenceMetricRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tên chỉ số đã tồn tại");
                });

        boolean isMulti = isMultiAdminMode();
        ReferenceMetric metric = new ReferenceMetric();
        metric.setName(request.name().trim());
        metric.setDisplayNameVi(request.displayNameVi().trim());
        metric.setUnit(request.unit().trim());
        metric.setStatus(isMulti ? "pending" : "active");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);

        String status = isMulti ? "pending" : "active";
        List<AdminReferenceRangeResponse> ranges = request.ranges().stream()
                .map(range -> toRangeEntity(savedMetric, range, status))
                .map(referenceRangeRepository::save)
                .map(this::toRangeResponse)
                .toList();

        if (isMulti) {
            UUID changeSetId = persistChangeSet(
                    adminId,
                    savedMetric.getId(),
                    "METRIC",
                    "CREATE",
                    buildSnapshot(savedMetric, request, "pending"),
                    "pending"
            );
            recordReferenceAudit(
                    adminId,
                    AuditActions.SUBMIT_REFERENCE_CHANGE_SET,
                    savedMetric.getId(),
                    null,
                    Map.of(
                            "changeSetId", changeSetId,
                            "operation", "CREATE",
                            "snapshot", buildSnapshot(savedMetric, request, "pending")
                    )
            );
        } else {
            recordReferenceAudit(
                    adminId,
                    AuditActions.CREATE_REFERENCE_METRIC,
                    savedMetric.getId(),
                    null,
                    buildSnapshot(savedMetric, request, "active")
            );
        }

        return new AdminReferenceMetricResponse(
                savedMetric.getId(),
                savedMetric.getName(),
                savedMetric.getDisplayNameVi(),
                savedMetric.getUnit(),
                savedMetric.getStatus(),
                ranges.size(),
                ranges
        );
    }

    @Transactional
    public AdminReferenceChangeSetResponse updateMetric(UUID adminId, UUID metricId, AdminReferenceMetricRequest request) {
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần cập nhật"));
        validateMetricRequest(request);

        referenceMetricRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(metricId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tên chỉ số đã tồn tại");
                });

        if (!isMultiAdminMode()) {
            Map<String, Object> before = buildSnapshot(metric, null, metric.getStatus());
            metric.setName(request.name().trim());
            metric.setDisplayNameVi(request.displayNameVi().trim());
            metric.setUnit(request.unit().trim());
            // If it was pending/draft, make it active
            if (!"active".equals(metric.getStatus())) {
                metric.setStatus("active");
            }
            referenceMetricRepository.save(metric);
            replaceRanges(metric, request.ranges(), "active");
            recordReferenceAudit(
                    adminId,
                    AuditActions.UPDATE_REFERENCE_METRIC,
                    metricId,
                    before,
                    buildSnapshot(metric, request, "active")
            );
            return new AdminReferenceChangeSetResponse(
                    null,
                    "active",
                    "Đã cập nhật chỉ số trực tiếp.",
                    "updated"
            );
        }

        Map<String, Object> before = buildSnapshot(metric, null, metric.getStatus());
        UUID changeSetId = persistChangeSet(
                adminId,
                metricId,
                "METRIC",
                "UPDATE",
                buildSnapshot(metric, request, "pending"),
                "pending"
        );
        recordReferenceAudit(
                adminId,
                AuditActions.SUBMIT_REFERENCE_CHANGE_SET,
                metricId,
                before,
                Map.of(
                        "changeSetId", changeSetId,
                        "operation", "UPDATE",
                        "snapshot", buildSnapshot(metric, request, "pending")
                )
        );

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "pending",
                "Đã gửi yêu cầu thay đổi để phê duyệt.",
                "change-set-submitted"
        );
    }

    @Transactional
    public AdminReferenceChangeSetResponse deactivateMetric(UUID adminId, UUID metricId) {
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần xóa"));

        if (!isMultiAdminMode()) {
            Map<String, Object> before = buildSnapshot(metric, null, metric.getStatus());
            metric.setStatus("deactivated");
            referenceMetricRepository.save(metric);
            List<ReferenceRange> ranges = referenceRangeRepository
                    .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId);
            for (ReferenceRange range : ranges) {
                range.setStatus("deactivated");
                referenceRangeRepository.save(range);
            }
            recordReferenceAudit(
                    adminId,
                    AuditActions.DEACTIVATE_REFERENCE_METRIC,
                    metricId,
                    before,
                    Map.of("status", "deactivated")
            );
            return new AdminReferenceChangeSetResponse(
                    null,
                    "deactivated",
                    "Đã ngưng áp dụng chỉ số trực tiếp.",
                    "deactivated"
            );
        }

        Map<String, Object> before = buildSnapshot(metric, null, metric.getStatus());
        UUID changeSetId = persistChangeSet(
                adminId,
                metricId,
                "METRIC",
                "DEACTIVATE",
                Map.of("status", "deactivated"),
                "pending"
        );
        recordReferenceAudit(
                adminId,
                AuditActions.SUBMIT_REFERENCE_CHANGE_SET,
                metricId,
                before,
                Map.of("changeSetId", changeSetId, "operation", "DEACTIVATE", "status", "pending")
        );

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "pending",
                "Đã gửi yêu cầu ngưng áp dụng chỉ số để phê duyệt.",
                "change-set-submitted"
        );
    }

    @Transactional
    public AdminReferenceMetricResponse reactivateMetric(UUID adminId, UUID metricId) {
        Objects.requireNonNull(adminId, "adminId");
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần kích hoạt lại"));

        Map<String, Object> before = buildSnapshot(metric, null, metric.getStatus());
        metric.setStatus("active");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);
        List<AdminReferenceRangeResponse> ranges = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)
                .stream()
                .map(range -> {
                    range.setStatus("active");
                    return referenceRangeRepository.save(range);
                })
                .map(this::toRangeResponse)
                .toList();

        recordReferenceAudit(
                adminId,
                AuditActions.REACTIVATE_REFERENCE_METRIC,
                metricId,
                before,
                buildSnapshot(savedMetric, null, "active")
        );

        return new AdminReferenceMetricResponse(
                savedMetric.getId(),
                savedMetric.getName(),
                savedMetric.getDisplayNameVi(),
                savedMetric.getUnit(),
                savedMetric.getStatus(),
                ranges.size(),
                ranges
        );
    }

    @Transactional(readOnly = true)
    public AdminReferenceImportPreviewResponse previewImport(UUID adminId, MultipartFile file) {
        Objects.requireNonNull(adminId, "adminId");
        Instant now = clock.instant();
        pruneExpiredImportPreviewSessions(now);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp CSV hoặc JSON để nhập.");
        }
        if (file.getSize() > IMPORT_FILE_MAX_BYTES) {
            throw new IllegalArgumentException("Tệp vượt quá 5MB. Vui lòng chọn tệp nhỏ hơn.");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!SUPPORTED_IMPORT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ định dạng CSV hoặc JSON.");
        }

        List<ImportRowEnvelope> rawRows = parseImportFileRaw(file, extension);
        if (rawRows.isEmpty()) {
            throw new IllegalArgumentException("Tệp nhập không có dữ liệu.");
        }

        List<AdminReferenceImportPreviewRowResponse> validRows = new ArrayList<>();
        List<AdminReferenceImportErrorRowResponse> errorRows = new ArrayList<>();

        List<ImportRowCandidate> rowLevelValid = new ArrayList<>();
        for (ImportRowEnvelope raw : rawRows) {
            try {
                ImportRowCandidate candidate = ImportRowCandidate.fromMap(raw.line(), raw.data());
                if (!candidate.aliasOnly()) {
                    AdminReferenceRangeRequest range = candidate.toRangeRequest();
                    validateRange(candidate.metricName(), range);
                }
                rowLevelValid.add(candidate);
            } catch (Exception ex) {
                errorRows.add(new AdminReferenceImportErrorRowResponse(raw.line(), ex.getMessage()));
            }
        }

        Map<Integer, String> overlapErrorByLine = new LinkedHashMap<>();
        Map<MetricKey, List<ImportRowCandidate>> byMetric = rowLevelValid.stream()
                .filter(candidate -> !candidate.aliasOnly())
                .collect(Collectors.groupingBy(
                        c -> new MetricKey(c.metricName(), c.displayNameVi(), c.unit()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        for (List<ImportRowCandidate> group : byMetric.values()) {
            List<AdminReferenceRangeRequest> ranges = group.stream()
                    .map(ImportRowCandidate::toRangeRequest)
                    .toList();
            Set<Integer> badIndices = findOverlappingRangeIndices(ranges);
            if (badIndices.isEmpty()) {
                continue;
            }
            String conflictLines = badIndices.stream()
                    .map(group::get)
                    .map(ImportRowCandidate::line)
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            String message = "Trùng phân đoạn cột Giới tính hoặc Tuổi với một hoặc nhiều dòng khác trong cùng chỉ số "
                    + "(các dòng liên quan: " + conflictLines + "). "
                    + "Hãy điều chỉnh để các ngưỡng không chồng lấn.";
            for (int idx : badIndices) {
                overlapErrorByLine.put(group.get(idx).line(), message);
            }
        }

        for (ImportRowCandidate candidate : rowLevelValid) {
            String overlapMessage = overlapErrorByLine.get(candidate.line());
            if (overlapMessage != null) {
                errorRows.add(new AdminReferenceImportErrorRowResponse(candidate.line(), overlapMessage));
            } else {
                validRows.add(candidate.toPreviewRow());
            }
        }

        UUID importId = UUID.randomUUID();
        Instant expiresAt = now.plus(IMPORT_PREVIEW_SESSION_TTL);
        importPreviewSessions.put(
                importId,
                new ImportPreviewSession(adminId, expiresAt, validRows, errorRows)
        );
        return new AdminReferenceImportPreviewResponse(importId, validRows, errorRows);
    }

    @Transactional
    public AdminReferenceImportConfirmResponse confirmImport(UUID adminId, UUID importId) {
        Instant now = clock.instant();
        pruneExpiredImportPreviewSessions(now);
        ImportPreviewSession session = claimImportPreviewSession(adminId, importId, now);

        Map<MetricKey, List<AdminReferenceImportPreviewRowResponse>> groupedRows = session.validRows()
                .stream()
                .filter(this::hasRangeValues)
                .collect(Collectors.groupingBy(
                        row -> new MetricKey(row.metricName(), row.displayNameVi(), row.unit()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<UUID> changeSetIds = new ArrayList<>();
        List<AdminReferenceImportPreviewRowResponse> aliasOnlyRows = session.validRows().stream()
                .filter(row -> !hasRangeValues(row))
                .toList();
        for (Map.Entry<MetricKey, List<AdminReferenceImportPreviewRowResponse>> entry : groupedRows.entrySet()) {
            MetricKey key = entry.getKey();
            List<AdminReferenceImportPreviewRowResponse> rowsForMetric = new ArrayList<>(entry.getValue());
            aliasOnlyRows.stream()
                    .filter(row -> normalizeMetricName(row.metricName()).equals(normalizeMetricName(key.metricName())))
                    .forEach(rowsForMetric::add);
            List<AdminReferenceRangeRequest> ranges = entry.getValue().stream()
                    .filter(this::hasRangeValues)
                    .map(row -> new AdminReferenceRangeRequest(
                            row.minValue(),
                            row.maxValue(),
                            row.attentionMin(),
                            row.attentionMax(),
                            row.gender(),
                            row.minAge(),
                            row.maxAge()
                    ))
                    .toList();

            validateRangesNoOverlap(ranges);
            AdminReferenceMetricRequest request = new AdminReferenceMetricRequest(
                    key.metricName(),
                    key.displayNameVi(),
                    key.unit(),
                    ranges
            );
            validateMetricRequest(request);

            UUID metricId = referenceMetricRepository
                    .findByNameIgnoreCase(key.metricName().trim())
                    .map(ReferenceMetric::getId)
                    .orElse(null);
            UUID changeSetId = persistChangeSet(
                    adminId,
                    metricId,
                    "METRIC",
                    metricId == null ? "CREATE" : "UPDATE",
                    buildSnapshotForImport(metricId, request, rowsForMetric),
                    "draft"
            );
            changeSetIds.add(changeSetId);
        }

        for (AdminReferenceImportPreviewRowResponse aliasOnlyRow : aliasOnlyRows) {
            boolean mergedIntoRangeChangeSet = groupedRows.keySet().stream()
                    .anyMatch(key -> normalizeMetricName(aliasOnlyRow.metricName()).equals(normalizeMetricName(key.metricName())));
            if (mergedIntoRangeChangeSet) {
                continue;
            }
            ReferenceMetric metric = referenceMetricRepository.findByNameIgnoreCase(aliasOnlyRow.metricName().trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Dòng alias-only cần chỉ số đã tồn tại hoặc dòng range cùng chỉ số: " + aliasOnlyRow.metricName()));
            UUID changeSetId = persistChangeSet(
                    adminId,
                    metric.getId(),
                    "METRIC",
                    "UPDATE",
                    buildAliasOnlySnapshot(metric, List.of(aliasOnlyRow)),
                    "draft"
            );
            changeSetIds.add(changeSetId);
        }

        return new AdminReferenceImportConfirmResponse(
                importId,
                changeSetIds.size(),
                changeSetIds,
                "Đã tạo tập dữ liệu thay đổi  nháp từ dữ liệu import."
        );
    }

    private void recordReferenceAudit(
            UUID actorId,
            String action,
            UUID resourceId,
            Map<String, Object> oldValue,
            Map<String, Object> newValue
    ) {
        Objects.requireNonNull(actorId, "actorId is required for reference data audit");
        unifiedAuditLogWriter.record(
                actorId,
                action,
                AuditResourceTypes.REFERENCE_DATA,
                resourceId,
                oldValue != null ? writeJson(oldValue) : null,
                newValue != null ? writeJson(newValue) : null
        );
    }

    private void validateMetricRequest(AdminReferenceMetricRequest request) {
        String metricName = request.name().trim();
        request.ranges().forEach(range -> validateRange(metricName, range));
        validateRangesNoOverlap(request.ranges());
    }

    private void validateRangesNoOverlap(List<AdminReferenceRangeRequest> ranges) {
        if (ranges.size() < 2) {
            return;
        }
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (referenceRangesOverlap(ranges.get(i), ranges.get(j))) {
                    throw new IllegalArgumentException(
                            "Ngưỡng #" + (i + 1) + " và #" + (j + 1)
                                    + " trùng phân đoạn giới tính/độ tuổi. "
                                    + "Hãy điều chỉnh để các ngưỡng không chồng lấn."
                    );
                }
            }
        }
    }

    /** Indices in {@code ranges} that participate in at least one gender/age overlap with another range in the list. */
    private Set<Integer> findOverlappingRangeIndices(List<AdminReferenceRangeRequest> ranges) {
        Set<Integer> bad = new HashSet<>();
        if (ranges.size() < 2) {
            return bad;
        }
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                if (referenceRangesOverlap(ranges.get(i), ranges.get(j))) {
                    bad.add(i);
                    bad.add(j);
                }
            }
        }
        return bad;
    }

    private boolean referenceRangesOverlap(AdminReferenceRangeRequest a, AdminReferenceRangeRequest b) {
        return genderSegmentsOverlap(a.gender(), b.gender())
                && ageSegmentsOverlap(a.minAge(), a.maxAge(), b.minAge(), b.maxAge());
    }

    private boolean genderSegmentsOverlap(String genderA, String genderB) {
        if (genderA == null || genderA.isBlank() || genderB == null || genderB.isBlank()) {
            return true;
        }
        return genderA.equalsIgnoreCase(genderB);
    }

    private boolean ageSegmentsOverlap(Integer minA, Integer maxA, Integer minB, Integer maxB) {
        int effectiveMinA = minA != null ? minA : 0;
        int effectiveMaxA = maxA != null ? maxA : Integer.MAX_VALUE;
        int effectiveMinB = minB != null ? minB : 0;
        int effectiveMaxB = maxB != null ? maxB : Integer.MAX_VALUE;
        return effectiveMinA <= effectiveMaxB && effectiveMinB <= effectiveMaxA;
    }

    private void validateRange(String metricName, AdminReferenceRangeRequest range) {
        if (range.minValue().compareTo(range.maxValue()) >= 0) {
            throw new IllegalArgumentException("Giá trị cột Ngưỡng min phải nhỏ hơn cột Ngưỡng max.");
        }
        if (range.attentionMin().compareTo(range.attentionMax()) >= 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối thiểu phải nhỏ hơn ngưỡng cảnh báo tối đa");
        }
        if (range.attentionMin().compareTo(range.minValue()) > 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối thiểu không được lớn hơn ngưỡng tối thiểu bình thường");
        }
        if (range.attentionMax().compareTo(range.maxValue()) < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối đa không được nhỏ hơn ngưỡng tối đa bình thường");
        }
        if (range.minAge() != null && range.maxAge() != null && range.minAge() > range.maxAge()) {
            throw new IllegalArgumentException("Cột Tuổi: giá trị tối thiểu không được lớn hơn giá trị tối đa.");
        }
        if (isNonNegativeMetric(metricName)
                && (range.minValue().compareTo(BigDecimal.ZERO) < 0
                || range.maxValue().compareTo(BigDecimal.ZERO) < 0
                || range.attentionMin().compareTo(BigDecimal.ZERO) < 0
                || range.attentionMax().compareTo(BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("Cột Ngưỡng min và Ngưỡng max không được âm cho chỉ số này.");
        }
    }

    private boolean isNonNegativeMetric(String metricName) {
        return NON_NEGATIVE_METRICS.contains(normalizeMetricName(metricName));
    }

    private ReferenceRange toRangeEntity(ReferenceMetric metric, AdminReferenceRangeRequest range, String status) {
        ReferenceRange entity = new ReferenceRange();
        entity.setMetric(metric);
        entity.setMinValue(range.minValue());
        entity.setMaxValue(range.maxValue());
        entity.setAttentionMin(range.attentionMin());
        entity.setAttentionMax(range.attentionMax());
        entity.setGender(range.gender());
        entity.setMinAge(range.minAge());
        entity.setMaxAge(range.maxAge());
        entity.setStatus(status);
        return entity;
    }

    private AdminReferenceRangeResponse toRangeResponse(ReferenceRange range) {
        return new AdminReferenceRangeResponse(
                range.getId(),
                range.getMinValue(),
                range.getMaxValue(),
                range.getAttentionMin(),
                range.getAttentionMax(),
                range.getGender(),
                range.getMinAge(),
                range.getMaxAge(),
                range.getStatus()
        );
    }

    private UUID persistChangeSet(UUID adminId, UUID entityId, String entityType, String operation, Map<String, Object> changes, String status) {
        ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
        cs.setAdminId(adminId);
        cs.setEntityId(entityId);
        cs.setEntityType(entityType);
        cs.setOperation(operation);
        try {
            cs.setChangesJson(objectMapper.writeValueAsString(changes));
        } catch (JsonProcessingException e) {
            throw new BusinessException("Không thể serialize thay đổi", e);
        }
        cs.setStatus(status);
        return referenceDataChangeSetRepository.save(cs).getId();
    }

    private void replaceRanges(ReferenceMetric metric, List<AdminReferenceRangeRequest> ranges, String status) {
        List<ReferenceRange> existingRanges = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metric.getId());
        if (!existingRanges.isEmpty()) {
            referenceRangeRepository.deleteAll(existingRanges);
        }
        ranges.stream()
                .map(range -> toRangeEntity(metric, range, status))
                .forEach(referenceRangeRepository::save);
    }

    private Map<String, Object> buildSnapshot(ReferenceMetric metric, AdminReferenceMetricRequest request, String targetStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metricId", metric.getId());
        if (request != null) {
            payload.put("name", request.name().trim());
            payload.put("displayNameVi", request.displayNameVi().trim());
            payload.put("unit", request.unit().trim());
            payload.put("status", targetStatus);
            payload.put("ranges", request.ranges().stream().map(range -> {
                Map<String, Object> rangePayload = new LinkedHashMap<>();
                rangePayload.put("minValue", range.minValue());
                rangePayload.put("maxValue", range.maxValue());
                rangePayload.put("attentionMin", range.attentionMin());
                rangePayload.put("attentionMax", range.attentionMax());
                rangePayload.put("gender", range.gender());
                rangePayload.put("minAge", range.minAge());
                rangePayload.put("maxAge", range.maxAge());
                rangePayload.put("status", targetStatus);
                return rangePayload;
            }).toList());
        } else {
            payload.put("name", metric.getName());
            payload.put("displayNameVi", metric.getDisplayNameVi());
            payload.put("unit", metric.getUnit());
            payload.put("status", metric.getStatus());
            List<ReferenceRange> ranges = referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metric.getId());
            payload.put("ranges", ranges.stream().map(range -> {
                Map<String, Object> rangePayload = new LinkedHashMap<>();
                rangePayload.put("minValue", range.getMinValue());
                rangePayload.put("maxValue", range.getMaxValue());
                rangePayload.put("attentionMin", range.getAttentionMin());
                rangePayload.put("attentionMax", range.getAttentionMax());
                rangePayload.put("gender", range.getGender());
                rangePayload.put("minAge", range.getMinAge());
                rangePayload.put("maxAge", range.getMaxAge());
                rangePayload.put("status", range.getStatus());
                return rangePayload;
            }).toList());
        }
        return payload;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Không thể lưu tập dữ liệu thay đổi ", ex);
        }
    }

    private String normalizeMetricName(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private List<ImportRowEnvelope> parseImportFileRaw(MultipartFile file, String extension) {
        try {
            if ("json".equals(extension)) {
                return parseJsonImportRaw(file);
            }
            return parseCsvImportRaw(file);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc nội dung file import.");
        }
    }

    private List<ImportRowEnvelope> parseJsonImportRaw(MultipartFile file) throws IOException {
        String json = stripUtf8Bom(new String(file.getBytes(), StandardCharsets.UTF_8));
        List<Map<String, Object>> rows = objectMapper.readValue(json, new TypeReference<>() {});
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<ImportRowEnvelope> result = new ArrayList<>();
        int line = 1;
        for (Map<String, Object> row : rows) {
            result.add(new ImportRowEnvelope(line, row == null ? Map.of() : row));
            line++;
        }
        return result;
    }

    private List<ImportRowEnvelope> parseCsvImportRaw(MultipartFile file) {
        String content;
        try {
            content = stripUtf8Bom(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc nội dung file CSV.");
        }
        List<CsvRecord> records = parseCsvRecords(content);
        if (records.size() < 2) {
            return List.of();
        }

        List<String> headers = parseCsvLine(records.getFirst().text()).stream()
                .map(this::normalizeHeader)
                .toList();
        List<ImportRowEnvelope> result = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            CsvRecord record = records.get(i);
            if (record.text().isBlank()) {
                continue;
            }
            List<String> values = parseCsvLine(record.text());
            Map<String, Object> row = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                String value = col < values.size() ? values.get(col) : "";
                row.put(headers.get(col), value);
            }
            result.add(new ImportRowEnvelope(record.startLine(), row));
        }
        return result;
    }

    private record ImportRowEnvelope(int line, Map<String, Object> data) {}

    private record CsvRecord(int startLine, String text) {}

    private List<CsvRecord> parseCsvRecords(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<CsvRecord> records = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int line = 1;
        int recordStartLine = 1;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    current.append(ch);
                    current.append(content.charAt(i + 1));
                    i++;
                } else {
                    inQuotes = !inQuotes;
                    current.append(ch);
                }
            } else if ((ch == '\n' || ch == '\r') && !inQuotes) {
                records.add(new CsvRecord(recordStartLine, current.toString()));
                current.setLength(0);
                if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                    i++;
                }
                line++;
                recordStartLine = line;
            } else {
                current.append(ch);
                if (ch == '\n' || ch == '\r') {
                    line++;
                }
            }
        }
        if (!current.isEmpty()) {
            records.add(new CsvRecord(recordStartLine, current.toString()));
        }
        return records;
    }

    private List<String> parseCsvLine(String line) {
        if (line == null || line.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return stripUtf8Bom(header.trim()).toLowerCase(Locale.ROOT);
    }

    /** Strips UTF-8 BOM (\\uFEFF) often present in CSV exported from Excel/Google Sheets. */
    private static String stripUtf8Bom(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        if (text.charAt(0) == '\uFEFF') {
            return text.substring(1);
        }
        return text;
    }

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        String[] parts = filename.toLowerCase(Locale.ROOT).split("\\.");
        return parts[parts.length - 1];
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private boolean hasRangeValues(AdminReferenceImportPreviewRowResponse row) {
        return row.minValue() != null || row.maxValue() != null;
    }

    private Map<String, Object> buildSnapshotForImport(
            UUID metricId,
            AdminReferenceMetricRequest request,
            List<AdminReferenceImportPreviewRowResponse> rows
    ) {
        Map<String, Object> snapshot;
        if (metricId != null) {
            ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy metric hiện hữu để cập nhật."));
            snapshot = buildSnapshot(metric, request, "pending");
        } else {
            ReferenceMetric transientMetric = new ReferenceMetric();
            transientMetric.setId(UUID.randomUUID());
            transientMetric.setName(request.name());
            transientMetric.setDisplayNameVi(request.displayNameVi());
            transientMetric.setUnit(request.unit());
            transientMetric.setStatus("pending");
            snapshot = buildSnapshot(transientMetric, request, "pending");
        }
        List<Map<String, Object>> aliases = collectAliases(rows);
        if (!aliases.isEmpty()) {
            snapshot.put("aliases", aliases);
        }
        snapshot.put("provenance", collectProvenance(rows));
        addRangeProvenance(snapshot, rows);
        return snapshot;
    }

    private Map<String, Object> buildAliasOnlySnapshot(
            ReferenceMetric metric,
            List<AdminReferenceImportPreviewRowResponse> rows
    ) {
        Map<String, Object> snapshot = buildSnapshot(metric, null, "active");
        List<Map<String, Object>> aliases = collectAliases(rows);
        if (!aliases.isEmpty()) {
            snapshot.put("aliases", aliases);
        }
        snapshot.put("provenance", collectProvenance(rows));
        return snapshot;
    }

    private List<Map<String, Object>> collectAliases(List<AdminReferenceImportPreviewRowResponse> rows) {
        Map<String, Map<String, Object>> aliasesByNormalized = new LinkedHashMap<>();
        for (AdminReferenceImportPreviewRowResponse row : rows) {
            for (String alias : splitAliases(row.aliases())) {
                String normalized = normalizeMetricName(alias);
                if (normalized.isBlank()) {
                    continue;
                }
                aliasesByNormalized.putIfAbsent(normalized, Map.of(
                        "alias", alias,
                        "aliasNormalized", normalized,
                        "locale", inferAliasLocale(alias),
                        "active", true
                ));
            }
        }
        return new ArrayList<>(aliasesByNormalized.values());
    }

    private List<Map<String, Object>> collectProvenance(List<AdminReferenceImportPreviewRowResponse> rows) {
        List<Map<String, Object>> provenance = new ArrayList<>();
        for (AdminReferenceImportPreviewRowResponse row : rows) {
            provenance.add(rangeProvenance(row));
        }
        return provenance;
    }

    @SuppressWarnings("unchecked")
    private void addRangeProvenance(Map<String, Object> snapshot, List<AdminReferenceImportPreviewRowResponse> rows) {
        Object rangesObj = snapshot.get("ranges");
        if (!(rangesObj instanceof List<?> rangesList)) {
            return;
        }
        int limit = Math.min(rangesList.size(), rows.size());
        for (int index = 0; index < limit; index++) {
            Object rangeItem = rangesList.get(index);
            if (rangeItem instanceof Map<?, ?> rangeMap) {
                ((Map<String, Object>) rangeMap).put("provenance", rangeProvenance(rows.get(index)));
            }
        }
    }

    private Map<String, Object> rangeProvenance(AdminReferenceImportPreviewRowResponse row) {
        Map<String, Object> provenance = new LinkedHashMap<>();
        provenance.put("line", row.line());
        provenance.put("sourceUrl", row.sourceUrl());
        provenance.put("sourceTitle", row.sourceTitle());
        provenance.put("sourcePublisher", row.sourcePublisher());
        provenance.put("accessedDate", row.accessedDate());
        provenance.put("rangeType", row.rangeType());
        provenance.put("reviewerNote", row.reviewerNote());
        provenance.put("conversionNote", row.conversionNote());
        provenance.put("methodSpecimenNote", row.methodSpecimenNote());
        return provenance;
    }

    private List<String> splitAliases(String aliases) {
        if (aliases == null || aliases.isBlank()) {
            return List.of();
        }
        return Arrays.stream(aliases.split("[|;]"))
                .map(String::trim)
                .filter(alias -> !alias.isBlank())
                .distinct()
                .toList();
    }

    private String inferAliasLocale(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        String lower = alias.toLowerCase(Locale.ROOT);
        boolean vietnamese = lower.matches(".*[ăâđêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ].*")
                || lower.contains("bach ")
                || lower.contains("huyet ")
                || lower.contains("duong ");
        return vietnamese ? "vi" : "en";
    }

    private void pruneExpiredImportPreviewSessions(Instant now) {
        importPreviewSessions.entrySet().removeIf(entry -> entry.getValue().expired(now));
    }

    /**
     * Validates preview session then atomically removes it so only one confirm can proceed per importId.
     */
    private ImportPreviewSession claimImportPreviewSession(UUID adminId, UUID importId, Instant now) {
        ImportPreviewSession session = importPreviewSessions.get(importId);
        if (session == null) {
            throw new IllegalArgumentException("Không tìm thấy nhập dữ liệu hoặc nhập dữ liệu đã hết hạn.");
        }
        if (session.expired(now)) {
            importPreviewSessions.remove(importId);
            throw new IllegalArgumentException("Không tìm thấy nhập dữ liệu hoặc nhập dữ liệu đã hết hạn.");
        }
        if (!session.ownerAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Không tìm thấy nhập dữ liệu hoặc nhập dữ liệu đã hết hạn.");
        }
        if (session.validRows().isEmpty()) {
            throw new IllegalStateException("Không có dòng hợp lệ để tạo tập dữ liệu thay đổi");
        }

        ImportPreviewSession claimed = importPreviewSessions.remove(importId);
        if (claimed == null) {
            throw new IllegalArgumentException("Không tìm thấy nhập dữ liệu hoặc nhập dữ liệu đã hết hạn.");
        }
        return claimed;
    }

    private record ImportPreviewSession(
            UUID ownerAdminId,
            Instant expiresAt,
            List<AdminReferenceImportPreviewRowResponse> validRows,
            List<AdminReferenceImportErrorRowResponse> errorRows
    ) {
        boolean expired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }

    private record MetricKey(String metricName, String displayNameVi, String unit) {
    }

    private record ImportRowCandidate(
            int line,
            String metricName,
            String displayNameVi,
            String unit,
            BigDecimal minValue,
            BigDecimal maxValue,
            BigDecimal attentionMin,
            BigDecimal attentionMax,
            boolean attentionMinDefaulted,
            boolean attentionMaxDefaulted,
            boolean aliasOnly,
            String gender,
            Integer minAge,
            Integer maxAge,
            String aliases,
            String sourceUrl,
            String sourceTitle,
            String sourcePublisher,
            String accessedDate,
            String rangeType,
            String reviewerNote,
            String conversionNote,
            String methodSpecimenNote
    ) {
        private static final Set<String> MALE_VALUES = Set.of("male", "m", "nam");
        private static final Set<String> FEMALE_VALUES = Set.of("female", "f", "nu", "nữ");

        static ImportRowCandidate fromMap(int line, Map<String, Object> row) {
            String metricName = pickFirst(row, "metricname", "metric_name");
            String displayNameVi = pickFirst(row, "displaynamevi", "display_name_vi", "displayname");
            String unit = pickFirst(row, "unit");
            BigDecimal minValue = toDecimal(pickFirst(row, "minvalue", "min"), "Ngưỡng min");
            BigDecimal maxValue = toDecimal(pickFirst(row, "maxvalue", "max"), "Ngưỡng max");
            BigDecimal attentionMin = toDecimal(pickFirst(row, "attentionmin", "attention_min"), "Ngưỡng cảnh báo tối thiểu");
            BigDecimal attentionMax = toDecimal(pickFirst(row, "attentionmax", "attention_max"), "Ngưỡng cảnh báo tối đa");
            String gender = normalizeGender(pickFirst(row, "gender"));
            Integer minAge = toIntegerValue(pickFirst(row, "minage", "agemin"), "Tuổi");
            Integer maxAge = toIntegerValue(pickFirst(row, "maxage", "agemax"), "Tuổi");
            String aliases = pickFirst(row, "aliases", "alias");
            String sourceUrl = pickFirst(row, "sourceurl", "source_url");
            String sourceTitle = pickFirst(row, "sourcetitle", "source_title");
            String sourcePublisher = pickFirst(row, "sourcepublisher", "source_publisher", "publisher");
            String accessedDate = pickFirst(row, "accesseddate", "accessed_date");
            String rangeType = pickFirst(row, "rangetype", "range_type");
            String reviewerNote = pickFirst(row, "reviewernote", "reviewer_note");
            String conversionNote = pickFirst(row, "conversionnote", "conversion_note");
            String methodSpecimenNote = pickFirst(row, "methodspecimennote", "method_specimen_note");
            boolean attentionMinDefaulted = attentionMin == null;
            boolean attentionMaxDefaulted = attentionMax == null;
            boolean aliasOnly = minValue == null && maxValue == null && !aliases.isBlank();

            if (metricName.isBlank()) {
                throw new IllegalArgumentException("Thiếu cột Chỉ số (phần mã kỹ thuật).");
            }
            if (displayNameVi.isBlank() && !aliasOnly) {
                throw new IllegalArgumentException("Thiếu cột Chỉ số (phần tên hiển thị).");
            }
            if (unit.isBlank() && !aliasOnly) {
                throw new IllegalArgumentException("Thiếu cột Đơn vị.");
            }
            if (aliasOnly) {
                return new ImportRowCandidate(
                        line,
                        metricName,
                        displayNameVi,
                        unit,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        true,
                        gender,
                        minAge,
                        maxAge,
                        aliases,
                        sourceUrl,
                        sourceTitle,
                        sourcePublisher,
                        accessedDate,
                        rangeType,
                        reviewerNote,
                        conversionNote,
                        methodSpecimenNote
                );
            }
            if (minValue == null && maxValue == null) {
                throw new IllegalArgumentException("Thiếu cột Ngưỡng min và Ngưỡng max.");
            }
            if (minValue == null) {
                throw new IllegalArgumentException("Thiếu cột Ngưỡng min.");
            }
            if (maxValue == null) {
                throw new IllegalArgumentException("Thiếu cột Ngưỡng max.");
            }
            if (attentionMin == null) {
                attentionMin = minValue;
            }
            if (attentionMax == null) {
                attentionMax = maxValue;
            }
            return new ImportRowCandidate(
                    line,
                    metricName,
                    displayNameVi,
                    unit,
                    minValue,
                    maxValue,
                    attentionMin,
                    attentionMax,
                    attentionMinDefaulted,
                    attentionMaxDefaulted,
                    false,
                    gender,
                    minAge,
                    maxAge,
                    aliases,
                    sourceUrl,
                    sourceTitle,
                    sourcePublisher,
                    accessedDate,
                    rangeType,
                    reviewerNote,
                    conversionNote,
                    methodSpecimenNote
            );
        }

        AdminReferenceRangeRequest toRangeRequest() {
            return new AdminReferenceRangeRequest(minValue, maxValue, attentionMin, attentionMax, gender, minAge, maxAge);
        }

        AdminReferenceImportPreviewRowResponse toPreviewRow() {
            return new AdminReferenceImportPreviewRowResponse(
                    line,
                    metricName,
                    displayNameVi,
                    unit,
                    minValue,
                    maxValue,
                    attentionMin,
                    attentionMax,
                    attentionMinDefaulted,
                    attentionMaxDefaulted,
                    gender,
                    minAge,
                    maxAge,
                    aliases,
                    sourceUrl,
                    sourceTitle,
                    sourcePublisher,
                    accessedDate,
                    rangeType,
                    reviewerNote,
                    conversionNote,
                    methodSpecimenNote
            );
        }

        private static String pickFirst(Map<String, Object> row, String... keys) {
            for (String key : keys) {
                if (row.containsKey(key)) {
                    return stringifyImportValue(row.get(key));
                }
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String normalizedKey = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (Arrays.asList(keys).contains(normalizedKey)) {
                    return stringifyImportValue(entry.getValue());
                }
            }
            return "";
        }

        private static String stringifyImportValue(Object value) {
            if (value == null) {
                return "";
            }
            if (value instanceof List<?> list) {
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(String::trim)
                        .filter(item -> !item.isBlank())
                        .collect(Collectors.joining("|"));
            }
            return value.toString().trim();
        }

        private static BigDecimal toDecimal(String value, String columnLabel) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Giá trị cột " + columnLabel + " không hợp lệ: " + value);
            }
        }

        private static Integer toIntegerValue(String value, String columnLabel) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Giá trị cột " + columnLabel + " không hợp lệ: " + value);
            }
        }

        private static String normalizeGender(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (MALE_VALUES.contains(normalized)) {
                return "male";
            }
            if (FEMALE_VALUES.contains(normalized)) {
                return "female";
            }
            throw new IllegalArgumentException("Giá trị cột Giới tính không hợp lệ: " + value);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, AdminPendingChangeSetSummary> buildPendingChangeSetsMap(List<UUID> metricIds) {
        if (metricIds.isEmpty()) {
            return Map.of();
        }
        List<ReferenceDataChangeSet> draftSets = referenceDataChangeSetRepository
                .findAllByEntityIdInAndStatusOrderByCreatedAtDesc(metricIds, "draft");
        Map<UUID, AdminPendingChangeSetSummary> result = new LinkedHashMap<>();
        for (ReferenceDataChangeSet cs : draftSets) {
            if (result.containsKey(cs.getEntityId())) {
                continue; // only the latest draft per metric
            }
            try {
                Map<String, Object> snapshot = objectMapper.readValue(cs.getChangesJson(), Map.class);
                String proposedName = snapshot.getOrDefault("name", "").toString();
                String proposedDisplayNameVi = snapshot.getOrDefault("displayNameVi", "").toString();
                String proposedUnit = snapshot.getOrDefault("unit", "").toString();
                Object rangesObj = snapshot.get("ranges");
                int proposedRangesCount = rangesObj instanceof List<?> rangesList ? rangesList.size() : 0;
                result.put(cs.getEntityId(), new AdminPendingChangeSetSummary(
                        cs.getId(),
                        cs.getOperation(),
                        cs.getCreatedAt(),
                        proposedName,
                        proposedDisplayNameVi,
                        proposedUnit,
                        proposedRangesCount
                ));
            } catch (JsonProcessingException ignored) {
                // corrupted JSON — skip
            }
        }
        return result;
    }

    // ========================================================================
    // Approval Workflow (Story 7.4)
    // ========================================================================

    /**
     * Returns true when there are 2+ admin users, requiring cross-approval.
     * When false (single admin), the admin can publish drafts directly.
     */
    @Transactional(readOnly = true)
    public boolean isMultiAdminMode() {
        return userRepository.countByRole(UserRole.ROLE_ADMIN) > 1;
    }

    @Transactional(readOnly = true)
    public List<AdminChangeSetDetailResponse> listPendingChangeSets() {
        return referenceDataChangeSetRepository
                .findAllByStatusOrderByCreatedAtDesc("pending")
                .stream()
                .map(this::toChangeSetDetailResponse)
                .toList();
    }

    /**
     * Single-admin mode: directly activate a draft tập dữ liệu thay đổi  without peer review.
     * Combines submit + approve in one step.
     */
    @Transactional
    public AdminReferenceChangeSetResponse publishChangeSet(UUID changeSetId, UUID adminId) {
        if (isMultiAdminMode()) {
            throw new IllegalStateException(
                    "Chế độ nhiều quản trị viên: không thể kích hoạt trực tiếp. Vui lòng gửi duyệt để quản trị viên khác phê duyệt.");
        }

        ReferenceDataChangeSet cs = referenceDataChangeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tập dữ liệu thay đổi với mã: " + changeSetId));

        if (!List.of("draft", "pending").contains(cs.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể kích hoạt tập dữ liệu thay đổi ở trạng thái bản nháp hoặc chờ duyệt.");
        }

        if (!cs.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Chỉ người tạo bản nháp mới có thể kích hoạt trực tiếp.");
        }

        applyChangesToProduction(cs);

        cs.setStatus("approved");
        cs.setApprovedAt(Instant.now());
        cs.setReviewerId(adminId);
        referenceDataChangeSetRepository.save(cs);

        writeUnifiedChangeSetAudit(AuditActions.PUBLISH_CHANGE_SET, changeSetId, adminId, cs);

        log.info("tập dữ liệu thay đổi  {} published directly by admin {} (single-admin mode)", changeSetId, adminId);

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "approved",
                "Đã kích hoạt và áp dụng thay đổi vào dữ liệu sản xuất.",
                "published"
        );
    }

    /**
     * Multi-admin mode: approve a pending tập dữ liệu thay đổi . Blocks self-approval.
     */
    @Transactional
    public AdminReferenceChangeSetResponse approveChangeSet(UUID changeSetId, UUID reviewerId) {
        ReferenceDataChangeSet cs = referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending")
                .orElseThrow(() -> new IllegalArgumentException(
                    "Không tìm thấy tập dữ liệu thay đổi đang chờ duyệt với mã: " + changeSetId));

        // Block self-approval in multi-admin mode
        if (isMultiAdminMode() && cs.getAdminId().equals(reviewerId)) {
            throw new IllegalArgumentException(
                    "Không thể phê duyệt tập dữ liệu thay đổi do chính bạn tạo. Vui lòng nhờ quản trị viên khác duyệt.");
        }

        applyChangesToProduction(cs);

        cs.setStatus("approved");
        cs.setApprovedAt(Instant.now());
        cs.setReviewerId(reviewerId);
        referenceDataChangeSetRepository.save(cs);

        writeUnifiedChangeSetAudit(AuditActions.APPROVE_CHANGE_SET, changeSetId, reviewerId, cs);

        log.info("tập dữ liệu thay đổi  {} approved by reviewer {}", changeSetId, reviewerId);

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "approved",
                "Đã phê duyệt và áp dụng thay đổi vào dữ liệu sản xuất.",
                "approved"
        );
    }

    @Transactional
    public AdminReferenceChangeSetResponse rejectChangeSet(UUID changeSetId, UUID reviewerId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng cung cấp lý do từ chối.");
        }

        ReferenceDataChangeSet cs = referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending")
                .orElseThrow(() -> new IllegalArgumentException(
                    "Không tìm thấy tập dữ liệu thay đổi đang chờ duyệt với mã: " + changeSetId));

        // Block self-rejection in multi-admin mode
        if (isMultiAdminMode() && cs.getAdminId().equals(reviewerId)) {
            throw new IllegalArgumentException(
                    "Không thể từ chối tập dữ liệu thay đổi do chính bạn tạo. Vui lòng nhờ quản trị viên khác duyệt.");
        }

        cs.setStatus("rejected");
        cs.setReviewerId(reviewerId);
        cs.setRejectionReason(reason);
        referenceDataChangeSetRepository.save(cs);

        writeUnifiedChangeSetAudit(AuditActions.REJECT_CHANGE_SET, changeSetId, reviewerId, cs);

        log.info("tập dữ liệu thay đổi  {} rejected by reviewer {} — reason: {}", changeSetId, reviewerId, reason);

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "rejected",
                "Đã từ chối tập dữ liệu thay đổi. Dữ liệu sản xuất không bị thay đổi.",
                "rejected"
        );
    }

    /**
     * Multi-admin mode: Transition a draft tập dữ liệu thay đổi  to pending status
     * so it appears in the approval queue for another admin to review.
     */
    @Transactional
    public AdminReferenceChangeSetResponse submitChangeSetForApproval(UUID changeSetId, UUID adminId) {
        if (!isMultiAdminMode()) {
            throw new IllegalStateException(
                    "Chế độ một quản trị viên: vui lòng sử dụng chức năng 'Kích hoạt' thay vì gửi duyệt.");
        }

        ReferenceDataChangeSet cs = referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "draft")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy tập dữ liệu thay đổi nháp với mã: " + changeSetId));

        if (!cs.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Chỉ người tạo bản nháp mới có thể gửi duyệt.");
        }

        cs.setStatus("pending");
        referenceDataChangeSetRepository.save(cs);

        recordReferenceAudit(
                adminId,
                AuditActions.SUBMIT_REFERENCE_CHANGE_SET,
                cs.getEntityId(),
                null,
                Map.of("changeSetId", changeSetId, "operation", cs.getOperation(), "status", "pending")
        );

        log.info("tập dữ liệu thay đổi  {} submitted for approval by admin {}", changeSetId, adminId);

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "pending",
                "Đã gửi bản thay đổi để duyệt.",
                "submitted"
        );
    }

    @SuppressWarnings("unchecked")
    private void applyChangesToProduction(ReferenceDataChangeSet cs) {
        Map<String, Object> snapshot;
        try {
            snapshot = objectMapper.readValue(cs.getChangesJson(), new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Không thể đọc dữ liệu tập dữ liệu thay đổi : " + cs.getId(), ex);
        }

        if ("METRIC".equals(cs.getEntityType())) {
            applyMetricChanges(cs, snapshot);
        } else {
            throw new IllegalStateException("Loại dữ liệu không được hỗ trợ: " + cs.getEntityType());
        }
    }

    @SuppressWarnings("unchecked")
    private void applyMetricChanges(ReferenceDataChangeSet cs, Map<String, Object> snapshot) {
        String operation = cs.getOperation();

        if ("CREATE".equals(operation)) {
            if (cs.getEntityId() != null) {
                // createMetric() already persisted a pending metric; activate it on approval
                ReferenceMetric metric = referenceMetricRepository.findById(cs.getEntityId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Không tìm thấy chỉ số bản nháp: " + cs.getEntityId()));
                metric.setName(snapshot.getOrDefault("name", metric.getName()).toString());
                metric.setDisplayNameVi(snapshot.getOrDefault("displayNameVi", metric.getDisplayNameVi()).toString());
                metric.setUnit(snapshot.getOrDefault("unit", metric.getUnit()).toString());
                metric.setStatus("active");
                referenceMetricRepository.save(metric);
                replaceMetricAliases(metric, snapshot);

                List<ReferenceRange> ranges = referenceRangeRepository
                        .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metric.getId());
                for (ReferenceRange range : ranges) {
                    range.setStatus("active");
                    referenceRangeRepository.save(range);
                }
            } else {
                // Import draft: metric not in DB until approval
                createMetricFromSnapshot(cs, snapshot);
            }
        } else if ("UPDATE".equals(operation)) {
            UUID metricId = cs.getEntityId();
            if (metricId == null) {
                throw new IllegalStateException("Bản thay đổi cập nhật thiếu mã định danh dữ liệu");
            }

            ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Không tìm thấy chỉ số cần cập nhật: " + metricId));

            // Apply field updates
            metric.setName(snapshot.getOrDefault("name", metric.getName()).toString());
            metric.setDisplayNameVi(snapshot.getOrDefault("displayNameVi", metric.getDisplayNameVi()).toString());
            metric.setUnit(snapshot.getOrDefault("unit", metric.getUnit()).toString());
            metric.setStatus("active");
            referenceMetricRepository.save(metric);
            replaceMetricAliases(metric, snapshot);

            // Replace ranges from snapshot
            Object rangesObj = snapshot.get("ranges");
            if (rangesObj instanceof List<?> rangesList) {
                List<ReferenceRange> existingRanges = referenceRangeRepository
                        .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId);

                Set<String> incomingKeys = new HashSet<>();

                for (Object rangeItem : rangesList) {
                    if (rangeItem instanceof Map<?, ?> rangeMap) {
                        String gender = rangeMap.get("gender") != null ? rangeMap.get("gender").toString() : null;
                        Integer minAge = toInteger(rangeMap.get("minAge"));
                        Integer maxAge = toInteger(rangeMap.get("maxAge"));
                        String key = rangeKey(gender, minAge, maxAge);
                        incomingKeys.add(key);

                        ReferenceRange match = existingRanges.stream()
                                .filter(r -> Objects.equals(r.getGender(), gender)
                                        && Objects.equals(r.getMinAge(), minAge)
                                        && Objects.equals(r.getMaxAge(), maxAge))
                                .findFirst()
                                .orElse(null);

                        if (match != null) {
                            match.setMinValue(toBigDecimal(rangeMap.get("minValue")));
                            match.setMaxValue(toBigDecimal(rangeMap.get("maxValue")));
                            match.setAttentionMin(toBigDecimal(rangeMap.get("attentionMin")));
                            match.setAttentionMax(toBigDecimal(rangeMap.get("attentionMax")));
                            match.setGender(gender);
                            match.setMinAge(minAge);
                            match.setMaxAge(maxAge);
                            match.setStatus("active");
                            referenceRangeRepository.save(match);
                        } else {
                            ReferenceRange newRange = new ReferenceRange();
                            newRange.setMetric(metric);
                            newRange.setMinValue(toBigDecimal(rangeMap.get("minValue")));
                            newRange.setMaxValue(toBigDecimal(rangeMap.get("maxValue")));
                            newRange.setAttentionMin(toBigDecimal(rangeMap.get("attentionMin")));
                            newRange.setAttentionMax(toBigDecimal(rangeMap.get("attentionMax")));
                            newRange.setGender(gender);
                            newRange.setMinAge(minAge);
                            newRange.setMaxAge(maxAge);
                            newRange.setStatus("active");
                            referenceRangeRepository.save(newRange);
                        }
                    }
                }

                for (ReferenceRange existing : existingRanges) {
                    String key = rangeKey(existing.getGender(), existing.getMinAge(), existing.getMaxAge());
                    if (!incomingKeys.contains(key)) {
                        referenceRangeRepository.delete(existing);
                    }
                }
            }
        } else if ("DEACTIVATE".equals(operation)) {
            UUID metricId = cs.getEntityId();
            if (metricId != null) {
                referenceMetricRepository.findById(metricId).ifPresent(metric -> {
                    metric.setStatus("deactivated");
                    referenceMetricRepository.save(metric);
                });
            }
        }
    }

    /** Creates a new active metric and ranges from an import CREATE change set (entityId was null at draft time). */
    private void createMetricFromSnapshot(ReferenceDataChangeSet cs, Map<String, Object> snapshot) {
        String name = snapshot.getOrDefault("name", "").toString().trim();
        String displayNameVi = snapshot.getOrDefault("displayNameVi", "").toString().trim();
        String unit = snapshot.getOrDefault("unit", "").toString().trim();

        referenceMetricRepository.findByNameIgnoreCase(name)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tên chỉ số đã tồn tại");
                });

        ReferenceMetric metric = new ReferenceMetric();
        metric.setName(name);
        metric.setDisplayNameVi(displayNameVi);
        metric.setUnit(unit);
        metric.setStatus("active");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);
        replaceMetricAliases(savedMetric, snapshot);

        Object rangesObj = snapshot.get("ranges");
        if (rangesObj instanceof List<?> rangesList) {
            for (Object rangeItem : rangesList) {
                if (rangeItem instanceof Map<?, ?> rangeMap) {
                    ReferenceRange newRange = new ReferenceRange();
                    newRange.setMetric(savedMetric);
                    newRange.setMinValue(toBigDecimal(rangeMap.get("minValue")));
                    newRange.setMaxValue(toBigDecimal(rangeMap.get("maxValue")));
                    newRange.setAttentionMin(toBigDecimal(rangeMap.get("attentionMin")));
                    newRange.setAttentionMax(toBigDecimal(rangeMap.get("attentionMax")));
                    newRange.setGender(rangeMap.get("gender") != null ? rangeMap.get("gender").toString() : null);
                    newRange.setMinAge(toInteger(rangeMap.get("minAge")));
                    newRange.setMaxAge(toInteger(rangeMap.get("maxAge")));
                    newRange.setStatus("active");
                    referenceRangeRepository.save(newRange);
                }
            }
        }

        cs.setEntityId(savedMetric.getId());
    }

    private void replaceMetricAliases(ReferenceMetric metric, Map<String, Object> snapshot) {
        if (!snapshot.containsKey("aliases")) {
            return;
        }
        List<ReferenceMetricAlias> existingAliases = referenceMetricAliasRepository.findAllByMetric_Id(metric.getId());
        if (!existingAliases.isEmpty()) {
            referenceMetricAliasRepository.deleteAll(existingAliases);
        }

        Object aliasesObj = snapshot.get("aliases");
        if (!(aliasesObj instanceof List<?> aliasList)) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Object aliasItem : aliasList) {
            if (!(aliasItem instanceof Map<?, ?> aliasMap)) {
                continue;
            }
            String alias = aliasMap.get("alias") != null ? aliasMap.get("alias").toString().trim() : "";
            String normalized = aliasMap.get("aliasNormalized") != null
                    ? aliasMap.get("aliasNormalized").toString().trim()
                    : normalizeMetricName(alias);
            if (alias.isBlank() || normalized.isBlank() || !seen.add(normalized)) {
                continue;
            }

            referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue(normalized)
                    .filter(existing -> existing.getMetric() != null && !metric.getId().equals(existing.getMetric().getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "Alias đã thuộc chỉ số khác: " + alias);
                    });

            ReferenceMetricAlias entity = new ReferenceMetricAlias();
            entity.setMetric(metric);
            entity.setAlias(alias);
            entity.setAliasNormalized(normalized);
            entity.setLocale(aliasMap.get("locale") != null ? aliasMap.get("locale").toString() : null);
            entity.setActive(!Boolean.FALSE.equals(aliasMap.get("active")));
            referenceMetricAliasRepository.save(entity);
        }
    }

    private void writeUnifiedChangeSetAudit(String action, UUID changeSetId, UUID actorId, ReferenceDataChangeSet cs) {
        if (cs.getEntityId() == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("changeSetId", changeSetId);
        payload.put("operation", cs.getOperation());
        payload.put("entityType", cs.getEntityType());
        payload.put("changesJson", cs.getChangesJson());
        if (cs.getRejectionReason() != null) {
            payload.put("rejectionReason", cs.getRejectionReason());
        }
        unifiedAuditLogWriter.record(
                actorId,
                action,
                AuditResourceTypes.REFERENCE_DATA,
                cs.getEntityId(),
                null,
                writeJson(payload)
        );
        writeApprovalAuditLog(action, changeSetId, actorId, cs.getEntityType(), cs.getEntityId());
    }

    private void writeApprovalAuditLog(String action, UUID changeSetId, UUID reviewerId,
                                        String entityType, UUID entityId) {
        if (entityId == null) {
            return;
        }

        UUID referenceRangeId = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(entityId)
                .stream()
                .findFirst()
                .map(ReferenceRange::getId)
                .orElse(null);

        ReferenceRangeAuditLog auditLog = new ReferenceRangeAuditLog();
        auditLog.setMetricId(entityId);
        auditLog.setReferenceRangeId(referenceRangeId);
        auditLog.setProfileId(null);
        referenceRangeAuditLogRepository.save(auditLog);
    }

    private AdminChangeSetDetailResponse toChangeSetDetailResponse(ReferenceDataChangeSet cs) {
        String currentSnapshotJson = null;
        if ("UPDATE".equals(cs.getOperation()) && cs.getEntityId() != null) {
            ReferenceMetric metric = referenceMetricRepository.findById(cs.getEntityId()).orElse(null);
            if (metric != null) {
                Map<String, Object> current = buildSnapshot(metric, null, metric.getStatus());
                currentSnapshotJson = writeJson(current);
            }
        }

        String adminEmail = null;
        String adminName = null;
        if (cs.getAdminId() != null) {
            User adminUser = userRepository.findById(cs.getAdminId()).orElse(null);
            if (adminUser != null) {
                adminEmail = adminUser.getEmail();
                adminName = adminUser.getFullName();
            }
        }

        String reviewerEmail = null;
        String reviewerName = null;
        if (cs.getReviewerId() != null) {
            User reviewerUser = userRepository.findById(cs.getReviewerId()).orElse(null);
            if (reviewerUser != null) {
                reviewerEmail = reviewerUser.getEmail();
                reviewerName = reviewerUser.getFullName();
            }
        }

        return new AdminChangeSetDetailResponse(
                cs.getId(),
                cs.getAdminId(),
                adminEmail,
                adminName,
                cs.getEntityType(),
                cs.getEntityId(),
                cs.getOperation(),
                cs.getChangesJson(),
                currentSnapshotJson,
                cs.getStatus(),
                cs.getCreatedAt(),
                cs.getApprovedAt(),
                cs.getReviewerId(),
                reviewerEmail,
                reviewerName,
                cs.getRejectionReason()
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null || value.toString().isBlank()) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private static String rangeKey(String gender, Integer minAge, Integer maxAge) {
        return (gender != null ? gender : "") + "|"
                + (minAge != null ? minAge : "") + "|"
                + (maxAge != null ? maxAge : "");
    }
}
