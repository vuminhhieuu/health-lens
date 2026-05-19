package com.healthlens.api.service.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TrustedOnlineRagSourcePolicy {

    private final Set<String> allowlistedHosts;

    public TrustedOnlineRagSourcePolicy(
            @Value("${app.ai.online-rag.allowlisted-hosts:}") String allowlistedHosts
    ) {
        this.allowlistedHosts = Arrays.stream(allowlistedHosts.split(","))
                .map(TrustedOnlineRagSourcePolicy::normalizeHost)
                .filter(host -> !host.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isTrusted(URI uri) {
        String host = canonicalHost(uri);
        return isHttps(uri) && !host.isBlank() && allowlistedHosts.contains(host);
    }

    public String canonicalHost(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return "";
        }
        return normalizeHost(uri.getHost());
    }

    private static String normalizeHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    private static boolean isHttps(URI uri) {
        return uri != null && "https".equalsIgnoreCase(uri.getScheme());
    }
}
