package com.healthlens.api.service.rag;

import com.healthlens.api.entity.OnlineRagReviewStatus;
import com.healthlens.api.entity.OnlineRagSourceSnapshot;
import com.healthlens.api.repository.OnlineRagSourceSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class TrustedOnlineRagSourceAdapter {

    private static final int MAX_SOURCE_URL_LENGTH = 2048;
    private static final int MAX_HOST_LENGTH = 255;
    private static final int MAX_PUBLISHER_LENGTH = 255;

    private final TrustedOnlineRagSourcePolicy sourcePolicy;
    private final OnlineRagSourceSnapshotRepository snapshotRepository;
    private final OnlineRagHttpClient httpClient;
    private final Clock clock;
    private final Duration cacheMaxAge;

    @Autowired
    public TrustedOnlineRagSourceAdapter(
            TrustedOnlineRagSourcePolicy sourcePolicy,
            OnlineRagSourceSnapshotRepository snapshotRepository,
            OnlineRagHttpClient httpClient,
            @Value("${app.ai.online-rag.cache-max-age:PT24H}") Duration cacheMaxAge
    ) {
        this(sourcePolicy, snapshotRepository, httpClient, Clock.systemUTC(), cacheMaxAge);
    }

    TrustedOnlineRagSourceAdapter(
            TrustedOnlineRagSourcePolicy sourcePolicy,
            OnlineRagSourceSnapshotRepository snapshotRepository,
            OnlineRagHttpClient httpClient,
            Clock clock
    ) {
        this(sourcePolicy, snapshotRepository, httpClient, clock, Duration.ofHours(24));
    }

    TrustedOnlineRagSourceAdapter(
            TrustedOnlineRagSourcePolicy sourcePolicy,
            OnlineRagSourceSnapshotRepository snapshotRepository,
            OnlineRagHttpClient httpClient,
            Clock clock,
            Duration cacheMaxAge
    ) {
        this.sourcePolicy = sourcePolicy;
        this.snapshotRepository = snapshotRepository;
        this.httpClient = httpClient;
        this.clock = clock;
        this.cacheMaxAge = cacheMaxAge == null || cacheMaxAge.isNegative() ? Duration.ZERO : cacheMaxAge;
    }

    public OnlineRagRetrievalResult retrieve(URI sourceUrl, String publisher) {
        String sourceUrlValue = sourceUrl == null ? "" : sourceUrl.toString();
        String canonicalHost = sourcePolicy.canonicalHost(sourceUrl);
        if (!isValidSource(sourceUrlValue, canonicalHost) || !sourcePolicy.isTrusted(sourceUrl)) {
            return OnlineRagRetrievalResult.rejected(sourceUrl, canonicalHost);
        }

        Optional<OnlineRagSourceSnapshot> cached = snapshotRepository.findFirstBySourceUrlOrderByRetrievedAtDesc(sourceUrlValue);
        if (cached.isPresent() && isFresh(cached.get())) {
            return fromSnapshot(cached.get(), true);
        }

        String content = Optional.ofNullable(httpClient.fetch(sourceUrl)).orElse("");
        OnlineRagSourceSnapshot snapshot = new OnlineRagSourceSnapshot();
        snapshot.setSourceUrl(sourceUrlValue);
        snapshot.setCanonicalHost(canonicalHost);
        snapshot.setPublisher(safePublisher(publisher, canonicalHost));
        snapshot.setRetrievedAt(Instant.now(clock));
        snapshot.setSnapshotHash(sha256(content));
        snapshot.setReviewStatus(OnlineRagReviewStatus.REVIEW_REQUIRED);
        snapshot.setExcluded(true);
        snapshot.setContentLength(content.length());
        snapshot.setContentSnapshot(content);

        return fromSnapshot(snapshotRepository.save(snapshot), false);
    }

    private OnlineRagRetrievalResult fromSnapshot(OnlineRagSourceSnapshot snapshot, boolean cacheHit) {
        boolean usable = snapshot.getReviewStatus() == OnlineRagReviewStatus.APPROVED && !snapshot.isExcluded();
        return new OnlineRagRetrievalResult(
                usable ? Optional.of(snapshot.getContentSnapshot()) : Optional.empty(),
                new OnlineRagSourceMetadata(
                        snapshot.getSourceUrl(),
                        snapshot.getPublisher(),
                        snapshot.getRetrievedAt(),
                        snapshot.getSnapshotHash(),
                        snapshot.getReviewStatus(),
                        snapshot.isExcluded()
                ),
                usable,
                snapshot.getReviewStatus() == OnlineRagReviewStatus.REVIEW_REQUIRED,
                false,
                cacheHit
        );
    }

    private boolean isValidSource(String sourceUrl, String canonicalHost) {
        return !sourceUrl.isBlank()
                && sourceUrl.length() <= MAX_SOURCE_URL_LENGTH
                && !canonicalHost.isBlank()
                && canonicalHost.length() <= MAX_HOST_LENGTH;
    }

    private boolean isFresh(OnlineRagSourceSnapshot snapshot) {
        Instant retrievedAt = snapshot.getRetrievedAt();
        if (retrievedAt == null || cacheMaxAge.isZero()) {
            return false;
        }
        return !retrievedAt.plus(cacheMaxAge).isBefore(Instant.now(clock));
    }

    private String safePublisher(String publisher, String fallbackHost) {
        String value = publisher == null || publisher.isBlank() ? fallbackHost : publisher.trim();
        if (value.length() <= MAX_PUBLISHER_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PUBLISHER_LENGTH);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record OnlineRagSourceMetadata(
            String sourceUrl,
            String publisher,
            Instant retrievedAt,
            String snapshotHash,
            OnlineRagReviewStatus reviewStatus,
            boolean excluded
    ) {
    }

    public record OnlineRagRetrievalResult(
            Optional<String> content,
            OnlineRagSourceMetadata metadata,
            boolean usableForAi,
            boolean reviewRequired,
            boolean rejected,
            boolean cacheHit
    ) {
        private static OnlineRagRetrievalResult rejected(URI sourceUrl, String host) {
            return new OnlineRagRetrievalResult(
                    Optional.empty(),
                    new OnlineRagSourceMetadata(
                            sourceUrl == null ? "" : sourceUrl.toString(),
                            host,
                            null,
                            "",
                            OnlineRagReviewStatus.REJECTED,
                            true
                    ),
                    false,
                    false,
                    true,
                    false
            );
        }
    }
}
