package com.healthlens.api.service.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class RestTemplateOnlineRagHttpClient implements OnlineRagHttpClient {

    private static final int MAX_REDIRECTS = 5;

    private final TrustedOnlineRagSourcePolicy sourcePolicy;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final int maxResponseBytes;

    public RestTemplateOnlineRagHttpClient(
            TrustedOnlineRagSourcePolicy sourcePolicy,
            @Value("${app.ai.online-rag.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.ai.online-rag.read-timeout-ms:5000}") long readTimeoutMs,
            @Value("${app.ai.online-rag.max-response-bytes:262144}") int maxResponseBytes
    ) {
        this.sourcePolicy = sourcePolicy;
        this.requestTimeout = Duration.ofMillis(readTimeoutMs);
        this.maxResponseBytes = Math.max(1, maxResponseBytes);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public String fetch(URI uri) {
        URI currentUri = uri;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            ensureTrusted(currentUri);
            HttpResponse<InputStream> response = send(currentUri);
            int statusCode = response.statusCode();

            if (statusCode >= 200 && statusCode < 300) {
                return readLimited(response.body());
            }

            if (statusCode >= 300 && statusCode < 400) {
                try {
                    currentUri = resolveRedirect(currentUri, response);
                } finally {
                    closeBody(response.body());
                }
                continue;
            }

            closeBody(response.body());
            throw new IllegalStateException("Trusted online RAG fetch failed with HTTP " + statusCode);
        }

        throw new IllegalStateException("Trusted online RAG fetch exceeded redirect limit");
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException ex) {
            throw new IllegalStateException("Trusted online RAG fetch failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Trusted online RAG fetch was interrupted", ex);
        }
    }

    private URI resolveRedirect(URI currentUri, HttpResponse<?> response) {
        String location = response.headers().firstValue("location")
                .orElseThrow(() -> new IllegalStateException("Trusted online RAG redirect missing Location header"));
        URI redirectedUri = currentUri.resolve(location);
        ensureTrusted(redirectedUri);
        return redirectedUri;
    }

    private void ensureTrusted(URI uri) {
        if (!sourcePolicy.isTrusted(uri)) {
            throw new IllegalArgumentException("Trusted online RAG fetch attempted untrusted URL: " + uri);
        }
    }

    private String readLimited(InputStream inputStream) {
        try (InputStream body = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int totalBytes = 0;
            int bytesRead;
            while ((bytesRead = body.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > maxResponseBytes) {
                    throw new IllegalStateException("Trusted online RAG response exceeds max size");
                }
                output.write(buffer, 0, bytesRead);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Trusted online RAG response could not be read", ex);
        }
    }

    private void closeBody(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException ex) {
            throw new IllegalStateException("Trusted online RAG response could not be closed", ex);
        }
    }
}
