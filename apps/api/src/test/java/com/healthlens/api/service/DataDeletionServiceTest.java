package com.healthlens.api.service;

import com.healthlens.api.dto.request.DeleteAccountRequest;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.repository.ConsentLogRepository;
import com.healthlens.api.repository.DataDeletionRequestRepository;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.HealthRecordInvitationRepository;
import com.healthlens.api.repository.HealthRecordShareRepository;
import com.healthlens.api.repository.FollowUpReminderRepository;
import com.healthlens.api.repository.OcrDeadLetterRepository;
import com.healthlens.api.repository.OcrJobExecutionRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.ProfileInvitationRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.AccountStatusCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataDeletionService Tests")
class DataDeletionServiceTest {

    @Mock private DataDeletionRequestRepository deletionRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailEventPublisher emailEventPublisher;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ProfileShareRepository profileShareRepository;
    @Mock private ProfileInvitationRepository profileInvitationRepository;
    @Mock private HealthRecordShareRepository healthRecordShareRepository;
    @Mock private HealthRecordInvitationRepository healthRecordInvitationRepository;
    @Mock private FollowUpReminderRepository followUpReminderRepository;
    @Mock private OcrDeadLetterRepository ocrDeadLetterRepository;
    @Mock private OcrJobExecutionRepository ocrJobExecutionRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private ConsentLogRepository consentLogRepository;
    @Mock private StorageService storageService;
    @Mock private AccountStatusCache accountStatusCache;
    @Mock private DataDeletionService selfProxy;
    @Mock private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;

    private DataDeletionService dataDeletionService;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        dataDeletionService = new DataDeletionService(
                deletionRequestRepository,
                userRepository,
                passwordEncoder,
                emailEventPublisher,
                healthRecordRepository,
                profileRepository,
                profileShareRepository,
                profileInvitationRepository,
                healthRecordShareRepository,
                healthRecordInvitationRepository,
                followUpReminderRepository,
                ocrDeadLetterRepository,
                ocrJobExecutionRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                consentLogRepository,
                storageService,
                accountStatusCache,
                auditEventRecorder,
                selfProxy,
                "http://localhost:3000/cancel-deletion"
        );

