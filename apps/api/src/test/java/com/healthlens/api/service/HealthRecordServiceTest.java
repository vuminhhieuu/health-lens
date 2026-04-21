package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    @Mock private StorageService storageService;
    @Mock private ProfileRepository profileRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
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

    private Profile buildProfile(UUID userId, UUID profileId) {
        User user = new User();
        user.setId(userId);
        Profile profile = new Profile();
        profile.setId(profileId);
        profile.setUser(user);
        return profile;
    }
}
