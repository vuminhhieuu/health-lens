package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.response.AcceptHealthRecordInvitationResultResponse;
import com.healthlens.api.dto.response.HealthRecordInvitationResponse;
import com.healthlens.api.dto.response.IncomingHealthRecordInvitationResponse;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.HealthRecordInvitation;
import com.healthlens.api.entity.HealthRecordShare;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ProfileShareAuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.HealthRecordInvitationRepository;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.HealthRecordShareRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthRecordShareService {
    private static final int INVITE_VALID_DAYS = 7;

    private final HealthRecordRepository healthRecordRepository;
    private final HealthRecordInvitationRepository healthRecordInvitationRepository;
    private final HealthRecordShareRepository healthRecordShareRepository;
    private final ProfileRepository profileRepository;
    private final ProfileShareRepository profileShareRepository;
    private final ProfileShareAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final EmailEventPublisher emailEventPublisher;
    private final AuditEventRecorder auditEventRecorder;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public HealthRecordShareService(
            HealthRecordRepository healthRecordRepository,
            HealthRecordInvitationRepository healthRecordInvitationRepository,
            HealthRecordShareRepository healthRecordShareRepository,
            ProfileRepository profileRepository,
            ProfileShareRepository profileShareRepository,
            ProfileShareAuditLogRepository auditLogRepository,
            UserRepository userRepository,
            EmailEventPublisher emailEventPublisher,
            AuditEventRecorder auditEventRecorder
    ) {
        this.healthRecordRepository = healthRecordRepository;
        this.healthRecordInvitationRepository = healthRecordInvitationRepository;
        this.healthRecordShareRepository = healthRecordShareRepository;
        this.profileRepository = profileRepository;
        this.profileShareRepository = profileShareRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.emailEventPublisher = emailEventPublisher;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Transactional
    public HealthRecordInvitationResponse inviteByEmail(UUID requesterId, UUID recordId, String email, String accessLevel) {
        HealthRecord record = loadRecord(recordId);
        Profile profile = loadProfile(record.getProfileId());
        assertCanShareRecord(requesterId, profile);

        User inviter = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String resolvedAccessLevel = normalizeAccessLevel(accessLevel);

        HealthRecordInvitation invitation = healthRecordInvitationRepository
                .findTopByHealthRecordIdAndInviteeEmailIgnoreCaseOrderByCreatedAtDesc(recordId, normalizedEmail)
                .orElseGet(HealthRecordInvitation::new);
        invitation.setHealthRecordId(recordId);
        invitation.setProfileId(record.getProfileId());
        invitation.setInviterId(requesterId);
        invitation.setInviteeEmail(normalizedEmail);
        invitation.setToken(newToken());
        invitation.setStatus("pending");
        invitation.setAccessLevel(resolvedAccessLevel);
        invitation.setAcceptedAt(null);
        invitation.setRevokedAt(null);
        invitation.setCreatedAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plus(INVITE_VALID_DAYS, ChronoUnit.DAYS));

        User invitee = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (invitee != null) {
            HealthRecordShare existingShare = healthRecordShareRepository
                    .findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, invitee.getId())
                    .orElse(null);
            if (existingShare != null) {
                existingShare.setAccessLevel(resolvedAccessLevel);
                healthRecordShareRepository.save(existingShare);
                invitation.setStatus("accepted");
                invitation.setAcceptedAt(Instant.now());
            }
        }

        healthRecordInvitationRepository.save(invitation);
        writeAuditLog(
                requesterId,
                recordId,
                record.getProfileId(),
                null,
                AuditActions.INVITE_HEALTH_RECORD_SHARE,
                "HEALTH_RECORD_INVITATION",
                invitation.getId(),
                Map.of(
                        "inviteeEmail", normalizedEmail,
                        "accessLevel", resolvedAccessLevel,
                        "status", invitation.getStatus()
                )
        );

        if (!"accepted".equals(invitation.getStatus())) {
            emailEventPublisher.publishHealthRecordInvitation(inviter, normalizedEmail, buildInvitationLink(invitation.getToken()));
        }

        return mapInvitation(invitation, invitee != null ? invitee.getId() : null);
    }

    @Transactional
    public List<HealthRecordInvitationResponse> listInvitations(UUID requesterId, UUID recordId) {
        HealthRecord record = loadRecord(recordId);
        Profile profile = loadProfile(record.getProfileId());
        assertCanShareRecord(requesterId, profile);

        Instant now = Instant.now();
        List<HealthRecordInvitation> invitations =
                healthRecordInvitationRepository.findAllByHealthRecordIdOrderByCreatedAtDesc(recordId);
        for (HealthRecordInvitation invitation : invitations) {
            if (isPendingAndPastExpiry(invitation, now)) {
                invitation.setStatus("expired");
                healthRecordInvitationRepository.save(invitation);
            }
        }

        List<HealthRecordShare> activeShares = healthRecordShareRepository.findAllByHealthRecordIdAndRevokedAtIsNull(recordId);
        List<com.healthlens.api.entity.ProfileShare> activeProfileShares =
                profileShareRepository.findAllByProfileIdAndRevokedAtIsNull(record.getProfileId());

        List<UUID> viewerIds = new ArrayList<>();
        viewerIds.addAll(activeShares.stream().map(HealthRecordShare::getViewerId).toList());
        viewerIds.addAll(activeProfileShares.stream().map(com.healthlens.api.entity.ProfileShare::getViewerId).toList());

        Map<UUID, User> viewersById = userRepository.findAllById(
                        viewerIds.stream().distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<String, HealthRecordInvitationResponse> latestByEmail = new LinkedHashMap<>();
        for (HealthRecordInvitation invitation : invitations) {
            String emailKey = invitation.getInviteeEmail().trim().toLowerCase(Locale.ROOT);
            if ("accepted".equals(invitation.getStatus()) && activeShares.stream().noneMatch(share -> {
                User viewer = viewersById.get(share.getViewerId());
                return viewer != null && viewer.getEmail() != null && viewer.getEmail().equalsIgnoreCase(emailKey);
            }) && activeProfileShares.stream().noneMatch(share -> {
                User viewer = viewersById.get(share.getViewerId());
                return viewer != null && viewer.getEmail() != null && viewer.getEmail().equalsIgnoreCase(emailKey);
            })) {
                continue;
            }
            User invitee = userRepository.findByEmailIgnoreCase(emailKey).orElse(null);
            latestByEmail.putIfAbsent(emailKey, mapInvitation(invitation, invitee != null ? invitee.getId() : null));
        }

        for (HealthRecordShare share : activeShares) {
            User viewer = viewersById.get(share.getViewerId());
            if (viewer == null || viewer.getEmail() == null || viewer.getEmail().isBlank()) {
                continue;
            }
            String emailKey = viewer.getEmail().trim().toLowerCase(Locale.ROOT);
            latestByEmail.put(emailKey, new HealthRecordInvitationResponse(
                    share.getId(),
                    share.getViewerId(),
                    emailKey,
                    "accepted",
                    "record",
                    share.getAccessLevel(),
                    null,
                    share.getGrantedAt(),
                    share.getGrantedAt()
            ));
        }

        for (com.healthlens.api.entity.ProfileShare share : activeProfileShares) {
            User viewer = viewersById.get(share.getViewerId());
            if (viewer == null || viewer.getEmail() == null || viewer.getEmail().isBlank()) {
                continue;
            }
            String emailKey = viewer.getEmail().trim().toLowerCase(Locale.ROOT);
            latestByEmail.put(emailKey, new HealthRecordInvitationResponse(
                    share.getId(),
                    share.getViewerId(),
                    emailKey,
                    "accepted",
                    "profile",
                    share.getAccessLevel(),
                    null,
                    share.getGrantedAt(),
                    share.getGrantedAt()
            ));
        }

        return new ArrayList<>(latestByEmail.values());
    }

    @Transactional
    public List<IncomingHealthRecordInvitationResponse> listIncomingInvitations(UUID userId) {
        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Instant now = Instant.now();
        List<HealthRecordInvitation> invitations = healthRecordInvitationRepository
                .findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(viewer.getEmail(), "pending");

        List<HealthRecordInvitation> activeInvitations = new ArrayList<>();
        for (HealthRecordInvitation invitation : invitations) {
            if (isPendingAndPastExpiry(invitation, now)) {
                invitation.setStatus("expired");
                healthRecordInvitationRepository.save(invitation);
                continue;
            }
            activeInvitations.add(invitation);
        }

        Map<UUID, User> invitersById = userRepository.findAllById(
                        activeInvitations.stream()
                                .map(HealthRecordInvitation::getInviterId)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return activeInvitations.stream()
                .map(invitation -> new IncomingHealthRecordInvitationResponse(
                        invitation.getId(),
                        invitation.getHealthRecordId(),
                        invitation.getProfileId(),
                        invitersById.get(invitation.getInviterId()) != null
                                ? invitersById.get(invitation.getInviterId()).getFullName()
                                : "Một người dùng",
                        invitation.getExpiresAt(),
                        invitation.getCreatedAt(),
                        "/health-record-invitations/accept?token="
                                + URLEncoder.encode(invitation.getToken(), StandardCharsets.UTF_8)
                ))
                .toList();
    }

    @Transactional
    public AcceptHealthRecordInvitationResultResponse acceptInvitation(String token, UUID userId) {
        HealthRecordInvitation invitation = healthRecordInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời không hợp lệ"));

        Instant now = Instant.now();
        if (isPendingAndPastExpiry(invitation, now)) {
            invitation.setStatus("expired");
            healthRecordInvitationRepository.save(invitation);
            return new AcceptHealthRecordInvitationResultResponse(
                    "expired", "/health-records", invitation.getHealthRecordId(), invitation.getProfileId());
        }

        if (userId == null) {
            boolean inviteeExists = userRepository.findByEmailIgnoreCase(invitation.getInviteeEmail()).isPresent();
            return new AcceptHealthRecordInvitationResultResponse(
                    inviteeExists ? "require-login" : "require-register",
                    inviteeExists ? buildLoginReturnUrl(token) : buildRegisterReturnUrl(token),
                    invitation.getHealthRecordId(),
                    invitation.getProfileId()
            );
        }

        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        if (!viewer.getEmail().trim().equalsIgnoreCase(invitation.getInviteeEmail().trim())) {
            throw new AccessDeniedException("Email đăng nhập không khớp với lời mời");
        }

        HealthRecord record = loadRecord(invitation.getHealthRecordId());
        Profile profile = loadProfile(record.getProfileId());
        UUID ownerId = profile.getUser().getId();

        if (!"accepted".equals(invitation.getStatus()) && !"pending".equals(invitation.getStatus())) {
            return new AcceptHealthRecordInvitationResultResponse(
                    "expired", "/health-records", invitation.getHealthRecordId(), invitation.getProfileId());
        }

        HealthRecordShare share = healthRecordShareRepository
                .findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(invitation.getHealthRecordId(), viewer.getId())
                .orElseGet(() -> {
                    HealthRecordShare created = new HealthRecordShare();
                    created.setHealthRecordId(invitation.getHealthRecordId());
                    created.setProfileId(invitation.getProfileId());
                    created.setOwnerId(ownerId);
                    created.setViewerId(viewer.getId());
                    created.setAccessLevel(normalizeAccessLevel(invitation.getAccessLevel()));
                    return healthRecordShareRepository.save(created);
                });
        share.setAccessLevel(normalizeAccessLevel(invitation.getAccessLevel()));
        healthRecordShareRepository.save(share);

        invitation.setStatus("accepted");
        invitation.setAcceptedAt(now);
        healthRecordInvitationRepository.save(invitation);
        writeAuditLog(
                userId,
                invitation.getHealthRecordId(),
                record.getProfileId(),
                viewer.getId(),
                AuditActions.ACCEPT_HEALTH_RECORD_SHARE,
                "HEALTH_RECORD_SHARE",
                share.getId(),
                Map.of("invitationId", invitation.getId().toString())
        );

        return new AcceptHealthRecordInvitationResultResponse(
                "accepted",
                "/health-records/review/" + invitation.getHealthRecordId(),
                invitation.getHealthRecordId(),
                invitation.getProfileId()
        );
    }

    @Transactional
    public void revokeShare(UUID requesterId, UUID recordId, UUID viewerId) {
        HealthRecord record = loadRecord(recordId);
        Profile profile = loadProfile(record.getProfileId());
        if (!profile.getUser().getId().equals(requesterId)) {
            throw new AccessDeniedException("Chỉ chủ hồ sơ mới có thể thu hồi quyền chia sẻ kết quả");
        }

        HealthRecordShare share = healthRecordShareRepository
                .findByHealthRecordIdAndViewerIdAndRevokedAtIsNull(recordId, viewerId)
                .orElseGet(() -> healthRecordShareRepository.findById(viewerId)
                        .filter(found -> recordId.equals(found.getHealthRecordId()))
                        .filter(found -> found.getRevokedAt() == null)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền chia sẻ đang hoạt động")));
        UUID resolvedViewerId = share.getViewerId();
        share.setRevokedAt(Instant.now());
        healthRecordShareRepository.save(share);

        userRepository.findById(resolvedViewerId)
                .map(User::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .ifPresent(email -> {
                    List<HealthRecordInvitation> invitations = healthRecordInvitationRepository
                            .findAllByHealthRecordIdAndInviteeEmailIgnoreCase(recordId, email.trim());
                    for (HealthRecordInvitation invitation : invitations) {
                        invitation.setStatus("revoked");
                        invitation.setRevokedAt(Instant.now());
                    }
                    healthRecordInvitationRepository.saveAll(invitations);
                });
        writeAuditLog(
                requesterId,
                recordId,
                record.getProfileId(),
                resolvedViewerId,
                AuditActions.REVOKE_HEALTH_RECORD_SHARE,
                "HEALTH_RECORD_SHARE",
                share.getId(),
                Map.of()
        );
    }

    private HealthRecord loadRecord(UUID recordId) {
        return healthRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả khám"));
    }

    private Profile loadProfile(UUID profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
    }

    private void assertCanShareRecord(UUID requesterId, Profile profile) {
        if (profile.getUser().getId().equals(requesterId)) {
            return;
        }
        boolean canEdit = profileShareRepository.existsByProfileIdAndViewerIdAndAccessLevelIgnoreCaseAndRevokedAtIsNull(
                profile.getId(), requesterId, "edit");
        if (!canEdit) {
            throw new AccessDeniedException("Bạn không có quyền chia sẻ kết quả khám này");
        }
    }

    private static boolean isPendingAndPastExpiry(HealthRecordInvitation invitation, Instant now) {
        return "pending".equals(invitation.getStatus())
                && invitation.getExpiresAt() != null
                && now.isAfter(invitation.getExpiresAt());
    }

    private HealthRecordInvitationResponse mapInvitation(HealthRecordInvitation invitation, UUID viewerId) {
        return new HealthRecordInvitationResponse(
                invitation.getId(),
                viewerId,
                invitation.getInviteeEmail(),
                invitation.getStatus(),
                "record",
                normalizeAccessLevel(invitation.getAccessLevel()),
                invitation.getExpiresAt(),
                invitation.getCreatedAt(),
                invitation.getAcceptedAt()
        );
    }

    private String normalizeAccessLevel(String accessLevel) {
        if (accessLevel == null || accessLevel.isBlank()) {
            return "view";
        }
        return "edit".equalsIgnoreCase(accessLevel.trim()) ? "edit" : "view";
    }

    private String buildInvitationLink(String token) {
        String base = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        return base + buildAcceptFrontendPath(token);
    }

    private static String buildAcceptFrontendPath(String token) {
        return "/health-record-invitations/accept?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static String buildLoginReturnUrl(String invitationToken) {
        String acceptPath =
                "/health-record-invitations/accept?token=" + URLEncoder.encode(invitationToken, StandardCharsets.UTF_8);
        return "/login?returnUrl=" + URLEncoder.encode(acceptPath, StandardCharsets.UTF_8);
    }

    private static String buildRegisterReturnUrl(String invitationToken) {
        String acceptPath =
                "/health-record-invitations/accept?token=" + URLEncoder.encode(invitationToken, StandardCharsets.UTF_8);
        return "/register?inviteToken="
                + URLEncoder.encode(invitationToken, StandardCharsets.UTF_8)
                + "&returnUrl="
                + URLEncoder.encode(acceptPath, StandardCharsets.UTF_8);
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private void writeAuditLog(
            UUID actorId,
            UUID healthRecordId,
            UUID profileId,
            UUID viewerId,
            String action,
            String relatedResourceType,
            UUID relatedResourceId,
            Map<String, Object> extraDetails
    ) {
        ProfileShareAuditLog auditLog = new ProfileShareAuditLog();
        auditLog.setActorId(actorId);
        auditLog.setProfileId(profileId);
        auditLog.setViewerId(viewerId != null ? viewerId : actorId);
        auditLog.setAction(action);
        auditLog.setResourceType(relatedResourceType);
        auditLog.setResourceId(relatedResourceId);
        auditLogRepository.save(auditLog);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("profileId", profileId.toString());
        details.put("relatedResourceType", relatedResourceType);
        if ("HEALTH_RECORD_INVITATION".equals(relatedResourceType)) {
            details.put("invitationId", relatedResourceId.toString());
        } else {
            details.put("shareId", relatedResourceId.toString());
        }
        if (viewerId != null) {
            details.put("viewerId", viewerId.toString());
        }
        if (extraDetails != null && !extraDetails.isEmpty()) {
            details.putAll(extraDetails);
        }
        auditEventRecorder.recordEvent(actorId, action, AuditResourceTypes.HEALTH_RECORD, healthRecordId, details);
    }
}
