package com.healthlens.api.repository;

import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, UUID> {

    /**
     * Find the most recent deletion request for a user.
     */
    Optional<DataDeletionRequest> findFirstByUserIdOrderByRequestedAtDesc(UUID userId);

    /**
     * Claim one due request and hold its row lock for the caller's deletion transaction.
     */
    @Query(value = """
            SELECT *
            FROM data_deletion_requests
            WHERE status = :status
              AND scheduled_deletion_at <= :now
            ORDER BY scheduled_deletion_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<DataDeletionRequest> findNextDuePendingForUpdateSkipLocked(
            @Param("status") String status,
            @Param("now") Instant now
    );

    /**
     * Find a deletion request by cancellation token hash.
     */
    Optional<DataDeletionRequest> findByCancellationTokenHash(String cancellationTokenHash);

    /**
     * Lock a deletion request by token hash while a cancellation is being validated and applied.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DataDeletionRequest r WHERE r.cancellationTokenHash = :cancellationTokenHash")
    Optional<DataDeletionRequest> findByCancellationTokenHashForUpdate(
            @Param("cancellationTokenHash") String cancellationTokenHash);

    /**
     * Lock a deletion request before the scheduler mutates account data.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DataDeletionRequest r WHERE r.id = :id")
    Optional<DataDeletionRequest> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Check if a user has a pending deletion request.
     */
    boolean existsByUserIdAndStatus(UUID userId, DeletionRequestStatus status);

    /**
     * Cancel all pending deletion requests for a user.
     */
    @Modifying
    @Query("UPDATE DataDeletionRequest r SET r.status = :cancelled WHERE r.userId = :userId AND r.status = :pending")
    void cancelAllPendingByUserId(
        @Param("userId") UUID userId,
        @Param("pending") DeletionRequestStatus pending,
        @Param("cancelled") DeletionRequestStatus cancelled
    );
}
