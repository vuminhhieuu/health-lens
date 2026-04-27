package com.healthlens.api.repository;

import com.healthlens.api.entity.ReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReferenceRangeRepository extends JpaRepository<ReferenceRange, UUID> {

    @Query("""
        SELECT rr FROM ReferenceRange rr
        WHERE rr.metric.id = :metricId
          AND rr.status = 'active'
        """)
    List<ReferenceRange> findActiveRangesByMetricId(@Param("metricId") UUID metricId);

    @Query("""
        SELECT rr FROM ReferenceRange rr
        WHERE rr.metric.id = :metricId
          AND rr.status = 'active'
          AND (:gender IS NULL OR rr.gender IS NULL OR LOWER(rr.gender) = :gender)
          AND (:age IS NULL OR rr.minAge IS NULL OR rr.minAge <= :age)
          AND (:age IS NULL OR rr.maxAge IS NULL OR rr.maxAge >= :age)
        ORDER BY
          CASE WHEN rr.gender IS NULL THEN 1 ELSE 0 END,
          CASE WHEN rr.minAge IS NULL THEN 1 ELSE 0 END,
          CASE WHEN rr.maxAge IS NULL THEN 1 ELSE 0 END
        """)
    List<ReferenceRange> findMatchingRanges(
            @Param("metricId") UUID metricId,
            @Param("age") Integer age,
            @Param("gender") String gender
    );
}
