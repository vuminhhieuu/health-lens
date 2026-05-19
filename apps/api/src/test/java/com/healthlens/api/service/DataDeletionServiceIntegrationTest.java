package com.healthlens.api.service;

import com.healthlens.api.support.PostgresTestContainerBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class DataDeletionServiceIntegrationTest extends PostgresTestContainerBase {

    @Autowired
    private DataDeletionService dataDeletionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    @DisplayName("executeDataDeletion cleans FK graph, invitee emails, dead letters, and retains audit logs")
    void executeDataDeletion_cleansRealDatabaseGraph() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID otherProfileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID otherRecordId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String originalEmail = "delete-me-%s@example.com".formatted(userId);

        insertUser(userId, originalEmail, "PENDING_DELETION");
        insertUser(otherUserId, "other-%s@example.com".formatted(otherUserId), "ACTIVE");
        insertProfile(profileId, userId, "Owner Profile", true);
        insertProfile(otherProfileId, otherUserId, "Other Profile", true);
        insertHealthRecord(recordId, profileId, userId, "custom/%s/report.pdf".formatted(userId));
        insertHealthRecord(otherRecordId, otherProfileId, otherUserId, "custom/%s/other.pdf".formatted(otherUserId));
        insertDeletionRequest(requestId, userId);

        jdbcTemplate.update("""
                INSERT INTO profile_share_audit_logs
                    (id, actor_id, profile_id, viewer_id, action, resource_type, resource_id)
                VALUES (?, ?, ?, ?, 'REVOKE_PROFILE_SHARE', 'PROFILE_SHARE', ?)
                """, UUID.randomUUID(), userId, profileId, otherUserId, UUID.randomUUID());
        jdbcTemplate.update("""
                INSERT INTO profile_invitations
                    (id, profile_id, inviter_id, invitee_email, token, status, access_level, expires_at)
                VALUES (?, ?, ?, ?, ?, 'pending', 'view', NOW() + INTERVAL '1 day')
                """, UUID.randomUUID(), otherProfileId, otherUserId, originalEmail, "profile-in-" + userId);
        jdbcTemplate.update("""
                INSERT INTO health_record_invitations
                    (id, health_record_id, profile_id, inviter_id, invitee_email, token, status, access_level, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, 'pending', 'view', NOW() + INTERVAL '1 day')
                """, UUID.randomUUID(), otherRecordId, otherProfileId, otherUserId, originalEmail, "record-in-" + userId);
        jdbcTemplate.update("""
                INSERT INTO profile_shares (id, profile_id, viewer_id, owner_id, access_level, granted_at)
                VALUES (?, ?, ?, ?, 'view', NOW())
                """, UUID.randomUUID(), profileId, otherUserId, userId);
        jdbcTemplate.update("""
                INSERT INTO health_record_shares (id, health_record_id, profile_id, owner_id, viewer_id, access_level, granted_at)
                VALUES (?, ?, ?, ?, ?, 'view', NOW())
                """, UUID.randomUUID(), recordId, profileId, userId, otherUserId);
        jdbcTemplate.update("""
                INSERT INTO follow_up_reminders (id, profile_id, reminder_date, reminder_type)
                VALUES (?, ?, CURRENT_DATE, 'lab_follow_up')
                """, UUID.randomUUID(), profileId);
        jdbcTemplate.update("""
                INSERT INTO ocr_job_executions
                    (id, record_id, job_id, file_key, idempotency_key, state)
                VALUES (?, ?, 'job-1', 'custom/file.pdf', ?, 'FAILED')
                """, UUID.randomUUID(), recordId, "idem-" + userId);
        jdbcTemplate.update("""
                INSERT INTO ocr_dead_letters
                    (id, record_id, job_id, idempotency_key, sanitized_payload, failure_category, attempts)
                VALUES (?, ?, 'job-1', ?, '{"fileKey":"custom/file.pdf"}'::jsonb, 'api_error', 3)
                """, UUID.randomUUID(), recordId, "dead-" + userId);
        jdbcTemplate.update("""
                INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at)
                VALUES (?, ?, ?, NOW() + INTERVAL '1 day')
                """, UUID.randomUUID(), userId, "refresh-" + userId);
        jdbcTemplate.update("""
                INSERT INTO email_verification_tokens (id, user_id, token, expires_at)
                VALUES (?, ?, ?, NOW() + INTERVAL '1 day')
                """, UUID.randomUUID(), userId, "verify-" + userId);
        jdbcTemplate.update("""
                INSERT INTO password_reset_tokens (id, user_id, token, expires_at)
                VALUES (?, ?, ?, NOW() + INTERVAL '1 day')
                """, UUID.randomUUID(), userId, "reset-" + userId);
        jdbcTemplate.update("""
                INSERT INTO consent_logs (id, user_id, consent_version)
                VALUES (?, ?, 'v1')
                """, UUID.randomUUID(), userId);

        when(storageService.deleteObjects(anyCollection())).thenReturn(1);
        when(storageService.deleteObjectsByPrefix(anyString())).thenReturn(0);

        dataDeletionService.executeDataDeletion(requestId);

        assertThat(count("health_records", "user_id", userId)).isZero();
        assertThat(count("profiles", "user_id", userId)).isZero();
        assertThat(count("profile_shares", "owner_id", userId)).isZero();
        assertThat(count("health_record_shares", "owner_id", userId)).isZero();
        assertThat(count("follow_up_reminders", "profile_id", profileId)).isZero();
        assertThat(count("ocr_job_executions", "record_id", recordId)).isZero();
        assertThat(count("ocr_dead_letters", "idempotency_key", "dead-" + userId)).isZero();
        assertThat(count("profile_invitations", "invitee_email", originalEmail)).isZero();
        assertThat(count("health_record_invitations", "invitee_email", originalEmail)).isZero();
        assertThat(count("refresh_tokens", "user_id", userId)).isZero();
        assertThat(count("email_verification_tokens", "user_id", userId)).isZero();
        assertThat(count("password_reset_tokens", "user_id", userId)).isZero();
        assertThat(count("consent_logs", "user_id", userId)).isZero();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM data_deletion_requests WHERE id = ?",
                String.class,
                requestId
        );
        assertThat(status).isEqualTo("COMPLETED");

        String accountStatus = jdbcTemplate.queryForObject(
                "SELECT account_status FROM users WHERE id = ?",
                String.class,
                userId
        );
        String email = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId);
        assertThat(accountStatus).isEqualTo("DELETED");
        assertThat(email).startsWith("deleted+").endsWith("@deleted.local");

        List<UUID> retainedAuditProfileIds = jdbcTemplate.query(
                "SELECT profile_id FROM profile_share_audit_logs WHERE actor_id = ?",
                (rs, rowNum) -> rs.getObject("profile_id", UUID.class),
                userId
        );
        assertThat(retainedAuditProfileIds).containsExactly((UUID) null);
    }

    private void insertUser(UUID id, String email, String accountStatus) {
        jdbcTemplate.update("""
                INSERT INTO users
                    (id, email, full_name, date_of_birth, password_hash, email_verified, role, account_status)
                VALUES (?, ?, 'Test User', DATE '1990-01-01', '[hash]', true, 'ROLE_USER', ?)
                """, id, email, accountStatus);
    }

    private void insertProfile(UUID id, UUID userId, String displayName, boolean isDefault) {
        jdbcTemplate.update("""
                INSERT INTO profiles (id, user_id, display_name, is_default)
                VALUES (?, ?, ?, ?)
                """, id, userId, displayName, isDefault);
    }

    private void insertHealthRecord(UUID id, UUID profileId, UUID userId, String fileKey) {
        jdbcTemplate.update("""
                INSERT INTO health_records (id, profile_id, user_id, source_type, status, file_key, metrics)
                VALUES (?, ?, ?, 'ocr', 'done', ?, '[]'::jsonb)
                """, id, profileId, userId, fileKey);
    }

    private void insertDeletionRequest(UUID id, UUID userId) {
        jdbcTemplate.update("""
                INSERT INTO data_deletion_requests
                    (id, user_id, requested_at, scheduled_deletion_at, status, cancellation_token_hash)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                """,
                id,
                userId,
                Timestamp.from(Instant.now().minusSeconds(72L * 3600 + 60)),
                Timestamp.from(Instant.now().minusSeconds(60)),
                "0".repeat(64)
        );
    }

    private int count(String table, String column, Object value) {
        Integer result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Integer.class,
                value
        );
        return result == null ? 0 : result;
    }
}
