package com.healthlens.api.service;

import com.healthlens.api.dto.request.DeleteAccountRequest;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.ConsentLogRepository;
import com.healthlens.api.repository.DataDeletionRequestRepository;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
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
    @Mock private EmailService emailService;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private ConsentLogRepository consentLogRepository;
    @Mock private StorageService storageService;
    @Mock private DataDeletionService selfProxy;

    private DataDeletionService dataDeletionService;
    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        dataDeletionService = new DataDeletionService(
                deletionRequestRepository,
                userRepository,
                passwordEncoder,
                emailService,
                healthRecordRepository,
                profileRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                consentLogRepository,
                storageService,
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
        when(deletionRequestRepository.save(any(DataDeletionRequest.class)))
                .thenAnswer(inv -> {
                    DataDeletionRequest r = inv.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(UUID.randomUUID());
                    }
                    return r;
                });

        var response = dataDeletionService.createDeletionRequest(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.message()).contains("72 gio");
        assertThat(response.requestId()).isNotNull();
        assertThat(response.scheduledDeletionAt()).isNotNull();
        assertThat(response.cancellationLink()).contains("cancel-deletion");

        ArgumentCaptor<DataDeletionRequest> captor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(deletionRequestRepository).save(captor.capture());

        DataDeletionRequest savedRequest = captor.getValue();
        assertThat(savedRequest.getUserId()).isEqualTo(userId);
        assertThat(savedRequest.getStatus()).isEqualTo(DeletionRequestStatus.PENDING);
        assertThat(savedRequest.getCancellationToken()).isNotNull();
        assertThat(savedRequest.getScheduledDeletionAt())
                .isAfter(savedRequest.getRequestedAt().plusSeconds(72L * 3600 - 5));

        // AC #3: revoke active sessions and switch account status to PENDING_DELETION
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.PENDING_DELETION);

        // AC #4: confirmation email
        verify(emailService).sendDeletionConfirmationEmail(any(User.class), any(DataDeletionRequest.class), anyString());
    }

    @Test
    @DisplayName("AC #1: Password verification failed — no request, no email")
    void createDeletionRequest_invalidPassword() {
        DeleteAccountRequest request = new DeleteAccountRequest("WrongPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mat khau khong dung");

        verify(deletionRequestRepository, never()).save(any());
        verify(emailService, never()).sendDeletionConfirmationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("AC #1: User not found — no request, no email")
    void createDeletionRequest_userNotFound() {
        DeleteAccountRequest request = new DeleteAccountRequest("ValidPassword123");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataDeletionService.createDeletionRequest(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");

        verify(deletionRequestRepository, never()).save(any());
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
                .hasMessage("Ban da co mot yeu cau xoa dang cho xu ly");
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
        deletionRequest.setCancellationToken(cancellationToken);

        when(deletionRequestRepository.findByCancellationToken(cancellationToken))
                .thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        dataDeletionService.cancelDeletionRequest(cancellationToken);

        ArgumentCaptor<DataDeletionRequest> captor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(deletionRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DeletionRequestStatus.CANCELLED);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(emailService).sendCancellationConfirmationEmail(any(User.class));
    }

    @Test
    @DisplayName("AC #5: Invalid cancellation token rejected")
    void cancelDeletionRequest_invalidToken() {
        String invalidToken = "invalid-token-xyz";

        when(deletionRequestRepository.findByCancellationToken(invalidToken))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực.");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendCancellationConfirmationEmail(any());
    }

    @Test
    @DisplayName("AC #5: Cannot cancel non-pending deletion request")
    void cancelDeletionRequest_notPending() {
        String token = "some-token";
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.COMPLETED);
        deletionRequest.setCancellationToken(token);

        when(deletionRequestRepository.findByCancellationToken(token))
                .thenReturn(Optional.of(deletionRequest));

        assertThatThrownBy(() -> dataDeletionService.cancelDeletionRequest(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Yeu cau xoa nay khong the huy duoc");

        verify(userRepository, never()).save(any());
    }

    // ========== SCHEDULED DELETION + EXECUTE DATA DELETION ==========

    @Test
    @DisplayName("AC #2: processDeletionRequests dispatches each overdue request via the self-proxy")
    void processDeletionRequests_dispatchesOverdueRequests() {
        DataDeletionRequest first = overdueRequest();
        DataDeletionRequest second = overdueRequest();
        when(deletionRequestRepository.findByStatusAndScheduledDeletionAtBefore(
                eq(DeletionRequestStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(first, second));

        dataDeletionService.processDeletionRequests();

        verify(selfProxy).executeDataDeletion(first.getId());
        verify(selfProxy).executeDataDeletion(second.getId());
    }

    @Test
    @DisplayName("AC #2: processDeletionRequests continues if one request fails")
    void processDeletionRequests_continuesAfterFailure() {
        DataDeletionRequest first = overdueRequest();
        DataDeletionRequest second = overdueRequest();
        when(deletionRequestRepository.findByStatusAndScheduledDeletionAtBefore(
                eq(DeletionRequestStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(first, second));
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(selfProxy).executeDataDeletion(first.getId());

        dataDeletionService.processDeletionRequests();

        verify(selfProxy).executeDataDeletion(first.getId());
        verify(selfProxy).executeDataDeletion(second.getId());
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

        when(deletionRequestRepository.findById(requestId)).thenReturn(Optional.of(deletionRequest));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        dataDeletionService.executeDataDeletion(requestId);

        // S3 files wiped under the user-scoped prefix
        verify(storageService).deleteObjectsByPrefix(startsWith("health-records/" + userId));

        // DB tables wiped in dependency order
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

        verify(emailService, times(1)).sendDeletionCompletionEmail(any(User.class));
    }

    @Test
    @DisplayName("AC #2: executeDataDeletion is a no-op if request is already terminal")
    void executeDataDeletion_skipsNonPending() {
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        UUID requestId = UUID.randomUUID();
        deletionRequest.setId(requestId);
        deletionRequest.setUserId(userId);
        deletionRequest.setStatus(DeletionRequestStatus.CANCELLED);

        when(deletionRequestRepository.findById(requestId)).thenReturn(Optional.of(deletionRequest));

        dataDeletionService.executeDataDeletion(requestId);

        verify(storageService, never()).deleteObjectsByPrefix(anyString());
        verify(healthRecordRepository, never()).deleteAllByUserId(any());
        verify(userRepository, never()).save(any());
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
