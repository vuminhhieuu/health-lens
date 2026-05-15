package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.UpdateUserRequest;
import com.healthlens.api.dto.response.UserResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AuditEventRecorder auditEventRecorder;

    public UserService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            AuditEventRecorder auditEventRecorder
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        user.setFullName(request.fullName().trim());
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        if (request.gender() != null) {
            user.setGender(normalizeGender(request.gender()));
        }

        user = userRepository.save(user);
        syncDefaultProfile(userId, user);

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.UPDATE_USER,
                AuditResourceTypes.USER,
                userId,
                Map.of(
                        "email", user.getEmail(),
                        "fullName", user.getFullName()
                )
        );

        return mapToResponse(user);
    }

    private void syncDefaultProfile(UUID userId, User user) {
        profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .ifPresent(profile -> applyUserIdentityToProfile(profile, user));
    }

    private void applyUserIdentityToProfile(Profile profile, User user) {
        String displayName = user.getFullName() != null ? user.getFullName().trim() : "Hồ sơ của tôi";
        if (displayName.length() > 50) {
            displayName = displayName.substring(0, 50);
        }
        profile.setDisplayName(displayName);
        profile.setBirthDate(user.getBirthDate());
        profile.setGender(user.getGender());
        profileRepository.save(profile);
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return null;
        }
        String normalized = gender.trim().toLowerCase();
        if ("male".equals(normalized) || "female".equals(normalized) || "other".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getBirthDate(),
                user.getGender(),
                user.isEmailVerified(),
                false // consentGiven is currently not persisted so we just return false
        );
    }
}
