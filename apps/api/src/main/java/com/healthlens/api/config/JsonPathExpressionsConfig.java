package com.healthlens.api.config;

import com.healthlens.api.persistence.json.JsonPathExpressions;
import com.healthlens.api.persistence.json.PostgreSqlJsonPathExpressions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Registers dialect-specific JSON path helpers. Health Lens requires PostgreSQL for JSONB audit
 * columns and native JSON functions in admin audit filters.
 */
@Configuration
public class JsonPathExpressionsConfig {

    @Bean
    @ConditionalOnMissingBean(JsonPathExpressions.class)
    JsonPathExpressions jsonPathExpressions(
            @Value("${spring.datasource.url:}") String jdbcUrl,
            PostgreSqlJsonPathExpressions postgreSqlJsonPathExpressions
    ) {
        if (!StringUtils.hasText(jdbcUrl) || jdbcUrl.toLowerCase().contains("postgresql")) {
            return postgreSqlJsonPathExpressions;
        }
        throw new IllegalStateException(
                "Unsupported datasource for JSON audit filters: "
                        + jdbcUrl
                        + ". Health Lens requires PostgreSQL (jsonb / jsonb_extract_path_text)."
        );
    }
}
