package com.healthlens.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.dto.response.AcceptInvitationResultResponse;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.support.PostgresTestContainerBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
class ProfileShareConcurrencyIntegrationTest extends PostgresTestContainerBase {

    @Autowired
    private ProfileShareService profileShareService;

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
    @DisplayName("Concurrent accept with same token yields one active profile_share (Story 3.2)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentAccept_sameToken_singleActiveShare() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        String viewerEmail = "viewer-accept-conc-%s@example.com".formatted(viewerId);

        insertUser(ownerId, "owner-accept-conc-%s@example.com".formatted(ownerId));
        insertUser(viewerId, viewerEmail);
        insertProfile(profileId, ownerId, "Family profile", true);

        profileShareService.inviteByEmail(ownerId, profileId, viewerEmail);

        String token = jdbcTemplate.queryForObject(
                "SELECT token FROM profile_invitations WHERE profile_id = ? AND status = 'pending'",
                String.class,
                profileId
        );

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AcceptInvitationResultResponse>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return profileShareService.acceptInvitation(token, viewerId);
            }));
        }
        start.countDown();
        for (Future<AcceptInvitationResultResponse> future : futures) {
            AcceptInvitationResultResponse r = future.get();
            assertThat(r.outcome()).isEqualTo("accepted");
        }
        executor.shutdown();

        Integer activeShares = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM profile_shares
                        WHERE profile_id = ? AND viewer_id = ? AND revoked_at IS NULL
                        """,
                Integer.class,
                profileId,
                viewerId
        );
        assertThat(activeShares).isEqualTo(1);

        String invitationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM profile_invitations WHERE token = ?",
                String.class,
                token
        );
        assertThat(invitationStatus).isEqualTo("accepted");
    }

    @Test
    @DisplayName("Concurrent revoke: one succeeds, other sees no active share (Story 3.2)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentRevoke_onlyOneSucceeds() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID viewerId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID shareId = UUID.randomUUID();

        insertUser(ownerId, "owner-revoke-conc-%s@example.com".formatted(ownerId));
        insertUser(viewerId, "viewer-revoke-conc-%s@example.com".formatted(viewerId));
        insertProfile(profileId, ownerId, "Shared profile", true);

        jdbcTemplate.update(
                """
                        INSERT INTO profile_shares (id, profile_id, viewer_id, owner_id, access_level, granted_at)
                        VALUES (?, ?, ?, ?, 'view', NOW())
                        """,
                shareId,
                profileId,
                viewerId,
                ownerId
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger notFound = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        Future<?> f1 = executor.submit(() -> {
            start.await();
            try {
                profileShareService.revokeShare(ownerId, profileId, viewerId);
                success.incrementAndGet();
            } catch (ResourceNotFoundException ex) {
                notFound.incrementAndGet();
            }
            return null;
        });
        Future<?> f2 = executor.submit(() -> {
            start.await();
            try {
                profileShareService.revokeShare(ownerId, profileId, viewerId);
                success.incrementAndGet();
            } catch (ResourceNotFoundException ex) {
                notFound.incrementAndGet();
            }
            return null;
        });
        start.countDown();
        f1.get();
        f2.get();
        executor.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(notFound.get()).isEqualTo(1);

        Integer active = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM profile_shares WHERE id = ? AND revoked_at IS NULL",
                Integer.class,
                shareId
        );
        assertThat(active).isZero();
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
}
