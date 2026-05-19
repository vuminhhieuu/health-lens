package com.healthlens.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class SecurityStartupValidatorTest {

    @Test
    void run_failsWhenJwtSecretIsTooShort() {
        MockEnvironment environment = new MockEnvironment();
        SecurityStartupValidator validator =
                new SecurityStartupValidator(environment, "short-secret", List.of("https://healthlens.vn"));

        assertThatThrownBy(validator::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret must be at least 32 bytes");
    }

    @Test
    void run_failsProductionWhenJwtSecretUsesKnownDevelopmentDefault() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                "dev-test-secret-key-min-32-chars-for-local",
                List.of("https://healthlens.vn"));

        assertThatThrownBy(validator::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is weak or uses an insecure development/default value");
    }

    @Test
    void run_failsProductionWhenJwtSecretHasLowDiversityDespiteValidLength() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                List.of("https://healthlens.vn"));

        assertThatThrownBy(validator::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is weak or uses an insecure development/default value");
    }

    @Test
    void run_failsProductionWhenCorsAllowsWildcardWithCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                "HealthLens-Production-Secret-2026-Randomized!",
                List.of("*"));

        assertThatThrownBy(validator::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard CORS origins cannot be used with credentials");
    }

    @Test
    void run_acceptsValidProductionSecurityConfig() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                "HealthLens-Production-Secret-2026-Randomized!",
                List.of("https://healthlens.vn", "https://app.healthlens.vn"));

        assertThatCode(validator::run).doesNotThrowAnyException();
    }

    @Test
    void run_acceptsStrongProductionSecretContainingDefaultSubstring() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                "HealthLens-default-Rotation-2026-Secret-9!",
                List.of("https://healthlens.vn"));

        assertThatCode(validator::run).doesNotThrowAnyException();
    }

    @Test
    void run_logsSafeSummaryWithoutJwtSecret(CapturedOutput output) {
        String secret = "HealthLens-Production-Unique-Key-2026-Not-Logged!";
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        SecurityStartupValidator validator = new SecurityStartupValidator(
                environment,
                secret,
                List.of("https://healthlens.vn"));

        validator.run();

        assertThat(output).contains("Security startup validation passed");
        assertThat(output).contains("jwtSecretBytes=");
        assertThat(output).doesNotContain(secret);
    }
}
