package com.healthlens.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.MetricDto;
import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.request.UpdateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.dto.response.SharedProfileResponse;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ProfileShare;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileLimitExceededException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final int MAX_PROFILES_PER_USER = 10;

    private final ProfileRepository profileRepository;
    private final ProfileShareRepository profileShareRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProfileService(
            ProfileRepository profileRepository,
            ProfileShareRepository profileShareRepository,
            HealthRecordRepository healthRecordRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.profileRepository = profileRepository;
        this.profileShareRepository = profileShareRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfiles(UUID userId) {
        return profileRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharedProfileResponse> getSharedProfiles(UUID userId) {
        List<ProfileShare> activeShares = profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(userId);
        Map<UUID, ProfileShare> uniqueSharesByProfileId = new LinkedHashMap<>();
        for (ProfileShare share : activeShares) {
            uniqueSharesByProfileId.putIfAbsent(share.getProfileId(), share);
        }
        List<UUID> profileIds = new ArrayList<>(uniqueSharesByProfileId.keySet());
        if (profileIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Profile> profilesById = profileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(Profile::getId, p -> p));
        Map<UUID, HealthRecord> latestRecordByProfileId = healthRecordRepository
            .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(profileIds)
            .stream()
            .collect(Collectors.toMap(HealthRecord::getProfileId, record -> record));

        List<SharedProfileResponse> responses = new ArrayList<>();
        for (ProfileShare share : uniqueSharesByProfileId.values()) {
            UUID profileId = share.getProfileId();
            Profile profile = profilesById.get(profileId);
            if (profile == null) {
                throw new ResourceNotFoundException("Không tìm thấy hồ sơ được chia sẻ");
            }
            HealthRecord latest = latestRecordByProfileId.get(profileId);
            String latestStatus = "unverified";
            Instant lastUpdated = profile.getUpdatedAt();
            if (latest != null) {
                latestStatus = resolveLatestSharedStatus(latest);
                lastUpdated = latest.getUpdatedAt();
            }
            responses.add(new SharedProfileResponse(
                    profile.getId(),
                    profile.getDisplayName(),
                    share.getAccessLevel(),
                    latestStatus,
                    lastUpdated,
                    profile.getLastRecordAt(),
                    profile.getBirthDate(),
                    profile.getGender(),
                    profile.getNotes()
            ));
        }
        return responses;
    }

    private String resolveLatestSharedStatus(HealthRecord latest) {
        List<MetricDto> metrics = parseMetrics(latest.getMetrics());
        String overallStatus = metrics.stream()
                .map(MetricDto::getStatus)
                .filter(status -> status != null && !status.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .max((left, right) -> Integer.compare(statusPriority(left), statusPriority(right)))
                .orElse(null);
        if (overallStatus != null) {
            return overallStatus;
        }
        if ("done".equals(latest.getStatus())) {
            return "normal";
        }
        if ("ocr_failed".equals(latest.getStatus()) || "failed".equals(latest.getStatus()) || "error".equals(latest.getStatus())) {
            return "error";
        }
        return "unverified";
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "abnormal" -> 3;
            case "attention", "warning" -> 2;
            case "normal" -> 1;
            default -> 0;
        };
    }

    private List<MetricDto> parseMetrics(String rawMetrics) {
        if (rawMetrics == null || rawMetrics.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawMetrics, new TypeReference<List<MetricDto>>() {
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * Nếu user chưa có hồ sơ nào trong DB, tạo một hồ sơ mặc định từ thông tin tài khoản.
     * Idempotent: đã có hồ sơ thì không tạo thêm.
     */
    @Transactional
    public ProfileResponse ensureDefaultProfile(UUID userId) {
        List<Profile> existing = profileRepository.findAllByUserId(userId);
        if (!existing.isEmpty()) {
            return mapToResponse(existing.getFirst());
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // Re-check after acquiring lock to reduce race conditions.
        existing = profileRepository.findAllByUserId(userId);
        if (!existing.isEmpty()) {
            return mapToResponse(existing.getFirst());
        }

        String displayName = user.getFullName() != null ? user.getFullName().trim() : "Hồ sơ của tôi";
        if (displayName.length() > 50) {
            displayName = displayName.substring(0, 50);
        }

        try {
            Profile profile = new Profile();
            profile.setUser(user);
            profile.setDisplayName(displayName);
            profile.setBirthDate(user.getBirthDate());
            profile.setGender(user.getGender());
            profile.setNotes(null);
            profile.setDefault(true);
            profile = profileRepository.save(profile);
            return mapToResponse(profile);
        } catch (DataIntegrityViolationException ex) {
            return profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                    .or(() -> profileRepository.findTopByUserIdOrderByCreatedAtAsc(userId))
                    .map(this::mapToResponse)
                    .orElseThrow(() -> ex);
        }
    }

    @Transactional
    public ProfileResponse createProfile(UUID userId, CreateProfileRequest request) {
        long currentCount = profileRepository.countByUserId(userId);
        if (currentCount >= MAX_PROFILES_PER_USER) {
            throw new ProfileLimitExceededException("Đã đạt giới hạn " + MAX_PROFILES_PER_USER + " hồ sơ");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setDisplayName(request.displayName().trim());
        profile.setBirthDate(request.birthDate());
        profile.setGender(request.gender());
        profile.setNotes(request.notes() != null ? request.notes().trim() : null);
        profile.setDefault(false);

        profile = profileRepository.save(profile);

        return mapToResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UUID profileId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));

        // Access check: Ensure user is owner OR has "edit" shared access
        UUID profileOwnerId = profile.getUser().getId();
        boolean isOwner = userId.equals(profileOwnerId);
        boolean hasEditAccess = profileShareRepository.existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(
                profileId, userId, "edit");
        
        if (!isOwner && !hasEditAccess) {
            throw new AccessDeniedException("Không có quyền chỉnh sửa hồ sơ này");
        }

        // Validate displayName with defensive programming:
        // DTO @NotBlank ensures non-null, but service validates after trimming
        // (covers case where user submits spaces-only input that passes DTO validation)
        // Note: String.length() counts Java chars, not grapheme clusters. Vietnamese text is safe.
        String normalizedDisplayName = request.displayName().trim();
        if (normalizedDisplayName.isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }
        if (normalizedDisplayName.length() > 100) {
            throw new IllegalArgumentException("Tên hiển thị tối đa 100 ký tự");
        }

        // Validate notes: optional field, max 500 chars
        String normalizedNotes = normalizeOptionalText(request.notes());
        if (normalizedNotes != null && normalizedNotes.length() > 500) {
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
        }

        profile.setDisplayName(normalizedDisplayName);
        profile.setBirthDate(request.birthDate());
        profile.setGender(normalizeOptionalText(request.gender()));
        profile.setNotes(normalizedNotes);
        
        // Sync with User entity if this is a default profile
        if (profile.isDefault()) {
            User user = profile.getUser();
            user.setFullName(normalizedDisplayName);
            user.setBirthDate(request.birthDate());
            user.setGender(normalizeOptionalText(request.gender()));
            userRepository.save(user);
        }

        Profile updatedProfile = profileRepository.save(profile);
        return mapToResponse(updatedProfile);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProfileResponse mapToResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getBirthDate(),
                profile.getGender(),
                profile.getNotes(),
                profile.isDefault(),
                profile.getLastRecordAt(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
