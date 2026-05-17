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
            List<OcrResult.OcrPage> pages = extractPages(first.path("fullTextAnnotation"));
            List<OcrResult.OcrSegment> blocks = extractBlocks(first.path("fullTextAnnotation"));
            List<OcrResult.OcrSegment> lines = extractLines(first.path("fullTextAnnotation"));

            return OcrResult.builder()
                    .provider("gcv")
                    .modelVersion("google-document-text-detection")
                    .mimeType("image/*")
                    .retentionMode("external_provider")
                    .providerRequestId(null)
                    .latencyMs(System.currentTimeMillis() - started)
                    .pages(pages)
                    .blocks(blocks)
                    .lines(lines)
                    .text(text)
                    .confidence(clampConfidence(confidence))
                    .language(language)
                    .diagnostics(List.of())
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

    private List<OcrResult.OcrPage> extractPages(JsonNode annotation) {
        List<OcrResult.OcrPage> pagesOut = new java.util.ArrayList<>();
        JsonNode pages = annotation.path("pages");
        if (!pages.isArray()) {
            return pagesOut;
        }
        int pageNumber = 1;
        for (JsonNode page : pages) {
            Float confidence = page.has("confidence") ? (float) page.path("confidence").asDouble() : null;
            pagesOut.add(OcrResult.OcrPage.builder()
                    .pageNumber(pageNumber++)
                    .confidence(confidence)
                    .build());
        }
        return pagesOut;
    }

    private List<OcrResult.OcrSegment> extractBlocks(JsonNode annotation) {
        List<OcrResult.OcrSegment> blocksOut = new java.util.ArrayList<>();
        JsonNode pages = annotation.path("pages");
        if (!pages.isArray()) {
            return blocksOut;
        }
        int pageNumber = 1;
        for (JsonNode page : pages) {
            JsonNode blocks = page.path("blocks");
            if (!blocks.isArray()) {
                pageNumber++;
                continue;
            }
            for (JsonNode block : blocks) {
                Float confidence = block.has("confidence") ? (float) block.path("confidence").asDouble() : null;
                blocksOut.add(OcrResult.OcrSegment.builder()
                        .pageNumber(pageNumber)
                        .confidence(confidence)
                        .boundingBox(toBoundingBox(block.path("boundingBox")))
                        .kind("block")
                        .build());
            }
            pageNumber++;
        }
        return blocksOut;
    }

    private List<OcrResult.OcrSegment> extractLines(JsonNode annotation) {
        List<OcrResult.OcrSegment> linesOut = new java.util.ArrayList<>();
        JsonNode pages = annotation.path("pages");
        if (!pages.isArray()) {
            return linesOut;
        }
        int pageNumber = 1;
        for (JsonNode page : pages) {
            JsonNode blocks = page.path("blocks");
            if (!blocks.isArray()) {
                pageNumber++;
                continue;
            }
            for (JsonNode block : blocks) {
                JsonNode paragraphs = block.path("paragraphs");
                if (!paragraphs.isArray()) {
                    continue;
                }
                for (JsonNode paragraph : paragraphs) {
                    StringBuilder lineText = new StringBuilder();
                    JsonNode words = paragraph.path("words");
                    if (words.isArray()) {
                        for (JsonNode word : words) {
                            JsonNode symbols = word.path("symbols");
                            if (!symbols.isArray()) {
                                continue;
                            }
                            for (JsonNode symbol : symbols) {
                                lineText.append(symbol.path("text").asText(""));
                            }
                            lineText.append(" ");
                        }
                    }
                    Float confidence = paragraph.has("confidence") ? (float) paragraph.path("confidence").asDouble() : null;
                    linesOut.add(OcrResult.OcrSegment.builder()
                            .pageNumber(pageNumber)
                            .text(lineText.toString().trim())
                            .confidence(confidence)
                            .boundingBox(toBoundingBox(paragraph.path("boundingBox")))
                            .kind("line")
                            .build());
                }
            }
            pageNumber++;
        }
        return linesOut;
    }

    private OcrResult.BoundingBox toBoundingBox(JsonNode boundingBox) {
        JsonNode vertices = boundingBox.path("vertices");
        if (!vertices.isArray() || vertices.size() < 4) {
            return null;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (JsonNode vertex : vertices) {
            float x = (float) vertex.path("x").asDouble(0);
            float y = (float) vertex.path("y").asDouble(0);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return OcrResult.BoundingBox.builder()
                .left(minX)
                .top(minY)
                .width(Math.max(0, maxX - minX))
                .height(Math.max(0, maxY - minY))
                .build();
    }
}
