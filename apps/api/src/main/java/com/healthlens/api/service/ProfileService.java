package com.healthlens.api.service;

import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileLimitExceededException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
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

        profile = profileRepository.save(profile);

        return mapToResponse(profile);
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
