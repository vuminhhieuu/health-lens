package com.healthlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.dto.response.NotificationInboxItemType;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.support.PostgresTestContainerBase;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class NotificationInboxIntegrationTest extends PostgresTestContainerBase {

    @Autowired
    private NotificationInboxService notificationInboxService;

    @Autowired
    private ProfileShareService profileShareService;

    @Autowired
    private HealthRecordShareService healthRecordShareService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailEventPublisher emailEventPublisher;

    @MockitoBean
    private AuditEventRecorder auditEventRecorder;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    @DisplayName("listInbox returns only pending invitations for the signed-in user's email")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listInbox_scopedToCurrentUserEmail() {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        String inviteeEmail = "invitee-inbox-%s@example.com".formatted(inviteeId);
        String otherEmail = "other-inbox-%s@example.com".formatted(otherUserId);

        insertUser(ownerId, "owner-inbox-%s@example.com".formatted(ownerId));
        insertUser(inviteeId, inviteeEmail);
        insertUser(otherUserId, otherEmail);
        insertProfile(profileId, ownerId, "Family profile", true);

        profileShareService.inviteByEmail(ownerId, profileId, inviteeEmail);

        var inviteeItems = notificationInboxService.listInbox(inviteeId);
        var otherItems = notificationInboxService.listInbox(otherUserId);
        var inviteePage = notificationInboxService.listInbox(inviteeId, 0, 10);
        var otherPage = notificationInboxService.listInbox(otherUserId, 0, 10);

        assertThat(inviteeItems).hasSize(1);
        assertThat(inviteeItems.get(0).type()).isEqualTo(NotificationInboxItemType.PROFILE_INVITATION);
        assertThat(inviteeItems.get(0).id()).startsWith("PROFILE_INVITATION:");
        assertThat(inviteeItems.get(0).read()).isFalse();
        assertThat(inviteeItems.get(0).actionUrl()).contains("/invitations/accept");
        assertThat(inviteePage.data()).hasSize(1);
        assertThat(inviteePage.unreadCount()).isEqualTo(1);

        assertThat(otherItems).isEmpty();
        assertThat(otherPage.data()).isEmpty();
        assertThat(otherPage.unreadCount()).isZero();
    }

    @Test
    @DisplayName("markAsRead persists read flag while invitation remains in inbox")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void markAsRead_itemStaysVisibleAsRead() {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        String inviteeEmail = "invitee-read-%s@example.com".formatted(inviteeId);

        insertUser(ownerId, "owner-read-%s@example.com".formatted(ownerId));
        insertUser(inviteeId, inviteeEmail);
        insertProfile(profileId, ownerId, "Family profile", true);

        profileShareService.inviteByEmail(ownerId, profileId, inviteeEmail);

        var beforeRead = notificationInboxService.listInbox(inviteeId);
        assertThat(beforeRead).hasSize(1);
        assertThat(beforeRead.get(0).read()).isFalse();

        notificationInboxService.markAsRead(inviteeId, beforeRead.get(0).id());

        var afterRead = notificationInboxService.listInbox(inviteeId);
        assertThat(afterRead).hasSize(1);
        assertThat(afterRead.get(0).read()).isTrue();
    }

    @Test
    @DisplayName("listInbox includes health record invitations for invitee email")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listInbox_includesHealthRecordInvitation() {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        String inviteeEmail = "invitee-hr-inbox-%s@example.com".formatted(inviteeId);

        insertUser(ownerId, "owner-hr-inbox-%s@example.com".formatted(ownerId));
        insertUser(inviteeId, inviteeEmail);
        insertProfile(profileId, ownerId, "Family profile", true);
        insertHealthRecord(recordId, profileId, ownerId, "inbox-test-%s.pdf".formatted(recordId));

        healthRecordShareService.inviteByEmail(ownerId, recordId, inviteeEmail, "view");

        var items = notificationInboxService.listInbox(inviteeId);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).type()).isEqualTo(NotificationInboxItemType.HEALTH_RECORD_INVITATION);
        assertThat(items.get(0).id()).startsWith("HEALTH_RECORD_INVITATION:");
        assertThat(items.get(0).actionUrl()).contains("/health-record-invitations/accept");
    }

    private void insertUser(UUID id, String email) {
        jdbcTemplate.update(
                """
                        INSERT INTO users
                            (id, email, full_name, date_of_birth, password_hash, email_verified, role, account_status)
                        VALUES (?, ?, 'Test User', DATE '1990-01-01', '[hash]', true, 'ROLE_USER', 'ACTIVE')
                        """,
                id,
                email
        );
    }

    private void insertProfile(UUID id, UUID userId, String displayName, boolean isDefault) {
        jdbcTemplate.update(
                """
                        INSERT INTO profiles (id, user_id, display_name, is_default)
                        VALUES (?, ?, ?, ?)
                        """,
                id,
                userId,
                displayName,
                isDefault
        );
    }

    private void insertHealthRecord(UUID id, UUID profileId, UUID userId, String fileKey) {
        jdbcTemplate.update(
                """
                        INSERT INTO health_records (id, profile_id, user_id, source_type, status, file_key, metrics)
                        VALUES (?, ?, ?, 'ocr', 'done', ?, '[]'::jsonb)
                        """,
                id,
                profileId,
                userId,
                fileKey
        );
    }
}
