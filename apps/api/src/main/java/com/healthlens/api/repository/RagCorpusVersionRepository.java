package com.healthlens.api.repository;

import com.healthlens.api.entity.RagCorpusVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RagCorpusVersionRepository extends JpaRepository<RagCorpusVersion, String> {

    Optional<RagCorpusVersion> findFirstByStatusAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
            String status,
            Instant effectiveDate
    );

    Optional<RagCorpusVersion> findFirstByStatusAndSourceVersionNotAndEffectiveDateLessThanEqualOrderByEffectiveDateDescCreatedAtDesc(
            String status,
            String sourceVersion,
            Instant effectiveDate
    );
}
