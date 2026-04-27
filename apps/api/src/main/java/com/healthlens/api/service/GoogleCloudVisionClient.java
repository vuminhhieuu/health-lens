package com.healthlens.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Component
public class GoogleCloudVisionClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleCloudVisionClient(@Qualifier("ocrRestTemplate") RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${app.ocr.gcv.enabled:false}")
    private boolean gcvEnabled;

    @Value("${OCR_GCV_API_KEY:}")
    private String gcvApiKey;

    @Value("${app.ocr.gcv.endpoint:https://vision.googleapis.com/v1/images:annotate}")
    private String gcvEndpoint;

    @Value("${app.ocr.gcv.project-id:}")
    private String gcvProjectId;

    public OcrResult extract(String imageUrl) {
        if (!gcvEnabled) {
            throw new OcrProcessingException("Google Cloud Vision is disabled");
        }
        if (gcvApiKey == null || gcvApiKey.isBlank()) {
            throw new OcrProcessingException("Google Cloud Vision API key is missing");
        }
        if (gcvProjectId == null || gcvProjectId.isBlank()) {
            log.warn("Google Cloud Vision project-id is empty; please set OCR_GCV_PROJECT_ID for environment traceability");
        }

        long started = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = Map.of(
                    "requests", List.of(
                            Map.of(
                                    "image", Map.of("source", Map.of("imageUri", imageUrl)),
                                    "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION"))
                            )
                    )
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    gcvEndpoint + "?key=" + gcvApiKey,
                    new HttpEntity<>(request, headers),
                    String.class
            );
            String body = response.getBody();
            if (body == null || body.isBlank()) {
                throw new OcrProcessingException("Google Vision returned empty response");
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode first = root.path("responses").isArray() && !root.path("responses").isEmpty()
                    ? root.path("responses").get(0)
                    : null;
            if (first == null || first.isMissingNode()) {
                throw new OcrProcessingException("Google Vision missing response payload");
            }
            if (first.has("error")) {
                throw new OcrProcessingException("Google Vision error: " + first.path("error").path("message").asText());
            }

            String text = first.path("fullTextAnnotation").path("text").asText("");
            float confidence = extractConfidence(first.path("fullTextAnnotation"));
            String language = extractLanguage(first.path("fullTextAnnotation"));

            return OcrResult.builder()
                    .text(text)
                    .confidence(clampConfidence(confidence))
                    .source("gcv")
                    .language(language)
                    .processingTimeMs((int) (System.currentTimeMillis() - started))
                    .build();
        } catch (Exception ex) {
            throw new OcrProcessingException("Google Cloud Vision OCR failed", ex);
        }
    }

    private float extractConfidence(JsonNode annotation) {
        JsonNode pages = annotation.path("pages");
        if (!pages.isArray() || pages.isEmpty()) {
            return 0.0f;
        }

        float sum = 0.0f;
        int count = 0;
        for (JsonNode page : pages) {
            JsonNode blocks = page.path("blocks");
            if (!blocks.isArray()) {
                continue;
            }
            for (JsonNode block : blocks) {
                JsonNode paragraphs = block.path("paragraphs");
                if (!paragraphs.isArray()) {
                    continue;
                }
                for (JsonNode paragraph : paragraphs) {
                    if (paragraph.has("confidence")) {
                        sum += (float) paragraph.path("confidence").asDouble(0.0d);
                        count++;
                    }
                }
            }
        }
        return count == 0 ? 0.0f : sum / count;
    }

    private String extractLanguage(JsonNode annotation) {
        JsonNode pages = annotation.path("pages");
        if (!pages.isArray() || pages.isEmpty()) {
            return "unknown";
        }
        JsonNode languages = pages.get(0).path("property").path("detectedLanguages");
        if (!languages.isArray() || languages.isEmpty()) {
            return "unknown";
        }
        String code = languages.get(0).path("languageCode").asText("");
        return code.isBlank() ? "unknown" : code.toLowerCase(Locale.ROOT);
    }

    private float clampConfidence(float confidence) {
        if (Float.isNaN(confidence) || Float.isInfinite(confidence)) {
            return 0.0f;
        }
        if (confidence < 0f) return 0f;
        if (confidence > 1f) return 1f;
        return confidence;
    }
}
