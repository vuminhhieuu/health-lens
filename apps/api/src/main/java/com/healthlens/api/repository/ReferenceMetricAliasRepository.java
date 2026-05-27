package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceMetricAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReferenceMetricAliasRepository extends JpaRepository<ReferenceMetricAlias, UUID> {
    Optional<ReferenceMetricAlias> findByAliasNormalizedAndActiveTrue(String aliasNormalized);
    java.util.List<ReferenceMetricAlias> findAllByMetric_Id(UUID metricId);
    java.util.List<ReferenceMetricAlias> findByAliasNormalizedAndActiveTrueAndMetricStatusNotOrderByMetricStatusAsc(
            String aliasNormalized,
            String excludedStatus
    );
}
