package com.healthlens.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiChatProviderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AiChatProviderEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "healthlensAiChatProvider";
    private static final String PROVIDER_OPENAI_COMPATIBLE = "openai-compatible";
    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai";
    private static final String DEFAULT_MODEL = "qwen-2.5-72b-versatile";
    private static final String DEFAULT_TIMEOUT_MS = "30000";
    private static final String DEV_PLACEHOLDER = "dev-placeholder";
    private static final long MIN_TIMEOUT_MS = 1_000;
    private static final long MAX_TIMEOUT_MS = 300_000;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new LinkedHashMap<>();

        String provider = firstText(environment, "AI_CHAT_PROVIDER");
        if (!StringUtils.hasText(provider) && hasLegacyChatConfig(environment)) {
            provider = PROVIDER_OPENAI_COMPATIBLE;
        }
        if (!StringUtils.hasText(provider)) {
            provider = isProduction(environment) ? "" : PROVIDER_OPENAI_COMPATIBLE;
        }

        if (StringUtils.hasText(provider) && !PROVIDER_OPENAI_COMPATIBLE.equals(normalizeProvider(provider))) {
            throw new IllegalStateException("Unsupported AI_CHAT_PROVIDER '" + provider + "'. "
                    + "HealthLens currently has no native adapter for this provider; env-only switching is supported only for OpenAI-compatible providers.");
        }

        addIfPresent(properties, "app.ai.chat.provider", provider);
        mapChatValue(environment, properties, "AI_CHAT_BASE_URL", "GROQ_BASE_URL",
                "app.ai.chat.base-url", "spring.ai.openai.base-url");
        mapChatValue(environment, properties, "AI_CHAT_API_KEY", "GROQ_API_KEY",
                "app.ai.chat.api-key", "spring.ai.openai.api-key");
        mapChatValue(environment, properties, "AI_CHAT_MODEL", "GROQ_CHAT_MODEL",
                "app.ai.chat.model", "spring.ai.openai.chat.options.model");
        mapChatValue(environment, properties, "AI_CHAT_TIMEOUT_MS", null,
                "app.ai.chat.timeout-ms");

        if (!isProduction(environment)) {
            putDefaultIfMissing(environment, properties, "app.ai.chat.base-url", DEFAULT_BASE_URL);
            putDefaultIfMissing(environment, properties, "spring.ai.openai.base-url", DEFAULT_BASE_URL);
            putDefaultIfMissing(environment, properties, "app.ai.chat.model", DEFAULT_MODEL);
            putDefaultIfMissing(environment, properties, "spring.ai.openai.chat.options.model", DEFAULT_MODEL);
            putDefaultIfMissing(environment, properties, "app.ai.chat.timeout-ms", DEFAULT_TIMEOUT_MS);
        }

        validateProductionConfig(environment, properties);
        validateTimeoutConfig(environment, properties);

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    private static void mapChatValue(
            ConfigurableEnvironment environment,
            Map<String, Object> properties,
            String genericKey,
            String legacyKey,
            String... targetProperties
    ) {
        String value = firstText(environment, genericKey);
        if (!StringUtils.hasText(value) && legacyKey != null) {
            value = firstText(environment, legacyKey);
            if (StringUtils.hasText(value)) {
                log.warn("{} is deprecated. Please migrate to {}.", legacyKey, genericKey);
            }
        }
        if (StringUtils.hasText(value)) {
            for (String targetProperty : targetProperties) {
                properties.put(targetProperty, value);
            }
            return;
        }

        value = firstExternalText(environment, targetProperties);
        if (!StringUtils.hasText(value)) {
            return;
        }

        for (String targetProperty : targetProperties) {
            if (hasExternalText(environment, targetProperty)) {
                continue;
            }
            properties.put(targetProperty, value);
        }
    }

    private static void validateProductionConfig(ConfigurableEnvironment environment, Map<String, Object> properties) {
        if (!requiresFailFastValidation(environment)) {
            return;
        }
        List<String> missing = new ArrayList<>();
        requireText(environment, properties, "AI_CHAT_API_KEY", missing,
                "AI_CHAT_API_KEY", "GROQ_API_KEY", "spring.ai.openai.api-key", "app.ai.chat.api-key");
        requireText(environment, properties, "AI_CHAT_BASE_URL", missing,
                "AI_CHAT_BASE_URL", "GROQ_BASE_URL", "spring.ai.openai.base-url", "app.ai.chat.base-url");
        requireText(environment, properties, "AI_CHAT_MODEL", missing,
                "AI_CHAT_MODEL", "GROQ_CHAT_MODEL", "spring.ai.openai.chat.options.model", "app.ai.chat.model");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required AI chat configuration for this runtime: "
                    + String.join(", ", missing)
                    + ". Set the generic AI_CHAT_* environment variables. Legacy GROQ_* keys are accepted temporarily for migration only.");
        }
    }

    private static void validateTimeoutConfig(ConfigurableEnvironment environment, Map<String, Object> properties) {
        String timeout = effectiveText(properties, "app.ai.chat.timeout-ms");
        if (!StringUtils.hasText(timeout)) {
            timeout = firstText(environment, "app.ai.chat.timeout-ms");
        }
        if (!StringUtils.hasText(timeout)) {
            return;
        }
        try {
            long timeoutMs = Long.parseLong(timeout);
            if (timeoutMs < MIN_TIMEOUT_MS || timeoutMs > MAX_TIMEOUT_MS) {
                throw new IllegalStateException("AI_CHAT_TIMEOUT_MS must be between "
                        + MIN_TIMEOUT_MS + " and " + MAX_TIMEOUT_MS + " milliseconds.");
            }
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("AI_CHAT_TIMEOUT_MS must be a whole number of milliseconds.", ex);
        }
    }

    private static void requireText(
            ConfigurableEnvironment environment,
            Map<String, Object> properties,
            String envKey,
            List<String> missing,
            String... propertyNames
    ) {
        String value = effectiveText(properties, propertyNames);
        if (!StringUtils.hasText(value)) {
            value = firstExternalText(environment, propertyNames);
        }
        if (!StringUtils.hasText(value) || DEV_PLACEHOLDER.equals(value)) {
            missing.add(envKey);
        }
    }

    private static boolean hasLegacyChatConfig(ConfigurableEnvironment environment) {
        return StringUtils.hasText(firstText(environment, "GROQ_API_KEY"))
                || StringUtils.hasText(firstText(environment, "GROQ_BASE_URL"))
                || StringUtils.hasText(firstText(environment, "GROQ_CHAT_MODEL"));
    }

    private static boolean isProduction(ConfigurableEnvironment environment) {
        return environment.acceptsProfiles(Profiles.of("production"));
    }

    private static boolean requiresFailFastValidation(ConfigurableEnvironment environment) {
        return environment.acceptsProfiles(Profiles.of("production", "staging"))
                || environment.getProperty("app.ai.chat.required", Boolean.class, false);
    }

    private static String normalizeProvider(String provider) {
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private static void addIfPresent(Map<String, Object> properties, String property, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(property, value);
        }
    }

    private static void putDefaultIfMissing(
            ConfigurableEnvironment environment,
            Map<String, Object> properties,
            String property,
            String defaultValue
    ) {
        if (!properties.containsKey(property) && !StringUtils.hasText(firstText(environment, property))) {
            properties.put(property, defaultValue);
        }
    }

    private static String effectiveText(
            Map<String, Object> properties,
            String... propertyNames
    ) {
        for (String propertyName : propertyNames) {
            Object override = properties.get(propertyName);
            if (override != null && StringUtils.hasText(override.toString())) {
                return override.toString().trim();
            }
        }
        return null;
    }

    private static String firstExternalText(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = findByNonApplicationSourceName(environment, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasExternalText(ConfigurableEnvironment environment, String key) {
        return StringUtils.hasText(findByNonApplicationSourceName(environment, key));
    }

    private static String firstText(ConfigurableEnvironment environment, String key) {
        String value = environment.getProperty(key);
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        String relaxedKey = key.toLowerCase(Locale.ROOT).replace('_', '.');
        value = environment.getProperty(relaxedKey);
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return findByExactSourceName(environment, key);
    }

    private static String findByExactSourceName(ConfigurableEnvironment environment, String key) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            Object value = propertySource.getProperty(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString().trim();
            }
        }
        return null;
    }

    private static String findByNonApplicationSourceName(ConfigurableEnvironment environment, String key) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (isApplicationConfigSource(propertySource)) {
                continue;
            }
            for (String candidateKey : candidateKeys(key)) {
                Object value = propertySource.getProperty(candidateKey);
                if (value != null && StringUtils.hasText(value.toString())) {
                    return value.toString().trim();
                }
            }
        }
        return null;
    }

    private static boolean isApplicationConfigSource(PropertySource<?> propertySource) {
        String name = propertySource.getName();
        return "configurationProperties".equals(name)
                || name.startsWith("Config resource '")
                || name.startsWith("applicationConfig:")
                || name.contains("[application.yml]")
                || name.contains("[application-docker.yml]");
    }

    private static List<String> candidateKeys(String key) {
        String envStyle = key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
        if (envStyle.equals(key)) {
            return List.of(key);
        }
        return List.of(key, envStyle);
    }
}
