package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.HealthRecordDetailResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HealthRecordService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration UPLOAD_RESERVATION_TTL = Duration.ofMinutes(20);
    private static final String STATUS_PROCESSING = "processing";

    private final StorageService storageService;
    private final ProfileRepository profileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final ReferenceDataService referenceDataService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String ocrStreamName;

    public HealthRecordService(
            StorageService storageService,
            ProfileRepository profileRepository,
            HealthRecordRepository healthRecordRepository,
            ReferenceDataService referenceDataService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.stream.ocr-events:ocr.events}") String ocrStreamName
    ) {
        this.storageService = storageService;
        this.profileRepository = profileRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.referenceDataService = referenceDataService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ocrStreamName = ocrStreamName;
    }

    @Transactional(readOnly = true)
    public UploadUrlResponse createUploadUrl(UUID userId, CreateUploadUrlRequest request) {
        Profile profile = profileRepository.findById(request.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Profile khong ton tai"));
        if (!profile.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Profile khong thuoc ve nguoi dung hien tai");
        }

        UploadFormat uploadFormat = resolveUploadFormat(request.fileType());

        UUID recordId = UUID.randomUUID();
        String fileKey = "health-records/%s/%s/%s/original.%s"
                .formatted(userId, request.profileId(), recordId, uploadFormat.extension());
        String uploadUrl = storageService.generateUploadUrl(fileKey, UPLOAD_URL_TTL, uploadFormat.contentType());

        persistUploadReservation(recordId, userId, request.profileId(), fileKey);

        return new UploadUrlResponse(uploadUrl, recordId, fileKey);
    }

    @Transactional
    public ConfirmUploadResponse confirmUpload(UUID userId, UUID recordId) {
        UploadReservation reservation = loadUploadReservation(recordId);
        if (!reservation.userId().equals(userId)) {
            throw new IllegalArgumentException("Record khong thuoc ve nguoi dung hien tai");
        }

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setProfileId(reservation.profileId());
        record.setFileKey(reservation.fileKey());
        record.setStatus(STATUS_PROCESSING);
        record.setSourceType("ocr");
        healthRecordRepository.save(record);

        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        String jobId = UUID.randomUUID().toString();
        streamOps.add(ocrStreamName, Map.of(
                "jobId", jobId,
                "recordId", recordId.toString(),
                "fileKey", reservation.fileKey(),
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

        HealthRecord record = healthRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        
        java.util.List<MetricDto> metricsList = null;
        if ("review_required".equals(record.getStatus()) || "done".equals(record.getStatus())) {
            try {
                metricsList = objectMapper.readValue(record.getMetrics(), new TypeReference<java.util.List<MetricDto>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse metrics for {}", recordId);
            }
        }
        
        HealthRecordStatusResponse response = new HealthRecordStatusResponse(
            record.getId(), 
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
        
        // Cache the result for 5 seconds
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Failed to cache status for {}", recordId);
        }
        
        return response;
    }

    @Transactional(readOnly = true)
    public HealthRecordDetailResponse getDetail(UUID userId, UUID recordId) {
        HealthRecord record = healthRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));

        Profile profile = profileRepository.findById(record.getProfileId()).orElse(null);
        Integer age = resolveAge(profile, record.getExamDate());
        String gender = profile != null ? profile.getGender() : null;

        List<MetricDto> metricsList = parseMetrics(record.getMetrics()).stream()
                .map(metric -> enrichMetric(metric, age, gender))
                .collect(Collectors.toList());

        return new HealthRecordDetailResponse(
                record.getId(),
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

    @Transactional(readOnly = true)
    public java.util.List<HealthRecordStatusResponse> getRecordsByProfile(UUID userId, UUID profileId) {
        return healthRecordRepository.findAllByProfileIdAndUserIdOrderByCreatedAtDesc(profileId, userId)
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
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markOcrCompleted(UUID recordId, String rawOcrJson, OcrService.OcrExtractionResult parsedData) {
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
        healthRecordRepository.save(record);
    }

    @Transactional
    public void markOcrFailed(UUID recordId) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        record.setStatus("ocr_failed");
        healthRecordRepository.save(record);
    }

    private void persistUploadReservation(UUID recordId, UUID userId, UUID profileId, String fileKey) {
        UploadReservation reservation = new UploadReservation(userId, profileId, fileKey);
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

        if (!record.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Ban khong co quyen xac nhan health record nay");
        }

        if (!"review_required".equals(record.getStatus()) && !"done".equals(record.getStatus())) {
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
                try {
                    record.setMetrics(objectMapper.writeValueAsString(request.getMetrics()));
                } catch (Exception e) {
                    log.warn("Failed to serialize metrics in confirmRecord for {}", recordId);
                }
            }
        }

        healthRecordRepository.save(record);
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

    private MetricDto enrichMetric(MetricDto metric, Integer age, String gender) {
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

        MetricClassificationDto classification = referenceDataService.classifyMetric(metric.getName(), metric.getNormalizedValue(), age, gender);
        metric.setStatus(classification.status());
        metric.setStatusSource("no_data".equals(classification.status()) ? "none" : "system");
        metric.setReferenceRange(classification.referenceRange());
        metric.setReferenceRangeSource(classification.referenceRange() == null ? "none" : "system");
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

    private record UploadFormat(String extension, String contentType) {}
    private record UploadReservation(UUID userId, UUID profileId, String fileKey) {}
}