        userId = UUID.randomUUID();
        testUser = createTestUser();
    }

    // ========== CREATE DELETION REQUEST TESTS ==========

    @Test
    @DisplayName("AC #1: Create deletion request with valid password schedules 72h deletion and freezes account")
    void createDeletionRequest_success() {
        DeleteAccountRequest request = new DeleteAccountRequest("ValidPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("ValidPassword123", testUser.getPasswordHash())).thenReturn(true);
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionRequestStatus.PENDING)).thenReturn(false);
        when(deletionRequestRepository.saveAndFlush(any(DataDeletionRequest.class)))
                .thenAnswer(inv -> {
                    DataDeletionRequest r = inv.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(UUID.randomUUID());
                    }
                    return r;
                });

        var response = dataDeletionService.createDeletionRequest(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("72 giờ");
        assertThat(response.requestId()).isNotNull();
        assertThat(response.scheduledDeletionAt()).isNotNull();
        assertThat(response.cancellationLink()).contains("cancel-deletion");
        assertThat(response.cancellationLink()).contains("?token=");
        assertThat(response.cancellationLink()).doesNotContain("email=");
        assertThat(response.cancellationLink()).doesNotContain(testUser.getEmail());
        assertThat(response.cancellationLink()).doesNotContain("requestedAt=");
        assertThat(response.cancellationLink()).doesNotContain("scheduledDeletionAt=");

        ArgumentCaptor<DataDeletionRequest> captor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(deletionRequestRepository).saveAndFlush(captor.capture());

        DataDeletionRequest savedRequest = captor.getValue();
        assertThat(savedRequest.getUserId()).isEqualTo(userId);
        assertThat(savedRequest.getStatus()).isEqualTo(DeletionRequestStatus.PENDING);
        assertThat(savedRequest.getCancellationTokenHash()).isNotNull();
        assertThat(savedRequest.getCancellationTokenHash()).hasSize(64);
        assertThat(response.cancellationLink()).doesNotContain(savedRequest.getCancellationTokenHash());
        assertThat(savedRequest.getScheduledDeletionAt())
                .isAfter(savedRequest.getRequestedAt().plusSeconds(72L * 3600 - 5));

        // AC #3: revoke active sessions and switch account status to PENDING_DELETION
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING_DELETION);
        verify(accountStatusCache).put(userId, AccountStatus.PENDING_DELETION);

        // AC #4: confirmation email
        verify(emailEventPublisher).publishDeletionConfirmation(any(User.class), any(DataDeletionRequest.class), anyString());

        ArgumentCaptor<Map<String, ?>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditEventRecorder).recordEvent(eq(userId), anyString(), anyString(), eq(userId), auditCaptor.capture());
        assertThat(auditCaptor.getValue()).containsKey("requestId");
        assertThat(auditCaptor.getValue()).containsKey("scheduledDeletionAt");
        assertThat(auditCaptor.getValue()).doesNotContainKey("email");
    }

    @Test
    @DisplayName("AC #1: Password verification failed — no request, no email")
    void createDeletionRequest_invalidPassword() {
        DeleteAccountRequest request = new DeleteAccountRequest("WrongPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mật khẩu không đúng");

        verify(deletionRequestRepository, never()).saveAndFlush(any());
        verify(emailEventPublisher, never()).publishDeletionConfirmation(any(), any(), any());
    }

    @Test
    @DisplayName("AC #1: User not found — no request, no email")
    void createDeletionRequest_userNotFound() {
        DeleteAccountRequest request = new DeleteAccountRequest("ValidPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(deletionRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC #1: User already has pending deletion request")
    void createDeletionRequest_alreadyPending() {
        DeleteAccountRequest request = new DeleteAccountRequest("ValidPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("ValidPassword123", testUser.getPasswordHash())).thenReturn(true);
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionRequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bạn đã có một yêu cầu xóa đang chờ xử lý");

        verify(deletionRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC #1: Concurrent insert loses race — partial unique index maps to same message as duplicate pending")
    void createDeletionRequest_duplicatePendingFromUniqueConstraint() {
        DeleteAccountRequest request = new DeleteAccountRequest("ValidPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("ValidPassword123", testUser.getPasswordHash())).thenReturn(true);
        when(deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionRequestStatus.PENDING)).thenReturn(false);
        when(deletionRequestRepository.saveAndFlush(any(DataDeletionRequest.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        new RuntimeException(
                                "ERROR: duplicate key value violates unique constraint \""
                                        + DataDeletionService.PENDING_DELETION_UNIQUE_INDEX_NAME + "\"")));

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bạn đã có một yêu cầu xóa đang chờ xử lý");

        verify(refreshTokenRepository, never()).revokeAllByUserId(any(), any());
        verify(userRepository, never()).save(any());
        verify(emailEventPublisher, never()).publishDeletionConfirmation(any(), any(), any());
    }

    // ========== CANCEL DELETION REQUEST TESTS ==========

    @Test
    @DisplayName("AC #5: Cancel deletion request with valid token restores account")
    void cancelDeletionRequest_success() {
        String cancellationToken = "valid-token-123";
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setId(UUID.randomUUID());
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setScheduledDeletionAt(Instant.now().plusSeconds(3600));
        deletionRequest.setCancellationTokenHash(cancellationToken);

        User frozenUser = createTestUser();
        frozenUser.setAccountStatus(AccountStatus.PENDING_DELETION);

        when(deletionRequestRepository.findByCancellationTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(frozenUser));

        dataDeletionService.cancelDeletionRequest(cancellationToken);

        ArgumentCaptor<String> tokenLookupCaptor = ArgumentCaptor.forClass(String.class);
        verify(deletionRequestRepository).findByCancellationTokenHashForUpdate(tokenLookupCaptor.capture());
        assertThat(tokenLookupCaptor.getValue()).hasSize(64);
        assertThat(tokenLookupCaptor.getValue()).isNotEqualTo(cancellationToken);

        ArgumentCaptor<DataDeletionRequest> captor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(deletionRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeletionRequestStatus.CANCELLED);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(accountStatusCache).put(userId, AccountStatus.ACTIVE);

        verify(emailEventPublisher).publishDeletionCancellation(any(User.class));
        ArgumentCaptor<Map<String, ?>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditEventRecorder).recordEvent(eq(userId), anyString(), anyString(), eq(userId), auditCaptor.capture());
        assertThat(auditCaptor.getValue()).containsKey("requestId");
        assertThat(auditCaptor.getValue()).doesNotContainKey("email");
    }

    @Test
    @DisplayName("AC #5: Invalid cancellation token rejected")
    void cancelDeletionRequest_invalidToken() {
        String invalidToken = "invalid-token-xyz";

        when(deletionRequestRepository.findByCancellationTokenHashForUpdate(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(invalidToken))
                .isInstanceOf(com.healthlens.api.exception.DeletionCancellationTokenException.class)
                .hasMessage("Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực.");

        verify(userRepository, never()).save(any());
        verify(emailEventPublisher, never()).publishDeletionCancellation(any());
    }

    @Test
    @DisplayName("AC #5: Cannot cancel non-pending deletion request")
    void cancelDeletionRequest_notPending() {
        String token = "some-token";
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.COMPLETED);
        deletionRequest.setCancellationTokenHash(token);

        when(deletionRequestRepository.findByCancellationTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(deletionRequest));

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(token))
                .isInstanceOf(com.healthlens.api.exception.DeletionCancellationConflictException.class)
                .hasMessage("Yêu cầu xóa này không thể hủy được");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC #5: Cannot cancel after grace period — scheduledDeletionAt has passed")
    void cancelDeletionRequest_afterGracePeriod() {
        String token = "expired-token";
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setScheduledDeletionAt(Instant.now().minusSeconds(60));
        deletionRequest.setCancellationTokenHash(token);

        when(deletionRequestRepository.findByCancellationTokenHashForUpdate(anyString()))
                .thenReturn(Optional.of(deletionRequest));

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(token))
                .isInstanceOf(com.healthlens.api.exception.DeletionCancellationTokenException.class)
                .hasMessageContaining("Thời gian cho phép hủy");

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
        verify(deletionRequestRepository, never()).save(any());
        verify(emailEventPublisher, never()).publishDeletionCancellation(any());
    }

    @Test
    @DisplayName("AC #5: Cannot cancel when account is no longer frozen for deletion (e.g. deletion already executed)")
    void cancelDeletionRequest_accountNotPendingDeletion() {
        String token = "token-still-pending-but-user-not";
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setScheduledDeletionAt(Instant.now().plusSeconds(3600));
        deletionRequest.setCancellationTokenHash(token);

        User deletedUser = createTestUser();
        deletedUser.setAccountStatus(AccountStatus.DELETED);

        when(deletionRequestRepository.findByCancellationTokenHashForUpdate(anyString())).thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(token))
                .isInstanceOf(com.healthlens.api.exception.DeletionCancellationForbiddenException.class)
                .hasMessageContaining("không còn trong trạng thái chờ xóa");

        verify(deletionRequestRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    // ========== SCHEDULED DELETION + EXECUTE DATA DELETION ==========

    @Test
    @DisplayName("AC #2: processDeletionRequests dispatches each overdue request via the self-proxy")
    void processDeletionRequests_dispatchesOverdueRequests() {
        when(selfProxy.executeNextDueDataDeletion(any(Instant.class)))
                .thenReturn(true, true, false);

        dataDeletionService.processDeletionRequests();

        verify(selfProxy, times(3)).executeNextDueDataDeletion(any(Instant.class));
    }

    @Test
    @DisplayName("AC #2: processDeletionRequests stops current polling cycle if one claimed request fails")
    void processDeletionRequests_continuesAfterFailure() {
        when(selfProxy.executeNextDueDataDeletion(any(Instant.class)))
                .thenReturn(true)
                .thenThrow(new RuntimeException("boom"));

        dataDeletionService.processDeletionRequests();

        verify(selfProxy, times(2)).executeNextDueDataDeletion(any(Instant.class));
    }

    @Test
    @DisplayName("AC #2: executeDataDeletion wipes files, health data, tokens and consent logs")
    void executeDataDeletion_wipesAllUserData() {
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        UUID requestId = UUID.randomUUID();
        deletionRequest.setId(requestId);
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setRequestedAt(Instant.now().minusSeconds(72L * 3600 + 60));
        deletionRequest.setScheduledDeletionAt(Instant.now().minusSeconds(60));

        when(deletionRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(healthRecordRepository.findFileKeysByUserId(userId))
                .thenReturn(List.of(
                        "health-records/" + userId + "/profile/record/original.pdf",
                        "custom-imports/" + userId + "/legacy-image.png"
                ));
        String originalEmail = testUser.getEmail();

        dataDeletionService.executeDataDeletion(requestId);

        // S3 objects are collected from reachable DB rows before those rows are deleted.
        verify(healthRecordRepository).findFileKeysByUserId(userId);
        verify(storageService).deleteObjects(List.of(
                "health-records/" + userId + "/profile/record/original.pdf",
                "custom-imports/" + userId + "/legacy-image.png"
        ));
        // Prefix cleanup remains as a backstop for abandoned upload reservations.
        verify(storageService).deleteObjectsByPrefix(startsWith("health-records/" + userId));
        verify(storageService).deleteObjectsByPrefix(startsWith("avatars/" + userId));

        // DB tables wiped in dependency order
        verify(healthRecordShareRepository).deleteAllByUserParticipation(userId);
        verify(healthRecordInvitationRepository).deleteAllByUserParticipation(userId);
        verify(profileShareRepository).deleteAllByUserParticipation(userId);
        verify(profileInvitationRepository).deleteAllByUserParticipation(userId);
        verify(profileInvitationRepository).deleteAllByInviteeEmailIgnoreCase(originalEmail);
        verify(healthRecordInvitationRepository).deleteAllByInviteeEmailIgnoreCase(originalEmail);
        verify(followUpReminderRepository).deleteAllByUserId(userId);
        verify(ocrDeadLetterRepository).deleteAllByUserId(userId);
        verify(ocrJobExecutionRepository).deleteAllByUserId(userId);
        verify(healthRecordRepository).deleteAllByUserId(userId);
        verify(profileRepository).deleteAllByUserId(userId);
        verify(emailVerificationTokenRepository).deleteAllByUserId(userId);
        verify(passwordResetTokenRepository).deleteAllByUserId(userId);
        verify(refreshTokenRepository).deleteAllByUserId(userId);
        verify(consentLogRepository).deleteAllByUserId(userId);

        // Deletion request marked completed
        ArgumentCaptor<DataDeletionRequest> reqCaptor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(deletionRequestRepository).save(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getStatus()).isEqualTo(DeletionRequestStatus.COMPLETED);
        assertThat(reqCaptor.getValue().getCompletedAt()).isNotNull();

        // User row anonymised + status DELETED
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(saved.getEmail()).startsWith("deleted+").endsWith("@deleted.local");
        assertThat(saved.getFullName()).isEqualTo("[Deleted User]");
        assertThat(saved.isEmailVerified()).isFalse();

        verify(accountStatusCache).put(userId, AccountStatus.DELETED);
        verify(emailEventPublisher, times(1)).publishDeletionCompletion(any(User.class));
        ArgumentCaptor<Map<String, ?>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditEventRecorder).recordEvent(
                eq(userId),
                eq(com.healthlens.api.audit.AuditActions.COMPLETE_ACCOUNT_DELETION),
                eq(com.healthlens.api.audit.AuditResourceTypes.USER),
                eq(userId),
                auditCaptor.capture()
        );
        assertThat(auditCaptor.getValue().get("requestId")).isEqualTo(requestId.toString());
        assertThat(auditCaptor.getValue().get("retentionClass")).isEqualTo("RIGHT_TO_DELETE_EVIDENCE");
        assertThat(auditCaptor.getValue()).doesNotContainKey("email");
    }

    @Test
    @DisplayName("AC #4: executeDataDeletion does not make DB rows unreachable when exact object deletion fails")
    void executeDataDeletion_abortsWhenExactObjectDeletionFails() {
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        UUID requestId = UUID.randomUUID();
        deletionRequest.setId(requestId);
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setRequestedAt(Instant.now().minusSeconds(72L * 3600 + 60));
        deletionRequest.setScheduledDeletionAt(Instant.now().minusSeconds(60));

        when(deletionRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(healthRecordRepository.findFileKeysByUserId(userId))
                .thenReturn(List.of("custom/%s/report.pdf".formatted(userId)));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage unavailable"))
                .when(storageService).deleteObjects(any());

        assertThatThrownBy(() -> dataDeletionService.executeDataDeletion(requestId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage unavailable");

        verify(healthRecordRepository, never()).deleteAllByUserId(any());
        verify(profileRepository, never()).deleteAllByUserId(any());
        verify(deletionRequestRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(emailEventPublisher, never()).publishDeletionCompletion(any());
    }

    @Test
    @DisplayName("AC #1: executeNextDueDataDeletion keeps SKIP LOCKED row in the deletion transaction")
    void executeNextDueDataDeletion_processesOneSkipLockedRequest() {
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        UUID requestId = UUID.randomUUID();
        deletionRequest.setId(requestId);
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setRequestedAt(Instant.now().minusSeconds(72L * 3600 + 60));
        deletionRequest.setScheduledDeletionAt(Instant.now().minusSeconds(60));

        when(deletionRequestRepository.findNextDuePendingForUpdateSkipLocked(
                eq(DeletionRequestStatus.PENDING.name()), any(Instant.class)))
                .thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(healthRecordRepository.findFileKeysByUserId(userId)).thenReturn(List.of());

        boolean processed = dataDeletionService.executeNextDueDataDeletion(Instant.now());

        assertThat(processed).isTrue();
        verify(deletionRequestRepository).findNextDuePendingForUpdateSkipLocked(
                eq(DeletionRequestStatus.PENDING.name()), any(Instant.class));
        verify(deletionRequestRepository).save(deletionRequest);
        assertThat(deletionRequest.getStatus()).isEqualTo(DeletionRequestStatus.COMPLETED);
    }

    @Test
    @DisplayName("AC #2: executeDataDeletion is a no-op if request is already terminal")
    void executeDataDeletion_skipsNonPending() {
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        UUID requestId = UUID.randomUUID();
        deletionRequest.setId(requestId);
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.CANCELLED);

        when(deletionRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(deletionRequest));

        dataDeletionService.executeDataDeletion(requestId);

        verify(storageService, never()).deleteObjectsByPrefix(anyString());
        verify(healthRecordRepository, never()).deleteAllByUserId(any());
        verify(userRepository, never()).save(any());
        verify(accountStatusCache, never()).put(any(), any());
    }

    // ========== HELPER METHODS ==========

    private DataDeletionRequest overdueRequest() {
        DataDeletionRequest r = new DataDeletionRequest();
        r.setId(UUID.randomUUID());
        r.setUserId(UUID.randomUUID());
        r.setStatus(DeletionRequestStatus.PENDING);
        r.setRequestedAt(Instant.now().minusSeconds(72L * 3600 + 60));
        r.setScheduledDeletionAt(Instant.now().minusSeconds(60));
        return r;
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        user.setPasswordHash("$2a$12$hashed_password_here");
        user.setEmailVerified(true);
        user.setRole(UserRole.ROLE_USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}
