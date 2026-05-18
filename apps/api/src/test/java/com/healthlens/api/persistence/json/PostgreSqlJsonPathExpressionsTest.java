package com.healthlens.api.persistence.json;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgreSqlJsonPathExpressionsTest {

    @Mock
    private Root<Object> root;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> jsonColumn;

    @Mock
    private Expression<String> extracted;

    @Test
    @DisplayName("extractPathText uses jsonb_extract_path_text with column and path literals")
    void extractPathText_usesPostgresFunction() {
        when(root.get("newValueJson")).thenReturn(jsonColumn);
        when(cb.literal("email")).thenReturn(null);
        when(cb.function(
                eq(PostgreSqlJsonPathExpressions.EXTRACT_PATH_TEXT),
                eq(String.class),
                eq(jsonColumn),
                eq(null)
        )).thenReturn(extracted);

        PostgreSqlJsonPathExpressions expressions = new PostgreSqlJsonPathExpressions();
        Expression<String> result = expressions.extractPathText(root, cb, "newValueJson", "email");

        assertThat(result).isSameAs(extracted);
        verify(cb).function(
                eq(PostgreSqlJsonPathExpressions.EXTRACT_PATH_TEXT),
                eq(String.class),
                eq(jsonColumn),
                eq(null)
        );
        assertThat(expressions.dialectId()).isEqualTo("postgresql");
    }
}
