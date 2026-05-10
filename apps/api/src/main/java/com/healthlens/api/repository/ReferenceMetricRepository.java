package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReferenceMetricRepository extends JpaRepository<ReferenceMetric, UUID> {
    Optional<ReferenceMetric> findByNameIgnoreCase(String name);
    Optional<ReferenceMetric> findByNameIgnoreCaseAndStatus(String name, String status);
    java.util.List<ReferenceMetric> findAllByOrderByNameAsc();
    java.util.List<ReferenceMetric> findAllByStatusOrderByNameAsc(String status);

    @Query("""
        SELECT rm FROM ReferenceMetric rm
        WHERE LOWER(rm.name) = LOWER(:name)
          AND rm.status <> 'draft'
        ORDER BY CASE WHEN rm.status = 'active' THEN 0 ELSE 1 END
        """)
    java.util.List<ReferenceMetric> findNonDraftByName(@Param("name") String name);
}
