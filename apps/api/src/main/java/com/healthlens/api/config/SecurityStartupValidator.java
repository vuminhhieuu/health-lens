package com.healthlens.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class SecurityStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupValidator.class);
    private static final int MIN_JWT_SECRET_BYTES = 32;
    private static final List<String> STRICT_PROFILES = List.of("production", "staging");
    private static final List<String> INSECURE_JWT_SECRET_VALUES = List.of(
            "dev-test-secret-key-min-32-chars-for-local",
            "healthlens-dev-secret-key-must-be-at-least-256-bits-long-for-hs256-algo",
            "dev-placeholder",
            "change-me",
            "changeme"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final List<String> allowedOrigins;

    public SecurityStartupValidator(
            Environment environment,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void run(ApplicationArguments args) {
        run();
    }

    void run() {
        validateJwtSecret();
        validateCors();
        log.info(
                "Security startup validation passed: strictProfile={}, jwtSecretBytes={}, corsOrigins={}",
                isStrictProfile(),
                jwtSecret == null ? 0 : jwtSecret.getBytes(StandardCharsets.UTF_8).length,
                allowedOrigins == null ? 0 : allowedOrigins.size()
        );
    }

    private void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        int secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8).length;
        if (secretBytes < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256 signing");
        }

        if (isStrictProfile() && isWeakJwtSecret(jwtSecret)) {
            throw new IllegalStateException("JWT secret is weak or uses an insecure development/default value");
        }
    }

    private void validateCors() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("CORS allowed origins must be configured");
        }

        boolean hasWildcard = allowedOrigins.stream()
                .map(String::trim)
                .anyMatch("*"::equals);

        if (hasWildcard && isStrictProfile()) {
            throw new IllegalStateException("Wildcard CORS origins cannot be used with credentials");
        }
    }

    private boolean isStrictProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(STRICT_PROFILES::contains);
    }

    private static boolean isInsecureDefaultJwtSecret(String secret) {
        String normalized = secret.trim().toLowerCase(Locale.ROOT);
        return INSECURE_JWT_SECRET_VALUES.contains(normalized);
    }

    private static boolean isWeakJwtSecret(String secret) {
        String normalized = secret.trim();
        if (isInsecureDefaultJwtSecret(normalized)) {
            return true;
        }

        long distinctChars = normalized.chars().distinct().count();
        boolean hasLower = normalized.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = normalized.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = normalized.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = normalized.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return distinctChars < 12 || countTrue(hasLower, hasUpper, hasDigit, hasSymbol) < 3;
    }

    private static int countTrue(boolean... values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }
}
