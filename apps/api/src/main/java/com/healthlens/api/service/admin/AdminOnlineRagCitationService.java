package com.healthlens.api.service.admin;

import com.healthlens.api.dto.admin.OnlineRagCitationEntryDto;
import com.healthlens.api.dto.admin.OnlineRagCitationPageDto;
import com.healthlens.api.entity.OnlineRagAnswerCitation;
import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.repository.OnlineRagAnswerCitationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminOnlineRagCitationService {

    private static final int EXPORT_PAGE_SIZE = 500;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final OnlineRagAnswerCitationRepository citationRepository;

    @Value("${app.ai.online-rag.cache-max-age:PT24H}")
    private Duration cacheMaxAge = Duration.ofHours(24);

    @Transactional(readOnly = true)
    public OnlineRagCitationPageDto query(
            UUID healthRecordId,
            String metricName,
            String answerHash,
            String sourceUrl,
            String publisher,
            String snapshotHash,
            String reviewStatus,
            int page,
            int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 200);
        int safePage = Math.max(page, 0);
        Page<OnlineRagAnswerCitation> result = citationRepository.findAll(
                buildSpec(healthRecordId, metricName, answerHash, sourceUrl, publisher, snapshotHash, reviewStatus),
                PageRequest.of(safePage, safeLimit, DEFAULT_SORT)
        );
        return new OnlineRagCitationPageDto(
                result.getContent().stream().map(this::toDto).toList(),
                result.getTotalElements(),
                safePage,
                safeLimit
        );
    }

    @Transactional(readOnly = true)
    public void writeCsv(
            UUID healthRecordId,
            String metricName,
            String answerHash,
            String sourceUrl,
            String publisher,
            String snapshotHash,
            String reviewStatus,
            Writer writer,
            int maxRows
    ) throws IOException {
        Specification<OnlineRagAnswerCitation> spec =
                buildSpec(healthRecordId, metricName, answerHash, sourceUrl, publisher, snapshotHash, reviewStatus);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(
                        "id",
                        "healthRecordId",
                        "metricName",
                        "answerHash",
                        "sourceSnapshotId",
                        "sourceUrl",
                        "publisher",
                        "retrievedAt",
                        "snapshotHash",
                        "reviewStatus",
                        "excluded",
                        "cacheHit",
                        "usableForAi",
                        "reviewRequired",
                        "rejected",
                        "stale",
                        "retrievalSource",
                        "promptVersion",
                        "modelVersion",
                        "createdAt"
                )
                .get();
        int cap = Math.min(Math.max(maxRows, 1), 50_000);
        try (CSVPrinter printer = new CSVPrinter(new NonClosingWriter(writer), format)) {
            int written = 0;
            int page = 0;
            while (written < cap) {
                int batchSize = Math.min(EXPORT_PAGE_SIZE, cap - written);
                Page<OnlineRagAnswerCitation> rows = citationRepository.findAll(
                        spec,
                        PageRequest.of(page, batchSize, DEFAULT_SORT)
                );
                if (rows.isEmpty()) {
                    break;
                }
                for (OnlineRagAnswerCitation row : rows.getContent()) {
                    OnlineRagCitationEntryDto dto = toDto(row);
                    printer.printRecord(
                            dto.id(),
                            dto.healthRecordId(),
                            dto.metricName(),
                            dto.answerHash(),
                            dto.sourceSnapshotId(),
                            dto.sourceUrl(),
                            dto.publisher(),
                            dto.retrievedAt(),
                            dto.snapshotHash(),
                            dto.reviewStatus(),
                            dto.excluded(),
                            dto.cacheHit(),
                            dto.usableForAi(),
                            dto.reviewRequired(),
                            dto.rejected(),
                            dto.stale(),
                            dto.retrievalSource(),
                            dto.promptVersion(),
                            dto.modelVersion(),
                            dto.createdAt()
                    );
                    written++;
                }
                if (!rows.hasNext()) {
                    break;
                }
                page++;
            }
            printer.flush();
        }
    }

    private Specification<OnlineRagAnswerCitation> buildSpec(
            UUID healthRecordId,
            String metricName,
            String answerHash,
            String sourceUrl,
            String publisher,
            String snapshotHash,
            String reviewStatus
    ) {
        OnlineRagReviewStatus parsedReviewStatus = StringUtils.hasText(reviewStatus)
                ? parseReviewStatus(reviewStatus)
                : null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (healthRecordId != null) {
                predicates.add(cb.equal(root.get("healthRecordId"), healthRecordId));
            }
            if (StringUtils.hasText(metricName)) {
                predicates.add(cb.like(cb.lower(root.get("metricName")), "%" + metricName.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(answerHash)) {
                predicates.add(cb.equal(root.get("answerHash"), answerHash.trim()));
            }
            if (StringUtils.hasText(sourceUrl)) {
                predicates.add(cb.like(cb.lower(root.get("sourceUrl")), "%" + sourceUrl.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(publisher)) {
                predicates.add(cb.like(cb.lower(root.get("publisher")), "%" + publisher.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(snapshotHash)) {
                predicates.add(cb.equal(root.get("snapshotHash"), snapshotHash.trim()));
            }
            if (parsedReviewStatus != null) {
                predicates.add(cb.equal(root.get("reviewStatus"), parsedReviewStatus));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private OnlineRagCitationEntryDto toDto(OnlineRagAnswerCitation citation) {
        return new OnlineRagCitationEntryDto(
                citation.getId(),
                citation.getHealthRecordId(),
                citation.getMetricName(),
                citation.getAnswerHash(),
                citation.getSourceSnapshotId(),
                citation.getSourceUrl(),
                citation.getPublisher(),
                citation.getRetrievedAt(),
                citation.getSnapshotHash(),
                citation.getReviewStatus().name(),
                citation.isExcluded(),
                citation.isCacheHit(),
                citation.isUsableForAi(),
                citation.isReviewRequired(),
                citation.isRejected(),
                isStale(citation.getRetrievedAt()),
                citation.getRetrievalSource(),
                citation.getPromptVersion(),
                citation.getModelVersion(),
                citation.getCreatedAt()
        );
    }

    private OnlineRagReviewStatus parseReviewStatus(String reviewStatus) {
        try {
            return OnlineRagReviewStatus.valueOf(reviewStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("reviewStatus không hợp lệ. Giá trị hợp lệ: APPROVED, REVIEW_REQUIRED, REJECTED");
        }
    }

    private boolean isStale(Instant retrievedAt) {
        if (retrievedAt == null || cacheMaxAge == null || cacheMaxAge.isZero() || cacheMaxAge.isNegative()) {
            return false;
        }
        return retrievedAt.plus(cacheMaxAge).isBefore(Instant.now());
    }

    private static final class NonClosingWriter extends FilterWriter {
        private NonClosingWriter(Writer out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
