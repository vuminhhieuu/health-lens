package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.dto.MetricDto;
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
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRepository profileRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private ReferenceDataService referenceDataService;
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

        UploadUrlResponse response = healthRecordService.createUploadUrl(userId, new CreateUploadUrlRequest(profileId, "pdf"));

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

        UploadUrlResponse response = healthRecordService.createUploadUrl(userId, new CreateUploadUrlRequest(profileId, "image/png"));

        assertThat(response.uploadUrl()).isEqualTo("https://signed-upload-url-png");
        assertThat(response.fileKey()).endsWith("/original.png");
        verify(valueOperations).set(any(), any(), any(Duration.class));
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
    @DisplayName("confirmRecord khong doi source_type khi khong yeu cau keepPartial")
    void confirmRecord_keepsSourceTypeWhenNotKeepPartial() {
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
                .build();
        ConfirmRecordRequest request = ConfirmRecordRequest.builder()
                .metrics(List.of(highMetric))
                .build();

        healthRecordService.confirmRecord(userId, recordId, request);

        assertThat(record.getSourceType()).isEqualTo("ocr_partial");
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

    private Profile buildProfile(UUID userId, UUID profileId) {
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setUser(user);
        return profile;
    }
}
