CREATE TABLE online_rag_answer_citations (
    id UUID PRIMARY KEY,
    health_record_id UUID NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    metric_name VARCHAR(255) NOT NULL,
    answer_hash VARCHAR(64) NOT NULL,
    source_snapshot_id UUID REFERENCES online_rag_source_snapshots(id),
    source_url VARCHAR(2048) NOT NULL,
    publisher VARCHAR(255) NOT NULL,
    retrieved_at TIMESTAMP,
    snapshot_hash VARCHAR(64) NOT NULL,
    review_status VARCHAR(30) NOT NULL,
    excluded BOOLEAN NOT NULL DEFAULT TRUE,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    usable_for_ai BOOLEAN NOT NULL DEFAULT FALSE,
    review_required BOOLEAN NOT NULL DEFAULT FALSE,
    rejected BOOLEAN NOT NULL DEFAULT FALSE,
    retrieval_source VARCHAR(80) NOT NULL,
    prompt_version VARCHAR(120) NOT NULL,
    model_version VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_online_rag_answer_citations_record_metric
    ON online_rag_answer_citations(health_record_id, metric_name, created_at DESC);

CREATE INDEX idx_online_rag_answer_citations_source_url
    ON online_rag_answer_citations(md5(source_url));

CREATE INDEX idx_online_rag_answer_citations_snapshot_hash
    ON online_rag_answer_citations(snapshot_hash);

CREATE INDEX idx_online_rag_answer_citations_review_status
    ON online_rag_answer_citations(review_status, excluded);

CREATE UNIQUE INDEX uq_online_rag_answer_citations_answer_source
    ON online_rag_answer_citations(health_record_id, metric_name, answer_hash, md5(source_url), snapshot_hash);
