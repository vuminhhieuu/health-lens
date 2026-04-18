package com.healthlens.api.repository;

import com.healthlens.api.entity.ConsentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentLogRepository extends JpaRepository<ConsentLog, UUID> {
    Optional<ConsentLog> findFirstByUser_IdOrderByConsentedAtDesc(UUID userId);
}
