CREATE TABLE online_rag_source_snapshots (
    id UUID PRIMARY KEY,
    source_url VARCHAR(2048) NOT NULL,
    canonical_host VARCHAR(255) NOT NULL,
    publisher VARCHAR(255) NOT NULL,
    retrieved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    snapshot_hash VARCHAR(64) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    excluded BOOLEAN NOT NULL DEFAULT TRUE,
    content_length INTEGER NOT NULL,
    content_snapshot TEXT NOT NULL
);

CREATE INDEX idx_online_rag_snapshots_source_url_retrieved
    ON online_rag_source_snapshots(source_url, retrieved_at DESC);

CREATE INDEX idx_online_rag_snapshots_review_status
    ON online_rag_source_snapshots(review_status, excluded);
