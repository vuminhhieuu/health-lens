package com.healthlens.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiChatProviderEnvironmentPostProcessorTest {

    private final AiChatProviderEnvironmentPostProcessor postProcessor =
            new AiChatProviderEnvironmentPostProcessor();

    @Test
    void postProcessEnvironment_mapsGenericAiChatKeysToSpringAiOpenAiProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AI_CHAT_PROVIDER", "openai-compatible")
                .withProperty("AI_CHAT_BASE_URL", "https://example.test/openai")
                .withProperty("AI_CHAT_API_KEY", "generic-key")
                .withProperty("AI_CHAT_MODEL", "generic-model")
                .withProperty("AI_CHAT_TIMEOUT_MS", "15000");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("app.ai.chat.provider")).isEqualTo("openai-compatible");
        assertThat(environment.getProperty("app.ai.chat.base-url")).isEqualTo("https://example.test/openai");
        assertThat(environment.getProperty("app.ai.chat.api-key")).isEqualTo("generic-key");
        assertThat(environment.getProperty("app.ai.chat.model")).isEqualTo("generic-model");
        assertThat(environment.getProperty("app.ai.chat.timeout-ms")).isEqualTo("15000");
        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("generic-key");
        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://example.test/openai");
        assertThat(environment.getProperty("spring.ai.openai.chat.options.model")).isEqualTo("generic-model");
    }

    @Test
    void postProcessEnvironment_mapsLegacyGroqKeysWhenGenericKeysAreAbsent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("GROQ_BASE_URL", "https://api.groq.com/openai")
                .withProperty("GROQ_API_KEY", "legacy-key")
                .withProperty("GROQ_CHAT_MODEL", "qwen-legacy");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("app.ai.chat.provider")).isEqualTo("openai-compatible");
        assertThat(environment.getProperty("app.ai.chat.base-url")).isEqualTo("https://api.groq.com/openai");
        assertThat(environment.getProperty("app.ai.chat.api-key")).isEqualTo("legacy-key");
        assertThat(environment.getProperty("app.ai.chat.model")).isEqualTo("qwen-legacy");
        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("legacy-key");
    }

    @Test
    void postProcessEnvironment_prefersGenericKeysOverLegacyGroqKeys() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AI_CHAT_API_KEY", "generic-key")
                .withProperty("AI_CHAT_BASE_URL", "https://generic.test/openai")
                .withProperty("AI_CHAT_MODEL", "generic-model")
                .withProperty("GROQ_API_KEY", "legacy-key")
                .withProperty("GROQ_BASE_URL", "https://legacy.test/openai")
                .withProperty("GROQ_CHAT_MODEL", "legacy-model");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("app.ai.chat.api-key")).isEqualTo("generic-key");
        assertThat(environment.getProperty("app.ai.chat.base-url")).isEqualTo("https://generic.test/openai");
        assertThat(environment.getProperty("app.ai.chat.model")).isEqualTo("generic-model");
    }

    @Test
    void postProcessEnvironment_failsProductionStartupWhenRequiredChatConfigIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required AI chat configuration for this runtime")
                .hasMessageContaining("AI_CHAT_API_KEY")
                .hasMessageContaining("AI_CHAT_BASE_URL")
                .hasMessageContaining("AI_CHAT_MODEL");
    }

    @Test
    void postProcessEnvironment_failsStagingStartupWhenRequiredChatConfigIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required AI chat configuration for this runtime")
                .hasMessageContaining("AI_CHAT_API_KEY");
    }

    @Test
    void postProcessEnvironment_failsStagingWhenOnlyApplicationDefaultsFillRequiredChatConfig() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AI_CHAT_API_KEY", "configured-key");
        environment.setActiveProfiles("staging");
        addApplicationDefaults(environment);

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing required AI chat configuration for this runtime")
                .hasMessageContaining("AI_CHAT_BASE_URL")
                .hasMessageContaining("AI_CHAT_MODEL");
    }

    @Test
    void postProcessEnvironment_acceptsDirectSpringAiPropertiesInRequiredProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "spring-key")
                .withProperty("spring.ai.openai.base-url", "https://spring.test/openai")
                .withProperty("spring.ai.openai.chat.options.model", "spring-model");
        environment.setActiveProfiles("production");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("spring-key");
        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://spring.test/openai");
        assertThat(environment.getProperty("spring.ai.openai.chat.options.model")).isEqualTo("spring-model");
    }

    @Test
    void postProcessEnvironment_doesNotOverrideDirectSpringAiPropertiesWithLocalDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.openai.base-url", "https://spring.test/openai")
                .withProperty("spring.ai.openai.chat.options.model", "spring-model");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://spring.test/openai");
        assertThat(environment.getProperty("spring.ai.openai.chat.options.model")).isEqualTo("spring-model");
    }

    @Test
    void postProcessEnvironment_doesNotOverrideDirectSpringAiPropertiesWithApplicationDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.openai.api-key", "spring-key")
                .withProperty("spring.ai.openai.base-url", "https://spring.test/openai")
                .withProperty("spring.ai.openai.chat.options.model", "spring-model");
        environment.setActiveProfiles("production");
        addApplicationDefaults(environment);

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("spring-key");
        assertThat(environment.getProperty("spring.ai.openai.base-url")).isEqualTo("https://spring.test/openai");
        assertThat(environment.getProperty("spring.ai.openai.chat.options.model")).isEqualTo("spring-model");
        assertThat(environment.getProperty("app.ai.chat.base-url")).isEqualTo("https://spring.test/openai");
        assertThat(environment.getProperty("app.ai.chat.model")).isEqualTo("spring-model");
    }

    @Test
    void postProcessEnvironment_rejectsInvalidTimeoutValues() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AI_CHAT_TIMEOUT_MS", "0");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_CHAT_TIMEOUT_MS must be between");
    }

    @Test
    void springFactories_registersAiChatProviderEnvironmentPostProcessor() {
        assertThat(SpringFactoriesLoader
                .loadFactoryNames(EnvironmentPostProcessor.class, getClass().getClassLoader()))
                .contains(AiChatProviderEnvironmentPostProcessor.class.getName());
    }

    @Test
    void postProcessEnvironment_failsWhenNativeProviderIsRequestedWithoutAdapter() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AI_CHAT_PROVIDER", "anthropic")
                .withProperty("AI_CHAT_BASE_URL", "https://api.anthropic.com")
                .withProperty("AI_CHAT_API_KEY", "native-key")
                .withProperty("AI_CHAT_MODEL", "claude-sonnet");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported AI_CHAT_PROVIDER 'anthropic'")
                .hasMessageContaining("env-only switching is supported only for OpenAI-compatible providers");
    }

    private static void addApplicationDefaults(MockEnvironment environment) {
        environment.getPropertySources().addLast(new MapPropertySource(
                "Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'",
                Map.of(
                        "app.ai.chat.base-url", "https://api.groq.com/openai",
                        "app.ai.chat.api-key", "dev-placeholder",
                        "app.ai.chat.model", "qwen-2.5-72b-versatile",
                        "spring.ai.openai.base-url", "https://api.groq.com/openai",
                        "spring.ai.openai.api-key", "dev-placeholder",
                        "spring.ai.openai.chat.options.model", "qwen-2.5-72b-versatile"
                )
        ));
    }
}
