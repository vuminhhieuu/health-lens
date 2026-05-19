package com.healthlens.api.repository;

import com.healthlens.api.entity.OnlineRagAnswerCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OnlineRagAnswerCitationRepository
        extends JpaRepository<OnlineRagAnswerCitation, UUID>, JpaSpecificationExecutor<OnlineRagAnswerCitation> {

    boolean existsByHealthRecordIdAndMetricNameAndAnswerHashAndSourceUrlAndSnapshotHash(
            UUID healthRecordId,
            String metricName,
            String answerHash,
            String sourceUrl,
            String snapshotHash
    );
}
