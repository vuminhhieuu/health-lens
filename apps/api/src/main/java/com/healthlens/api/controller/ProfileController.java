package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.request.InviteProfileMemberRequest;
import com.healthlens.api.dto.request.UpdateProfileRequest;
import com.healthlens.api.dto.response.HealthRecordHistoryPageResponse;
import com.healthlens.api.dto.response.ProfileInvitationResponse;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.service.HealthRecordService;
import com.healthlens.api.service.ProfileService;
import com.healthlens.api.service.ProfileShareService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.PROFILES_BASE)
public class ProfileController {

    private final ProfileService profileService;
    private final HealthRecordService healthRecordService;
    private final ProfileShareService profileShareService;

    public ProfileController(
            ProfileService profileService,
            HealthRecordService healthRecordService,
            ProfileShareService profileShareService
    ) {
        this.profileService = profileService;
        this.healthRecordService = healthRecordService;
        this.profileShareService = profileShareService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfiles(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<ProfileResponse> profiles = profileService.getProfiles(userId);
        return ResponseEntity.ok(buildResponseBody(profiles));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProfile(
            Authentication authentication,
            @Valid @RequestBody CreateProfileRequest request) {
        
        UUID userId = extractUserId(authentication);
        ProfileResponse profile = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponseBody(profile));
    }

    /**
     * Đảm bảo user có ít nhất một hồ sơ (tạo từ dữ liệu tài khoản nếu danh sách đang rỗng).
     * Dùng cho upload kết quả khám: UI "Tôi" trên web không phải bản ghi profiles.
     */
    @PostMapping("/ensure-default")
    public ResponseEntity<Map<String, Object>> ensureDefaultProfile(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        ProfileResponse profile = profileService.ensureDefaultProfile(userId);
        return ResponseEntity.ok(buildResponseBody(profile));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            Authentication authentication,
            @PathVariable UUID profileId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = extractUserId(authentication);
        ProfileResponse profile = profileService.updateProfile(userId, profileId, request);
        return ResponseEntity.ok(buildResponseBody(profile));
    }

    @DeleteMapping(ApiRoutes.PROFILE_BY_ID_REL)
    public ResponseEntity<Void> deleteProfile(
            Authentication authentication,
            @PathVariable UUID profileId
    ) {
        UUID userId = extractUserId(authentication);
        profileService.deleteProfile(userId, profileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{profileId}/health-records")
    public ResponseEntity<Map<String, Object>> getProfileHealthRecords(
            Authentication authentication,
            @PathVariable UUID profileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        UUID userId = extractUserId(authentication);
        HealthRecordHistoryPageResponse response = healthRecordService.getProfileHistory(userId, profileId, page, limit);
        return ResponseEntity.ok(Map.of(
                "data", response.data(),
                "pagination", response.pagination(),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    @PostMapping("/{profileId}/invitations")
    public ResponseEntity<Map<String, Object>> inviteProfileMember(
            Authentication authentication,
            @PathVariable UUID profileId,
            @Valid @RequestBody InviteProfileMemberRequest request
    ) {
        UUID userId = extractUserId(authentication);
        ProfileInvitationResponse invitation =
                profileShareService.inviteByEmail(userId, profileId, request.email(), request.accessLevel());
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponseBody(invitation));
    }

    @GetMapping("/{profileId}/invitations")
    public ResponseEntity<Map<String, Object>> getProfileInvitations(
            Authentication authentication,
            @PathVariable UUID profileId
    ) {
        UUID userId = extractUserId(authentication);
        List<ProfileInvitationResponse> invitations = profileShareService.listInvitations(userId, profileId);
        return ResponseEntity.ok(buildResponseBody(invitations));
    }

    @DeleteMapping("/{profileId}/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            Authentication authentication,
            @PathVariable UUID profileId,
            @PathVariable UUID invitationId
    ) {
        UUID userId = extractUserId(authentication);
        profileShareService.cancelInvitation(userId, profileId, invitationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(ApiRoutes.PROFILE_SHARE_BY_VIEWER_REL)
    public ResponseEntity<Void> revokeShare(
            Authentication authentication,
            @PathVariable UUID profileId,
            @PathVariable UUID viewerId
    ) {
        UUID userId = extractUserId(authentication);
        profileShareService.revokeShare(userId, profileId, viewerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{profileId}/invitations/{invitationId}/resend")
    public ResponseEntity<Map<String, Object>> resendInvitation(
            Authentication authentication,
            @PathVariable UUID profileId,
            @PathVariable UUID invitationId
    ) {
        UUID userId = extractUserId(authentication);
        ProfileInvitationResponse invitation = profileShareService.resendInvitation(userId, profileId, invitationId);
        return ResponseEntity.ok(buildResponseBody(invitation));
    }

    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private Map<String, Object> buildResponseBody(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );
    }
}
