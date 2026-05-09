package com.healthlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfileShareServiceTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private ProfileInvitationRepository profileInvitationRepository;
    @Mock
    private ProfileShareRepository profileShareRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    private ProfileShareService profileShareService;

    @BeforeEach
    void setUp() {
        profileShareService = new ProfileShareService(
                profileRepository,
                profileInvitationRepository,
                profileShareRepository,
                userRepository,
                emailService
        );
        ReflectionTestUtils.setField(profileShareService, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    void inviteByEmail_createsPendingInvitationAndSendsEmail() {
        UUID ownerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        Profile profile = profile(profileId, ownerId);
        User inviter = user(ownerId, "owner@healthlens.vn");

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(inviter));
        when(profileInvitationRepository.save(any(ProfileInvitation.class))).thenAnswer(invocation -> {
            ProfileInvitation invitation = invocation.getArgument(0);
            invitation.setId(UUID.randomUUID());
            invitation.setCreatedAt(Instant.now());
            return invitation;
        });

        ProfileInvitationResponse response = profileShareService.inviteByEmail(ownerId, profileId, "viewer@healthlens.vn");

        assertThat(response.email()).isEqualTo("viewer@healthlens.vn");
        assertThat(response.status()).isEqualTo("pending");
        verify(emailService).sendProfileInvitationEmail(any(User.class), any(String.class), any(String.class));
    }

    @Test
    void acceptInvitation_withoutLogin_returnsRegisterRedirect() {
        ProfileInvitation invitation = invitation(UUID.randomUUID(), "viewer@healthlens.vn", "pending");
        when(profileInvitationRepository.findByToken("token-1")).thenReturn(Optional.of(invitation));

        AcceptInvitationResultResponse result = profileShareService.acceptInvitation("token-1", null);
        assertThat(result.outcome()).isEqualTo("require-login");
        assertThat(result.redirectUrl()).contains("/login?returnUrl=");
        assertThat(result.redirectUrl()).contains("invitations%2Faccept");
        assertThat(result.redirectUrl()).contains("token-1");
    }

    @Test
    void acceptInvitation_withExpiredToken_returnsFamilyProfilesWithoutUpdatingInvitation() {
        ProfileInvitation invitation = invitation(UUID.randomUUID(), "viewer@healthlens.vn", "pending");
        invitation.setExpiresAt(Instant.now().minusSeconds(5));
        when(profileInvitationRepository.findByToken("token-2")).thenReturn(Optional.of(invitation));

        AcceptInvitationResultResponse result = profileShareService.acceptInvitation("token-2", UUID.randomUUID());

        assertThat(result.outcome()).isEqualTo("expired");
        assertThat(result.redirectUrl()).isEqualTo("/profiles");
        verify(profileInvitationRepository, never()).save(any(ProfileInvitation.class));
    }

    @Test
    void acceptInvitation_acceptedButPastExpiry_revisit_redirectsFamilyProfiles() {
        UUID profileId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        ProfileInvitation invitation = invitation(profileId, "viewer@healthlens.vn", "accepted");
        invitation.setExpiresAt(Instant.now().minusSeconds(60));

        when(profileInvitationRepository.findByToken("token-accepted-old")).thenReturn(Optional.of(invitation));

        AcceptInvitationResultResponse result = profileShareService.acceptInvitation("token-accepted-old", viewerId);

        assertThat(result.outcome()).isEqualTo("expired");
        assertThat(result.redirectUrl()).isEqualTo("/profiles");
        verify(profileInvitationRepository, never()).save(invitation);
    }

    @Test
    void acceptInvitation_acceptedButPastExpiry_anonymous_redirectsFamilyProfiles() {
        UUID profileId = UUID.randomUUID();
        ProfileInvitation invitation = invitation(profileId, "viewer@healthlens.vn", "accepted");
        invitation.setExpiresAt(Instant.now().minusSeconds(60));

        when(profileInvitationRepository.findByToken("token-accepted-anon")).thenReturn(Optional.of(invitation));

        AcceptInvitationResultResponse result = profileShareService.acceptInvitation("token-accepted-anon", null);

        assertThat(result.outcome()).isEqualTo("expired");
        assertThat(result.redirectUrl()).isEqualTo("/profiles");
        verify(profileInvitationRepository, never()).save(invitation);
    }

    @Test
    void acceptInvitation_loggedInAndEmailMatch_createsShareAndRedirectsToHistory() {
        UUID ownerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        ProfileInvitation invitation = invitation(profileId, "viewer@healthlens.vn", "pending");
        User viewer = user(viewerId, "viewer@healthlens.vn");
        Profile profile = profile(profileId, ownerId);

        when(profileInvitationRepository.findByToken("token-3")).thenReturn(Optional.of(invitation));
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

        when(profileShareRepository.existsByProfileIdAndViewerIdAndRevokedAtIsNull(profileId, viewerId))
                .thenReturn(false);
        when(profileShareRepository.save(any(ProfileShare.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profileInvitationRepository.save(any(ProfileInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptInvitationResultResponse result = profileShareService.acceptInvitation("token-3", viewerId);

        assertThat(result.outcome()).isEqualTo("accepted");
        assertThat(result.redirectUrl()).isEqualTo("/profiles");
        verify(profileShareRepository).save(any(ProfileShare.class));
        verify(profileInvitationRepository).save(invitation);
    }

    @Test
    void acceptInvitation_emailMismatch_throwsForbidden() {
        UUID viewerId = UUID.randomUUID();
        ProfileInvitation invitation = invitation(UUID.randomUUID(), "other@healthlens.vn", "pending");
        User viewer = user(viewerId, "viewer@healthlens.vn");

        when(profileInvitationRepository.findByToken("token-4")).thenReturn(Optional.of(invitation));
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> profileShareService.acceptInvitation("token-4", viewerId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listInvitations_nonOwner_forbidden() {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile(profileId, ownerId)));

        assertThatThrownBy(() -> profileShareService.listInvitations(strangerId, profileId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listInvitations_marksPastDuePendingAsExpired() {
        UUID ownerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        ProfileInvitation stale = invitation(profileId, "late@healthlens.vn", "pending");
        stale.setExpiresAt(Instant.now().minusSeconds(60));

        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile(profileId, ownerId)));
        when(profileInvitationRepository.findAllByProfileIdOrderByCreatedAtDesc(profileId)).thenReturn(List.of(stale));
        when(profileInvitationRepository.save(any(ProfileInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProfileInvitationResponse> result = profileShareService.listInvitations(ownerId, profileId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().status()).isEqualTo("expired");
        verify(profileInvitationRepository).save(stale);
    }

    @Test
    void inviteByEmail_profileNotFound_throwsNotFound() {
        when(profileRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profileShareService.inviteByEmail(UUID.randomUUID(), UUID.randomUUID(), "a@b.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listIncomingInvitations_returnsPendingRowsWithAcceptPath() {
        UUID ownerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        ProfileInvitation inv = invitation(profileId, "viewer@healthlens.vn", "pending");
        inv.setInviterId(inviterId);
        inv.setToken("invite-token-xyz");
        Profile profile = profile(profileId, ownerId);
        profile.setDisplayName("Ba");
        User viewer = user(viewerId, "viewer@healthlens.vn");
        User inviterUser = user(inviterId, "owner@healthlens.vn");
        inviterUser.setFullName("Nguyễn Văn A");

        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(profileInvitationRepository.findAllByInviteeEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
                "viewer@healthlens.vn", "pending")).thenReturn(Collections.singletonList(inv));
        when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviterUser));

        List<IncomingProfileInvitationResponse> list = profileShareService.listIncomingInvitations(viewerId);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().profileDisplayName()).isEqualTo("Ba");
        assertThat(list.getFirst().inviterName()).isEqualTo("Nguyễn Văn A");
        assertThat(list.getFirst().acceptPath()).contains("invite-token-xyz");
    }

    @Test
    void rejectIncomingInvitation_marksInvitationRejected() {
        UUID viewerId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();
        ProfileInvitation invitation = invitation(UUID.randomUUID(), "viewer@healthlens.vn", "pending");
        invitation.setId(invitationId);
        User viewer = user(viewerId, "viewer@healthlens.vn");

        when(profileInvitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(profileInvitationRepository.save(any(ProfileInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileShareService.rejectIncomingInvitation(invitationId, viewerId);

        assertThat(invitation.getStatus()).isEqualTo("rejected");
        verify(profileInvitationRepository).save(invitation);
    }

    private static Profile profile(UUID profileId, UUID ownerId) {
        Profile profile = new Profile();
        profile.setId(profileId);
        User owner = new User();
        owner.setId(ownerId);
        owner.setEmail("owner@healthlens.vn");
        owner.setBirthDate(LocalDate.of(1990, 1, 1));
        owner.setFullName("Owner");
        owner.setPasswordHash("hash");
        profile.setUser(owner);
        return profile;
    }

    private static User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("User");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPasswordHash("hash");
        return user;
    }

    private static ProfileInvitation invitation(UUID profileId, String email, String status) {
        ProfileInvitation invitation = new ProfileInvitation();
        invitation.setId(UUID.randomUUID());
        invitation.setProfileId(profileId);
        invitation.setInviterId(UUID.randomUUID());
        invitation.setInviteeEmail(email);
        invitation.setToken("token");
        invitation.setStatus(status);
        invitation.setCreatedAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        return invitation;
    }
}
