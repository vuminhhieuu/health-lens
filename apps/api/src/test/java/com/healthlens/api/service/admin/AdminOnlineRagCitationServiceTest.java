package com.healthlens.api.service.admin;

import com.healthlens.api.dto.admin.OnlineRagCitationPageDto;
import com.healthlens.api.entity.OnlineRagAnswerCitation;
import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.repository.OnlineRagAnswerCitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOnlineRagCitationServiceTest {

    @Mock
    private OnlineRagAnswerCitationRepository citationRepository;

    private AdminOnlineRagCitationService service;

    @BeforeEach
    void setUp() {
        service = new AdminOnlineRagCitationService(citationRepository);
    }

    @Test
    @DisplayName("query trả metadata citation có phân trang bounded và không có raw source snapshot")
    void query_returnsBoundedMetadataOnly() {
        OnlineRagAnswerCitation citation = citation(OnlineRagReviewStatus.REVIEW_REQUIRED, true);
        when(citationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(citation)));

        OnlineRagCitationPageDto result = service.query(
                citation.getHealthRecordId(),
                "glu",
                citation.getAnswerHash(),
                "who.int",
                "WHO",
                citation.getSnapshotHash(),
                "REVIEW_REQUIRED",
                -1,
                500
        );

        assertThat(result.page()).isZero();
        assertThat(result.limit()).isEqualTo(200);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).sourceUrl()).contains("who.int");
        assertThat(result.content().get(0).reviewStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.content().get(0).excluded()).isTrue();
        assertThat(result.content().get(0).usableForAi()).isFalse();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(citationRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("export CSV chỉ xuất metadata được phép và bị giới hạn maxRows")
    void writeCsv_exportsMetadataOnlyWithinCap() throws Exception {
        OnlineRagAnswerCitation citation = citation(OnlineRagReviewStatus.REJECTED, true);
        when(citationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(citation)));
        StringWriter writer = new StringWriter();

        service.writeCsv(
                citation.getHealthRecordId(),
                citation.getMetricName(),
                citation.getAnswerHash(),
                citation.getSourceUrl(),
                citation.getPublisher(),
                citation.getSnapshotHash(),
                "REJECTED",
                writer,
                1
        );

        String csv = writer.toString();
        assertThat(csv).contains("sourceUrl,publisher,retrievedAt,snapshotHash,reviewStatus");
        assertThat(csv).contains("REJECTED");
        assertThat(csv).doesNotContain("contentSnapshot");
        assertThat(csv).doesNotContain("raw");
    }

    @Test
    @DisplayName("query chuẩn hóa reviewStatus và báo lỗi rõ khi giá trị không hợp lệ")
    void query_rejectsInvalidReviewStatus() {
        assertThatThrownBy(() -> service.query(
                null,
                null,
                null,
                null,
                null,
                null,
                "approved;drop",
                0,
                20
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewStatus không hợp lệ");
    }

    private OnlineRagAnswerCitation citation(OnlineRagReviewStatus status, boolean excluded) {
        OnlineRagAnswerCitation citation = new OnlineRagAnswerCitation();
        citation.setId(UUID.randomUUID());
        citation.setHealthRecordId(UUID.randomUUID());
        citation.setMetricName("Glucose");
        citation.setAnswerHash("a".repeat(64));
        citation.setSourceSnapshotId(UUID.randomUUID());
        citation.setSourceUrl("https://who.int/news/item/glucose");
        citation.setPublisher("WHO");
        citation.setRetrievedAt(Instant.parse("2026-05-19T08:00:00Z"));
        citation.setSnapshotHash("b".repeat(64));
        citation.setReviewStatus(status);
        citation.setExcluded(excluded);
        citation.setCacheHit(false);
        citation.setUsableForAi(status == OnlineRagReviewStatus.APPROVED && !excluded);
        citation.setReviewRequired(status == OnlineRagReviewStatus.REVIEW_REQUIRED);
        citation.setRejected(status == OnlineRagReviewStatus.REJECTED);
        citation.setRetrievalSource("qdrant");
        citation.setPromptVersion("prompt-v1");
        citation.setModelVersion("model-v1");
        citation.setCreatedAt(Instant.parse("2026-05-19T09:00:00Z"));
        return citation;
    }
}
