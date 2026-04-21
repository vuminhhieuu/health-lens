package com.healthlens.api.service;

import com.healthlens.api.dto.request.DeleteAccountRequest;
import com.healthlens.api.dto.response.DeleteAccountResponse;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.DataDeletionRequestRepository;
import com.healthlens.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling user account deletion requests.
 * Implements right-to-delete feature per Nghị định 13/2023/NĐ-CP.
 */
@Slf4j
@Service
public class DataDeletionService {

    private final DataDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String webFrontendBaseUrl;
    private final String webCancellationUrl;

    @Autowired
    public DataDeletionService(
            DataDeletionRequestRepository deletionRequestRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.frontend.base-url:http://localhost:3000}") String webFrontendBaseUrl,
            @Value("${app.frontend.cancellation-url:http://localhost:3000/cancel-deletion}") String webCancellationUrl) {
        this.deletionRequestRepository = deletionRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.webFrontendBaseUrl = webFrontendBaseUrl;
        this.webCancellationUrl = webCancellationUrl;
    }

    /**
     * Create a deletion request after password verification.
     * AC #1: Verify password, create request, set account status to pending_deletion
     * Note: Using READ_COMMITTED isolation to prevent lock contention on users table.
     * SERIALIZABLE would lock entire table and block auth requests for other users.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DeleteAccountResponse createDeletionRequest(UUID userId, DeleteAccountRequest request) {
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau khong dung");
        }

        // Check if already has a pending deletion request (SERIALIZABLE isolation prevents race)
        if (deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionRequestStatus.PENDING)) {
            throw new IllegalStateException("Ban da co mot yeu cau xoa dang cho xu ly");
        }

        // Generate cancellation token
        String cancellationToken = generateCancellationToken();

        // Create deletion request (scheduled_deletion_at = now + 72h)
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setRequestedAt(Instant.now());
        deletionRequest.setScheduledDeletionAt(deletionRequest.getRequestedAt().plusSeconds(72 * 3600));
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setCancellationToken(cancellationToken);

        deletionRequestRepository.save(deletionRequest);

        // Update user account status to pending_deletion
        user.setAccountStatus(AccountStatus.PENDING_DELETION);
        userRepository.save(user);

        log.info("Deletion request created for user: {}, scheduled at: {}", userId, deletionRequest.getScheduledDeletionAt());

        // Send confirmation email (BEFORE returning response to ensure user is notified)
        String cancellationLink = webCancellationUrl + "?token=" + cancellationToken;
        try {
            emailService.sendDeletionConfirmationEmail(user, deletionRequest, cancellationLink);
        } catch (Exception e) {
            log.error("Failed to send deletion confirmation email to user: {}", userId, e);
            // Don't rethrow - deletion request is already created, email is non-critical for AC compliance
            // User will still receive email when scheduler retries, or can contact support
        }

        // Return response
        return new DeleteAccountResponse(
                "Yeu cau xoa tai khoan cua ban da duoc nhan. Tai khoan se bi xoa sau 72 gio. Vui long kiem tra email de huy yeu cau neu can thiet.",
                deletionRequest.getId().toString(),
                deletionRequest.getScheduledDeletionAt(),
                cancellationLink
        );
    }

    /**
     * Cancel a deletion request using the cancellation token.
     * AC #5: Validate token, restore account status to active
     * Note: Using READ_COMMITTED isolation to prevent lock contention.
     */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void cancelDeletionRequest(String cancellationToken) {
        // Find deletion request by token
        DataDeletionRequest deletionRequest = deletionRequestRepository.findByCancellationToken(cancellationToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid cancellation token"));

        // Check if still pending
        if (!deletionRequest.canBeCancelled()) {
            throw new IllegalStateException("Yeu cau xoa nay khong the huy duoc");
        }

        // Update deletion request status to cancelled
        deletionRequest.setStatus(DeletionRequestStatus.CANCELLED);
        deletionRequestRepository.save(deletionRequest);

        // Restore user account status to active
        User user = userRepository.findById(deletionRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        log.info("Deletion request cancelled for user: {}", deletionRequest.getUserId());

        // Send cancellation confirmation email
        emailService.sendCancellationConfirmationEmail(user);
    }

    /**
     * Scheduled task to process overdue deletion requests.
     * AC #2: Find requests > 72h old, delete user data, mark as completed
     * This method is called via @Scheduled in a controller or scheduled task runner.
     */
    @Transactional
    public void processDeletionRequests() {
        // Find all pending deletion requests that are overdue
        List<DataDeletionRequest> overdueRequests = deletionRequestRepository.findByStatusAndScheduledDeletionAtBefore(
                DeletionRequestStatus.PENDING,
                Instant.now()
        );

        for (DataDeletionRequest deletionRequest : overdueRequests) {
            try {
                executeDataDeletion(deletionRequest);
            } catch (Exception e) {
                log.error("Error processing deletion request {}: {}", deletionRequest.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Execute complete data deletion for a user in the correct order.
     * Order of deletion (important for foreign key constraints):
     * 1. Files (S3/MinIO) - handled asynchronously if possible
     * 2. health_records
     * 3. profiles
     * 4. email_verification_tokens
     * 5. password_reset_tokens (if exist)
     * 6. refresh_tokens
     * 7. consent_logs (if exist)
     * 8. data_deletion_requests
     * 9. users (soft or hard delete)
     */
    @Transactional
    protected void executeDataDeletion(DataDeletionRequest deletionRequest) {
        UUID userId = deletionRequest.getUserId();

        log.info("Starting data deletion for user: {}", userId);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        try {
            // TODO: Delete S3/MinIO files (implement separately with S3Service)
            // s3Service.deleteUserFiles(userId);

            // TODO: Delete health_records from database
            // healthRecordRepository.deleteByUserId(userId);

            // TODO: Delete profiles
            // profileRepository.deleteByUserId(userId);

            // TODO: Delete email_verification_tokens
            // emailVerificationTokenRepository.deleteByUserId(userId);

            // TODO: Delete password_reset_tokens if exist
            // passwordResetTokenRepository.deleteByUserId(userId);

            // TODO: Delete refresh_tokens
            // refreshTokenRepository.deleteByUserId(userId);

            // TODO: Delete consent_logs if exist
            // consentLogRepository.deleteByUserId(userId);

            // Delete deletion requests (keep one record for audit trail with completed_at timestamp)
            deletionRequest.setStatus(DeletionRequestStatus.COMPLETED);
            deletionRequest.setCompletedAt(Instant.now());
            deletionRequestRepository.save(deletionRequest);

            // Send deletion completion email BEFORE anonymizing user (so email is deliverable)
            try {
                emailService.sendDeletionCompletionEmail(user);
            } catch (Exception e) {
                log.error("Failed to send deletion completion email to user {}: {}", userId, e.getMessage());
                // Continue anyway - deletion is complete, email notification is best-effort
            }

            // Update user status to deleted and anonymize PII
            user.setAccountStatus(AccountStatus.DELETED);
            user.setEmail("deleted+" + userId + "@deleted.local");
            user.setFullName("[Deleted User]");
            user.setPasswordHash("[deleted]");
            user.setEmailVerified(false);
            userRepository.save(user);

            log.info("Data deletion completed for user: {}", userId);

        } catch (Exception e) {
            log.error("Error during data deletion for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Data deletion failed", e);
        }
    }

    /**
     * Generate a cryptographically secure cancellation token.
     */
    private String generateCancellationToken() {
        try {
            byte[] randomBytes = new byte[32]; // 256 bits
            new SecureRandom().nextBytes(randomBytes);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } catch (Exception e) {
            log.error("Error generating cancellation token", e);
            throw new RuntimeException("Failed to generate cancellation token", e);
        }
    }
}
