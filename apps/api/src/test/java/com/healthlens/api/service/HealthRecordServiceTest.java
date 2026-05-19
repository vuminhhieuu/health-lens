package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.entity.OnlineRagAnswerCitation;
import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.dto.response.RecommendationsResponse;
import com.healthlens.api.dto.response.DownloadHealthRecordPdfResponse;
import com.healthlens.api.dto.request.UpdateMetricsRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.HealthRecordShare;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ConsentRequiredException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.HealthRecordShareRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.OnlineRagAnswerCitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRepository profileRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private HealthRecordShareRepository healthRecordShareRepository;
    @Mock private com.healthlens.api.audit.HealthRecordLegacyAuditWriter healthRecordLegacyAuditWriter;
    @Mock private com.healthlens.api.audit.UnifiedAuditCoordinator unifiedAuditCoordinator;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;
    @Mock private ProfileShareRepository profileShareRepository;
    @Mock private ReferenceDataService referenceDataService;
    @Mock private MetricExplanationRetrievalService metricExplanationRetrievalService;
    @Mock private LlmService llmService;
    @Mock private ConsentService consentService;
    @Mock private HealthRecordPdfService healthRecordPdfService;
    @Mock private OnlineRagAnswerCitationRepository onlineRagAnswerCitationRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private StreamOperations<String, Object, Object> streamOperations;

    private HealthRecordService healthRecordService;

    @BeforeEach
    void setUp() {
        healthRecordService = new HealthRecordService(
                storageService,
                profileRepository,
                healthRecordRepository,
                healthRecordShareRepository,
                profileShareRepository,
                referenceDataService,
                metricExplanationRetrievalService,
                llmService,
                consentService,
                healthRecordPdfService,
                onlineRagAnswerCitationRepository,
                healthRecordLegacyAuditWriter,
                unifiedAuditCoordinator,
                auditEventRecorder,
                redisTemplate,
                new ObjectMapper(),
                "ocr.events"
        );
        lenient().when(consentService.hasConsent(any(UUID.class), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("createUploadUrl tao presigned URL va luu reservation")
    void createUploadUrl_success() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile profile = buildProfile(userId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(storageService.generateUploadUrl(any(), any(Duration.class), eq("application/pdf")))
                .thenReturn("https://signed-upload-url");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UploadUrlResponse response = healthRecordService.createUploadUrl(
                userId,
                new CreateUploadUrlRequest(profileId, "pdf", null)
        );

        assertThat(response.uploadUrl()).isEqualTo("https://signed-upload-url");
        assertThat(response.fileKey()).contains("health-records/" + userId + "/" + profileId + "/");
        assertThat(response.fileKey()).endsWith("/original.pdf");
        verify(valueOperations).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("createUploadUrl cho phep PNG va tao key .png")
    void createUploadUrl_pngSuccess() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile profile = buildProfile(userId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(storageService.generateUploadUrl(any(), any(Duration.class), eq("image/png")))
                .thenReturn("https://signed-upload-url-png");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UploadUrlResponse response = healthRecordService.createUploadUrl(
                userId,
                new CreateUploadUrlRequest(profileId, "image/png", null)
        );

        assertThat(response.uploadUrl()).isEqualTo("https://signed-upload-url-png");
        assertThat(response.fileKey()).endsWith("/original.png");
        verify(valueOperations).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("createUploadUrl retry dung lai recordId va profileId cu khi OCR failed")
    void createUploadUrl_retryReuseExistingRecord() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID ignoredProfileId = UUID.randomUUID();
        UUID retryRecordId = UUID.randomUUID();

        HealthRecord existing = new HealthRecord();
        existing.setId(retryRecordId);
        existing.setUserId(userId);
        existing.setProfileId(profileId);
        existing.setStatus("ocr_failed");

        Profile profile = buildProfile(userId, profileId);
        when(healthRecordRepository.findByIdAndUserId(retryRecordId, userId)).thenReturn(Optional.of(existing));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(storageService.generateUploadUrl(any(), any(Duration.class), eq("application/pdf")))
                .thenReturn("https://signed-upload-url-retry");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UploadUrlResponse response = healthRecordService.createUploadUrl(
                userId,
                new CreateUploadUrlRequest(ignoredProfileId, "pdf", retryRecordId)
        );

        assertThat(response.recordId()).isEqualTo(retryRecordId);
        assertThat(response.fileKey()).contains("health-records/" + userId + "/" + profileId + "/" + retryRecordId + "/");
        verify(valueOperations).set(any(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("createUploadUrl retry that bai neu record khong o trang thai ocr_failed")
    void createUploadUrl_retryInvalidStatus() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID retryRecordId = UUID.randomUUID();

        HealthRecord existing = new HealthRecord();
        existing.setId(retryRecordId);
        existing.setUserId(userId);
        existing.setProfileId(profileId);
        existing.setStatus("done");

        when(healthRecordRepository.findByIdAndUserId(retryRecordId, userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> healthRecordService.createUploadUrl(
                userId,
                new CreateUploadUrlRequest(profileId, "pdf", retryRecordId)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("thử tải lên lại");
    }

    @Test
    @DisplayName("confirmUpload tao health record processing va enqueue OCR job")
    @SuppressWarnings("unchecked")
    void confirmUpload_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        String fileKey = "health-records/%s/%s/%s/original.jpg".formatted(userId, profileId, recordId);
        String reservationJson = new ObjectMapper().writeValueAsString(Map.of(
                "userId", userId,
                "profileId", profileId,
                "fileKey", fileKey,
                "mimeType", "image/jpeg"
        ));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health-record-upload:" + recordId)).thenReturn(reservationJson);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfirmUploadResponse response = healthRecordService.confirmUpload(userId, recordId);

        assertThat(response.recordId()).isEqualTo(recordId);
        assertThat(response.status()).isEqualTo("processing");
        ArgumentCaptor<Map<String, String>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("ocr.events"), payloadCaptor.capture());
        verify(redisTemplate).delete("health-record-upload:" + recordId);
        Map<String, String> payload = payloadCaptor.getValue();
        assertThat(payload).containsKeys("jobId", "recordId", "fileKey", "profileId", "mimeType", "correlationId");
        assertThat(payload.get("recordId")).isEqualTo(recordId.toString());
        assertThat(payload.get("mimeType")).isEqualTo("image/jpeg");
        assertThat(payload.get("correlationId")).isEqualTo(payload.get("jobId"));
    }

    @Test
    @DisplayName("confirmUpload fail khi reservation khong ton tai")
    void confirmUpload_missingReservation() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health-record-upload:" + recordId)).thenReturn(null);

        assertThatThrownBy(() -> healthRecordService.confirmUpload(userId, recordId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phiên tải lên");
    }

    @Test
    @DisplayName("confirmUpload fail neu record ton tai nhung khong o trang thai ocr_failed")
    void confirmUpload_existingRecordInvalidStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        String fileKey = "health-records/%s/%s/%s/original.jpg".formatted(userId, profileId, recordId);
        String reservationJson = new ObjectMapper().writeValueAsString(Map.of(
                "userId", userId,
                "profileId", profileId,
                "fileKey", fileKey
        ));

        HealthRecord existing = new HealthRecord();
        existing.setId(recordId);
        existing.setUserId(userId);
        existing.setProfileId(profileId);
        existing.setStatus("done");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health-record-upload:" + recordId)).thenReturn(reservationJson);
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> healthRecordService.confirmUpload(userId, recordId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OCR thất bại cần thử lại");
    }

    @Test
    @DisplayName("confirmRecord thanh cong - update status thanh done")
    void confirmRecord_success() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .examDate(LocalDate.of(2023, 10, 10))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        assertThat(record.getExamDate()).isEqualTo(LocalDate.of(2023, 10, 10));
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord fail neu khong phai review_required")
    void confirmRecord_invalidStatus() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("processing"); // invalid status

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.confirmRecord(userId, recordId, new ConfirmRecordRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không ở trạng thái cho phép cập nhật");
    }

    @Test
    @DisplayName("markOcrCompleted khong ghi de record dang review_required da co OCR result")
    void markOcrCompleted_existingReviewRequiredResult_skipsOverwrite() {
        UUID recordId = UUID.randomUUID();
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setProfileId(UUID.randomUUID());
        record.setStatus("review_required");
        record.setRawOcrResult("{\"text\":\"existing\"}");
        record.setDiagnosis("user edit");
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        healthRecordService.markOcrCompleted(recordId, "{\"text\":\"new\"}",
                new OcrService.OcrExtractionResult("2026-01-01", "new", "new", "new", List.of()),
                false);

        assertThat(record.getRawOcrResult()).isEqualTo("{\"text\":\"existing\"}");
        assertThat(record.getDiagnosis()).isEqualTo("user edit");
        verify(healthRecordRepository, never()).save(record);
    }

    @Test
    @DisplayName("confirmRecord cho phep ocr_failed khi keepPartial=true")
    void confirmRecord_allowOcrFailedWhenKeepPartial() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("ocr_failed");
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .keepPartial(true)
                .metrics(List.of(MetricDto.builder().name("Glucose").value("5.6").unit("mmol/L").build()))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        assertThat(record.getSourceType()).isEqualTo("ocr_partial");
    }

    @Test
    @DisplayName("confirmRecord cho phep ocr_failed manual va set source_type=manual")
    void confirmRecord_allowOcrFailedManualRecovery() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("ocr_failed");
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .keepPartial(false)
                .metrics(List.of(MetricDto.builder().name("Glucose").value("5.6").unit("mmol/L").build()))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        assertThat(record.getSourceType()).isEqualTo("manual");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord cho phep cap nhat khi da o trang thai done")
    void confirmRecord_updateWhenDone() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("done");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .hospitalName("BV Moi")
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        assertThat(record.getHospitalName()).isEqualTo("BV Moi");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord set source_type ocr_partial khi keepPartial=true")
    void confirmRecord_setsSourceTypePartialWhenKeepPartialRequested() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto mediumMetric = MetricDto.builder()
                .name("Glucose")
                .value("6.1")
                .unit("mmol/L")
                .confidenceLevel("medium")
                .build();
        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .keepPartial(true)
                .metrics(List.of(mediumMetric))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("ocr_partial");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord tinh lai source_type khi khong yeu cau keepPartial")
    void confirmRecord_recomputesSourceTypeWhenNotKeepPartial() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("ocr_partial");
        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto highMetric = MetricDto.builder()
                .name("Glucose")
                .value("6.1")
                .unit("mmol/L")
                .confidenceLevel("high")
                .source("manual")
                .build();
        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(List.of(highMetric))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("manual");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord fail neu user_id khong khop")
    void confirmRecord_profileMismatch() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(otherUserId);
        record.setStatus("review_required");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.confirmRecord(userId, recordId, new ConfirmRecordRequest()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("quyền xác nhận");
    }

    @Test
    @DisplayName("getMetricExplanation tra ve explanation va source tu LLM service")
    void getMetricExplanation_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .normalizedValue("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        UUID profileId = UUID.randomUUID();
        Profile profile = buildProfile(userId, profileId);
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                any(MetricExplanationRetrievalService.RetrievalContext.class)))
                .thenReturn(new MetricExplanationRetrievalService.RetrievalResult(
                        "Metric identity: ...\nClinical relation: ...\nOut-of-range impact: ...",
                        "qdrant",
                        true,
                        0.91
                ));
        when(llmService.generateExplanationResult(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                nullable(String.class)))
                .thenReturn(new LlmService.ExplanationResult("Giải thích đơn giản", "llm"));

        MetricExplanationResponse response = healthRecordService.getMetricExplanation(userId, recordId, "Glucose");

        assertThat(response.explanation()).isEqualTo("Giải thích đơn giản");
        assertThat(response.source()).isEqualTo("llm");
        assertThat(response.retrievalTrace().source()).isEqualTo("qdrant");
        ArgumentCaptor<MetricExplanationRetrievalService.RetrievalContext> contextCaptor =
                ArgumentCaptor.forClass(MetricExplanationRetrievalService.RetrievalContext.class);
        verify(metricExplanationRetrievalService).retrieve(
                eq("Glucose"),
                eq("normal"),
                any(ReferenceRangeDto.class),
                eq("vi"),
                contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue().profileContextSnippet()).contains("\"accessScope\":\"owner\"");
    }

    @Test
    @DisplayName("getMetricExplanation trả citation metadata và lưu link answer-record-metric không chứa raw snapshot")
    void getMetricExplanation_onlineRagCitationPersistsMetadataOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        Profile profile = buildProfile(userId, profileId);
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        MetricExplanationRetrievalService.OnlineCitationMetadata citation =
                new MetricExplanationRetrievalService.OnlineCitationMetadata(
                        snapshotId,
                        "https://who.int/news/item/glucose",
                        "WHO",
                        Instant.parse("2026-05-19T08:00:00Z"),
                        "c".repeat(64),
                        OnlineRagReviewStatus.REVIEW_REQUIRED,
                        true,
                        false,
                        false,
                        true,
                        false,
                        false
                );

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                any(MetricExplanationRetrievalService.RetrievalContext.class)))
                .thenReturn(new MetricExplanationRetrievalService.RetrievalResult(
                        "Metric identity: ...",
                        new MetricExplanationRetrievalService.RetrievalTrace("qdrant", true, 0.91, "none"),
                        List.of(citation)
                ));
        when(llmService.generateExplanationResult(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                nullable(String.class)))
                .thenReturn(new LlmService.ExplanationResult("Giải thích đơn giản", "llm"));

        MetricExplanationResponse response = healthRecordService.getMetricExplanation(userId, recordId, "Glucose");

        assertThat(response.onlineRagCitations()).hasSize(1);
        assertThat(response.onlineRagCitations().get(0).sourceSnapshotId()).isEqualTo(snapshotId);
        assertThat(response.onlineRagCitations().get(0).sourceUrl()).isEqualTo("https://who.int/news/item/glucose");
        assertThat(response.onlineRagCitations().get(0).reviewStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(response.onlineRagCitations().get(0).excluded()).isTrue();
        assertThat(response.onlineRagCitations().get(0).cacheHit()).isFalse();

        ArgumentCaptor<OnlineRagAnswerCitation> citationCaptor =
                ArgumentCaptor.forClass(OnlineRagAnswerCitation.class);
        verify(onlineRagAnswerCitationRepository).saveAndFlush(citationCaptor.capture());
        OnlineRagAnswerCitation saved = citationCaptor.getValue();
        assertThat(saved.getHealthRecordId()).isEqualTo(recordId);
        assertThat(saved.getMetricName()).isEqualTo("Glucose");
        assertThat(saved.getSourceSnapshotId()).isEqualTo(snapshotId);
        assertThat(saved.getSnapshotHash()).isEqualTo("c".repeat(64));
        assertThat(saved.getReviewStatus()).isEqualTo(OnlineRagReviewStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("getMetricExplanation không lưu trùng citation nếu answer/source đã tồn tại")
    void getMetricExplanation_onlineRagCitationSkipsDuplicatePersistence() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        MetricExplanationRetrievalService.OnlineCitationMetadata citation =
                new MetricExplanationRetrievalService.OnlineCitationMetadata(
                        UUID.randomUUID(),
                        "https://who.int/news/item/glucose",
                        "WHO",
                        Instant.parse("2026-05-19T08:00:00Z"),
                        "d".repeat(64),
                        OnlineRagReviewStatus.APPROVED,
                        false,
                        true,
                        true,
                        false,
                        false,
                        false
                );

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(buildProfile(userId, profileId)));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                any(MetricExplanationRetrievalService.RetrievalContext.class)))
                .thenReturn(new MetricExplanationRetrievalService.RetrievalResult(
                        "Metric identity: ...",
                        new MetricExplanationRetrievalService.RetrievalTrace("qdrant", true, 0.91, "none"),
                        List.of(citation)
                ));
        when(llmService.generateExplanationResult(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                nullable(String.class)))
                .thenReturn(new LlmService.ExplanationResult("Giải thích đơn giản", "llm"));
        when(onlineRagAnswerCitationRepository.existsByHealthRecordIdAndMetricNameAndAnswerHashAndSourceUrlAndSnapshotHash(
                eq(recordId),
                eq("Glucose"),
                anyString(),
                eq("https://who.int/news/item/glucose"),
                eq("d".repeat(64))
        )).thenReturn(true);

        healthRecordService.getMetricExplanation(userId, recordId, "Glucose");

        verify(onlineRagAnswerCitationRepository, never()).saveAndFlush(any(OnlineRagAnswerCitation.class));
    }

    @Test
    @DisplayName("getMetricExplanation bỏ qua duplicate-key race khi lưu citation")
    void getMetricExplanation_onlineRagCitationDuplicateRaceDoesNotFailExplanation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        MetricExplanationRetrievalService.OnlineCitationMetadata citation =
                new MetricExplanationRetrievalService.OnlineCitationMetadata(
                        UUID.randomUUID(),
                        "https://who.int/news/item/glucose",
                        "WHO",
                        Instant.parse("2026-05-19T08:00:00Z"),
                        "e".repeat(64),
                        OnlineRagReviewStatus.APPROVED,
                        false,
                        true,
                        true,
                        false,
                        false,
                        false
                );

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(buildProfile(userId, profileId)));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                any(MetricExplanationRetrievalService.RetrievalContext.class)))
                .thenReturn(new MetricExplanationRetrievalService.RetrievalResult(
                        "Metric identity: ...",
                        new MetricExplanationRetrievalService.RetrievalTrace("qdrant", true, 0.91, "none"),
                        List.of(citation)
                ));
        when(llmService.generateExplanationResult(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                nullable(String.class)))
                .thenReturn(new LlmService.ExplanationResult("Giải thích đơn giản", "llm"));
        when(onlineRagAnswerCitationRepository.saveAndFlush(any(OnlineRagAnswerCitation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        MetricExplanationResponse response = healthRecordService.getMetricExplanation(userId, recordId, "Glucose");

        assertThat(response.explanation()).isEqualTo("Giải thích đơn giản");
        assertThat(response.onlineRagCitations()).hasSize(1);
    }

    @Test
    @DisplayName("getMetricExplanation record-level share không đưa profile context và không yêu cầu consent")
    void getMetricExplanation_recordLevelShare_omitsProfileContextAndConsent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();
        HealthRecord record = newOwnedRecord(ownerId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        HealthRecordShare share = new HealthRecordShare();
        share.setHealthRecordId(recordId);
        share.setProfileId(profileId);
        share.setOwnerId(ownerId);
        share.setViewerId(viewerId);
        share.setAccessLevel("view");

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, viewerId)).thenReturn(Optional.empty());
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId)).thenReturn(false);
        when(healthRecordShareRepository.existsByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, viewerId)).thenReturn(true);
        when(healthRecordShareRepository.findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, viewerId)).thenReturn(Optional.of(share));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                any(MetricExplanationRetrievalService.RetrievalContext.class)))
                .thenReturn(new MetricExplanationRetrievalService.RetrievalResult(
                        "Metric identity: ...",
                        "reference-data",
                        false,
                        0.0
                ));
        when(llmService.generateExplanationResult(
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class),
                nullable(String.class)))
                .thenReturn(new LlmService.ExplanationResult("Giải thích đơn giản", "llm"));

        healthRecordService.getMetricExplanation(viewerId, recordId, "Glucose");

        ArgumentCaptor<MetricExplanationRetrievalService.RetrievalContext> contextCaptor =
                ArgumentCaptor.forClass(MetricExplanationRetrievalService.RetrievalContext.class);
        verify(metricExplanationRetrievalService).retrieve(
                eq("Glucose"),
                eq("normal"),
                any(ReferenceRangeDto.class),
                eq("vi"),
                contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue().profileContextAllowed()).isFalse();
        assertThat(contextCaptor.getValue().profileContextSnippet()).isNull();
        verify(consentService, never()).hasConsent(eq(viewerId), anyString());
        verify(profileRepository, never()).findById(profileId);
    }

    @Test
    @DisplayName("getMetricExplanation chặn profile context khi user chưa consent")
    void getMetricExplanation_withoutConsent_blocksBeforeRetrieval() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.6")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(consentService.hasConsent(eq(userId), anyString())).thenReturn(false);

        assertThatThrownBy(() -> healthRecordService.getMetricExplanation(userId, recordId, "Glucose"))
                .isInstanceOf(ConsentRequiredException.class)
                .hasMessageContaining("đồng thuận");

        verify(metricExplanationRetrievalService, never()).retrieve(
                anyString(),
                anyString(),
                any(),
                anyString(),
                any(MetricExplanationRetrievalService.RetrievalContext.class)
        );
        verify(llmService, never()).generateExplanationResult(
                anyString(),
                anyString(),
                anyString(),
                any(),
                anyString(),
                nullable(String.class)
        );
    }

    @Test
    @DisplayName("getMetricExplanation fail khi record không tồn tại")
    void getMetricExplanation_recordNotFound() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.getMetricExplanation(userId, recordId, "Glucose"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Kết quả khám không tồn tại");
    }

    @Test
    @DisplayName("getMetricExplanation fail khi metric không tồn tại trong record")
    void getMetricExplanation_metricNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder().name("HbA1c").build();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.getMetricExplanation(userId, recordId, "Glucose"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy chỉ số");
    }

    @Test
    @DisplayName("getProfileHistory tra ve du lieu phan trang va summary fields")
    void getProfileHistory_returnsPaginatedSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile profile = buildProfile(userId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        MetricDto abnormalMetric = MetricDto.builder()
                .name("Glucose")
                .value("8.1")
                .normalizedValue("8.1")
                .unit("mmol/L")
                .status("abnormal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        HealthRecord record = new HealthRecord();
        record.setId(UUID.randomUUID());
        record.setUserId(userId);
        record.setProfileId(profileId);
        record.setRecordType("Xét nghiệm máu");
        record.setSourceType("ocr");
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(abnormalMetric)));
        record.setCreatedAt(java.time.Instant.now());
        record.setExamDate(LocalDate.of(2026, 4, 1));

        when(healthRecordRepository.findAllByProfileIdAndUserIdAndDeletedAtIsNull(eq(profileId), eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(record), PageRequest.of(0, 20), 1));

        var result = healthRecordService.getProfileHistory(userId, profileId, 0, 20);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().getFirst().overallStatus()).isEqualTo("abnormal");
        assertThat(result.data().getFirst().abnormalCount()).isEqualTo(1);
        assertThat(result.pagination().page()).isEqualTo(0);
        assertThat(result.pagination().limit()).isEqualTo(20);
        assertThat(result.pagination().total()).isEqualTo(1);
    }

    @Test
    @DisplayName("getProfileHistory chan truy cap profile khong thuoc user")
    void getProfileHistory_forbiddenWhenNotOwner() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Profile profile = buildProfile(otherUserId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, userId))
                .thenReturn(false);

        assertThatThrownBy(() -> healthRecordService.getProfileHistory(userId, profileId, 0, 20))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("không thuộc về người dùng");
    }

    @Test
    @DisplayName("getProfileHistory cho phep viewer duoc share profile")
    void getProfileHistory_allowsSharedViewer() {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile profile = buildProfile(ownerId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId))
                .thenReturn(true);
        when(healthRecordRepository.findAllByProfileIdAndUserIdAndDeletedAtIsNull(eq(profileId), eq(ownerId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var result = healthRecordService.getProfileHistory(viewerId, profileId, 0, 20);

        assertThat(result.data()).isEmpty();
        verify(healthRecordRepository).findAllByProfileIdAndUserIdAndDeletedAtIsNull(eq(profileId), eq(ownerId), any(PageRequest.class));
    }

    @Test
    @DisplayName("getProfileHistory gioi han page size toi da 20")
    void getProfileHistory_clampsLimitToTwenty() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile profile = buildProfile(userId, profileId);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(healthRecordRepository.findAllByProfileIdAndUserIdAndDeletedAtIsNull(eq(profileId), eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        healthRecordService.getProfileHistory(userId, profileId, 0, 200);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(healthRecordRepository).findAllByProfileIdAndUserIdAndDeletedAtIsNull(eq(profileId), eq(userId), pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("updateMetrics cap nhat metrics va dat source_type = ocr khi tat ca la ocr")
    void updateMetrics_allOcr_sourceTypeOcr() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto m1 = new MetricDto();
        m1.setName("Glucose");
        m1.setValue("5.4");
        m1.setUnit("mmol/L");
        m1.setSource("ocr");

        MetricDto m2 = new MetricDto();
        m2.setName("HbA1c");
        m2.setValue("6.1");
        m2.setUnit("%");
        m2.setSource("ocr");

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of(m1, m2));
        healthRecordService.updateMetrics(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("ocr");
        assertThat(record.getMetrics()).contains("Glucose");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("updateMetrics coi ocr_regex_fallback la OCR khi tinh source_type")
    void updateMetrics_ocrRegexFallback_sourceTypeOcr() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("manual");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto metric = new MetricDto();
        metric.setName("Glucose");
        metric.setValue("5.4");
        metric.setUnit("mmol/L");
        metric.setSource("ocr_regex_fallback");

        healthRecordService.updateMetrics(userId, recordId, new UpdateMetricsRequest(List.of(metric)));

        assertThat(record.getSourceType()).isEqualTo("ocr");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("updateMetrics dat source_type = manual khi tat ca metrics la manual")
    void updateMetrics_allManual_sourceTypeManual() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("done");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto m1 = new MetricDto();
        m1.setName("Glucose");
        m1.setValue("5.4");
        m1.setUnit("mmol/L");
        m1.setSource("manual");

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of(m1));
        healthRecordService.updateMetrics(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("manual");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("updateMetrics dat source_type = mixed khi co ca ocr va manual")
    void updateMetrics_mixed_sourceTypeMixed() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto ocrMetric = new MetricDto();
        ocrMetric.setName("Glucose");
        ocrMetric.setValue("5.4");
        ocrMetric.setUnit("mmol/L");
        ocrMetric.setSource("ocr");

        MetricDto manualMetric = new MetricDto();
        manualMetric.setName("HbA1c");
        manualMetric.setValue("6.1");
        manualMetric.setUnit("%");
        manualMetric.setSource("manual");

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of(ocrMetric, manualMetric));
        healthRecordService.updateMetrics(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("mixed");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("updateMetrics that bai khi user khong phai chu so huu record")
    void updateMetrics_wrongUser_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(otherUserId);
        record.setStatus("review_required");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of());

        assertThatThrownBy(() -> healthRecordService.updateMetrics(userId, recordId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("quyền cập nhật");
    }

    @Test
    @DisplayName("updateMetrics that bai khi status khong hop le")
    void updateMetrics_invalidStatus_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("processing");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of());

        assertThatThrownBy(() -> healthRecordService.updateMetrics(userId, recordId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không ở trạng thái cho phép cập nhật");
    }

    @Test
    @DisplayName("confirmRecord tinh toan source_type = mixed khi co ca ocr va manual metrics")
    void confirmRecord_withMixedMetrics_sourceTypeMixed() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        MetricDto ocrMetric = new MetricDto();
        ocrMetric.setName("Glucose");
        ocrMetric.setValue("5.4");
        ocrMetric.setUnit("mmol/L");
        ocrMetric.setSource("ocr");

        MetricDto manualMetric = new MetricDto();
        manualMetric.setName("HbA1c");
        manualMetric.setValue("6.1");
        manualMetric.setUnit("%");
        manualMetric.setSource("manual");

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(List.of(ocrMetric, manualMetric))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        assertThat(record.getSourceType()).isEqualTo("mixed");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord that bai khi metrics co phan tu null")
    void confirmRecord_nullMetric_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(Arrays.asList((MetricDto) null))
                .build();

        assertThatThrownBy(() -> healthRecordService.confirmRecord(userId, recordId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được null");
        verify(healthRecordRepository, never()).save(record);
    }

    @Test
    @DisplayName("confirmRecord chap nhan gia tri dang chu")
    void confirmRecord_textualMetricValue_success() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setProfileId(UUID.randomUUID());
        record.setStatus("review_required");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(List.of(MetricDto.builder()
                        .name("HBsAg")
                        .value("Negative")
                        .unit("mg/L")
                        .source("manual")
                        .build()))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getStatus()).isEqualTo("done");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("confirmRecord bao loi don vi bang ten chi so")
    void confirmRecord_missingUnit_usesMetricNameInMessage() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setStatus("review_required");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(List.of(MetricDto.builder()
                        .name("HBsAg")
                        .value("Negative")
                        .unit("")
                        .build()))
                .build();

        assertThatThrownBy(() -> healthRecordService.confirmRecord(userId, recordId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ số \"HBsAg\": đơn vị không được để trống");
        verify(healthRecordRepository, never()).save(record);
    }

    @Test
    @DisplayName("updateMetrics giu nguyen source_type hien tai khi danh sach metrics rong")
    void updateMetrics_emptyList_preservesCurrentSourceType() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("done");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(redisTemplate.delete(any(String.class))).thenReturn(true);

        UpdateMetricsRequest request = new UpdateMetricsRequest(java.util.Collections.emptyList());
        healthRecordService.updateMetrics(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("ocr");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("updateMetrics dat source_type = mixed khi co metric voi source = null (khong xac dinh)")
    void updateMetrics_nullSource_treatedAsNonOcr() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setStatus("review_required");
        record.setSourceType("ocr");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(redisTemplate.delete(any(String.class))).thenReturn(true);

        MetricDto ocrMetric = new MetricDto();
        ocrMetric.setName("Glucose");
        ocrMetric.setValue("5.4");
        ocrMetric.setUnit("mmol/L");
        ocrMetric.setSource("ocr");

        MetricDto nullSourceMetric = new MetricDto();
        nullSourceMetric.setName("HbA1c");
        nullSourceMetric.setValue("6.1");
        nullSourceMetric.setUnit("%");
        nullSourceMetric.setSource(null);

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of(ocrMetric, nullSourceMetric));
        healthRecordService.updateMetrics(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("mixed");
        verify(healthRecordRepository).save(record);
    }

    @Test
    @DisplayName("deleteHealthRecord soft delete thành công")
    void deleteHealthRecord_success() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = newOwnedRecord(userId, recordId);

        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));

        healthRecordService.deleteHealthRecord(userId, recordId);

        assertThat(record.getDeletedAt()).isNotNull();
        verify(healthRecordRepository).save(record);
        verify(unifiedAuditCoordinator).persist(
                eq(userId),
                eq(com.healthlens.api.audit.AuditActions.DELETE_HEALTH_RECORD),
                eq(com.healthlens.api.audit.AuditResourceTypes.HEALTH_RECORD),
                any()
        );
        verify(healthRecordLegacyAuditWriter).record(userId, com.healthlens.api.audit.AuditActions.DELETE_HEALTH_RECORD, recordId);
    }

    @Test
    @DisplayName("deleteHealthRecord từ user khác thì trả not found")
    void deleteHealthRecord_notFoundWhenNotOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(ownerId);

        record.setProfileId(ownerId);
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(ownerId, requesterId, "edit"))
                .thenReturn(false);

        assertThatThrownBy(() -> healthRecordService.deleteHealthRecord(requesterId, recordId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("quyền xóa");
    }

    @Test
    @DisplayName("downloadHealthRecordPdf owner tạo PDF và ghi audit")
    void downloadHealthRecordPdf_ownerSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Profile profile = buildProfile(userId, profileId);
        profile.setDisplayName("Nguyễn Văn A");
        HealthRecord record = buildDoneRecord(userId, profileId, recordId);
        record.setRecordType("Xét nghiệm máu");

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(referenceDataService.classifyMetricWithoutAudit(anyString(), anyString(), eq(profile), eq(LocalDate.of(2026, 5, 15))))
                .thenReturn(new com.healthlens.api.dto.MetricClassificationDto("normal", null, "Glucose", null));
        when(healthRecordPdfService.generatePdf(any())).thenReturn("%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        DownloadHealthRecordPdfResponse response = healthRecordService.downloadHealthRecordPdf(userId, recordId);

        assertThat(response.bytes()).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.filename()).isEqualTo("healthlens-ket-qua-xet-nghiem-mau.pdf");
        verify(healthRecordPdfService).generatePdf(any());
        verify(healthRecordLegacyAuditWriter).recordPdfDownload(eq(userId), eq(record), eq("owner"));
        verify(metricExplanationRetrievalService, never()).retrieve(anyString(), anyString(), any(), anyString());
        verify(llmService, never()).generateExplanationResult(anyString(), anyString(), anyString(), any(), anyString(), nullable(String.class));
    }

    @Test
    @DisplayName("downloadHealthRecordPdf record-level viewer được phép tải")
    void downloadHealthRecordPdf_recordShareViewerSuccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Profile profile = buildProfile(ownerId, profileId);
        HealthRecord record = buildDoneRecord(ownerId, profileId, recordId);
        com.healthlens.api.entity.HealthRecordShare share = new com.healthlens.api.entity.HealthRecordShare();
        share.setHealthRecordId(recordId);
        share.setProfileId(profileId);
        share.setOwnerId(ownerId);
        share.setViewerId(viewerId);
        share.setAccessLevel("view");

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, viewerId)).thenReturn(Optional.empty());
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId)).thenReturn(false);
        when(healthRecordShareRepository.existsByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, viewerId)).thenReturn(true);
        when(healthRecordShareRepository.findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, viewerId)).thenReturn(Optional.of(share));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(referenceDataService.classifyMetricWithoutAudit(anyString(), anyString(), eq(profile), eq(LocalDate.of(2026, 5, 15))))
                .thenReturn(new com.healthlens.api.dto.MetricClassificationDto("normal", null, "Glucose", null));
        when(healthRecordPdfService.generatePdf(any())).thenReturn("%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        DownloadHealthRecordPdfResponse response = healthRecordService.downloadHealthRecordPdf(viewerId, recordId);

        assertThat(response.bytes()).isNotEmpty();
        verify(healthRecordLegacyAuditWriter).recordPdfDownload(eq(viewerId), eq(record), anyString());
    }

    @Test
    @DisplayName("downloadHealthRecordPdf profile shared viewer được phép tải")
    void downloadHealthRecordPdf_profileShareViewerSuccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        Profile profile = buildProfile(ownerId, profileId);
        HealthRecord record = buildDoneRecord(ownerId, profileId, recordId);

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, viewerId)).thenReturn(Optional.empty());
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId)).thenReturn(true);
        when(profileShareRepository.existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(profileId, viewerId, "edit"))
                .thenReturn(false);
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(referenceDataService.classifyMetricWithoutAudit(anyString(), anyString(), eq(profile), eq(LocalDate.of(2026, 5, 15))))
                .thenReturn(new com.healthlens.api.dto.MetricClassificationDto("normal", null, "Glucose", null));
        when(healthRecordPdfService.generatePdf(any())).thenReturn("%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        DownloadHealthRecordPdfResponse response = healthRecordService.downloadHealthRecordPdf(viewerId, recordId);

        assertThat(response.bytes()).isNotEmpty();
        verify(healthRecordLegacyAuditWriter).recordPdfDownload(eq(viewerId), eq(record), eq("profile"));
    }

    @Test
    @DisplayName("downloadHealthRecordPdf không tạo PDF khi không có quyền")
    void downloadHealthRecordPdf_forbiddenDoesNotGeneratePdf() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = buildDoneRecord(ownerId, profileId, recordId);

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, requesterId)).thenReturn(Optional.empty());
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.of(record));
        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, requesterId)).thenReturn(false);
        when(healthRecordShareRepository.existsByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, requesterId)).thenReturn(false);

        assertThatThrownBy(() -> healthRecordService.downloadHealthRecordPdf(requesterId, recordId))
                .isInstanceOf(com.healthlens.api.exception.ProfileAccessRevokedException.class);
        verify(healthRecordPdfService, never()).generatePdf(any());
        verify(healthRecordLegacyAuditWriter, never()).recordPdfDownload(any(), any(), any());
        verify(healthRecordLegacyAuditWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("downloadHealthRecordPdf không tạo PDF khi record đã soft-delete")
    void downloadHealthRecordPdf_softDeletedDoesNotGeneratePdf() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.empty());
        when(healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.downloadHealthRecordPdf(userId, recordId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(healthRecordPdfService, never()).generatePdf(any());
        verify(healthRecordLegacyAuditWriter, never()).recordPdfDownload(any(), any(), any());
        verify(healthRecordLegacyAuditWriter, never()).record(any(), any(), any());
    }

    @Test
    @DisplayName("getRecommendations tra ve thong diep khich le khi tat ca chi so normal")
    void getRecommendations_allNormal_returnsPositiveMessage() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        Profile profile = buildProfile(userId, profileId);
        profile.setBirthDate(LocalDate.of(1995, 1, 1));
        profile.setGender("male");

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.4")
                .normalizedValue("5.4")
                .status("normal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        RecommendationsResponse response = healthRecordService.getRecommendations(userId, recordId);

        assertThat(response.allNormal()).isTrue();
        assertThat(response.recommendations()).hasSize(1);
        verify(llmService, never()).generateRecommendationsResult(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getRecommendations goi LLM khi co chi so attention hoac abnormal")
    void getRecommendations_withRiskyMetrics_callsLlm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        Profile profile = buildProfile(userId, profileId);
        profile.setBirthDate(LocalDate.of(1980, 1, 1));
        profile.setGender("female");

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("8.2")
                .normalizedValue("8.2")
                .status("abnormal")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(llmService.generateRecommendationsResult(any(), any(), any(), any()))
                .thenReturn(new LlmService.RecommendationResult(
                        List.of("Giam duong trong bua an", "Tap the duc deu dan"),
                        "llm",
                        "v8-medical-disclaimer-vi",
                        "qwen-test"
                ));

        RecommendationsResponse response = healthRecordService.getRecommendations(userId, recordId);

        assertThat(response.allNormal()).isFalse();
        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.promptVersion()).isEqualTo("v8-medical-disclaimer-vi");
        assertThat(response.modelVersion()).isEqualTo("qwen-test");
        verify(llmService).generateRecommendationsResult(any(), any(), eq("female"), any());
    }

    @Test
    @DisplayName("getRecommendations goi LLM khi phiếu luu attention nhung sau enrich thanh normal (tranh luon allNormal)")
    void getRecommendations_priorAttentionMismatch_stillCallsLlm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        Profile profile = buildProfile(userId, profileId);
        profile.setBirthDate(LocalDate.of(1980, 1, 1));
        profile.setGender("female");

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.4")
                .normalizedValue("5.4")
                .status("attention")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(llmService.generateRecommendationsResult(any(), any(), any(), any()))
                .thenReturn(new LlmService.RecommendationResult(
                        List.of("Goi y 1", "Goi y 2"),
                        "llm",
                        "v8-medical-disclaimer-vi",
                        "qwen-test"
                ));

        RecommendationsResponse response = healthRecordService.getRecommendations(userId, recordId);

        assertThat(response.allNormal()).isFalse();
        verify(llmService).generateRecommendationsResult(any(), any(), eq("female"), any());
    }

    @Test
    @DisplayName("getRecommendations goi LLM khi phiếu luu warning nhung sau enrich thanh normal")
    void getRecommendations_priorWarningMismatch_stillCallsLlm() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        Profile profile = buildProfile(userId, profileId);
        profile.setBirthDate(LocalDate.of(1980, 1, 1));
        profile.setGender("female");

        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.4")
                .normalizedValue("5.4")
                .status("warning")
                .referenceRange(new ReferenceRangeDto(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(6.4),
                        BigDecimal.valueOf(3.2),
                        BigDecimal.valueOf(7.1),
                        "mmol/L"
                ))
                .build();

        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserIdAndDeletedAtIsNull(recordId, userId)).thenReturn(Optional.of(record));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(llmService.generateRecommendationsResult(any(), any(), any(), any()))
                .thenReturn(new LlmService.RecommendationResult(
                        List.of("Goi y 1", "Goi y 2"),
                        "llm",
                        "v8-medical-disclaimer-vi",
                        "qwen-test"
                ));

        RecommendationsResponse response = healthRecordService.getRecommendations(userId, recordId);

        assertThat(response.allNormal()).isFalse();
        verify(llmService).generateRecommendationsResult(any(), any(), eq("female"), any());
    }

    private HealthRecord newOwnedRecord(UUID userId, UUID recordId) {
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setProfileId(UUID.randomUUID());
        return record;
    }

    private Profile buildProfile(UUID userId, UUID profileId) {
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setUser(user);
        return profile;
    }

    private HealthRecord buildDoneRecord(UUID userId, UUID profileId, UUID recordId) throws Exception {
        MetricDto metric = MetricDto.builder()
                .name("Glucose")
                .value("5.4")
                .unit("mmol/L")
                .status("normal")
                .build();
        HealthRecord record = newOwnedRecord(userId, recordId);
        record.setProfileId(profileId);
        record.setStatus("done");
        record.setSourceType("ocr");
        record.setFileKey("health-records/%s/%s/%s/original.pdf".formatted(userId, profileId, recordId));
        record.setExamDate(LocalDate.of(2026, 5, 15));
        record.setRecordType("Xét nghiệm máu");
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        return record;
    }
}
