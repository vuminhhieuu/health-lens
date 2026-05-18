package com.healthlens.api.persistence.json;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL {@code jsonb_extract_path_text(jsonb, variadic text)} for Criteria API queries.
 */
@Component
public class PostgreSqlJsonPathExpressions implements JsonPathExpressions {

    static final String EXTRACT_PATH_TEXT = "jsonb_extract_path_text";

    @Override
    public <T> Expression<String> extractPathText(
            Root<T> root,
            CriteriaBuilder cb,
            String jsonColumnAttribute,
            String... pathSegments
    ) {
        List<Expression<?>> args = new ArrayList<>(pathSegments.length + 1);
        args.add(root.get(jsonColumnAttribute));
        for (String segment : pathSegments) {
            args.add(cb.literal(segment));
        }
        return cb.function(
                EXTRACT_PATH_TEXT,
                String.class,
                args.toArray(Expression[]::new)
        );
    }

    @Override
    public String dialectId() {
        return "postgresql";
    }
}
