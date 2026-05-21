package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditOutcome;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogEntryDto;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.persistence.json.JsonPathExpressions;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final Sort EXPORT_CSV_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Sort TRACE_SORT = Sort.by(Sort.Direction.ASC, "createdAt")
            .and(Sort.by(Sort.Direction.ASC, "id"));

    private final AuditLogRepository auditLogRepository;
    private final ReferenceMetricRepository referenceMetricRepository;
    private final JsonPathExpressions jsonPathExpressions;
    private final ObjectMapper objectMapper;

    /** Self-proxy for {@link Propagation#REQUIRES_NEW} per export batch (avoids one long read-only transaction). */
    private AdminAuditLogService self;

    @Autowired
    @Lazy
    void setSelf(AdminAuditLogService self) {
        this.self = self;
    }

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
        return query(resourceType, resourceId, actorEmail, action, null, from, to, page, limit);
    }

    @Transactional(readOnly = true)
    public AuditLogPageDto query(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            String correlationId,
            Instant from,
            Instant to,
            int page,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int safePage = Math.max(page, 0);
        Specification<AuditLog> spec = buildSpec(resourceType, resourceId, actorEmail, action, correlationId, from, to);

        Page<AuditLog> result = auditLogRepository.findAll(
                withActorFetched(spec),
                PageRequest.of(safePage, safeLimit, sortForQuery(correlationId))
        );

        List<AuditLog> rows = result.getContent();
        Map<UUID, ReferenceMetric> metricsById = loadReferenceMetrics(rows);

        return new AuditLogPageDto(
                rows.stream().map(row -> toDto(row, metricsById)).toList(),
                result.getTotalElements(),
                safePage,
                safeLimit
        );
    }

    /** Export all rows matching filters (bounded by maxRows). No class-level transaction — each batch uses a short read-only TX. */
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
        writeCsv(resourceType, resourceId, actorEmail, action, null, from, to, writer, maxRows);
    }

    /** Export all rows matching filters (bounded by maxRows). No class-level transaction — each batch uses a short read-only TX. */
    public void writeCsv(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            String correlationId,
            Instant from,
            Instant to,
            Writer writer,
            int maxRows
    ) throws IOException {
        Specification<AuditLog> spec = buildSpec(resourceType, resourceId, actorEmail, action, correlationId, from, to);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "id",
                        "actorEmail",
                        "action",
                        "resourceType",
                        "resourceId",
                        "entityLabel",
                        "detailSummary",
                        "outcome",
                        "oldValueJson",
                        "newValueJson",
                        "metadataJson",
                        "correlationId",
                        "requestId",
                        "traceId",
                        "ipAddress",
                        "createdAt"
                )
                .get();

        int cap = Math.min(Math.max(maxRows, 1), 50_000);
        Map<UUID, ReferenceMetric> metricsById = loadAllReferenceMetricsById();
        // CSVPrinter.close() closes the delegate Writer; caller owns flush/close of `writer`.
        try (CSVPrinter printer = new CSVPrinter(new NonClosingWriter(writer), format)) {
            int written = 0;
            Instant cursorCreatedAt = null;
            UUID cursorId = null;
            while (written < cap) {
                int batchSize = Math.min(EXPORT_PAGE_SIZE, cap - written);
                Specification<AuditLog> exportSpec = spec;
                if (cursorCreatedAt != null && cursorId != null) {
                    exportSpec = exportSpec.and(seekBeforeCursor(cursorCreatedAt, cursorId));
                }
                List<AuditLog> rows = loadExportBatch(exportSpec, batchSize);
                if (rows.isEmpty()) {
                    break;
                }
                for (AuditLog row : rows) {
                    if (written >= cap) {
                        break;
                    }
                    AuditLogEntryDto dto = toDto(row, metricsById);
                    printer.printRecord(
                            dto.id(),
                            dto.actorEmail(),
                            dto.action(),
                            dto.resourceType(),
                            dto.resourceId() != null ? dto.resourceId().toString() : "",
                            dto.entityLabel(),
                            dto.detailSummary(),
                            dto.outcome(),
                            dto.oldValueJson() != null ? dto.oldValueJson() : "",
                            dto.newValueJson() != null ? dto.newValueJson() : "",
                            dto.metadataJson() != null ? dto.metadataJson() : "",
                            dto.correlationId() != null ? dto.correlationId() : "",
                            dto.requestId() != null ? dto.requestId() : "",
                            dto.traceId() != null ? dto.traceId() : "",
                            dto.ipAddress() != null ? dto.ipAddress() : "",
                            dto.createdAt().toString()
                    );
                    written++;
                }
                if (written >= cap || rows.size() < batchSize) {
                    break;
                }
                AuditLog lastRow = rows.get(rows.size() - 1);
                cursorCreatedAt = lastRow.getCreatedAt();
                cursorId = lastRow.getId();
            }
            printer.flush();
        }
    }

    /**
     * Loads one keyset page for CSV export in a dedicated read-only transaction so streaming export
     * does not hold a single connection for the full {@code maxRows} loop.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    List<AuditLog> fetchExportBatch(Specification<AuditLog> exportSpec, int batchSize) {
        return auditLogRepository.findAll(
                withActorFetched(exportSpec),
                PageRequest.of(0, batchSize, EXPORT_CSV_SORT)
        ).getContent();
    }

    private List<AuditLog> loadExportBatch(Specification<AuditLog> exportSpec, int batchSize) {
        if (self != null) {
            return self.fetchExportBatch(exportSpec, batchSize);
        }
        return auditLogRepository.findAll(
                withActorFetched(exportSpec),
                PageRequest.of(0, batchSize, EXPORT_CSV_SORT)
        ).getContent();
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

    private AuditLogEntryDto toDto(AuditLog log, Map<UUID, ReferenceMetric> metricsById) {
        String email = resolveActorEmail(log);
        String label = buildEntityLabel(log, metricsById);
        String detail = buildDetailSummary(log, label);
        return new AuditLogEntryDto(
                log.getId(),
                email,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                label,
                detail,
                StringUtils.hasText(log.getOutcome()) ? log.getOutcome() : AuditOutcome.fromAction(log.getAction()),
                log.getOldValueJson(),
                log.getNewValueJson(),
                log.getMetadataJson(),
                log.getCorrelationId(),
                log.getRequestId(),
                log.getTraceId(),
                log.getIpAddress(),
                log.getCreatedAt()
        );
    }

    private String resolveActorEmail(AuditLog log) {
        User actor = log.getActor();
        if (actor != null && actor.getEmail() != null && !actor.getEmail().isBlank()) {
            return actor.getEmail();
        }
        return extractJsonString(payloadJson(log), "email")
                .or(() -> extractJsonString(log.getOldValueJson(), "email"))
                .orElse("");
    }

    private Optional<String> extractJsonString(String json, String field) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode value = objectMapper.readTree(json).get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return Optional.of(value.asText());
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Optional.empty();
    }

    private static String payloadJson(AuditLog log) {
        return StringUtils.hasText(log.getNewValueJson()) ? log.getNewValueJson() : log.getMetadataJson();
    }

    private Optional<String> extractJsonField(String json, String field) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode value = objectMapper.readTree(json).get(field);
            if (value != null && !value.isNull() && !value.isMissingNode()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return Optional.of(text);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Optional.empty();
    }

    private Map<UUID, ReferenceMetric> loadReferenceMetrics(List<AuditLog> rows) {
        Set<UUID> metricIds = rows.stream()
                .filter(log -> AuditResourceTypes.REFERENCE_DATA.equals(log.getResourceType()))
                .filter(log -> referenceMetricLabelFromJson(log).isEmpty())
                .map(AuditLog::getResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (metricIds.isEmpty()) {
            return Map.of();
        }
        return referenceMetricRepository.findAllById(metricIds).stream()
                .collect(Collectors.toMap(ReferenceMetric::getId, Function.identity()));
    }

    /** Single query for CSV export; reference metric catalog is small and bounded. */
    private Map<UUID, ReferenceMetric> loadAllReferenceMetricsById() {
        return referenceMetricRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.toMap(ReferenceMetric::getId, Function.identity(), (left, right) -> left));
    }

    private String buildEntityLabel(AuditLog log, Map<UUID, ReferenceMetric> metricsById) {
        if (AuditResourceTypes.AUTH.equals(log.getResourceType())) {
            return buildAuthEntityLabel(log);
        }
        if (AuditResourceTypes.USER.equals(log.getResourceType())) {
            return buildUserEntityLabel(log);
        }
        if (AuditResourceTypes.CONSENT.equals(log.getResourceType())) {
            return extractJsonString(payloadJson(log), "version")
                    .map(version -> "Đồng ý điều khoản v" + version)
                    .orElse("Đồng ý điều khoản");
        }
        if (AuditResourceTypes.PROFILE.equals(log.getResourceType()) && log.getResourceId() != null) {
            return profileLabelFromJson(log).orElse("Hồ sơ gia đình");
        }
        if (AuditResourceTypes.REFERENCE_DATA.equals(log.getResourceType()) && log.getResourceId() != null) {
            return referenceMetricLabelFromJson(log)
                    .orElseGet(() -> {
                        ReferenceMetric metric = metricsById.get(log.getResourceId());
                        if (metric != null) {
                            return formatReferenceMetricLabel(metric);
                        }
                        return "reference_metric:" + log.getResourceId();
                    });
        }
        if (AuditResourceTypes.HEALTH_RECORD.equals(log.getResourceType()) && log.getResourceId() != null) {
            return healthRecordLabelFromJson(log)
                    .orElse("Hồ sơ sức khỏe");
        }
        return log.getResourceType() + (log.getResourceId() != null ? "/" + log.getResourceId() : "");
    }

    /**
     * Context for AUTH rows — avoids repeating the actor email already shown in the list.
     */
    private String buildAuthEntityLabel(AuditLog log) {
        String action = log.getAction();
        if (AuditActions.LOGIN_FAILED.equals(action)) {
            return "Phiên đăng nhập thất bại";
        }
        if (AuditActions.ADMIN_LOGIN.equals(action)
                && extractJsonString(payloadJson(log), "via").filter("totp_setup"::equals).isPresent()) {
            return "Đăng nhập admin (sau thiết lập TOTP)";
        }

        String contextual = authContextualLabel(action);
        if (contextual != null) {
            Optional<String> subjectEmail = subjectEmailFromJson(log);
            String actorEmail = resolveActorEmail(log);
            if (subjectEmail.isPresent() && !emailsEqual(subjectEmail.get(), actorEmail)) {
                return contextual + " · " + subjectEmail.get();
            }
            return contextual;
        }

        return subjectEmailFromJson(log)
                .map(email -> "Xác thực · " + email)
                .orElse("Xác thực");
    }

    private String buildUserEntityLabel(AuditLog log) {
        String action = log.getAction();
        if (AuditActions.REQUEST_ACCOUNT_DELETION.equals(action)) {
            return extractJsonField(payloadJson(log), "scheduledDeletionAt")
                    .map(at -> "Yêu cầu xóa tài khoản · lịch " + at)
                    .orElse("Yêu cầu xóa tài khoản");
        }
        if (AuditActions.CANCEL_ACCOUNT_DELETION.equals(action)) {
            return "Hủy yêu cầu xóa tài khoản";
        }
        if (AuditActions.UPDATE_USER.equals(action)) {
            return extractJsonString(payloadJson(log), "fullName")
                    .filter(name -> !name.isBlank())
                    .map(name -> "Cập nhật hồ sơ · " + name)
                    .orElse("Cập nhật thông tin tài khoản");
        }
        return log.getResourceId() != null ? "Tài khoản" : "Tài khoản";
    }

    private Optional<String> authLoginFailedReasonLabel(AuditLog log) {
        return extractJsonString(payloadJson(log), "reason").map(this::mapAuthFailureReason);
    }

    private String mapAuthFailureReason(String reason) {
        return switch (reason) {
            case "bad_credentials" -> "Sai email hoặc mật khẩu";
            case "email_not_verified" -> "Email chưa được xác thực";
            default -> "Đăng nhập thất bại";
        };
    }

    private static String authContextualLabel(String action) {
        if (action == null) {
            return null;
        }
        return switch (action) {
            case AuditActions.LOGIN -> "Phiên đăng nhập";
            case AuditActions.LOGOUT -> "Phiên đăng xuất";
            case AuditActions.REGISTER -> "Đăng ký tài khoản mới";
            case AuditActions.VERIFY_EMAIL -> "Xác thực email";
            case AuditActions.REFRESH_TOKEN -> "Làm mới phiên";
            case AuditActions.REFRESH_TOKEN_REUSE_FAILED -> "Phát hiện dùng lại refresh token";
            case AuditActions.FORGOT_PASSWORD -> "Yêu cầu đặt lại mật khẩu";
            case AuditActions.RESET_PASSWORD -> "Đặt lại mật khẩu";
            case AuditActions.ADMIN_LOGIN -> "Phiên đăng nhập admin";
            case AuditActions.ADMIN_TOTP_SETUP -> "Thiết lập TOTP admin";
            case AuditActions.ADMIN_TOTP_VERIFY -> "Xác minh TOTP admin";
            default -> null;
        };
    }

    private Optional<String> subjectEmailFromJson(AuditLog log) {
        return extractJsonString(payloadJson(log), "email")
                .or(() -> extractJsonString(log.getOldValueJson(), "email"));
    }

    private static boolean emailsEqual(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    /** Narrative for the Chi tiết column — complements the short action category badge. */
    private String buildDetailSummary(AuditLog log, String entityLabel) {
        String resourceType = log.getResourceType();
        if (AuditResourceTypes.AUTH.equals(resourceType)) {
            return buildAuthDetailSummary(log);
        }
        if (AuditResourceTypes.USER.equals(resourceType)) {
            return buildUserDetailSummary(log, entityLabel);
        }
        if (AuditResourceTypes.CONSENT.equals(resourceType)) {
            return appendIpSuffix("Ghi nhận " + nullToDefault(entityLabel, "đồng ý điều khoản"), log.getIpAddress());
        }
        if (AuditResourceTypes.PROFILE.equals(resourceType)) {
            return buildProfileDetailSummary(log, entityLabel);
        }
        if (AuditResourceTypes.REFERENCE_DATA.equals(resourceType)) {
            return buildReferenceDataDetailSummary(log, entityLabel);
        }
        if (AuditResourceTypes.HEALTH_RECORD.equals(resourceType)) {
            return buildHealthRecordDetailSummary(log, entityLabel);
        }
        return nullToDefault(entityLabel, resourceType);
    }

    private String buildAuthDetailSummary(AuditLog log) {
        String action = log.getAction();
        String browser = describeUserAgent(log.getUserAgent());
        String ip = log.getIpAddress();

        if (AuditActions.LOGIN_FAILED.equals(action)) {
            String reason = authLoginFailedReasonLabel(log).orElse("không xác định");
            return appendIpSuffix("Thất bại xác thực: " + reason, ip);
        }
        if (AuditActions.LOGIN.equals(action) || AuditActions.ADMIN_LOGIN.equals(action)) {
            if (AuditActions.ADMIN_LOGIN.equals(action)
                    && extractJsonString(payloadJson(log), "via").filter("totp_setup"::equals).isPresent()) {
                return appendIpSuffix("Đăng nhập admin sau khi hoàn tất thiết lập TOTP qua " + browser, ip);
            }
            String scope = AuditActions.ADMIN_LOGIN.equals(action) ? "cổng quản trị" : "hệ thống";
            return appendIpSuffix("Đăng nhập " + scope + " qua " + browser, ip);
        }
        if (AuditActions.LOGOUT.equals(action)) {
            return appendIpSuffix("Đăng xuất và thu hồi phiên làm việc", ip);
        }
        if (AuditActions.REGISTER.equals(action)) {
            return appendIpSuffix("Đăng ký tài khoản mới qua " + browser, ip);
        }
        if (AuditActions.VERIFY_EMAIL.equals(action)) {
            return appendIpSuffix("Xác thực địa chỉ email tài khoản", ip);
        }
        if (AuditActions.REFRESH_TOKEN.equals(action)) {
            return appendIpSuffix("Làm mới phiên truy cập (refresh token)", ip);
        }
        if (AuditActions.REFRESH_TOKEN_REUSE_FAILED.equals(action)) {
            return appendIpSuffix("Phát hiện dùng lại refresh token đã xoay và thu hồi session family", ip);
        }
        if (AuditActions.FORGOT_PASSWORD.equals(action)) {
            return appendIpSuffix("Gửi yêu cầu đặt lại mật khẩu qua email", ip);
        }
        if (AuditActions.RESET_PASSWORD.equals(action)) {
            return appendIpSuffix("Đặt lại mật khẩu và thu hồi phiên cũ", ip);
        }
        if (AuditActions.ADMIN_TOTP_SETUP.equals(action)) {
            return appendIpSuffix("Khởi tạo thiết lập xác thực hai lớp (TOTP) cho admin", ip);
        }
        if (AuditActions.ADMIN_TOTP_VERIFY.equals(action)) {
            return appendIpSuffix("Xác minh mã TOTP và kích hoạt MFA admin", ip);
        }
        return appendIpSuffix(nullToDefault(authContextualLabel(action), "Sự kiện xác thực"), ip);
    }

    private String buildUserDetailSummary(AuditLog log, String entityLabel) {
        String action = log.getAction();
        if (AuditActions.REQUEST_ACCOUNT_DELETION.equals(action)) {
            return extractJsonField(payloadJson(log), "scheduledDeletionAt")
                    .map(at -> "Yêu cầu xóa tài khoản, lịch thực hiện " + at)
                    .orElse("Yêu cầu xóa tài khoản theo quy trình ND13");
        }
        if (AuditActions.CANCEL_ACCOUNT_DELETION.equals(action)) {
            return "Hủy yêu cầu xóa tài khoản trong thời gian ân hạn";
        }
        if (AuditActions.UPDATE_USER.equals(action)) {
            return extractJsonString(payloadJson(log), "fullName")
                    .filter(name -> !name.isBlank())
                    .map(name -> "Cập nhật thông tin tài khoản: " + name)
                    .orElse("Cập nhật thông tin hồ sơ người dùng");
        }
        return nullToDefault(entityLabel, "Thao tác tài khoản");
    }

    private String buildProfileDetailSummary(AuditLog log, String entityLabel) {
        String action = log.getAction();
        String subject = nullToDefault(entityLabel, "hồ sơ gia đình");
        if (AuditActions.INVITE_PROFILE_SHARE.equals(action)) {
            return "Gửi lời mời chia sẻ — " + subject;
        }
        if (AuditActions.ACCEPT_PROFILE_INVITATION.equals(action)) {
            return "Chấp nhận lời mời chia sẻ — " + subject;
        }
        if (AuditActions.REJECT_PROFILE_INVITATION.equals(action)) {
            return "Từ chối lời mời chia sẻ — " + subject;
        }
        if (AuditActions.REVOKE_PROFILE_SHARE.equals(action)) {
            return "Thu hồi quyền chia sẻ — " + subject;
        }
        if (AuditActions.CANCEL_PROFILE_INVITATION.equals(action)) {
            return "Hủy lời mời chia sẻ đang chờ — " + subject;
        }
        if (AuditActions.RESEND_PROFILE_INVITATION.equals(action)) {
            return "Gửi lại lời mời chia sẻ — " + subject;
        }
        if (AuditActions.PROFILE_SHARE_ACCESS_DENIED_FAILED.equals(action)) {
            return extractJsonString(payloadJson(log), "reason")
                    .map(reason -> "Truy cập chia sẻ bị từ chối — " + reason + " — " + subject)
                    .orElse("Truy cập chia sẻ bị từ chối — " + subject);
        }
        if (AuditActions.CREATE_PROFILE.equals(action)) {
            return "Tạo hồ sơ gia đình mới — " + subject;
        }
        if (AuditActions.UPDATE_PROFILE.equals(action)) {
            return "Cập nhật hồ sơ gia đình — " + subject;
        }
        return subject;
    }

    private String buildReferenceDataDetailSummary(AuditLog log, String entityLabel) {
        String subject = nullToDefault(entityLabel, "chỉ số tham chiếu");
        String action = log.getAction();

        if (AuditActions.UPDATE_REFERENCE_METRIC_DISPLAY.equals(action)) {
            Optional<String> oldName = extractJsonString(log.getOldValueJson(), "displayNameVi");
            Optional<String> newName = extractJsonString(log.getNewValueJson(), "displayNameVi");
            if (oldName.isPresent() && newName.isPresent() && !oldName.get().equals(newName.get())) {
                return "Chỉ số " + subject + " — đổi tên hiển thị: «" + oldName.get() + "» → «" + newName.get() + "»";
            }
            return "Chỉ số " + subject + " — cập nhật tên hiển thị";
        }
        if (AuditActions.CREATE_REFERENCE_METRIC.equals(action)) {
            return "Tạo chỉ số tham chiếu mới: " + subject;
        }
        if (AuditActions.UPDATE_REFERENCE_METRIC.equals(action)) {
            return "Cập nhật ngưỡng và thuộc tính chỉ số: " + subject;
        }
        if (AuditActions.DEACTIVATE_REFERENCE_METRIC.equals(action)) {
            return "Ngưng áp dụng chỉ số: " + subject;
        }
        if (AuditActions.REACTIVATE_REFERENCE_METRIC.equals(action)) {
            return "Kích hoạt lại chỉ số: " + subject;
        }
        if (AuditActions.SUBMIT_REFERENCE_CHANGE_SET.equals(action)) {
            return "Gửi bộ thay đổi chờ phê duyệt — " + subject;
        }
        if (AuditActions.APPROVE_CHANGE_SET.equals(action)) {
            return "Phê duyệt bộ thay đổi — " + subject;
        }
        if (AuditActions.REJECT_CHANGE_SET.equals(action)) {
            return "Từ chối bộ thay đổi — " + subject;
        }
        if (AuditActions.PUBLISH_CHANGE_SET.equals(action)) {
            return "Kích hoạt bộ thay đổi đã duyệt — " + subject;
        }
        if (AuditActions.CONFIRM_REFERENCE_IMPORT.equals(action)) {
            return "Xác nhận import dữ liệu tham chiếu — " + subject;
        }
        return "Dữ liệu tham chiếu — " + subject;
    }

    private String buildHealthRecordDetailSummary(AuditLog log, String entityLabel) {
        String recordRef = log.getResourceId() != null
                ? "#" + shortResourceId(log.getResourceId())
                : "";
        String subject = nullToDefault(entityLabel, "hồ sơ sức khỏe");
        String action = log.getAction();

        if (AuditActions.DELETE_HEALTH_RECORD.equals(action)) {
            String ref = recordRef.isEmpty() ? "" : " " + recordRef;
            return "Xóa hồ sơ sức khỏe" + ref + " — " + subject;
        }
        if (AuditActions.CREATE_HEALTH_RECORD.equals(action)) {
            return "Tạo hồ sơ sức khỏe mới " + recordRef;
        }
        if (AuditActions.CONFIRM_HEALTH_RECORD.equals(action)) {
            return "Xác nhận kết quả khám " + recordRef;
        }
        if (AuditActions.UPDATE_HEALTH_RECORD_METRICS.equals(action)) {
            return "Cập nhật chỉ số trong hồ sơ " + recordRef + " — " + subject;
        }
        if (AuditActions.DOWNLOAD_HEALTH_RECORD_PDF.equals(action)) {
            return appendIpSuffix("Tải xuống PDF hồ sơ " + recordRef + " — " + subject, log.getIpAddress());
        }
        if (AuditActions.INVITE_HEALTH_RECORD_SHARE.equals(action)) {
            return "Mời chia sẻ hồ sơ " + recordRef + " — " + subject;
        }
        if (AuditActions.ACCEPT_HEALTH_RECORD_SHARE.equals(action)) {
            return "Chấp nhận chia sẻ hồ sơ " + recordRef;
        }
        if (AuditActions.REVOKE_HEALTH_RECORD_SHARE.equals(action)) {
            return "Thu hồi chia sẻ hồ sơ " + recordRef;
        }
        return "Hồ sơ sức khỏe " + recordRef + " — " + subject;
    }

    private static String describeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "trình duyệt không xác định";
        }
        if (userAgent.contains("Edg/")) {
            return "Microsoft Edge";
        }
        if (userAgent.contains("Chrome/")) {
            return "Google Chrome";
        }
        if (userAgent.contains("Firefox/")) {
            return "Mozilla Firefox";
        }
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            return "Safari";
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return "Opera";
        }
        return "trình duyệt khác";
    }

    private static String shortResourceId(UUID id) {
        return id.toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String appendIpSuffix(String text, String ip) {
        if (ip == null || ip.isBlank()) {
            return text;
        }
        return text + " (IP " + ip.trim() + ")";
    }

    private static String nullToDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private Optional<String> profileLabelFromJson(AuditLog log) {
        Optional<String> displayName = extractJsonString(payloadJson(log), "displayName");
        Optional<String> inviteeMasked = extractJsonString(payloadJson(log), "inviteeEmailMasked");
        Optional<String> inviteeEmail = inviteeMasked.or(() -> extractJsonString(payloadJson(log), "inviteeEmail"));
        if (displayName.isPresent() && inviteeEmail.isPresent()) {
            return Optional.of("Hồ sơ · " + displayName.get() + " · mời " + inviteeEmail.get());
        }
        if (displayName.isPresent()) {
            return Optional.of("Hồ sơ · " + displayName.get());
        }
        if (inviteeEmail.isPresent()) {
            return Optional.of("Hồ sơ · mời " + inviteeEmail.get());
        }
        return Optional.empty();
    }

    private Optional<String> healthRecordLabelFromJson(AuditLog log) {
        Optional<String> invitee = extractJsonString(payloadJson(log), "inviteeEmail");
        if (invitee.isPresent()) {
            return Optional.of("Hồ sơ sức khỏe · mời " + invitee.get());
        }
        Optional<String> metricCount = extractJsonField(payloadJson(log), "metricCount");
        if (metricCount.isPresent()) {
            return Optional.of("Hồ sơ sức khỏe · " + metricCount.get() + " chỉ số");
        }
        Optional<String> shareScope = extractJsonString(payloadJson(log), "shareScope");
        if (shareScope.isPresent()) {
            return Optional.of("Hồ sơ sức khỏe · tải PDF (" + shareScope.get() + ")");
        }
        return Optional.empty();
    }

    private Optional<String> referenceMetricLabelFromJson(AuditLog log) {
        return referenceMetricLabelFromJson(payloadJson(log))
                .or(() -> referenceMetricLabelFromJson(log.getOldValueJson()));
    }

    private Optional<String> referenceMetricLabelFromJson(String json) {
        Optional<String> displayName = extractJsonString(json, "displayNameVi");
        Optional<String> name = extractJsonString(json, "name");
        if (displayName.isPresent() && name.isPresent()) {
            return Optional.of(formatReferenceMetricLabel(displayName.get(), name.get()));
        }
        return Optional.empty();
    }

    private static String formatReferenceMetricLabel(ReferenceMetric metric) {
        return formatReferenceMetricLabel(metric.getDisplayNameVi(), metric.getName());
    }

    private static String formatReferenceMetricLabel(String displayNameVi, String name) {
        return displayNameVi + " (" + name + ")";
    }

    private Specification<AuditLog> buildSpec(
            String resourceType,
            UUID resourceId,
            String actorEmail,
            String action,
            String correlationId,
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
        if (StringUtils.hasText(correlationId)) {
            parts.add((root, q, cb) -> cb.equal(root.get("correlationId"), correlationId.trim()));
        }
        if (from != null) {
            parts.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            parts.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (StringUtils.hasText(actorEmail)) {
            String normalized = actorEmail.trim().toLowerCase();
            parts.add((root, q, cb) -> matchesActorEmail(root, cb, normalized));
        }

        return parts.stream().reduce(Specification::and).orElse((root, q, cb) -> cb.conjunction());
    }

    private static Sort sortForQuery(String correlationId) {
        return StringUtils.hasText(correlationId) ? TRACE_SORT : Sort.by(Sort.Direction.DESC, "createdAt");
    }

    /**
     * Match actor email on the linked user and on JSON payloads (anonymous / pre-auth events).
     */
    private Predicate matchesActorEmail(Root<AuditLog> root, CriteriaBuilder cb, String normalizedEmail) {
        Predicate actorMatch = cb.equal(
                cb.lower(root.join("actor", JoinType.LEFT).get("email")),
                normalizedEmail
        );
        Predicate newJsonMatch = cb.equal(
                cb.lower(jsonPathExpressions.extractPathText(root, cb, "newValueJson", "email")),
                normalizedEmail
        );
        Predicate oldJsonMatch = cb.equal(
                cb.lower(jsonPathExpressions.extractPathText(root, cb, "oldValueJson", "email")),
                normalizedEmail
        );
        Predicate metadataJsonMatch = cb.equal(
                cb.lower(jsonPathExpressions.extractPathText(root, cb, "metadataJson", "email")),
                normalizedEmail
        );
        return cb.or(actorMatch, newJsonMatch, oldJsonMatch, metadataJsonMatch);
    }

    /**
     * Keyset predicate for {@link #EXPORT_CSV_SORT}: rows strictly older than the last exported row.
     * Avoids offset drift when new audit rows are inserted during export.
     */
    private static Specification<AuditLog> seekBeforeCursor(Instant createdAt, UUID id) {
        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get("createdAt"), createdAt),
                cb.and(
                        cb.equal(root.get("createdAt"), createdAt),
                        cb.lessThan(root.get("id"), id)
                )
        );
    }

    /** Eager-load actor in the same query to avoid N+1 when mapping actorEmail. */
    private static Specification<AuditLog> withActorFetched(Specification<AuditLog> spec) {
        return Specification.where(fetchActor()).and(spec);
    }

    private static Specification<AuditLog> fetchActor() {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("actor", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }

    /**
     * Prevents {@link CSVPrinter#close()} from closing a caller-owned {@link Writer}.
     * On close, only flushes buffered content to the delegate.
     */
    private static final class NonClosingWriter extends FilterWriter {

        NonClosingWriter(Writer delegate) {
            super(delegate);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
