package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.UpdateMetricsRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.DownloadHealthRecordPdfResponse;
import com.healthlens.api.dto.response.HealthRecordDetailResponse;
import com.healthlens.api.dto.response.HealthRecordHistoryItemResponse;
import com.healthlens.api.dto.response.HealthRecordHistoryPageResponse;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.dto.response.RecommendationsResponse;
import com.healthlens.api.dto.response.SharedHealthRecordResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.PaginationResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.entity.HealthRecordShare;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileAccessRevokedException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import com.healthlens.api.repository.HealthRecordShareRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HealthRecordService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration UPLOAD_RESERVATION_TTL = Duration.ofMinutes(20);
    private static final String STATUS_PROCESSING = "processing";
    private static final int HISTORY_PAGE_SIZE = 20;
    private static final String RECOMMENDATIONS_DISCLAIMER =
            "Thông tin này chỉ mang tính tham khảo và không thay thế tư vấn của bác sĩ chuyên khoa.";

    private final StorageService storageService;
    private final ProfileRepository profileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordShareRepository healthRecordShareRepository;
    private final ProfileShareRepository profileShareRepository;
    private final ReferenceDataService referenceDataService;
    private final MetricExplanationRetrievalService metricExplanationRetrievalService;
    private final LlmService llmService;
    private final HealthRecordPdfService healthRecordPdfService;
    private final HealthRecordAuditLogRepository healthRecordAuditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String ocrStreamName;

    public HealthRecordService(
            StorageService storageService,
            ProfileRepository profileRepository,
            HealthRecordRepository healthRecordRepository,
            HealthRecordShareRepository healthRecordShareRepository,
            ProfileShareRepository profileShareRepository,
            ReferenceDataService referenceDataService,
            MetricExplanationRetrievalService metricExplanationRetrievalService,
            LlmService llmService,
            HealthRecordPdfService healthRecordPdfService,
            HealthRecordAuditLogRepository healthRecordAuditLogRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.stream.ocr-events:ocr.events}") String ocrStreamName
    ) {
        this.storageService = storageService;
        this.profileRepository = profileRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.healthRecordShareRepository = healthRecordShareRepository;
        this.profileShareRepository = profileShareRepository;
        this.referenceDataService = referenceDataService;
        this.metricExplanationRetrievalService = metricExplanationRetrievalService;
        this.llmService = llmService;
        this.healthRecordPdfService = healthRecordPdfService;
        this.healthRecordAuditLogRepository = healthRecordAuditLogRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ocrStreamName = ocrStreamName;
    }

    @Transactional(readOnly = true)
    public UploadUrlResponse createUploadUrl(UUID userId, CreateUploadUrlRequest request) {
        UUID targetProfileId = request.profileId();
        UUID recordId = UUID.randomUUID();

        if (request.retryRecordId() != null) {
            HealthRecord existingRecord = healthRecordRepository.findByIdAndUserId(request.retryRecordId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
            if (!"ocr_failed".equals(existingRecord.getStatus())) {
                throw new IllegalStateException("Chi duoc retry upload khi OCR that bai");
            }
            targetProfileId = existingRecord.getProfileId();
            recordId = existingRecord.getId();
        }

        Profile profile = profileRepository.findById(targetProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile khong ton tai"));
        UUID profileOwnerId = profile.getUser().getId();
        boolean canUploadAsOwner = profileOwnerId.equals(userId);
        boolean canUploadAsSharedEditor = hasEditAccess(targetProfileId, userId);
        if (!canUploadAsOwner && !canUploadAsSharedEditor) {
            throw new ProfileAccessRevokedException("Ban khong co quyen tai len cho ho so nay");
        }

        UploadFormat uploadFormat = resolveUploadFormat(request.fileType());

        String fileKey = "health-records/%s/%s/%s/original.%s"
                .formatted(userId, targetProfileId, recordId, uploadFormat.extension());
        String uploadUrl = storageService.generateUploadUrl(fileKey, UPLOAD_URL_TTL, uploadFormat.contentType());

        persistUploadReservation(recordId, userId, targetProfileId, fileKey, uploadFormat.contentType());

        return new UploadUrlResponse(uploadUrl, recordId, fileKey);
    }

    @Transactional
    public ConfirmUploadResponse confirmUpload(UUID userId, UUID recordId) {
        UploadReservation reservation = loadUploadReservation(recordId);
        if (!reservation.userId().equals(userId)) {
            throw new IllegalArgumentException("Record khong thuoc ve nguoi dung hien tai");
        }

        HealthRecord record = healthRecordRepository.findById(recordId).orElseGet(HealthRecord::new);
        boolean isNewRecord = record.getId() == null;
        if (isNewRecord) {
            Optional<Profile> profileOptional = profileRepository.findById(reservation.profileId());
            record.setId(recordId);
            // Prefer owner id for shared-editor uploads; fallback for legacy/unit-test paths.
            UUID recordOwnerId = profileOptional
                    .map(Profile::getUser)
                    .map(User::getId)
                    .orElse(userId);
            record.setUserId(recordOwnerId);
            record.setProfileId(reservation.profileId());
        } else if (!reservation.profileId().equals(record.getProfileId())) {
            throw new IllegalStateException("Upload reservation khong khop voi health record da ton tai");
        } else if (!"ocr_failed".equals(record.getStatus()) && !"processing".equals(record.getStatus())) {
            throw new IllegalStateException("Chi duoc xac nhan upload cho record moi hoac retry OCR that bai");
        }
        record.setFileKey(reservation.fileKey());
        record.setStatus(STATUS_PROCESSING);
        record.setSourceType("ocr");
        record.setRawOcrResult(null);
        record.setMetrics("[]");
        record.setExamDate(null);
        record.setRecordType(null);
        record.setHospitalName(null);
        record.setDiagnosis(null);
        record.setAnalyzerModel(null);
        record.setTestMethod(null);
        record.setLabSite(null);
        healthRecordRepository.save(record);
        updateProfileLastRecordAt(reservation.profileId());

        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        String jobId = UUID.randomUUID().toString();
        String mimeType = reservation.mimeType() == null || reservation.mimeType().isBlank()
                ? deriveMimeTypeFromFileKey(reservation.fileKey())
                : reservation.mimeType();
        streamOps.add(ocrStreamName, Map.of(
                "jobId", jobId,
                "correlationId", jobId,
                "recordId", recordId.toString(),
                "fileKey", reservation.fileKey(),
                "mimeType", mimeType,
                "profileId", reservation.profileId().toString()
        ));
        redisTemplate.delete(uploadReservationKey(recordId));

        return new ConfirmUploadResponse(recordId, STATUS_PROCESSING);
    }

    @Transactional(readOnly = true)
    public HealthRecordStatusResponse getStatus(UUID userId, UUID recordId) {
        String cacheKey = "health-record-status:" + userId + ":" + recordId;
        String cachedStatus = redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedStatus != null) {
            try {
                return objectMapper.readValue(cachedStatus, HealthRecordStatusResponse.class);
            } catch (Exception e) {
                log.warn("Failed to parse cached status for {}", recordId);
            }
        }

        AccessibleRecord accessibleRecord = loadAccessibleRecord(userId, recordId);
        HealthRecord record = accessibleRecord.record();
        
        java.util.List<MetricDto> metricsList = null;
        boolean hasLowConfidenceMetrics = false;
        if ("review_required".equals(record.getStatus()) || "done".equals(record.getStatus()) || "ocr_failed".equals(record.getStatus())) {
            String metrics = record.getMetrics();
            if (metrics != null && !metrics.isBlank()) {
                try {
                    metricsList = objectMapper.readValue(metrics, new TypeReference<java.util.List<MetricDto>>() {});
                    hasLowConfidenceMetrics = containsLowConfidenceMetrics(metricsList);
                } catch (Exception e) {
                    log.warn("Failed to parse metrics for {}", recordId);
                }
            }
        }
        String ocrFailureReason = resolveOcrFailureReason(record);
        
        HealthRecordStatusResponse response = new HealthRecordStatusResponse(
            record.getId(), 
            accessibleRecord.isOwner(),
            accessibleRecord.canEdit(),
            record.getStatus(), 
            metricsList,
            hasLowConfidenceMetrics,
            ocrFailureReason,
            record.getExamDate() != null ? record.getExamDate().toString() : null,
            record.getRecordType(),
            record.getHospitalName(),
            record.getDiagnosis(),
            record.getAnalyzerModel(),
            record.getTestMethod(),
            record.getLabSite(),
            storageService.generateDownloadUrl(record.getFileKey(), Duration.ofHours(1))
        );
        
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Failed to cache status for {}", recordId);
        }
        
        return response;
    }

    @Transactional(readOnly = true)
    public HealthRecordDetailResponse getDetail(UUID userId, UUID recordId, UUID profileId) {
        AccessibleRecord accessibleRecord = loadAccessibleRecord(userId, recordId);
        HealthRecord record = accessibleRecord.record();

        UUID recordProfileId = record.getProfileId();
        if (profileId != null && !profileId.equals(recordProfileId)) {
            throw new AccessDeniedException("Profile khong khop voi health record");
        }
        Profile profile = profileRepository.findById(recordProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));
        List<MetricDto> metricsList = parseMetrics(record.getMetrics()).stream()
                .map(metric -> enrichMetric(metric, profile, record.getExamDate()))
                .collect(Collectors.toList());

        return new HealthRecordDetailResponse(
                record.getId(),
                record.getProfileId(),
                profile.getDisplayName(),
                accessibleRecord.isOwner(),
                accessibleRecord.canEdit(),
                accessibleRecord.shareScope(),
                record.getStatus(),
                metricsList,
                record.getExamDate() != null ? record.getExamDate().toString() : null,
                record.getRecordType(),
                record.getHospitalName(),
                record.getDiagnosis(),
                record.getAnalyzerModel(),
                record.getTestMethod(),
                record.getLabSite(),
                storageService.generateDownloadUrl(record.getFileKey(), Duration.ofHours(1))
        );
    }

    @Transactional
    public DownloadHealthRecordPdfResponse downloadHealthRecordPdf(UUID userId, UUID recordId) {
        AccessibleRecord accessibleRecord = loadAccessibleRecord(userId, recordId);
        HealthRecord record = accessibleRecord.record();
        Profile profile = profileRepository.findById(record.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));
        List<MetricDto> metrics = parseMetrics(record.getMetrics()).stream()
                .map(metric -> enrichMetric(metric, profile, record.getExamDate(), false))
                .collect(Collectors.toList());
        Map<String, String> explanations = safeMetricExplanations(metrics);
        RecommendationsResponse recommendations = safeRecommendations(userId, recordId);

        byte[] bytes = healthRecordPdfService.generatePdf(new HealthRecordPdfContext(
                record,
                profile,
                metrics,
                explanations,
                recommendations.recommendations(),
                recommendations.disclaimer(),
                Instant.now()
        ));
        writePdfDownloadAudit(userId, record, accessibleRecord.shareScope());
        return new DownloadHealthRecordPdfResponse(bytes, buildPdfFilename(profile, record));
    }

    @Transactional(readOnly = true)
    public List<SharedHealthRecordResponse> getSharedHealthRecords(UUID userId) {
        List<com.healthlens.api.entity.HealthRecordShare> shares =
                healthRecordShareRepository.findAllByViewerIdAndRevokedAtIsNull(userId);
        if (shares.isEmpty()) {
            return List.of();
        }

        List<UUID> profileIds = shares.stream()
                .map(com.healthlens.api.entity.HealthRecordShare::getProfileId)
                .distinct()
                .toList();
        List<UUID> recordIds = shares.stream()
                .map(com.healthlens.api.entity.HealthRecordShare::getHealthRecordId)
                .distinct()
                .toList();

        Map<UUID, Profile> profilesById = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getId, profile -> profile));
        Map<UUID, HealthRecord> recordsById = healthRecordRepository.findAllById(recordIds).stream()
                .filter(record -> record.getDeletedAt() == null)
                .collect(Collectors.toMap(HealthRecord::getId, record -> record));

        List<SharedHealthRecordResponse> responses = new ArrayList<>();
        for (com.healthlens.api.entity.HealthRecordShare share : shares) {
            if (profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(share.getProfileId(), userId)) {
                continue;
            }

            Profile profile = profilesById.get(share.getProfileId());
            HealthRecord record = recordsById.get(share.getHealthRecordId());
            if (profile == null || record == null) {
                continue;
            }

            String overallStatus = parseMetrics(record.getMetrics()).stream()
                    .map(MetricDto::getStatus)
                    .filter(Objects::nonNull)
                    .map(status -> status.toLowerCase(Locale.ROOT))
                    .max(Comparator.comparingInt(this::statusPriority))
                    .orElse("normal");

            responses.add(new SharedHealthRecordResponse(
                    record.getId(),
                    profile.getId(),
                    profile.getDisplayName(),
                    record.getRecordType(),
                    record.getExamDate(),
                    record.getHospitalName(),
                    overallStatus,
                    record.getUpdatedAt(),
                    share.getGrantedAt()
            ));
        }

        responses.sort(Comparator.comparing(SharedHealthRecordResponse::sharedAt).reversed());
        return responses;
    }

    @Transactional(readOnly = true)
    public MetricExplanationResponse getMetricExplanation(UUID userId, UUID recordId, String metricName) {
        HealthRecord record = loadAccessibleRecord(userId, recordId).record();

        MetricDto metric = parseMetrics(record.getMetrics()).stream()
                .filter(item -> item.getName() != null && item.getName().equalsIgnoreCase(metricName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay chi so trong health record"));

        MetricExplanationRetrievalService.RetrievalResult retrievalResult = metricExplanationRetrievalService.retrieve(
                metric.getName(),
                metric.getStatus(),
                metric.getReferenceRange(),
                "vi"
        );

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                metric.getName(),
                metric.getNormalizedValue() != null ? metric.getNormalizedValue() : metric.getValue(),
                metric.getStatus(),
                metric.getReferenceRange(),
                "vi",
                retrievalResult.knowledgeSnippet()
        );

        return new MetricExplanationResponse(result.explanation(), result.source());
    }

    @Transactional(readOnly = true)
    public RecommendationsResponse getRecommendations(UUID userId, UUID recordId) {
        AccessibleRecord accessibleRecord = loadAccessibleRecord(userId, recordId);
        HealthRecord record = accessibleRecord.record();

        if (record.getProfileId() == null) {
            throw new ResourceNotFoundException("Profile khong ton tai");
        }

        Profile profile = profileRepository.findById(record.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));
        if (!profile.getUser().getId().equals(userId) && !accessibleRecord.isShared()) {
            throw new AccessDeniedException("Profile khong thuoc ve nguoi dung");
        }

        /*
         * Story 4.4: classify sau enrich có thể khác status đã lưu trên phiếu/OCR (hoặc UI đã hiển thị).
         * Nếu chỉ lọc theo status sau enrich, riskyMetrics có thể rỗng → luôn trả allNormal.
         * Giữ union: attention/warning/abnormal trước enrich HOẶC sau enrich.
         * Khi đưa vào LLM: nếu sau enrich là normal nhưng trước đó là attention/warning/abnormal, vẫn truyền
         * status rủi ro (prior) để LlmService không lọc hết input.
         */
        List<RecommendationRiskMetric> recommendationRisks = new ArrayList<>();
        for (MetricDto metric : parseMetrics(record.getMetrics())) {
            if (metric == null) {
                continue;
            }
            String priorStatus = metric.getStatus();
            enrichMetric(metric, profile, record.getExamDate());
            if (isRiskStatus(metric.getStatus()) || isRiskStatus(priorStatus)) {
                recommendationRisks.add(new RecommendationRiskMetric(metric, priorStatus));
            }
        }

        if (recommendationRisks.isEmpty()) {
            return new RecommendationsResponse(
                    List.of("Kết quả của bạn nhìn chung tốt. Tiếp tục duy trì lối sống lành mạnh!"),
                    RECOMMENDATIONS_DISCLAIMER,
                    true
            );
        }

        Integer age = resolveAge(profile, record.getExamDate());
        List<LlmService.RecommendationMetricInput> llmInputs = recommendationRisks.stream()
                .map(rm -> new LlmService.RecommendationMetricInput(
                        rm.metric().getName(),
                        rm.metric().getNormalizedValue() != null ? rm.metric().getNormalizedValue() : rm.metric().getValue(),
                        rm.metric().getUnit(),
                        recommendationStatusForLlm(rm.metric(), rm.priorStatus()),
                        rm.metric().getDisplayNameVi()))
                .toList();
        List<String> recommendations = llmService.generateRecommendations(
                llmInputs,
                age,
                profile.getGender(),
                recommendationExamContext(record));
        return new RecommendationsResponse(recommendations, RECOMMENDATIONS_DISCLAIMER, false);
    }

    /** Ngữ cảnh phiếu khám giúp LLM không chỉ đưa khẩu phần chung cho mọi xét nghiệm. */
    private static String recommendationExamContext(HealthRecord record) {
        StringBuilder sb = new StringBuilder();
        if (record.getRecordType() != null && !record.getRecordType().isBlank()) {
            sb.append("Loại phiếu/xét nghiệm: ").append(record.getRecordType().trim());
        }
        if (record.getDiagnosis() != null && !record.getDiagnosis().isBlank()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            String d = record.getDiagnosis().trim().replaceAll("\\s+", " ");
            if (d.length() > 400) {
                d = d.substring(0, 400).concat("…");
            }
            sb.append("Kết luận/ghi chú trên phiếu (chỉ là ngữ cảnh, không thay chẩn đoán): ").append(d);
        }
        return sb.toString();
    }

    private Map<String, String> safeMetricExplanations(List<MetricDto> metrics) {
        Map<String, String> explanations = new LinkedHashMap<>();
        for (MetricDto metric : metrics) {
            String metricLabel = firstNonBlank(metric.getDisplayNameVi(), metric.getName());
            if (metricLabel == null || metricLabel.isBlank()) {
                continue;
            }
            String staticExplanation = metric.getExplanation();
            if (staticExplanation != null && !staticExplanation.isBlank()) {
                explanations.put(metricLabel, staticExplanation.trim());
            }
        }
        return explanations;
    }

    private RecommendationsResponse safeRecommendations(UUID userId, UUID recordId) {
        try {
            return getRecommendations(userId, recordId);
        } catch (RuntimeException ex) {
            log.warn("Failed to load recommendations for record {} while generating PDF", recordId, ex);
            return new RecommendationsResponse(
                    List.of("Chưa có khuyến nghị tại thời điểm tải xuống."),
                    RECOMMENDATIONS_DISCLAIMER,
                    false
            );
        }
    }

    private void writePdfDownloadAudit(UUID actorId, HealthRecord record, String shareScope) {
        HealthRecordAuditLog auditLog = new HealthRecordAuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setUserId(actorId);
        auditLog.setAction("DOWNLOAD_HEALTH_RECORD_PDF");
        auditLog.setRecordId(record.getId());
        auditLog.setProfileId(record.getProfileId());
        auditLog.setViewerId(actorId);
        auditLog.setShareScope(shareScope);
        auditLog.setResourceType("HEALTH_RECORD");
        healthRecordAuditLogRepository.save(auditLog);
    }

    private String buildPdfFilename(Profile profile, HealthRecord record) {
        String recordType = slugify(record.getRecordType() != null ? record.getRecordType() : "kham");
        return "healthlens-ket-qua-%s.pdf".formatted(recordType);
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "ho-so" : normalized;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    @Transactional(readOnly = true)
    public java.util.List<HealthRecordStatusResponse> getRecordsByProfile(UUID userId, UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));
        if (!hasFullProfileHistoryAccess(profile, userId)) {
            throw new AccessDeniedException("Profile khong thuoc ve nguoi dung");
        }
        boolean isOwner = profile.getUser().getId().equals(userId);
        UUID ownerId = profile.getUser().getId();
        boolean canEdit = isOwner || hasEditAccess(profileId, userId);
        return healthRecordRepository.findAllByProfileIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(profileId, ownerId)
                .stream()
                .map(record -> {
                    java.util.List<MetricDto> metricsList = null;
                    if (record.getMetrics() != null) {
                        try {
                            metricsList = objectMapper.readValue(record.getMetrics(), new TypeReference<java.util.List<MetricDto>>() {});
                        } catch (Exception e) {
                            log.warn("Failed to parse metrics for {}", record.getId());
                        }
                    }
                    return new HealthRecordStatusResponse(
                        record.getId(),
                        isOwner,
                        canEdit,
                        record.getStatus(),
                        metricsList,
                        containsLowConfidenceMetrics(metricsList),
                        resolveOcrFailureReason(record),
                        record.getExamDate() != null ? record.getExamDate().toString() : null,
                        record.getRecordType(),
                        record.getHospitalName(),
                        record.getDiagnosis(),
                        record.getAnalyzerModel(),
                        record.getTestMethod(),
                        record.getLabSite(),
                        storageService.generateDownloadUrl(record.getFileKey(), Duration.ofHours(1))
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HealthRecordHistoryPageResponse getProfileHistory(UUID userId, UUID profileId, int page, int limit) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai"));
        boolean hasFullProfileAccess = hasFullProfileHistoryAccess(profile, userId);
        boolean hasRecordLevelAccess = hasRecordLevelHistoryAccess(profile.getId(), userId);
        if (!hasFullProfileAccess && !hasRecordLevelAccess) {
            throw new AccessDeniedException("Profile khong thuoc ve nguoi dung");
        }

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(limit, 1), HISTORY_PAGE_SIZE),
                Sort.by(Sort.Order.desc("examDate").nullsLast(), Sort.Order.desc("createdAt"))
        );
        UUID profileOwnerId = profile.getUser().getId();
        boolean canDelete = profileOwnerId.equals(userId) || hasEditAccess(profileId, userId);

        List<HealthRecordHistoryItemResponse> items;
        long totalItems;
        int totalPages;
        int currentPage;
        int pageSize;

        if (hasFullProfileAccess) {
            Page<HealthRecord> records = healthRecordRepository.findAllByProfileIdAndUserIdAndDeletedAtIsNull(
                    profileId, profileOwnerId, pageable);
            items = records.getContent().stream()
                    .map(record -> toHistoryItem(record, profile, canDelete))
                    .collect(Collectors.toList());
            totalItems = records.getTotalElements();
            totalPages = records.getTotalPages();
            currentPage = records.getNumber();
            pageSize = records.getSize();
        } else {
            List<HealthRecordShare> shares = healthRecordShareRepository
                    .findAllByViewerIdAndProfileIdAndRevokedAtIsNull(userId, profileId);
            List<UUID> sharedRecordIds = shares.stream()
                    .map(HealthRecordShare::getHealthRecordId)
                    .distinct()
                    .toList();
            List<HealthRecord> sharedRecords = healthRecordRepository.findAllById(sharedRecordIds).stream()
                    .filter(record -> record.getDeletedAt() == null)
                    .sorted(Comparator
                            .comparing(HealthRecord::getExamDate, Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(HealthRecord::getCreatedAt, Comparator.reverseOrder()))
                    .toList();

            pageSize = pageable.getPageSize();
            currentPage = pageable.getPageNumber();
            totalItems = sharedRecords.size();
            totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);

            int fromIndex = Math.min(currentPage * pageSize, sharedRecords.size());
            int toIndex = Math.min(fromIndex + pageSize, sharedRecords.size());
            items = sharedRecords.subList(fromIndex, toIndex).stream()
                    .map(record -> toHistoryItem(record, profile, false))
                    .collect(Collectors.toList());
        }

        return new HealthRecordHistoryPageResponse(
                items,
                new PaginationResponse(
                        currentPage,
                        pageSize,
                        totalItems,
                        totalPages
                )
        );
    }

    @Transactional
    public void markOcrCompleted(UUID recordId, String rawOcrJson, OcrService.OcrExtractionResult parsedData) {
        markOcrCompleted(recordId, rawOcrJson, parsedData, false);
    }

    @Transactional
    public void markOcrCompleted(UUID recordId, String rawOcrJson, OcrService.OcrExtractionResult parsedData, boolean hasLowConfidenceMetrics) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        record.setStatus("review_required");
        record.setRawOcrResult(rawOcrJson);
        
        if (parsedData.examDate() != null) {
            try {
                record.setExamDate(java.time.LocalDate.parse(parsedData.examDate()));
            } catch (Exception e) {
                log.warn("Invalid exam date format: {}", parsedData.examDate());
            }
        }
        record.setRecordType(parsedData.recordType());
        record.setHospitalName(parsedData.hospitalName());
        record.setDiagnosis(parsedData.diagnosis());

        try {
            record.setMetrics(objectMapper.writeValueAsString(parsedData.metrics()));
        } catch (JsonProcessingException e) {
            record.setMetrics("[]");
        }
        if (hasLowConfidenceMetrics) {
            record.setSourceType("ocr_partial");
        } else if (record.getSourceType() == null || record.getSourceType().isBlank()) {
            record.setSourceType("ocr");
        }
        healthRecordRepository.save(record);
        updateProfileLastRecordAt(record.getProfileId());
    }

    @Transactional
    public void markOcrFailed(UUID recordId) {
        markOcrFailed(recordId, "processing_error");
    }

    @Transactional
    public void markOcrFailed(UUID recordId, String reason) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        record.setStatus("ocr_failed");
        record.setRawOcrResult(buildFailurePayload(reason));
        healthRecordRepository.save(record);
    }

    private void persistUploadReservation(UUID recordId, UUID userId, UUID profileId, String fileKey, String mimeType) {
        UploadReservation reservation = new UploadReservation(userId, profileId, fileKey, mimeType);
        try {
            String json = objectMapper.writeValueAsString(reservation);
            redisTemplate.opsForValue().set(uploadReservationKey(recordId), json, UPLOAD_RESERVATION_TTL);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Khong the luu upload reservation", ex);
        }
    }

    private UploadReservation loadUploadReservation(UUID recordId) {
        String json = redisTemplate.opsForValue().get(uploadReservationKey(recordId));
        if (json == null) {
            throw new IllegalArgumentException("Upload session da het han hoac khong ton tai");
        }
        try {
            return objectMapper.readValue(json, UploadReservation.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Upload session bi loi du lieu", ex);
        }
    }

    @Transactional
    public void confirmRecord(UUID userId, UUID recordId, ConfirmRecordRequest request) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));

        if (!canEditRecord(userId, record)) {
            throw new AccessDeniedException("Ban khong co quyen xac nhan health record nay");
        }

        String previousStatus = record.getStatus();
        boolean keepPartialRequested = request != null && Boolean.TRUE.equals(request.getKeepPartial());
        boolean canUpdateFromStatus = "review_required".equals(record.getStatus())
                || "done".equals(record.getStatus())
                || "ocr_failed".equals(record.getStatus());
        if (!canUpdateFromStatus) {
            throw new IllegalStateException("Health record khong o trang thai cho phep cap nhat");
        }

        record.setStatus("done");
        if (request != null) {
            if (request.getExamDate() != null) {
                record.setExamDate(request.getExamDate());
            }
            if (request.getRecordType() != null) {
                record.setRecordType(request.getRecordType());
            }
            if (request.getHospitalName() != null) {
                record.setHospitalName(request.getHospitalName());
            }
            if (request.getDiagnosis() != null) {
                record.setDiagnosis(request.getDiagnosis());
            }
            if (request.getAnalyzerModel() != null) {
                record.setAnalyzerModel(request.getAnalyzerModel());
            }
            if (request.getTestMethod() != null) {
                record.setTestMethod(request.getTestMethod());
            }
            if (request.getLabSite() != null) {
                record.setLabSite(request.getLabSite());
            }
            
            if (request.getMetrics() != null) {
                List<MetricDto> metrics = request.getMetrics();
                validateMetrics(metrics);
                String computedSourceType = computeSourceType(metrics, record.getSourceType());
                try {
                    record.setMetrics(objectMapper.writeValueAsString(metrics));
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException("Failed to serialize metrics for " + recordId, e);
                }

                if (keepPartialRequested) {
                    record.setSourceType("ocr_partial");
                } else if ("ocr_failed".equals(previousStatus)) {
                    record.setSourceType("manual");
                } else {
                    record.setSourceType(computedSourceType);
                }
            }
        }

        healthRecordRepository.save(record);
        updateProfileLastRecordAt(record.getProfileId());
    }

    @Transactional
    public void updateMetrics(UUID userId, UUID recordId, UpdateMetricsRequest request) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));

        if (!canEditRecord(userId, record)) {
            throw new AccessDeniedException("Ban khong co quyen cap nhat health record nay");
        }

        if (!"review_required".equals(record.getStatus())
                && !"done".equals(record.getStatus())
                && !"ocr_failed".equals(record.getStatus())) {
            throw new IllegalStateException("Health record khong o trang thai cho phep cap nhat");
        }

        List<MetricDto> metrics = request.getMetrics();
        validateMetrics(metrics);
        record.setSourceType(computeSourceType(metrics, record.getSourceType()));
        try {
            record.setMetrics(objectMapper.writeValueAsString(metrics));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize metrics for " + recordId);
        }
        healthRecordRepository.save(record);
        updateProfileLastRecordAt(record.getProfileId());

        String cacheKey = "health-record-status:" + userId + ":" + recordId;
        redisTemplate.delete(cacheKey);
    }

    @Transactional
    @Auditable(action = "DELETE_HEALTH_RECORD")
    public void deleteHealthRecord(UUID userId, UUID recordId) {
        HealthRecord record = healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));

        if (!record.getUserId().equals(userId) && !hasEditAccess(record.getProfileId(), userId)) {
            throw new AccessDeniedException("Ban khong co quyen xoa health record nay");
        }

        record.setDeletedAt(Instant.now());
        healthRecordRepository.save(record);
        redisTemplate.delete("health-record-status:" + userId + ":" + recordId);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void purgeSoftDeletedRecords() {
        Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
        int batchSize = 100;

        while (true) {
            Page<HealthRecord> staleRecords = healthRecordRepository.findAllByDeletedAtBefore(
                    threshold,
                    PageRequest.of(0, batchSize)
            );
            if (staleRecords.isEmpty()) {
                break;
            }

            for (HealthRecord record : staleRecords.getContent()) {
                try {
                    storageService.deleteObject(record.getFileKey());
                    healthRecordRepository.delete(record);
                } catch (Exception ex) {
                    log.error("Failed to purge soft-deleted health record {}", record.getId(), ex);
                }
            }
        }
    }

    private static final Set<String> VALID_SOURCES = Set.of(
            "ocr", "manual", "ocr_regex_fallback"
    );
    private static final Set<String> OCR_SOURCES = Set.of("ocr", "ocr_regex_fallback");

    private void validateMetrics(List<MetricDto> metrics) {
        for (int i = 0; i < metrics.size(); i++) {
            MetricDto m = metrics.get(i);
            if (m == null) {
                throw new IllegalArgumentException(metricValidationPrefix(null, i) + "không được null");
            }
            if (m.getName() == null || m.getName().isBlank()) {
                throw new IllegalArgumentException(metricValidationPrefix(m, i) + "tên chỉ số không được để trống");
            }
            if (m.getValue() == null || m.getValue().isBlank()) {
                throw new IllegalArgumentException(metricValidationPrefix(m, i) + "giá trị không được để trống");
            }
            if (m.getUnit() == null || m.getUnit().isBlank()) {
                throw new IllegalArgumentException(metricValidationPrefix(m, i) + "đơn vị không được để trống");
            }
            if (m.getSource() != null && !VALID_SOURCES.contains(m.getSource())) {
                throw new IllegalArgumentException(
                        metricValidationPrefix(m, i) + "source '" + m.getSource() + "' không hợp lệ"
                );
            }
        }
    }

    private String metricValidationPrefix(MetricDto metric, int index) {
        String name = metric != null && metric.getName() != null ? metric.getName().trim() : "";
        if (!name.isBlank()) {
            return "Chỉ số \"" + name + "\": ";
        }
        return "Chỉ số #" + (index + 1) + ": ";
    }

    /**
     * Tính toán source_type từ danh sách metrics.
     * - Nếu danh sách rỗng/null: giữ nguyên sourceType hiện tại của record.
     * - Nếu tất cả là "ocr": trả về "ocr".
     * - Nếu không có metric nào là "ocr" (kể cả null source): trả về "manual".
     * - Còn lại (có cả ocr lẫn non-ocr): trả về "mixed".
     */
    private String computeSourceType(List<MetricDto> metrics, String currentSourceType) {
        if (metrics == null || metrics.isEmpty()) {
            return currentSourceType != null ? currentSourceType : "manual";
        }
        boolean anyOcr = metrics.stream()
                .filter(Objects::nonNull)
                .map(MetricDto::getSource)
                .anyMatch(this::isOcrSource);
        boolean anyNonOcr = metrics.stream()
                .anyMatch(m -> m == null || !isOcrSource(m.getSource()));
        if (anyOcr && anyNonOcr) return "mixed";
        if (anyOcr) return "ocr";
        return "manual";
    }

    private boolean isOcrSource(String source) {
        return source != null && OCR_SOURCES.contains(source);
    }

    private String uploadReservationKey(UUID recordId) {
        return "health-record-upload:%s".formatted(recordId);
    }

    private List<MetricDto> parseMetrics(String rawMetrics) {
        if (rawMetrics == null || rawMetrics.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawMetrics, new TypeReference<List<MetricDto>>() {
            });
        } catch (Exception e) {
            log.warn("Failed to parse metrics JSON", e);
            return List.of();
        }
    }

    private static boolean isRiskStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String s = status.trim().toLowerCase(Locale.ROOT);
        return "attention".equals(s) || "warning".equals(s) || "abnormal".equals(s);
    }

    /**
     * Status gửi sang LLM cache/recommendations: ưu tiên phân loại sau enrich;
     * nếu đã về normal nhưng phiếu trước đó đánh dấu attention/abnormal thì giữ prior để không bị LlmService lọc mất.
     */
    private static String recommendationStatusForLlm(MetricDto enriched, String priorStatus) {
        if (isRiskStatus(enriched.getStatus())) {
            return enriched.getStatus();
        }
        if (isRiskStatus(priorStatus)) {
            return priorStatus.trim();
        }
        return enriched.getStatus() != null ? enriched.getStatus() : "unknown";
    }

    private record RecommendationRiskMetric(MetricDto metric, String priorStatus) {
    }

    private void updateProfileLastRecordAt(UUID profileId) {
        profileRepository.findById(profileId).ifPresent(profile -> {
            Instant maxCreatedAt = healthRecordRepository.findMaxCreatedAtByProfileId(profileId)
                    .orElse(null);
            profile.setLastRecordAt(maxCreatedAt);
            profileRepository.save(profile);
        });
    }

    private MetricDto enrichMetric(MetricDto metric, Profile profile, LocalDate examDate) {
        return enrichMetric(metric, profile, examDate, true);
    }

    private MetricDto enrichMetric(MetricDto metric, Profile profile, LocalDate examDate, boolean persistAudit) {
        hydrateRawAndNormalizedFields(metric);

        if (metric.getReferenceRange() != null) {
            String status = classifyByRange(metric.getNormalizedValue(), metric.getReferenceRange());
            metric.setStatus(status);
            metric.setStatusSource("document");
            metric.setReferenceRangeSource("document");
            metric.setDisplayNameVi(metric.getDisplayNameVi() != null ? metric.getDisplayNameVi() : metric.getName());
            metric.setInterpretation(resolveInterpretation(metric, status));
            metric.setInterpretationSource(metric.getInterpretationSource() != null ? metric.getInterpretationSource() : "computed");
            metric.setCritical("abnormal".equals(status) && isCritical(metric.getNormalizedValue(), metric.getReferenceRange()));
            return metric;
        }

        MetricClassificationDto classification = persistAudit
                ? referenceDataService.classifyMetric(
                metric.getName(),
                metric.getNormalizedValue(),
                profile,
                examDate
        )
                : referenceDataService.classifyMetricWithoutAudit(
                metric.getName(),
                metric.getNormalizedValue(),
                profile,
                examDate
        );
        metric.setStatus(classification.status());
        metric.setStatusSource("no_data".equals(classification.status()) ? "none" : "system");
        metric.setReferenceRange(classification.referenceRange());
        metric.setReferenceRangeSource(classification.referenceRange() == null ? "none" : "system");
        metric.setRangeContext(classification.rangeContext());
        metric.setDisplayNameVi(classification.displayNameVi() != null ? classification.displayNameVi() : metric.getDisplayNameVi());
        metric.setInterpretation(resolveInterpretation(metric, classification.status()));
        metric.setInterpretationSource(metric.getInterpretationSource() != null ? metric.getInterpretationSource() : "computed");
        metric.setCritical("abnormal".equals(classification.status()) && isCritical(metric.getNormalizedValue(), classification.referenceRange()));
        return metric;
    }

    private void hydrateRawAndNormalizedFields(MetricDto metric) {
        if (metric.getRawName() == null) {
            metric.setRawName(metric.getName());
        }
        if (metric.getRawUnit() == null) {
            metric.setRawUnit(metric.getUnit());
        }
        if (metric.getRawValue() == null) {
            metric.setRawValue(metric.getValue());
        }
        if (metric.getNormalizedName() == null && metric.getName() != null) {
            metric.setNormalizedName(metric.getName().trim().toLowerCase(Locale.ROOT));
        }
        if (metric.getNormalizedUnit() == null && metric.getUnit() != null) {
            metric.setNormalizedUnit(metric.getUnit().trim().toLowerCase(Locale.ROOT));
        }
        if (metric.getNormalizedValue() == null && metric.getValue() != null) {
            metric.setNormalizedValue(metric.getValue().replace(",", ".").trim());
        }
    }

    private String classifyByRange(String normalizedValue, ReferenceRangeDto range) {
        if (range == null || range.min() == null || range.max() == null) {
            return "no_data";
        }
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return "no_data";
        }
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(normalizedValue);
            java.math.BigDecimal attentionMin = range.attentionMin() != null ? range.attentionMin() : range.min();
            java.math.BigDecimal attentionMax = range.attentionMax() != null ? range.attentionMax() : range.max();
            if (value.compareTo(attentionMin) < 0 || value.compareTo(attentionMax) > 0) {
                return "abnormal";
            }
            if (value.compareTo(range.min()) < 0 || value.compareTo(range.max()) > 0) {
                return "attention";
            }
            return "normal";
        } catch (NumberFormatException ex) {
            return "no_data";
        }
    }

    private String resolveInterpretation(MetricDto metric, String status) {
        if (metric.getInterpretation() != null && !"unknown".equals(metric.getInterpretation())) {
            return metric.getInterpretation();
        }
        if ("normal".equals(status)) {
            return "normal";
        }
        if (!"abnormal".equals(status) && !"attention".equals(status)) {
            return "unknown";
        }

        ReferenceRangeDto range = metric.getReferenceRange();
        if (range == null || metric.getNormalizedValue() == null || metric.getNormalizedValue().isBlank()) {
            return "unknown";
        }
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(metric.getNormalizedValue());
            java.math.BigDecimal lowerBound = "abnormal".equals(status)
                    ? (range.attentionMin() != null ? range.attentionMin() : range.min())
                    : range.min();
            java.math.BigDecimal upperBound = "abnormal".equals(status)
                    ? (range.attentionMax() != null ? range.attentionMax() : range.max())
                    : range.max();
            if (lowerBound != null && value.compareTo(lowerBound) < 0) {
                return "low";
            }
            if (upperBound != null && value.compareTo(upperBound) > 0) {
                return "high";
            }
        } catch (NumberFormatException ignored) {
            return "unknown";
        }
        return "unknown";
    }

    private boolean isCritical(String normalizedValue, ReferenceRangeDto range) {
        if (normalizedValue == null || range == null || range.attentionMin() == null || range.attentionMax() == null) {
            return false;
        }
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(normalizedValue);
            java.math.BigDecimal lowerBuffer = range.attentionMin().subtract(range.attentionMin().multiply(java.math.BigDecimal.valueOf(0.2)));
            java.math.BigDecimal upperBuffer = range.attentionMax().add(range.attentionMax().multiply(java.math.BigDecimal.valueOf(0.2)));
            return value.compareTo(lowerBuffer) < 0 || value.compareTo(upperBuffer) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Integer resolveAge(Profile profile, LocalDate examDate) {
        if (profile == null || profile.getBirthDate() == null) {
            return null;
        }
        LocalDate pointInTime = examDate != null ? examDate : LocalDate.now();
        return Period.between(profile.getBirthDate(), pointInTime).getYears();
    }

    private UploadFormat resolveUploadFormat(String fileTypeRaw) {
        String fileType = fileTypeRaw.toLowerCase();
        return switch (fileType) {
            case "pdf", "application/pdf" -> new UploadFormat("pdf", "application/pdf");
            case "image", "jpg", "jpeg", "image/jpg", "image/jpeg" -> new UploadFormat("jpg", "image/jpeg");
            case "png", "image/png" -> new UploadFormat("png", "image/png");
            default -> throw new IllegalArgumentException(
                    "fileType chi chap nhan 'pdf', 'image/jpeg', hoac 'image/png'"
            );
        };
    }

    private String deriveMimeTypeFromFileKey(String fileKey) {
        if (fileKey == null) {
            return "application/octet-stream";
        }
        String normalized = fileKey.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (normalized.endsWith(".png")) {
            return "image/png";
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private boolean containsLowConfidenceMetrics(List<MetricDto> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return false;
        }
        return metrics.stream()
                .map(MetricDto::getConfidenceLevel)
                .anyMatch(level -> level != null && !"high".equals(level));
    }

    private String resolveOcrFailureReason(HealthRecord record) {
        if (record == null || record.getRawOcrResult() == null || record.getRawOcrResult().isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(record.getRawOcrResult());
            if (!node.has("failureReason") || node.get("failureReason").isNull()) {
                return null;
            }
            return node.get("failureReason").asText();
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildFailurePayload(String reason) {
        try {
            return objectMapper.writeValueAsString(Map.of("failureReason", reason == null ? "processing_error" : reason));
        } catch (JsonProcessingException ex) {
            return "{\"failureReason\":\"processing_error\"}";
        }
    }

    private HealthRecordHistoryItemResponse toHistoryItem(HealthRecord record, Profile profile, boolean canDelete) {
        List<MetricDto> metrics = parseMetrics(record.getMetrics()).stream()
                .map(metric -> enrichMetric(metric, profile, record.getExamDate(), false))
                .collect(Collectors.toList());
        long abnormalCountLong = metrics.stream()
                .map(MetricDto::getStatus)
                .filter(Objects::nonNull)
                .map(status -> status.toLowerCase(Locale.ROOT))
                .filter(status -> "abnormal".equals(status) || "attention".equals(status))
                .count();
        int abnormalCount = Math.toIntExact(Math.min(abnormalCountLong, Integer.MAX_VALUE));

        String overallStatus = metrics.stream()
                .map(MetricDto::getStatus)
                .filter(Objects::nonNull)
                .map(status -> status.toLowerCase(Locale.ROOT))
                .max(Comparator.comparingInt(this::statusPriority))
                .orElse("normal");

        String testType = (record.getRecordType() != null && !record.getRecordType().isBlank())
                ? record.getRecordType()
                : inferTestTypeFromMetrics(metrics);

        return new HealthRecordHistoryItemResponse(
                record.getId(),
                record.getStatus(),
                record.getExamDate(),
                testType,
                overallStatus,
                abnormalCount,
                record.getHospitalName(),
                record.getSourceType(),
                record.getCreatedAt(),
                canDelete
        );
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "abnormal" -> 3;
            case "attention" -> 2;
            case "normal" -> 1;
            default -> 0;
        };
    }

    private String inferTestTypeFromMetrics(List<MetricDto> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "Phiếu khám bệnh";
        }
        String firstMetricName = metrics.stream()
                .map(MetricDto::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse(null);
        if (firstMetricName == null) {
            return "Xét nghiệm";
        }
        return "Xét nghiệm - " + firstMetricName;
    }

    private boolean hasFullProfileHistoryAccess(Profile profile, UUID userId) {
        if (profile.getUser().getId().equals(userId)) {
            return true;
        }
        return profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profile.getId(), userId);
    }

    private boolean hasRecordLevelHistoryAccess(UUID profileId, UUID userId) {
        return !healthRecordShareRepository.findAllByViewerIdAndProfileIdAndRevokedAtIsNull(userId, profileId).isEmpty();
    }

    private AccessibleRecord loadAccessibleRecord(UUID userId, UUID recordId) {
        HealthRecord ownedRecord = healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)
                .orElse(null);
        if (ownedRecord != null) {
            return new AccessibleRecord(ownedRecord, true, false, true, "owner");
        }
        HealthRecord record = healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Health record khong ton tai"));
        boolean profileShared = profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(record.getProfileId(), userId);
        if (profileShared) {
            boolean canEdit = hasEditAccess(record.getProfileId(), userId);
            return new AccessibleRecord(record, false, true, canEdit, "profile");
        }
        boolean recordShared = healthRecordShareRepository.existsByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, userId);
        if (!recordShared) {
            throw new ProfileAccessRevokedException("Ban khong co quyen truy cap health record nay");
        }
        HealthRecordShare share = healthRecordShareRepository
                .findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, userId)
                .orElseThrow(() -> new ProfileAccessRevokedException("Ban khong co quyen truy cap health record nay"));
        boolean canEdit = "edit".equalsIgnoreCase(share.getAccessLevel());
        return new AccessibleRecord(record, false, true, canEdit, "record");
    }

    private boolean hasEditAccess(UUID profileId, UUID userId) {
        return profileShareRepository.existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(
                profileId, userId, "edit");
    }

    private boolean canEditRecord(UUID userId, HealthRecord record) {
        if (record.getUserId() != null && record.getUserId().equals(userId)) {
            return true;
        }
        if (record.getProfileId() != null && hasEditAccess(record.getProfileId(), userId)) {
            return true;
        }
        return healthRecordShareRepository.findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(record.getId(), userId)
                .map(HealthRecordShare::getAccessLevel)
                .filter(accessLevel -> accessLevel != null && "edit".equalsIgnoreCase(accessLevel))
                .isPresent();
    }

    private record UploadFormat(String extension, String contentType) {}
    private record UploadReservation(UUID userId, UUID profileId, String fileKey, String mimeType) {}
    private record AccessibleRecord(HealthRecord record, boolean isOwner, boolean isShared, boolean canEdit, String shareScope) {}
}
