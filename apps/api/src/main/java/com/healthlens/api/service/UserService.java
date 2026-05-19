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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    public static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
    private static final Duration AVATAR_URL_TTL = Duration.ofMinutes(15);
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AuditEventRecorder auditEventRecorder;
    private final StorageService storageService;

    public UserService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            AuditEventRecorder auditEventRecorder,
            StorageService storageService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.auditEventRecorder = auditEventRecorder;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse uploadAvatar(UUID userId, MultipartFile file) {
        validateAvatarFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        String previousKey = user.getAvatarStorageKey();
        String contentType = file.getContentType();

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không đọc được ảnh đại diện", ex);
        }

        String objectKey = "avatars/%s/%s.%s".formatted(
                userId,
                UUID.randomUUID(),
                extensionForContentType(contentType)
        );

        try {
            storageService.uploadObject(objectKey, bytes, contentType);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Không thể lưu ảnh đại diện. Vui lòng thử lại sau.", ex);
        }
        cleanupUploadedAvatarOnRollback(objectKey);

        try {
            user.setAvatarStorageKey(objectKey);
            user.setAvatarContentType(contentType);
            user.setAvatarSizeBytes(file.getSize());
            user.setAvatarChecksumSha256(sha256(bytes));
            user.setAvatarUpdatedAt(Instant.now());
            user = userRepository.save(user);

            cleanupPreviousAvatarAfterCommit(previousKey);

            auditEventRecorder.recordEvent(
                    userId,
                    AuditActions.UPDATE_USER,
                    AuditResourceTypes.USER,
                    userId,
                    Map.of(
                            "action", "avatar_upload",
                            "contentType", contentType,
                            "sizeBytes", file.getSize()
                    )
            );

            return mapToResponse(user);
        } catch (RuntimeException ex) {
            cleanupUploadedAvatarImmediatelyWhenNoTransaction(objectKey);
            throw ex;
        }
    }

    @Transactional
    public UserResponse removeAvatar(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        String previousKey = user.getAvatarStorageKey();

        user.setAvatarStorageKey(null);
        user.setAvatarContentType(null);
        user.setAvatarSizeBytes(null);
        user.setAvatarChecksumSha256(null);
        user.setAvatarUpdatedAt(null);
        user = userRepository.save(user);

        cleanupPreviousAvatarAfterCommit(previousKey);

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.UPDATE_USER,
                AuditResourceTypes.USER,
                userId,
                Map.of("action", "avatar_remove")
        );

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

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalArgumentException("Ảnh đại diện không được để trống");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Định dạng ảnh đại diện không được hỗ trợ. Chỉ hỗ trợ JPEG, PNG hoặc WebP.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalArgumentException("Ảnh đại diện tối đa 2MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\")
                || filename.contains("..")) {
            throw new IllegalArgumentException("Tên tệp ảnh đại diện không an toàn");
        }
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Định dạng ảnh đại diện không được hỗ trợ");
        };
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không thể tính checksum ảnh đại diện", ex);
        }
    }

    private void deleteObjectBestEffort(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            storageService.deleteObject(key);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete avatar object: {}", ex.getMessage());
        }
    }

    private void cleanupPreviousAvatarAfterCommit(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteObjectBestEffort(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteObjectBestEffort(key);
            }
        });
    }

    private void cleanupUploadedAvatarOnRollback(String key) {
        if (key == null || key.isBlank() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteObjectBestEffort(key);
                }
            }
        });
    }

    private void cleanupUploadedAvatarImmediatelyWhenNoTransaction(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteObjectBestEffort(key);
        }
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getBirthDate(),
                user.getGender(),
                user.isEmailVerified(),
                false, // consentGiven is currently not persisted so we just return false
                buildAvatarUrl(user.getAvatarStorageKey())
        );
    }

    private String buildAvatarUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        try {
            return storageService.generateDownloadUrl(storageKey, AVATAR_URL_TTL);
        } catch (RuntimeException ex) {
            log.warn("Failed to generate avatar download URL: {}", ex.getMessage());
            return null;
        }
    }
}
