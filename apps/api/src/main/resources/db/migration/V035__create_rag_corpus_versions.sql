CREATE TABLE rag_corpus_versions (
    source_version VARCHAR(100) PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    reviewer VARCHAR(100),
    effective_date TIMESTAMP,
    embedding_model VARCHAR(100) NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    chunk_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP,
    rolled_back_at TIMESTAMP,
    rolled_back_by VARCHAR(100),
    rollback_reason TEXT
);

CREATE INDEX idx_rag_corpus_versions_status_effective
    ON rag_corpus_versions(status, effective_date DESC, created_at DESC);
