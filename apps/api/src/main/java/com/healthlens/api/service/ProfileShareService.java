package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.response.AcceptInvitationResultResponse;
import com.healthlens.api.dto.response.IncomingProfileInvitationResponse;
import com.healthlens.api.dto.response.ProfileInvitationResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ProfileInvitation;
import com.healthlens.api.entity.ProfileShareAuditLog;
import com.healthlens.api.entity.ProfileShare;
import com.healthlens.api.entity.User;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileInvitationRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareAuditLogRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileShareService {
    private static final int INVITE_VALID_DAYS = 7;

    private final ProfileRepository profileRepository;
    private final ProfileInvitationRepository profileInvitationRepository;
    private final ProfileShareAuditLogRepository profileShareAuditLogRepository;
    private final ProfileShareRepository profileShareRepository;
    private final UserRepository userRepository;
    private final EmailEventPublisher emailEventPublisher;
    private final AuditEventRecorder auditEventRecorder;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;
    private static final String FAMILY_PROFILES_PATH = "/profiles";

    /** Login then return to accept page so the invitation is completed after authentication. */
    private String buildLoginReturnUrl(String invitationToken) {
        String acceptPath =
                "/invitations/accept?token=" + URLEncoder.encode(invitationToken, StandardCharsets.UTF_8);
        return "/login?returnUrl=" + URLEncoder.encode(acceptPath, StandardCharsets.UTF_8);
    }

    public ProfileShareService(
            ProfileRepository profileRepository,
            ProfileInvitationRepository profileInvitationRepository,
            ProfileShareAuditLogRepository profileShareAuditLogRepository,
            ProfileShareRepository profileShareRepository,
            UserRepository userRepository,
            EmailEventPublisher emailEventPublisher,
            AuditEventRecorder auditEventRecorder
    ) {
        this.profileRepository = profileRepository;
        this.profileInvitationRepository = profileInvitationRepository;
        this.profileShareAuditLogRepository = profileShareAuditLogRepository;
        this.profileShareRepository = profileShareRepository;
        this.userRepository = userRepository;
        this.emailEventPublisher = emailEventPublisher;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional
    public List<ProfileInvitationResponse> listInvitations(UUID requesterId, UUID profileId) {
        assertProfileOwner(requesterId, profileId);
        Instant now = Instant.now();
        List<ProfileInvitation> invitations =
                profileInvitationRepository.findAllByProfileIdOrderByCreatedAtDesc(profileId);
        for (ProfileInvitation invitation : invitations) {
            if (isPendingAndPastExpiry(invitation, now)) {
                invitation.setStatus("expired");
                profileInvitationRepository.save(invitation);
            }
        }
        List<ProfileShare> activeShares = profileShareRepository.findAllByProfileIdAndRevokedAtIsNull(profileId);
        Map<UUID, User> viewersById = userRepository.findAllById(
                        activeShares.stream()
                                .map(ProfileShare::getViewerId)
                                .distinct()
                                .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<String, ProfileShare> activeShareByEmail = new LinkedHashMap<>();
        for (ProfileShare share : activeShares) {
            User viewer = viewersById.get(share.getViewerId());
            if (viewer == null || viewer.getEmail() == null || viewer.getEmail().isBlank()) {
                continue;
            }
            String emailKey = viewer.getEmail().trim().toLowerCase(Locale.ROOT);
            activeShareByEmail.put(emailKey, share);
        }

        Map<String, ProfileInvitationResponse> latestByEmail = new LinkedHashMap<>();
        for (ProfileInvitation invitation : invitations) {
            String emailKey = invitation.getInviteeEmail().trim().toLowerCase(Locale.ROOT);
            // Không hiển thị accepted đã hết hiệu lực (share đã bị revoke)
            if ("accepted".equals(invitation.getStatus()) && !activeShareByEmail.containsKey(emailKey)) {
                continue;
            }
            latestByEmail.putIfAbsent(emailKey, mapToResponse(invitation));
        }

        for (ProfileShare share : activeShares) {
            User viewer = viewersById.get(share.getViewerId());
            if (viewer == null || viewer.getEmail() == null || viewer.getEmail().isBlank()) {
                continue;
            }
            String emailKey = viewer.getEmail().trim().toLowerCase(Locale.ROOT);
            latestByEmail.put(emailKey, new ProfileInvitationResponse(
                    share.getId(),
                    share.getViewerId(),
                    emailKey,
                    "accepted",
                    null,
                    share.getGrantedAt(),
                    share.getAccessLevel()
            ));
        }

        return new ArrayList<>(latestByEmail.values());
    }

    private static boolean isPendingAndPastExpiry(ProfileInvitation invitation, Instant now) {
        return "pending".equals(invitation.getStatus())
                && invitation.getExpiresAt() != null
                && now.isAfter(invitation.getExpiresAt());
    }

    @Transactional
    public ProfileInvitationResponse inviteByEmail(UUID ownerId, UUID profileId, String email) {
        return inviteByEmail(ownerId, profileId, email, null);
    }

    @Transactional
    public ProfileInvitationResponse inviteByEmail(UUID ownerId, UUID profileId, String email, String accessLevel) {
        assertProfileOwner(ownerId, profileId);

        User inviter = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String resolvedAccessLevel = (accessLevel == null || accessLevel.isBlank()) ? "view" : accessLevel.trim();

        java.util.Optional<ProfileInvitation> latestInvitation = profileInvitationRepository
                .findTopByProfileIdAndInviteeEmailIgnoreCaseOrderByCreatedAtDesc(profileId, normalizedEmail);
        ProfileInvitation inv = (latestInvitation != null ? latestInvitation : java.util.Optional.<ProfileInvitation>empty())
                .orElseGet(ProfileInvitation::new);
        inv.setProfileId(profileId);
        inv.setInviterId(ownerId);
        inv.setInviteeEmail(normalizedEmail);
        inv.setToken(newToken());
        inv.setStatus("pending");
        inv.setAcceptedAt(null);
        inv.setCreatedAt(Instant.now());
        inv.setExpiresAt(Instant.now().plus(INVITE_VALID_DAYS, ChronoUnit.DAYS));
        inv.setAccessLevel(resolvedAccessLevel);

        User invitee = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (invitee != null) {
            ProfileShare existingShare = profileShareRepository
                    .findByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, invitee.getId())
                    .orElse(null);
            if (existingShare != null) {
                existingShare.setAccessLevel(resolvedAccessLevel);
                profileShareRepository.save(existingShare);
                inv.setStatus("accepted");
                inv.setAcceptedAt(Instant.now());
            }
        }

        profileInvitationRepository.save(inv);

        if (!"accepted".equals(inv.getStatus())) {
            String link = buildInvitationLink(inv.getToken());
            emailEventPublisher.publishProfileInvitation(inviter, normalizedEmail, link);
        }

        writeUnifiedProfileAudit(
                ownerId,
                AuditActions.INVITE_PROFILE_SHARE,
                profileId,
                Map.of(
                        "invitationId", inv.getId(),
                        "inviteeEmail", normalizedEmail,
                        "accessLevel", resolvedAccessLevel,
                        "status", inv.getStatus()
                )
        );

        return mapToResponse(inv);
    }

    @Transactional
    public void cancelInvitation(UUID ownerId, UUID profileId, UUID invitationId) {
        assertProfileOwner(ownerId, profileId);
        ProfileInvitation inv = profileInvitationRepository.findByIdAndProfileId(invitationId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        if (!"pending".equals(inv.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy lời mời đang chờ.");
        }
        writeUnifiedProfileAudit(
                ownerId,
                AuditActions.CANCEL_PROFILE_INVITATION,
                profileId,
                Map.of("invitationId", invitationId, "inviteeEmail", inv.getInviteeEmail())
        );
        profileInvitationRepository.delete(inv);
    }

    @Transactional
    public void revokeShare(UUID ownerId, UUID profileId, UUID viewerId) {
        assertProfileOwner(ownerId, profileId);
        // Path segment is viewer id only. Do not treat it as share PK — UUID collision could revoke the wrong row.
        ProfileShare share = profileShareRepository
                .findByProfileIdAndViewerIdAndRevokedAtIsNullForUpdate(profileId, viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền chia sẻ đang hoạt động"));
        UUID resolvedViewerId = share.getViewerId();
        share.setRevokedAt(Instant.now());
        profileShareRepository.save(share);
        userRepository.findById(resolvedViewerId)
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .ifPresent(email ->
                        profileInvitationRepository.deleteAllByProfileIdAndInviteeEmailIgnoreCase(profileId, email.trim())
                );
        writeRevokeAuditLog(ownerId, profileId, resolvedViewerId, share.getId());
    }

    @Transactional
    public ProfileInvitationResponse resendInvitation(UUID ownerId, UUID profileId, UUID invitationId) {
        assertProfileOwner(ownerId, profileId);
        ProfileInvitation inv = profileInvitationRepository.findByIdAndProfileId(invitationId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        if (!"pending".equals(inv.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gửi lại lời mời đang chờ.");
        }
        User inviter = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        inv.setToken(newToken());
        inv.setExpiresAt(Instant.now().plus(INVITE_VALID_DAYS, ChronoUnit.DAYS));
        profileInvitationRepository.save(inv);

        String link = buildInvitationLink(inv.getToken());
        emailEventPublisher.publishProfileInvitation(inviter, inv.getInviteeEmail(), link);

        writeUnifiedProfileAudit(
                ownerId,
                AuditActions.RESEND_PROFILE_INVITATION,
                profileId,
                Map.of("invitationId", invitationId, "inviteeEmail", inv.getInviteeEmail())
        );

        return mapToResponse(inv);
    }

    @Transactional
    public AcceptInvitationResultResponse acceptInvitation(String token, UUID userId) {
        ProfileInvitation invitation = profileInvitationRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không hợp lệ"));

        Instant now = Instant.now();
        boolean pastExpiry =
                invitation.getExpiresAt() != null && now.isAfter(invitation.getExpiresAt());
        if (pastExpiry) {
            return new AcceptInvitationResultResponse("expired", FAMILY_PROFILES_PATH, invitation.getProfileId());
        }

        if (userId == null) {
            return new AcceptInvitationResultResponse(
                    "require-login",
                    buildLoginReturnUrl(token),
                    invitation.getProfileId()
            );
        }

        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        if (!viewer.getEmail().trim().equalsIgnoreCase(invitation.getInviteeEmail().trim())) {
            throw new AccessDeniedException("Email đăng nhập không khớp với lời mời");
        }

        Profile profile = profileRepository.findById(invitation.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
        UUID profileId = profile.getId();
        UUID ownerId = profile.getUser().getId();

        if ("accepted".equals(invitation.getStatus())) {
            ensureShareForInvitation(invitation, viewer.getId(), ownerId);
            return new AcceptInvitationResultResponse("accepted", FAMILY_PROFILES_PATH, profileId);
        }

        if (!"pending".equals(invitation.getStatus())) {
            return new AcceptInvitationResultResponse("expired", FAMILY_PROFILES_PATH, profileId);
        }

        if (profileShareRepository
                .findByProfileIdAndViewerIdAndRevokedAtIsNullForUpdate(profileId, viewer.getId())
                .isEmpty()) {
            ProfileShare share = new ProfileShare();
            share.setProfileId(profileId);
            share.setOwnerId(ownerId);
            share.setViewerId(viewer.getId());
            share.setAccessLevel(invitation.getAccessLevel());
            saveNewActiveProfileShareHandlingDuplicate(share, profileId, viewer.getId());
        }

        invitation.setStatus("accepted");
        invitation.setAcceptedAt(now);
        profileInvitationRepository.save(invitation);

        writeUnifiedProfileAudit(
                userId,
                AuditActions.ACCEPT_PROFILE_INVITATION,
                profileId,
                Map.of("invitationId", invitation.getId(), "ownerId", ownerId.toString())
        );

        return new AcceptInvitationResultResponse("accepted", FAMILY_PROFILES_PATH, profileId);
    }

    @Transactional
    public void rejectIncomingInvitation(UUID invitationId, UUID userId) {
        ProfileInvitation invitation = profileInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));
        if (!"pending".equals(invitation.getStatus())) {
            throw new IllegalStateException("Chỉ có thể từ chối lời mời đang chờ.");
        }

        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        if (!viewer.getEmail().trim().equalsIgnoreCase(invitation.getInviteeEmail().trim())) {
            throw new AccessDeniedException("Email đăng nhập không khớp với lời mời");
        }

        if (isPendingAndPastExpiry(invitation, Instant.now())) {
            invitation.setStatus("expired");
        } else {
            invitation.setStatus("rejected");
        }
        profileInvitationRepository.save(invitation);

        writeUnifiedProfileAudit(
                userId,
                AuditActions.REJECT_PROFILE_INVITATION,
                invitation.getProfileId(),
                Map.of("invitationId", invitationId, "status", invitation.getStatus())
        );
    }

    private void ensureShareForInvitation(ProfileInvitation invitation, UUID viewerId, UUID ownerId) {
        if (profileShareRepository
                .findByProfileIdAndViewerIdAndRevokedAtIsNullForUpdate(invitation.getProfileId(), viewerId)
                .isPresent()) {
            return;
        }
        ProfileShare share = new ProfileShare();
        share.setProfileId(invitation.getProfileId());
        share.setOwnerId(ownerId);
        share.setViewerId(viewerId);
        share.setAccessLevel(invitation.getAccessLevel());
        saveNewActiveProfileShareHandlingDuplicate(share, invitation.getProfileId(), viewerId);
    }

    /**
     * Partial unique index uq_profile_shares_profile_viewer_active prevents duplicate active rows; concurrent
     * accepts must remain idempotent.
     */
    private void saveNewActiveProfileShareHandlingDuplicate(ProfileShare share, UUID profileId, UUID viewerId) {
        try {
            // Flush immediately so partial unique index violation is catchable in this transaction.
            profileShareRepository.saveAndFlush(share);
        } catch (DataIntegrityViolationException ex) {
            if (profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId)) {
                return;
            }
            throw ex;
        }
    }

    private void assertProfileOwner(UUID requesterId, UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
        if (!requesterId.equals(profile.getUser().getId())) {
            throw new AccessDeniedException("Không có quyền truy cập hồ sơ này");
        }
    }

    private ProfileInvitationResponse mapToResponse(ProfileInvitation inv) {
        return new ProfileInvitationResponse(
                inv.getId(),
                null,
                inv.getInviteeEmail(),
                inv.getStatus(),
                inv.getExpiresAt(),
                inv.getCreatedAt(),
                inv.getAccessLevel()
        );
    }

    private String buildInvitationLink(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return base + buildAcceptFrontendPath(token);
    }

    private static String buildAcceptFrontendPath(String token) {
        return "/invitations/accept?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * Lời mời đang chờ (theo email tài khoản) — dùng cho thông báo trong app (tab Hồ sơ gia đình).
     */
    @Transactional
    public List<IncomingProfileInvitationResponse> listIncomingInvitations(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        List<ProfileInvitation> pending =
                profileInvitationRepository.findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        email, "pending");
        List<IncomingProfileInvitationResponse> out = new ArrayList<>();
        for (ProfileInvitation inv : pending) {
            if (isPendingAndPastExpiry(inv, now)) {
                inv.setStatus("expired");
                profileInvitationRepository.save(inv);
                continue;
            }
            Profile profile = profileRepository.findById(inv.getProfileId())
                    .orElse(null);
            if (profile == null) {
                continue;
            }
            User inviter = userRepository.findById(inv.getInviterId()).orElse(null);
            String inviterName = inviter != null && inviter.getFullName() != null
                    ? inviter.getFullName()
                    : "Một thành viên";
            String profileName = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                    ? profile.getDisplayName()
                    : "Hồ sơ";
            out.add(new IncomingProfileInvitationResponse(
                    inv.getId(),
                    profile.getId(),
                    profileName,
                    inviterName,
                    inv.getExpiresAt(),
                    inv.getCreatedAt(),
                    inv.getAccessLevel(),
                    buildAcceptFrontendPath(inv.getToken())
            ));
        }
        return out;
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private void writeRevokeAuditLog(UUID actorId, UUID profileId, UUID viewerId, UUID shareId) {
        ProfileShareAuditLog auditLog = new ProfileShareAuditLog();
        auditLog.setActorId(actorId);
        auditLog.setProfileId(profileId);
        auditLog.setViewerId(viewerId);
        auditLog.setAction("REVOKE_PROFILE_SHARE");
        auditLog.setResourceType("PROFILE_SHARE");
        auditLog.setResourceId(shareId);
        profileShareAuditLogRepository.save(auditLog);

        writeUnifiedProfileAudit(
                actorId,
                AuditActions.REVOKE_PROFILE_SHARE,
                profileId,
                Map.of("shareId", shareId, "viewerId", viewerId.toString())
        );
    }

    private void writeUnifiedProfileAudit(
            UUID actorId,
            String action,
            UUID profileId,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        profileRepository.findById(profileId).ifPresent(profile -> {
            String displayName = profile.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                payload.put("displayName", displayName.trim());
            }
        });
        payload.putAll(details);
        auditEventRecorder.recordEvent(actorId, action, AuditResourceTypes.PROFILE, profileId, payload);
    }
}
