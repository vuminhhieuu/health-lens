package com.healthlens.api.repository;

import com.healthlens.api.entity.ConsentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {
    Optional<ConsentLog> findFirstByUser_IdOrderByConsentedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM ConsentLog c WHERE c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
