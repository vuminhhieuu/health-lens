package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
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
import java.util.Map;
import java.util.UUID;

@Service
public class HealthRecordService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration UPLOAD_RESERVATION_TTL = Duration.ofMinutes(20);
    private static final String STATUS_PROCESSING = "processing";

    private final StorageService storageService;
    private final ProfileRepository profileRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String ocrStreamName;

    public HealthRecordService(
            StorageService storageService,
            ProfileRepository profileRepository,
            HealthRecordRepository healthRecordRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.stream.ocr-events:ocr.events}") String ocrStreamName
    ) {
        this.storageService = storageService;
        this.profileRepository = profileRepository;
        this.healthRecordRepository = healthRecordRepository;
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
        HealthRecord record = healthRecordRepository.findByIdAndUserId(recordId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        return new HealthRecordStatusResponse(record.getId(), record.getStatus());
    }

    @Transactional
    public void markOcrCompleted(UUID recordId, String rawOcrJson) {
        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Health record khong ton tai"));
        record.setStatus("review_required");
        record.setRawOcrResult(rawOcrJson);
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

    private String uploadReservationKey(UUID recordId) {
        return "health-record-upload:%s".formatted(recordId);
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
