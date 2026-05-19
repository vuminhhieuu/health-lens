package com.healthlens.api.service.rag;

import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.entity.OnlineRagSourceSnapshot;
import com.healthlens.api.repository.OnlineRagSourceSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrustedOnlineRagSourceAdapterTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-19T06:00:00Z");

    private OnlineRagSourceSnapshotRepository snapshotRepository;
    private OnlineRagHttpClient httpClient;
    private TrustedOnlineRagSourceAdapter adapter;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(OnlineRagSourceSnapshotRepository.class);
        httpClient = mock(OnlineRagHttpClient.class);
        adapter = new TrustedOnlineRagSourceAdapter(
                new TrustedOnlineRagSourcePolicy("who.int,cdc.gov"),
                snapshotRepository,
                httpClient,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Reject source không nằm trong allowlist và không gọi HTTP")
    void retrieve_unallowlistedSource_rejectsWithoutHttpFetch() {
        URI uri = URI.create("https://unknown.example/health/article");

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, "Unknown");

        assertThat(result.rejected()).isTrue();
        assertThat(result.usableForAi()).isFalse();
        assertThat(result.metadata().reviewStatus()).isEqualTo(OnlineRagReviewStatus.REJECTED);
        verify(httpClient, never()).fetch(uri);
    }

    @Test
    @DisplayName("Trusted source mới được snapshot, hash, metadata và mặc định review required")
    void retrieve_newTrustedSource_persistsSnapshotMetadataAndRequiresReview() {
        URI uri = URI.create("https://www.who.int/news/item/glucose");
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.empty());
        when(httpClient.fetch(uri)).thenReturn("approved publisher content");
        when(snapshotRepository.save(any(OnlineRagSourceSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, "WHO");

        assertThat(result.rejected()).isFalse();
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.usableForAi()).isFalse();
        assertThat(result.content()).isEmpty();
        assertThat(result.metadata().sourceUrl()).isEqualTo(uri.toString());
        assertThat(result.metadata().publisher()).isEqualTo("WHO");
        assertThat(result.metadata().retrievedAt()).isEqualTo(FIXED_NOW);
        assertThat(result.metadata().snapshotHash()).hasSize(64);
        assertThat(result.metadata().reviewStatus()).isEqualTo(OnlineRagReviewStatus.REVIEW_REQUIRED);
    }

    @Test
    @DisplayName("Approved cached source được dùng cho AI và không fetch lại")
    void retrieve_approvedCachedSource_returnsContentWithoutRefetch() {
        URI uri = URI.create("https://cdc.gov/lab/glucose");
        OnlineRagSourceSnapshot snapshot = snapshot(uri, OnlineRagReviewStatus.APPROVED, false, "cached content");
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.of(snapshot));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, "CDC");

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.usableForAi()).isTrue();
        assertThat(result.content()).contains("cached content");
        assertThat(result.metadata().sourceUrl()).isEqualTo(uri.toString());
        assertThat(result.metadata().publisher()).isEqualTo("CDC");
        assertThat(result.metadata().snapshotHash()).isEqualTo("a".repeat(64));
        assertThat(result.metadata().reviewStatus()).isEqualTo(OnlineRagReviewStatus.APPROVED);
        assertThat(result.metadata().stale()).isFalse();
        verify(httpClient, never()).fetch(uri);
    }

    @Test
    @DisplayName("Unreviewed cached source bị exclude và đánh dấu cần review")
    void retrieve_unreviewedCachedSource_excludesFromAiAndMarksReviewRequired() {
        URI uri = URI.create("https://who.int/lab/glucose");
        OnlineRagSourceSnapshot snapshot = snapshot(uri, OnlineRagReviewStatus.REVIEW_REQUIRED, true, "unreviewed content");
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.of(snapshot));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, "WHO");

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.usableForAi()).isFalse();
        assertThat(result.content()).isEmpty();
        assertThat(result.metadata().reviewStatus()).isEqualTo(OnlineRagReviewStatus.REVIEW_REQUIRED);
        assertThat(result.metadata().excluded()).isTrue();
        verify(httpClient, never()).fetch(uri);
    }

    @Test
    @DisplayName("Reject HTTP source dù host nằm trong allowlist")
    void retrieve_httpAllowlistedSource_rejectsWithoutHttpFetch() {
        URI uri = URI.create("http://who.int/news/item/glucose");

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, "WHO");

        assertThat(result.rejected()).isTrue();
        assertThat(result.metadata().reviewStatus()).isEqualTo(OnlineRagReviewStatus.REJECTED);
        verify(httpClient, never()).fetch(uri);
    }

    @Test
    @DisplayName("Publisher quá dài được cắt trước khi lưu metadata")
    void retrieve_newTrustedSource_truncatesPublisherBeforeSave() {
        URI uri = URI.create("https://who.int/news/item/publisher");
        String longPublisher = "P".repeat(300);
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.empty());
        when(httpClient.fetch(uri)).thenReturn("trusted content");
        when(snapshotRepository.save(any(OnlineRagSourceSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = adapter.retrieve(uri, longPublisher);

        assertThat(result.metadata().publisher()).hasSize(255);
        assertThat(result.metadata().publisher()).isEqualTo("P".repeat(255));
    }

    @Test
    @DisplayName("Cache quá TTL được refresh thay vì dùng snapshot cũ")
    void retrieve_staleCachedSource_refreshesSnapshot() {
        URI uri = URI.create("https://cdc.gov/lab/stale");
        OnlineRagSourceSnapshot staleSnapshot = snapshot(uri, OnlineRagReviewStatus.APPROVED, false, "stale content");
        staleSnapshot.setRetrievedAt(FIXED_NOW.minus(Duration.ofHours(2)));
        TrustedOnlineRagSourceAdapter shortTtlAdapter = new TrustedOnlineRagSourceAdapter(
                new TrustedOnlineRagSourcePolicy("who.int,cdc.gov"),
                snapshotRepository,
                httpClient,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                Duration.ofHours(1)
        );
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.of(staleSnapshot));
        when(httpClient.fetch(uri)).thenReturn("fresh content");
        when(snapshotRepository.save(any(OnlineRagSourceSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = shortTtlAdapter.retrieve(uri, "CDC");

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.metadata().retrievedAt()).isEqualTo(FIXED_NOW);
        assertThat(result.metadata().stale()).isFalse();
        verify(httpClient).fetch(uri);
    }

    @Test
    @DisplayName("Cache quá TTL nhưng fetch lỗi trả metadata snapshot cũ là stale và không dùng cho AI")
    void retrieve_staleCachedSourceFetchFailure_returnsStaleMetadataOnly() {
        URI uri = URI.create("https://cdc.gov/lab/stale-timeout");
        OnlineRagSourceSnapshot staleSnapshot = snapshot(uri, OnlineRagReviewStatus.APPROVED, false, "stale content");
        staleSnapshot.setRetrievedAt(FIXED_NOW.minus(Duration.ofHours(2)));
        TrustedOnlineRagSourceAdapter shortTtlAdapter = new TrustedOnlineRagSourceAdapter(
                new TrustedOnlineRagSourcePolicy("who.int,cdc.gov"),
                snapshotRepository,
                httpClient,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC),
                Duration.ofHours(1)
        );
        when(snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(uri.toString())).thenReturn(Optional.of(staleSnapshot));
        when(httpClient.fetch(uri)).thenThrow(new IllegalStateException("timeout"));

        TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult result = shortTtlAdapter.retrieve(uri, "CDC");

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.usableForAi()).isFalse();
        assertThat(result.content()).isEmpty();
        assertThat(result.metadata().retrievedAt()).isEqualTo(FIXED_NOW.minus(Duration.ofHours(2)));
        assertThat(result.metadata().stale()).isTrue();
        verify(httpClient).fetch(uri);
    }

    private OnlineRagSourceSnapshot snapshot(
            URI uri,
            OnlineRagReviewStatus status,
            boolean excluded,
            String content
    ) {
        OnlineRagSourceSnapshot snapshot = new OnlineRagSourceSnapshot();
        snapshot.setSourceUrl(uri.toString());
        snapshot.setCanonicalHost(uri.getHost().replace("www.", ""));
        snapshot.setPublisher(snapshot.getCanonicalHost().equals("cdc.gov") ? "CDC" : "WHO");
        snapshot.setRetrievedAt(FIXED_NOW);
        snapshot.setSnapshotHash("a".repeat(64));
        snapshot.setReviewStatus(status);
        snapshot.setExcluded(excluded);
        snapshot.setContentLength(content.length());
        snapshot.setContentSnapshot(content);
        return snapshot;
    }
}
