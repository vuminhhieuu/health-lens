package com.healthlens.api.service;

import com.healthlens.api.dto.response.AcceptInvitationResultResponse;
import com.healthlens.api.dto.response.IncomingProfileInvitationResponse;
import com.healthlens.api.dto.response.ProfileInvitationResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ProfileInvitation;
import com.healthlens.api.entity.ProfileShare;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileInvitationRepository;
import com.healthlens.api.repository.ProfileRepository;
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
public class ProfileShareService {
    private static final int INVITE_VALID_DAYS = 7;

    private final ProfileRepository profileRepository;
    private final ProfileInvitationRepository profileInvitationRepository;
    private final ProfileShareRepository profileShareRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

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
            ProfileShareRepository profileShareRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.profileRepository = profileRepository;
        this.profileInvitationRepository = profileInvitationRepository;
        this.profileShareRepository = profileShareRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
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
        Map<String, ProfileInvitationResponse> latestByEmail = new LinkedHashMap<>();
        for (ProfileInvitation invitation : invitations) {
            String emailKey = invitation.getInviteeEmail().trim().toLowerCase(Locale.ROOT);
            latestByEmail.putIfAbsent(emailKey, mapToResponse(invitation));
        }

        List<ProfileShare> activeShares = profileShareRepository.findAllByProfileIdAndRevokedAtIsNull(profileId);
        Map<UUID, User> viewersById = userRepository.findAllById(
                        activeShares.stream()
                                .map(ProfileShare::getViewerId)
                                .distinct()
                                .collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (ProfileShare share : activeShares) {
            User viewer = viewersById.get(share.getViewerId());
            if (viewer == null || viewer.getEmail() == null || viewer.getEmail().isBlank()) {
                continue;
            }
            String emailKey = viewer.getEmail().trim().toLowerCase(Locale.ROOT);
            ProfileInvitationResponse existing = latestByEmail.get(emailKey);
            if (existing == null || !"accepted".equals(existing.status())) {
                latestByEmail.put(emailKey, new ProfileInvitationResponse(
                        share.getId(),
                        emailKey,
                        "accepted",
                        null,
                        share.getGrantedAt(),
                        share.getAccessLevel()
                ));
            }
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
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));

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
            emailService.sendProfileInvitationEmail(inviter, normalizedEmail, link);
        }

        return mapToResponse(inv);
    }

    @Transactional
    public void cancelInvitation(UUID ownerId, UUID profileId, UUID invitationId) {
        assertProfileOwner(ownerId, profileId);
        ProfileInvitation inv = profileInvitationRepository.findByIdAndProfileId(invitationId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loi moi"));
        if (!"pending".equals(inv.getStatus())) {
            throw new IllegalStateException("Chỉ có thể hủy lời mời đang chờ.");
        }
        profileInvitationRepository.delete(inv);
    }

    @Transactional
    public ProfileInvitationResponse resendInvitation(UUID ownerId, UUID profileId, UUID invitationId) {
        assertProfileOwner(ownerId, profileId);
        ProfileInvitation inv = profileInvitationRepository.findByIdAndProfileId(invitationId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loi moi"));
        if (!"pending".equals(inv.getStatus())) {
            throw new IllegalStateException("Chỉ có thể gửi lại lời mời đang chờ.");
        }
        User inviter = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));

        inv.setToken(newToken());
        inv.setExpiresAt(Instant.now().plus(INVITE_VALID_DAYS, ChronoUnit.DAYS));
        profileInvitationRepository.save(inv);

        String link = buildInvitationLink(inv.getToken());
        emailService.sendProfileInvitationEmail(inviter, inv.getInviteeEmail(), link);

        return mapToResponse(inv);
    }

    @Transactional
    public AcceptInvitationResultResponse acceptInvitation(String token, UUID userId) {
        ProfileInvitation invitation = profileInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Loi moi khong hop le"));

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
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));

        if (!viewer.getEmail().trim().equalsIgnoreCase(invitation.getInviteeEmail().trim())) {
            throw new AccessDeniedException("Email dang nhap khong khop voi loi moi");
        }

        Profile profile = profileRepository.findById(invitation.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so"));
        UUID profileId = profile.getId();
        UUID ownerId = profile.getUser().getId();

        if ("accepted".equals(invitation.getStatus())) {
            ensureShareForInvitation(invitation, viewer.getId(), ownerId);
            return new AcceptInvitationResultResponse("accepted", FAMILY_PROFILES_PATH, profileId);
        }

        if (!"pending".equals(invitation.getStatus())) {
            return new AcceptInvitationResultResponse("expired", FAMILY_PROFILES_PATH, profileId);
        }

        if (!profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewer.getId())) {
            ProfileShare share = new ProfileShare();
            share.setProfileId(profileId);
            share.setOwnerId(ownerId);
            share.setViewerId(viewer.getId());
            share.setAccessLevel(invitation.getAccessLevel());
            profileShareRepository.save(share);
        }

        invitation.setStatus("accepted");
        invitation.setAcceptedAt(now);
        profileInvitationRepository.save(invitation);

        return new AcceptInvitationResultResponse("accepted", FAMILY_PROFILES_PATH, profileId);
    }

    @Transactional
    public void rejectIncomingInvitation(UUID invitationId, UUID userId) {
        ProfileInvitation invitation = profileInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay loi moi"));
        if (!"pending".equals(invitation.getStatus())) {
            throw new IllegalStateException("Chi co the tu choi loi moi dang cho.");
        }

        User viewer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));
        if (!viewer.getEmail().trim().equalsIgnoreCase(invitation.getInviteeEmail().trim())) {
            throw new AccessDeniedException("Email dang nhap khong khop voi loi moi");
        }

        if (isPendingAndPastExpiry(invitation, Instant.now())) {
            invitation.setStatus("expired");
        } else {
            invitation.setStatus("rejected");
        }
        profileInvitationRepository.save(invitation);
    }

    private void ensureShareForInvitation(ProfileInvitation invitation, UUID viewerId, UUID ownerId) {
        if (profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(invitation.getProfileId(), viewerId)) {
            return;
        }
        ProfileShare share = new ProfileShare();
        share.setProfileId(invitation.getProfileId());
        share.setOwnerId(ownerId);
        share.setViewerId(viewerId);
        share.setAccessLevel(invitation.getAccessLevel());
        profileShareRepository.save(share);
    }

    private void assertProfileOwner(UUID requesterId, UUID profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so"));
        if (!requesterId.equals(profile.getUser().getId())) {
            throw new AccessDeniedException("Không có quyền truy cập hồ sơ này");
        }
    }

    private ProfileInvitationResponse mapToResponse(ProfileInvitation inv) {
        return new ProfileInvitationResponse(
                inv.getId(),
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
        return base + FAMILY_PROFILES_PATH;
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
                .orElseThrow(() -> new ResourceNotFoundException("User khong ton tai"));
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
}
