package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceDataChangeSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferenceDataChangeSetRepository extends JpaRepository<ReferenceDataChangeSet, UUID> {
    List<ReferenceDataChangeSet> findAllByEntityIdInAndStatusOrderByCreatedAtDesc(List<UUID> entityIds, String status);
}
