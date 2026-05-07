package com.healthlens.api.repository;

import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.DeletionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, UUID> {

    /**
     * Find the most recent deletion request for a user.
     */
    Optional<DataDeletionRequest> findFirstByUserIdOrderByRequestedAtDesc(UUID userId);

    /**
     * Find all pending deletion requests that have passed their scheduled deletion time.
     */
    List<DataDeletionRequest> findByStatusAndScheduledDeletionAtBefore(
        DeletionRequestStatus status,
        Instant now
    );

    /**
     * Find a deletion request by cancellation token.
     */
    Optional<DataDeletionRequest> findByCancellationToken(String cancellationToken);

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
