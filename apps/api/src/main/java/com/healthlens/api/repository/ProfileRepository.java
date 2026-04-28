package com.healthlens.api.repository;

import com.healthlens.api.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    List<Profile> findAllByUserId(UUID userId);

    Optional<Profile> findFirstByUserIdAndIsDefaultTrue(UUID userId);

    Optional<Profile> findTopByUserIdOrderByCreatedAtAsc(UUID userId);

    long countByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM Profile p WHERE p.user.id = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
