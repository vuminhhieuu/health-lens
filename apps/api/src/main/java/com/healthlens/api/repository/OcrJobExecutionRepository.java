package com.healthlens.api.repository;

import com.healthlens.api.entity.OcrJobExecution;
import com.healthlens.api.entity.OcrJobState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OcrJobExecutionRepository extends JpaRepository<OcrJobExecution, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OcrJobExecution> findByIdempotencyKey(String idempotencyKey);

    List<OcrJobExecution> findTop50ByStateAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            OcrJobState state,
            Instant nextRetryAt
    );

    @Modifying
    // The claim is atomic: competing schedulers rely on nextRetryAt <= now in this
    // UPDATE so only one worker can move the lease into the future.
    @Query("""
            UPDATE OcrJobExecution j
            SET j.nextRetryAt = :claimUntil
            WHERE j.id = :id
              AND j.state = :state
              AND j.nextRetryAt <= :now
            """)
    int claimDueRetry(
            @Param("id") UUID id,
            @Param("state") OcrJobState state,
            @Param("now") Instant now,
            @Param("claimUntil") Instant claimUntil
    );

    @Modifying
    @Query("""
            DELETE FROM OcrJobExecution j
            WHERE j.recordId IN (
                SELECT h.id FROM HealthRecord h WHERE h.userId = :userId
            )
            """)
    int deleteAllByUserId(@Param("userId") UUID userId);
}
