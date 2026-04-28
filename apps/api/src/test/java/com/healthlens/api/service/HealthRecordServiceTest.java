package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.dto.request.UpdateMetricsRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRepository profileRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private ReferenceDataService referenceDataService;
    @Mock private MetricExplanationRetrievalService metricExplanationRetrievalService;
    @Mock private LlmService llmService;
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
                referenceDataService,
                metricExplanationRetrievalService,
                llmService,
                redisTemplate,
                new ObjectMapper(),
                "ocr.events"
        );
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
                .hasMessageContaining("retry upload");
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
                "fileKey", fileKey
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
        assertThat(payload).containsKeys("jobId", "recordId", "fileKey", "profileId");
        assertThat(payload.get("recordId")).isEqualTo(recordId.toString());
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
                .hasMessageContaining("Upload session");
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
        when(healthRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> healthRecordService.confirmUpload(userId, recordId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retry OCR that bai");
    }

    @Test
    @DisplayName("confirmRecord thanh cong - update status thanh done")
    void confirmRecord_success() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setStatus("processing"); // invalid status

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.confirmRecord(userId, recordId, new ConfirmRecordRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("khong o trang thai cho phep cap nhat");
    }

    @Test
    @DisplayName("confirmRecord cho phep ocr_failed khi keepPartial=true")
    void confirmRecord_allowOcrFailedWhenKeepPartial() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quyen xac nhan");
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

        when(healthRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(record));
        when(metricExplanationRetrievalService.retrieve(
                nullable(String.class),
                nullable(String.class),
                any(ReferenceRangeDto.class),
                nullable(String.class)))
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
        verify(metricExplanationRetrievalService).retrieve(
                eq("Glucose"),
                eq("normal"),
                any(ReferenceRangeDto.class),
                eq("vi")
        );
    }

    @Test
    @DisplayName("getMetricExplanation fail khi record không tồn tại")
    void getMetricExplanation_recordNotFound() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        when(healthRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.getMetricExplanation(userId, recordId, "Glucose"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Health record khong ton tai");
    }

    @Test
    @DisplayName("getMetricExplanation fail khi metric không tồn tại trong record")
    void getMetricExplanation_metricNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        MetricDto metric = MetricDto.builder().name("HbA1c").build();
        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));
        when(healthRecordRepository.findByIdAndUserId(recordId, userId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> healthRecordService.getMetricExplanation(userId, recordId, "Glucose"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khong tim thay chi so");
    }

    @Test
    @DisplayName("updateMetrics cap nhat metrics va dat source_type = ocr khi tat ca la ocr")
    void updateMetrics_allOcr_sourceTypeOcr() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quyen cap nhat");
    }

    @Test
    @DisplayName("updateMetrics that bai khi status khong hop le")
    void updateMetrics_invalidStatus_throwsException() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
        record.setStatus("processing");

        when(healthRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        UpdateMetricsRequest request = new UpdateMetricsRequest(List.of());

        assertThatThrownBy(() -> healthRecordService.updateMetrics(userId, recordId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("khong o trang thai cho phep cap nhat");
    }

    @Test
    @DisplayName("confirmRecord tinh toan source_type = mixed khi co ca ocr va manual metrics")
    void confirmRecord_withMixedMetrics_sourceTypeMixed() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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
    @DisplayName("updateMetrics giu nguyen source_type hien tai khi danh sach metrics rong")
    void updateMetrics_emptyList_preservesCurrentSourceType() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

        HealthRecord record = new HealthRecord();
        record.setId(recordId);
        record.setUserId(userId);
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

    private Profile buildProfile(UUID userId, UUID profileId) {
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setUser(user);
        return profile;
    }
}
