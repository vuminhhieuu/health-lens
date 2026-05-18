package com.healthlens.api.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class QdrantVectorStoreEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "healthlensQdrantVectorStore";
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]$|^[A-Za-z0-9]$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new LinkedHashMap<>();

        String host = firstText(environment, "QDRANT_HOST", "spring.ai.vectorstore.qdrant.host");
        if (StringUtils.hasText(host)) {
            normalizeOrValidateHost(environment, properties, host);
        }

        String port = firstText(environment, "QDRANT_PORT", "spring.ai.vectorstore.qdrant.port");
        if (StringUtils.hasText(port)) {
            validatePort(port);
            properties.putIfAbsent("spring.ai.vectorstore.qdrant.port", port);
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 2;
    }

    private static void normalizeOrValidateHost(
            ConfigurableEnvironment environment,
            Map<String, Object> properties,
            String host
    ) {
        if (!containsProtocol(host)) {
            validateHostnameOnly(host);
            properties.put("spring.ai.vectorstore.qdrant.host", host);
            return;
        }

        URI uri = parseHostUri(host);
        String normalizedHost = uri.getHost();
        if (!StringUtils.hasText(normalizedHost)) {
            throw new IllegalStateException("QDRANT_HOST must be a hostname without protocol, for example cluster.qdrant.io.");
        }

        if (requiresStrictValidation(environment)) {
            throw new IllegalStateException("QDRANT_HOST must be a hostname without protocol in production/staging. "
                    + "Use '" + normalizedHost + "' instead of '" + host + "'.");
        }

        properties.put("spring.ai.vectorstore.qdrant.host", normalizedHost);
        if (uri.getPort() > 0 && !StringUtils.hasText(environment.getProperty("QDRANT_PORT"))) {
            int uriPort = uri.getPort();
            validatePortNumber(uriPort);
            properties.put("spring.ai.vectorstore.qdrant.port", Integer.toString(uriPort));
        }
    }

    private static URI parseHostUri(String host) {
        try {
            return new URI(host);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("QDRANT_HOST must be a valid hostname without protocol.", ex);
        }
    }

    private static void validatePort(String port) {
        try {
            validatePortNumber(Integer.parseInt(port));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("QDRANT_PORT must be a whole number between 1 and 65535.", ex);
        }
    }

    private static void validatePortNumber(int port) {
        if (port < 1 || port > 65_535) {
            throw new IllegalStateException("QDRANT_PORT must be between 1 and 65535.");
        }
    }

    private static boolean containsProtocol(String host) {
        return host.contains("://");
    }

    private static void validateHostnameOnly(String host) {
        if (host.contains(":")
                || host.contains("/")
                || host.contains("?")
                || host.contains("#")
                || host.contains("@")
                || !HOSTNAME_PATTERN.matcher(host).matches()
                || host.contains("..")) {
            throw new IllegalStateException("QDRANT_HOST must be a hostname without protocol, port, path, query, fragment, or user-info.");
        }
    }

    private static boolean requiresStrictValidation(ConfigurableEnvironment environment) {
        return environment.acceptsProfiles(Profiles.of("production", "staging"))
                || environment.getProperty("app.ai.rag.validation.required", Boolean.class, false);
    }

    private static String firstText(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
            String relaxedKey = key.toLowerCase(Locale.ROOT).replace('_', '.');
            value = environment.getProperty(relaxedKey);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
