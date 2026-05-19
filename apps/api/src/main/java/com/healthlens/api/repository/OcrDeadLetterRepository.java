package com.healthlens.api.repository;

import com.healthlens.api.entity.OcrDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OcrDeadLetterRepository extends JpaRepository<OcrDeadLetter, UUID> {
    @Modifying
    @Query("""
            DELETE FROM OcrDeadLetter d
            WHERE d.recordId IN (
                SELECT h.id FROM HealthRecord h WHERE h.userId = :userId
            )
            """)
    int deleteAllByUserId(@Param("userId") UUID userId);
}
