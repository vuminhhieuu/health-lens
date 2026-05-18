package com.healthlens.api.persistence.json;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

/**
 * Database-specific JSON path extraction for JPA Criteria queries.
 *
 * <p>Health Lens stores audit payloads as JSON/JSONB. Implementations must match the configured
 * datasource; production and tests use PostgreSQL ({@code jsonb} + {@code jsonb_extract_path_text}).
 */
public interface JsonPathExpressions {

    /**
     * Extract a text value from a JSON column using the backing database's native function.
     *
     * @param root entity root (e.g. {@code AuditLog})
     * @param cb criteria builder
     * @param jsonColumnAttribute JPA attribute name of the JSON column (e.g. {@code newValueJson})
     * @param pathSegments path within the JSON document (e.g. {@code "email"})
     */
    <T> Expression<String> extractPathText(
            Root<T> root,
            CriteriaBuilder cb,
            String jsonColumnAttribute,
            String... pathSegments
    );

    /** Short identifier for logs and startup validation (e.g. {@code postgresql}). */
    String dialectId();
}
