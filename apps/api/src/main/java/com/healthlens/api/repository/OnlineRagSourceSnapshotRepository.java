package com.healthlens.api.repository;

import com.healthlens.api.entity.OnlineRagSourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnlineRagSourceSnapshotRepository extends JpaRepository<OnlineRagSourceSnapshot, UUID> {

    Optional<OnlineRagSourceSnapshot> findFirstBySourceUrlOrderByRetrievedAtDesc(String sourceUrl);
}
