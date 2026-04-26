package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferenceMetricRepository extends JpaRepository<ReferenceMetric, UUID> {
    Optional<ReferenceMetric> findByNameIgnoreCase(String name);
    java.util.List<ReferenceMetric> findAllByOrderByNameAsc();
}
