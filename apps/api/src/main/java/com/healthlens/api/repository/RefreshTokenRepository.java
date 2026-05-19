package com.healthlens.api.repository;

import com.healthlens.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revokedAt = :now
            WHERE rt.id = :id
              AND rt.revokedAt IS NULL
              AND rt.expiresAt > :now
            """)
    int rotateActiveToken(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.id = :id AND rt.revokedAt IS NULL")
    int revokeByIdIfActive(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.sessionFamilyId = :sessionFamilyId AND rt.revokedAt IS NULL")
    void revokeAllBySessionFamilyId(@Param("sessionFamilyId") UUID sessionFamilyId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
