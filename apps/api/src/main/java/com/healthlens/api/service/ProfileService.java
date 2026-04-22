package com.healthlens.api.service;

import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.request.UpdateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileLimitExceededException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final int MAX_PROFILES_PER_USER = 10;

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfiles(UUID userId) {
        return profileRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so"));

        // Ownership check: Ensure user can only update their own profile
        UUID profileOwnerId = profile.getUser().getId();
        if (!userId.equals(profileOwnerId)) {
            throw new AccessDeniedException("Không có quyền truy cập hồ sơ này");
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
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
