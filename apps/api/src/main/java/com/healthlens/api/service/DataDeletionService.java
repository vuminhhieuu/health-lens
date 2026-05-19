package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.request.DeleteAccountRequest;
import com.healthlens.api.dto.response.CancelDeletionResponse;
import com.healthlens.api.dto.response.DeleteAccountResponse;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.DeletionCancellationConflictException;
import com.healthlens.api.exception.DeletionCancellationForbiddenException;
import com.healthlens.api.exception.DeletionCancellationTokenException;
import com.healthlens.api.repository.ConsentLogRepository;
import com.healthlens.api.repository.DataDeletionRequestRepository;
import com.healthlens.api.repository.EmailVerificationTokenRepository;
import com.healthlens.api.repository.FollowUpReminderRepository;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.HealthRecordInvitationRepository;
import com.healthlens.api.repository.HealthRecordShareRepository;
import com.healthlens.api.repository.OcrDeadLetterRepository;
import com.healthlens.api.repository.OcrJobExecutionRepository;
import com.healthlens.api.repository.PasswordResetTokenRepository;
import com.healthlens.api.repository.ProfileInvitationRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.RefreshTokenRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.security.AccountStatusCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service handling user account right-to-delete per Nghị định 13/2023/NĐ-CP (Story 1.6).
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #createDeletionRequest(UUID, DeleteAccountRequest)} — verify password, create
 *       request with 72-hour grace period, mark account {@code PENDING_DELETION}, send email
 *       (AC #1, AC #4).</li>
 *   <li>{@link #cancelDeletionRequest(String)} — restore account to {@code ACTIVE} via emailed
 *       cancellation token within the grace period (AC #5).</li>
 *   <li>{@link #processDeletionRequests()} — scheduler entry point: find overdue requests and
 *       wipe data (AC #2).</li>
 * </ol>
 */
@Slf4j
@Service
public class DataDeletionService {

    /**
     * Name of partial unique index on {@code data_deletion_requests(user_id) WHERE status='PENDING'};
     * referenced when mapping concurrent-insert violations to the same outcome as {@code existsByUserIdAndStatus}.
     */
    static final String PENDING_DELETION_UNIQUE_INDEX_NAME = "uq_data_deletion_requests_one_pending_per_user";

    private final DataDeletionRequestRepository deletionRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final HealthRecordRepository healthRecordRepository;
    private final ProfileRepository profileRepository;
    private final ProfileShareRepository profileShareRepository;
    private final ProfileInvitationRepository profileInvitationRepository;
    private final HealthRecordShareRepository healthRecordShareRepository;
    private final HealthRecordInvitationRepository healthRecordInvitationRepository;
    private final FollowUpReminderRepository followUpReminderRepository;
    private final OcrDeadLetterRepository ocrDeadLetterRepository;
    private final OcrJobExecutionRepository ocrJobExecutionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ConsentLogRepository consentLogRepository;
    private final StorageService storageService;
    private final DataDeletionService selfProxy;
    private final AccountStatusCache accountStatusCache;
    private final AuditEventRecorder auditEventRecorder;
    private final String webCancellationUrl;

    public DataDeletionService(
            DataDeletionRequestRepository deletionRequestRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            HealthRecordRepository healthRecordRepository,
            ProfileRepository profileRepository,
            ProfileShareRepository profileShareRepository,
            ProfileInvitationRepository profileInvitationRepository,
            HealthRecordShareRepository healthRecordShareRepository,
            HealthRecordInvitationRepository healthRecordInvitationRepository,
            FollowUpReminderRepository followUpReminderRepository,
            OcrDeadLetterRepository ocrDeadLetterRepository,
            OcrJobExecutionRepository ocrJobExecutionRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            ConsentLogRepository consentLogRepository,
            StorageService storageService,
            AccountStatusCache accountStatusCache,
            AuditEventRecorder auditEventRecorder,
            @Lazy DataDeletionService selfProxy,
            @Value("${app.frontend.cancellation-url:http://localhost:3000/cancel-deletion}") String webCancellationUrl) {
        this.deletionRequestRepository = deletionRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.healthRecordRepository = healthRecordRepository;
        this.profileRepository = profileRepository;
        this.profileShareRepository = profileShareRepository;
        this.profileInvitationRepository = profileInvitationRepository;
        this.healthRecordShareRepository = healthRecordShareRepository;
        this.healthRecordInvitationRepository = healthRecordInvitationRepository;
        this.followUpReminderRepository = followUpReminderRepository;
        this.ocrDeadLetterRepository = ocrDeadLetterRepository;
        this.ocrJobExecutionRepository = ocrJobExecutionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.consentLogRepository = consentLogRepository;
        this.storageService = storageService;
        this.accountStatusCache = accountStatusCache;
        this.auditEventRecorder = auditEventRecorder;
        this.selfProxy = selfProxy;
        this.webCancellationUrl = webCancellationUrl;
    }

    /**
     * AC #1, AC #4 — verify password, create request, schedule deletion 72h ahead, send email.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public DeleteAccountResponse createDeletionRequest(UUID userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }

        if (deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionRequestStatus.PENDING)) {
            throw new IllegalStateException("Bạn đã có một yêu cầu xóa đang chờ xử lý");
        }

        String cancellationToken = generateCancellationToken();
        String cancellationTokenHash = hashCancellationToken(cancellationToken);
        Instant requestedAt = Instant.now();

        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setUserId(userId);
        deletionRequest.setRequestedAt(requestedAt);
        deletionRequest.setScheduledDeletionAt(requestedAt.plusSeconds(72L * 3600));
        deletionRequest.setStatus(DeletionRequestStatus.PENDING);
        deletionRequest.setCancellationTokenHash(cancellationTokenHash);
        try {
            // Flush immediately so partial unique index catches concurrent creates (READ_COMMITTED + check-then-insert is not enough).
            deletionRequestRepository.saveAndFlush(deletionRequest);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicatePendingDeletionConstraint(ex)) {
                throw new IllegalStateException("Bạn đã có một yêu cầu xóa đang chờ xử lý");
            }
            throw ex;
        }

        // AC #3: revoke all active sessions and freeze the account immediately
        refreshTokenRepository.revokeAllByUserId(userId, requestedAt);
        user.setAccountStatus(AccountStatus.PENDING_DELETION);
        userRepository.save(user);
        accountStatusCache.put(userId, AccountStatus.PENDING_DELETION);

        log.info("Deletion request created: userId={} scheduledAt={}", userId, deletionRequest.getScheduledDeletionAt());

        String cancellationLink = webCancellationUrl
                + "?token=" + encodeQueryParam(cancellationToken);
        try {
            emailService.sendDeletionConfirmationEmail(user, deletionRequest, cancellationLink);
        } catch (Exception e) {
            // Email is best-effort: the request itself is already persisted and visible to the user
            log.error("Gửi email xác nhận yêu cầu xóa tài khoản thất bại cho userId={}", userId, e);
        }

        auditEventRecorder.recordEvent(
                userId,
                AuditActions.REQUEST_ACCOUNT_DELETION,
                AuditResourceTypes.USER,
                userId,
                Map.of(
                        "requestId", deletionRequest.getId().toString(),
                        "scheduledDeletionAt", deletionRequest.getScheduledDeletionAt().toString()
                )
        );

        return new DeleteAccountResponse(
                "Yêu cầu xóa tài khoản của bạn đã được nhận. Tài khoản sẽ bị xóa sau 72 giờ. Vui lòng kiểm tra email để hủy yêu cầu nếu cần thiết.",
                deletionRequest.getId().toString(),
                deletionRequest.getScheduledDeletionAt(),
                cancellationLink
        );
    }

    /**
     * AC #5 — validate token, restore account to ACTIVE if still in grace period.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CancelDeletionResponse cancelDeletionRequest(String cancellationToken) {
        String normalizedToken = normalizeCancellationToken(cancellationToken);
        String tokenHash = hashCancellationToken(normalizedToken);
        DataDeletionRequest deletionRequest = deletionRequestRepository.findByCancellationTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new DeletionCancellationTokenException(
                        "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực."));

        if (!deletionRequest.isPending()) {
            throw new DeletionCancellationConflictException("Yêu cầu xóa này không thể hủy được");
        }

        // Grace period ended but scheduler may not have run yet — still reject cancellation (ND13 semantics).
        if (deletionRequest.isOverdue()) {
            throw new DeletionCancellationTokenException(
                    "Thời gian cho phép hủy yêu cầu xóa đã kết thúc. Tài khoản đã hoặc sắp được xóa theo lịch đã đặt.");
        }

        User user = userRepository.findById(deletionRequest.getUserId())
                .orElseThrow(() -> new DeletionCancellationForbiddenException("Người dùng không tồn tại"));

        // Avoid reactivating an account if deletion already ran (or DB is inconsistent): only cancel while still frozen.
        if (user.getAccountStatus() != AccountStatus.PENDING_DELETION) {
            throw new DeletionCancellationForbiddenException(
                    "Không thể hủy yêu cầu xóa: tài khoản không còn trong trạng thái chờ xóa.");
        }

        deletionRequest.setStatus(DeletionRequestStatus.CANCELLED);
        deletionRequestRepository.save(deletionRequest);

        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        accountStatusCache.put(deletionRequest.getUserId(), AccountStatus.ACTIVE);

        log.info("Deletion request cancelled: userId={}", deletionRequest.getUserId());

        auditEventRecorder.recordEvent(
                deletionRequest.getUserId(),
                AuditActions.CANCEL_ACCOUNT_DELETION,
                AuditResourceTypes.USER,
                deletionRequest.getUserId(),
                Map.of("requestId", deletionRequest.getId().toString())
        );

        try {
            emailService.sendCancellationConfirmationEmail(user);
        } catch (Exception e) {
            log.error("Failed to send cancellation confirmation email to userId={}", deletionRequest.getUserId(), e);
        }

        return new CancelDeletionResponse(
                "Yêu cầu xóa tài khoản đã được hủy",
                user.getEmail(),
                Instant.now()
        );
    }

    /**
     * AC #2 — scheduler driver: process PENDING requests whose 72-hour grace period has lapsed.
     * Each request is wiped in its own transaction; a failure stops the current scheduler cycle.
     */
    public void processDeletionRequests() {
        Instant now = Instant.now();
        int processed = 0;
        while (true) {
            try {
                if (!selfProxy.executeNextDueDataDeletion(now)) {
                    break;
                }
                processed++;
            } catch (Exception e) {
                log.error("Failed to execute a due deletion request: {}", e.getMessage(), e);
                break;
            }
        }
        log.info("Processed {} overdue deletion request(s)", processed);
    }

    /**
     * AC #2 — wipe all user data in the order required by the foreign key graph.
     * Public so Spring can wrap it via the self-proxy with REQUIRES_NEW; never call directly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeNextDueDataDeletion(Instant now) {
        return deletionRequestRepository.findNextDuePendingForUpdateSkipLocked(
                        DeletionRequestStatus.PENDING.name(), now)
                .map(deletionRequest -> {
                    executeLockedDataDeletion(deletionRequest);
                    return true;
                })
                .orElse(false);
    }

    /**
     * AC #2 — wipe all user data in the order required by the foreign key graph.
     * Public so Spring can wrap it via the self-proxy with REQUIRES_NEW; direct invocations still lock by id.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDataDeletion(UUID deletionRequestId) {
        DataDeletionRequest deletionRequest = deletionRequestRepository.findByIdForUpdate(deletionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Deletion request not found: " + deletionRequestId));

        executeLockedDataDeletion(deletionRequest);
    }

    private void executeLockedDataDeletion(DataDeletionRequest deletionRequest) {
        UUID deletionRequestId = deletionRequest.getId();
        if (deletionRequest.getStatus() != DeletionRequestStatus.PENDING) {
            log.info("Skipping non-pending deletion request: requestId={} status={}",
                    deletionRequestId, deletionRequest.getStatus());
            return;
        }

        UUID userId = deletionRequest.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String originalEmail = user.getEmail();

        log.info("Starting data deletion: userId={} requestId={}", userId, deletionRequestId);

        // 1. Object storage (PDFs / images / avatar): exact-key deletion is fail-fast before DB rows
        // become unreachable; prefix deletion is a best-effort backstop.
        List<String> healthRecordFileKeys = healthRecordRepository.findFileKeysByUserId(userId);
        int exactFilesDeleted = storageService.deleteObjects(healthRecordFileKeys);
        int filesDeleted = storageService.deleteObjectsByPrefix("health-records/" + userId + "/");
        int avatarFilesDeleted = storageService.deleteObjectsByPrefix("avatars/" + userId + "/");

        // 2. Delete sharing/invitation/job/reminder children that can otherwise restrict owner/viewer anonymisation.
        int healthRecordSharesDeleted = healthRecordShareRepository.deleteAllByUserParticipation(userId);
        int healthRecordInvitesDeleted = healthRecordInvitationRepository.deleteAllByUserParticipation(userId);
        int profileSharesDeleted = profileShareRepository.deleteAllByUserParticipation(userId);
        int profileInvitesDeleted = profileInvitationRepository.deleteAllByUserParticipation(userId);
        int profileInviteeEmailDeleted = deleteProfileInvitationsByInviteeEmail(originalEmail);
        int healthRecordInviteeEmailDeleted = deleteHealthRecordInvitationsByInviteeEmail(originalEmail);
        int followUpRemindersDeleted = followUpReminderRepository.deleteAllByUserId(userId);
        int ocrDeadLettersDeleted = ocrDeadLetterRepository.deleteAllByUserId(userId);
        int ocrJobsDeleted = ocrJobExecutionRepository.deleteAllByUserId(userId);

        // 3-4. Health data tables (children of profile/user)
        int healthRecordsDeleted = healthRecordRepository.deleteAllByUserId(userId);
        int profilesDeleted = profileRepository.deleteAllByUserId(userId);

        // 4-7. Auth + consent artefacts
        int emailVerificationDeleted = emailVerificationTokenRepository.deleteAllByUserId(userId);
        int passwordResetDeleted = passwordResetTokenRepository.deleteAllByUserId(userId);
        int refreshTokensDeleted = refreshTokenRepository.deleteAllByUserId(userId);
        int consentLogsDeleted = consentLogRepository.deleteAllByUserId(userId);

        // 8. Mark this deletion request completed (kept as audit trail)
        deletionRequest.setStatus(DeletionRequestStatus.COMPLETED);
        deletionRequest.setCompletedAt(Instant.now());
        deletionRequestRepository.save(deletionRequest);

        // 9. Best-effort completion email BEFORE the user row is anonymised.
        try {
            emailService.sendDeletionCompletionEmail(user);
        } catch (Exception e) {
            log.error("Failed to send deletion completion email to userId={}", userId, e);
        }

        // 10. Anonymise the user row. We keep the record so foreign keys from external systems
        // (e.g. audit logs, deletion request itself) remain valid; PII is fully removed.
        user.setAccountStatus(AccountStatus.DELETED);
        user.setEmail("deleted+" + userId + "@deleted.local");
        user.setFullName("[Deleted User]");
        user.setPasswordHash("[deleted]");
        user.setEmailVerified(false);
        user.setAvatarStorageKey(null);
        user.setAvatarContentType(null);
        user.setAvatarSizeBytes(null);
        user.setAvatarChecksumSha256(null);
        user.setAvatarUpdatedAt(null);
        userRepository.save(user);
        accountStatusCache.put(userId, AccountStatus.DELETED);

        // AC #2 audit-trail line per architecture.md "Chiến Lược Audit Logging"
        log.info("User data deleted per right-to-delete request: userId={} requestId={} exactFiles={} prefixFiles={} avatarFiles={} recordShares={} recordInvites={} profileShares={} profileInvites={} profileInviteeEmails={} recordInviteeEmails={} reminders={} ocrDeadLetters={} ocrJobs={} records={} profiles={} emailTokens={} resetTokens={} refreshTokens={} consentLogs={}",
                userId, deletionRequestId, exactFilesDeleted, filesDeleted, avatarFilesDeleted,
                healthRecordSharesDeleted, healthRecordInvitesDeleted, profileSharesDeleted, profileInvitesDeleted,
                profileInviteeEmailDeleted, healthRecordInviteeEmailDeleted, followUpRemindersDeleted,
                ocrDeadLettersDeleted, ocrJobsDeleted, healthRecordsDeleted, profilesDeleted,
                emailVerificationDeleted, passwordResetDeleted, refreshTokensDeleted, consentLogsDeleted);
    }

    private String generateCancellationToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeCancellationToken(String cancellationToken) {
        if (cancellationToken == null || cancellationToken.isBlank()) {
            throw new DeletionCancellationTokenException(
                    "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực.");
        }
        return cancellationToken.trim();
    }

    private int deleteProfileInvitationsByInviteeEmail(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        return profileInvitationRepository.deleteAllByInviteeEmailIgnoreCase(email);
    }

    private int deleteHealthRecordInvitationsByInviteeEmail(String email) {
        if (email == null || email.isBlank()) {
            return 0;
        }
        return healthRecordInvitationRepository.deleteAllByInviteeEmailIgnoreCase(email);
    }

    private String hashCancellationToken(String cancellationToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(cancellationToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private static boolean isDuplicatePendingDeletionConstraint(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause != null ? cause.getMessage() : null;
        return msg != null && msg.contains(PENDING_DELETION_UNIQUE_INDEX_NAME);
    }
}
