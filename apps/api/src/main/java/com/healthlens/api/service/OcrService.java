package com.healthlens.api.service;

import com.healthlens.api.dto.OcrResult;
import com.healthlens.api.exception.OcrProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.chat.client.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.healthlens.api.dto.MetricDto;
import java.util.ArrayList;
import java.util.List;



/**
 * OCR Service — Xử lý OCR với EasyOCR primary và AWS Textract fallback
 *
 * <p>Service này gọi EasyOCR microservice (FastAPI) qua REST API.
 * Khi EasyOCR fail (connection error, timeout, HTTP error), service
 * tự động fallback sang AWS Textract.
 *
 * <p>Architecture (Option B+):
 * <pre>
 *   Upload → Spring API → EasyOCR Service (primary)
 *                       ↘ AWS Textract     (fallback)
 * </pre>
 *
 * <p>Timeout: Configurable via {@code app.ocr.service.timeout-ms} (default: 10s)
 *
 * @see com.healthlens.api.config.OcrServiceConfig
 */
@Slf4j
@Service
public class OcrService {

    private final RestTemplate ocrRestTemplate;
    private final AwsTextractClient textractClient;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String ocrServiceUrl;

    // =========================================
    // EasyOCR Response DTO (inner class)
    // =========================================
    public record OcrExtractionResult(
            String examDate,
            String recordType,
            String hospitalName,
            String diagnosis,
            List<MetricDto> metrics
    ) {}

    /**
     * Response DTO matching the EasyOCR FastAPI /ocr response.
     */
    record EasyOcrResponse(
            String text,
            float confidence,
            String language_detected,
            int processing_time_ms,
            int block_count
    ) {}

    /**
     * Request DTO for EasyOCR FastAPI /ocr endpoint.
     */
    record EasyOcrRequest(String image_url) {}

    public OcrService(
            @Qualifier("ocrRestTemplate") RestTemplate ocrRestTemplate,
            AwsTextractClient textractClient,
            ChatClient chatClient,
            ObjectMapper objectMapper,
            @Value("${app.ocr.service.url:http://localhost:8001}") String ocrServiceUrl) {
        this.ocrRestTemplate = ocrRestTemplate;
        this.textractClient = textractClient;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.ocrServiceUrl = ocrServiceUrl;
    }

    /**
     * Process image URL qua OCR pipeline.
     *
     * <p>Flow:
     * <ol>
     *   <li>Gọi EasyOCR service (primary)</li>
     *   <li>Nếu fail → log warning và fallback sang AwsTextractClient</li>
     *   <li>Nếu cả 2 fail → throw {@link OcrProcessingException}</li>
     * </ol>
     *
     * @param imageUrl URL của image cần OCR (http/https hoặc presigned S3/MinIO URL)
     * @return {@link OcrResult} chứa extracted text, confidence, source, language
     * @throws OcrProcessingException nếu tất cả OCR providers đều fail
     */
    public OcrResult processImage(String imageUrl) {
        try {
            log.info("Processing OCR for image: {}",
                    imageUrl.length() > 80 ? imageUrl.substring(0, 80) + "..." : imageUrl);
            return callEasyOcr(imageUrl);
        } catch (OcrProcessingException e) {
            log.warn("EasyOCR failed, attempting fallback: {}", e.getMessage());
            return callTextractFallback(imageUrl);
        }
    }

    /**
     * Gọi EasyOCR microservice qua REST API.
     *
     * @param imageUrl URL của image
     * @return {@link OcrResult} từ EasyOCR
     * @throws OcrProcessingException nếu EasyOCR service fail
     */
    OcrResult callEasyOcr(String imageUrl) {
        try {
            EasyOcrResponse response = ocrRestTemplate.postForObject(
                    ocrServiceUrl + "/ocr",
                    new EasyOcrRequest(imageUrl),
                    EasyOcrResponse.class
            );

            if (response == null) {
                throw new OcrProcessingException("EasyOCR returned null response");
            }

            float confidence = response.confidence();
            if (Float.isNaN(confidence) || Float.isInfinite(confidence) || confidence < 0f || confidence > 1f) {
                log.warn("EasyOCR returned invalid confidence: {}, clamping to 0.0", confidence);
                confidence = 0.0f;
            }

            int blockCount = response.block_count();
            if (blockCount < 0) {
                log.warn("EasyOCR returned negative block_count: {}, setting to 0", blockCount);
                blockCount = 0;
            }

            log.info("EasyOCR success: {} blocks, confidence: {}, time: {}ms",
                    blockCount, confidence, response.processing_time_ms());

            return OcrResult.builder()
                    .text(response.text() != null ? response.text() : "")
                    .confidence(confidence)
                    .source("easyocr")
                    .language(response.language_detected() != null ? response.language_detected() : "unknown")
                    .processingTimeMs(response.processing_time_ms())
                    .build();

        } catch (ResourceAccessException e) {
            log.error("EasyOCR service unreachable: {}", e.getMessage());
            throw new OcrProcessingException("EasyOCR service unavailable", e);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("EasyOCR service returned error: {} {}", e.getStatusCode(), e.getStatusText());
            throw new OcrProcessingException("EasyOCR service error: " + e.getStatusCode(), e);
        } catch (IllegalArgumentException e) {
            log.error("EasyOCR malformed response: {}", e.getMessage());
            throw new OcrProcessingException("EasyOCR malformed response", e);
        }
    }

    /**
     * AWS Textract fallback — delegates to {@link AwsTextractClient}.
     *
     * <p>Khi EasyOCR fail, method này delegate sang AwsTextractClient.
     * Nếu Textract cũng fail (stub mode hoặc actual error), catch exception
     * và trả về empty fallback result.
     *
     * @param imageUrl URL của image
     * @return {@link OcrResult} từ Textract hoặc empty fallback
     */
    OcrResult callTextractFallback(String imageUrl) {
        try {
            OcrResult result = textractClient.extract(imageUrl);
            if ("textract-stub".equals(result.getSource())) {
                log.warn("Textract fallback returned stub result — OCR will return empty text");
            }
            return result;
        } catch (Exception e) {
            log.error("Textract fallback failed: {}. All OCR providers exhausted.", e.getMessage());
            return OcrResult.builder()
                    .text("")
                    .confidence(0.0f)
                    .source("all-providers-failed")
                    .language("unknown")
                    .processingTimeMs(0)
                    .build();
        }
    }

    /**
     * Parse OCR raw text thành danh sách metrics bằng LLM.
     */
    public OcrExtractionResult parseMetrics(String ocrText, float overallConfidence) {
        if (ocrText == null || ocrText.isBlank()) {
            return new OcrExtractionResult(null, null, null, null, new ArrayList<>());
        }

        String prompt = """
            You are a medical data extraction expert. Extract and NORMALIZE information from the following OCR text of a Vietnamese medical record (Phiếu kết quả xét nghiệm/khám bệnh).
            
            RULES:
            1. Language: Vietnamese (Tiếng Việt).
            2. Normalization: If a metric name is misread or abbreviated, map it to the standard medical term.
               Standard terms reference:
               - Blood Sugar: Glucose, HbA1c.
               - Liver: AST (GOT), ALT (GPT), GGT, Bilirubin (Toàn phần/Trực tiếp/Gián tiếp), Albumin, Protein toàn phần.
               - Kidney: Urea (Urê), Creatinine (Creatinin), Acid Uric (Gout).
               - Lipids: Cholesterol toàn phần, Triglyceride, HDL-C, LDL-C.
               - Blood Count: WBC (Bạch cầu), RBC (Hồng cầu), HGB (Huyết sắc tố), HCT, PLT (Tiểu cầu), Neutrophil, Lymphocyte.
            
            3. Fields to extract:
               - "examDate": Date of examination (YYYY-MM-DD). Look for "Ngày khám", "Ngày chỉ định".
               - "recordType": Type of document (e.g., "Xét nghiệm máu", "Siêu âm").
               - "hospitalName": Medical facility name (top left/header).
               - "diagnosis": Doctor's diagnosis or conclusion ("Chẩn đoán", "Kết luận").
               - "metrics": List of laboratory results. Each has "name", "value", "unit".
            
            CRITICAL: 
            - Clean noisy numeric values (e.g., "5.4H" -> "5.4").
            - Ensure "name" is the full standard Vietnamese name if possible.
            - Return null for missing fields.
            
            Return ONLY a raw JSON object. NO markdown, NO preamble.
            {
              "examDate": "YYYY-MM-DD",
              "recordType": "...",
              "hospitalName": "...",
              "diagnosis": "...",
              "metrics": [
                {"name": "...", "value": "...", "unit": "..."}
              ]
            }
            
            OCR Text:
            """ + ocrText;

        String jsonResponse = null;
        try {
            log.info("[LLM] Sending OCR text to LLM (length: {})", ocrText.length());
            jsonResponse = chatClient.prompt().user(prompt).call().content();
            log.debug("[LLM] Received response from model (length: {})",
                    jsonResponse != null ? jsonResponse.length() : 0);

            if (jsonResponse == null || jsonResponse.isBlank()) {
                return new OcrExtractionResult(null, null, null, null, new ArrayList<>());
            }

            // Clean markdown and any preamble
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.contains("```json")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```json") + 7);
                if (jsonResponse.contains("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
                }
            } else if (jsonResponse.contains("```")) {
                jsonResponse = jsonResponse.substring(jsonResponse.indexOf("```") + 3);
                if (jsonResponse.contains("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.indexOf("```"));
                }
            }
            jsonResponse = jsonResponse.trim();

            // Simple attempt to fix truncated JSON if it looks like it ended early in an array
            if (!jsonResponse.endsWith("}") && !jsonResponse.endsWith("]")) {
                log.warn("[LLM] Detected truncated JSON, attempting to close blocks");
                if (jsonResponse.contains("[") && !jsonResponse.contains("]")) {
                    jsonResponse += "]}";
                } else {
                    jsonResponse += "}";
                }
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            String examDate = rootNode.has("examDate") && !rootNode.get("examDate").isNull() ? rootNode.get("examDate").asText() : null;
            String recordType = rootNode.has("recordType") && !rootNode.get("recordType").isNull() ? rootNode.get("recordType").asText() : null;
            String hospitalName = rootNode.has("hospitalName") && !rootNode.get("hospitalName").isNull() ? rootNode.get("hospitalName").asText() : null;
            String diagnosis = rootNode.has("diagnosis") && !rootNode.get("diagnosis").isNull() ? rootNode.get("diagnosis").asText() : null;

            JsonNode metricsNode = rootNode.get("metrics");
            List<MetricDto> metrics = new ArrayList<>();
            if (metricsNode != null && metricsNode.isArray()) {
                for (JsonNode node : metricsNode) {
                    MetricDto metric = new MetricDto();
                    metric.setName(node.has("name") ? node.get("name").asText() : "");
                    // Value can be numeric or string, handle gracefully
                    String value = "";
                    if (node.has("value")) {
                        JsonNode vNode = node.get("value");
                        value = vNode.isNull() ? "" : vNode.asText();
                    }
                    metric.setValue(value);
                    metric.setUnit(node.has("unit") && !node.get("unit").isNull() ? node.get("unit").asText() : "");
                    metric.setConfidence(overallConfidence);
                    metric.setSource("ocr");
                    metric.setConfidenceLevel(classifyConfidence(overallConfidence));
                    metrics.add(metric);
                }
            }
            log.info("[LLM] Parsed: date={}, type={}, hospital={}, diagnosis={}, metricsCount={}", 
                    examDate, recordType, hospitalName, diagnosis, metrics.size());
            return new OcrExtractionResult(examDate, recordType, hospitalName, diagnosis, metrics);
        } catch (Exception e) {
            log.error("[LLM] Failed to parse metrics using LLM. responseLength={}",
                    jsonResponse != null ? jsonResponse.length() : 0, e);
            return new OcrExtractionResult(null, null, null, null, new ArrayList<>());
        }
    }

    /**
     * Phân loại confidence level dựa trên rules từ PRD.
     */
    public String classifyConfidence(float confidence) {
        if (confidence >= 0.85f) {
            return "high";
        } else if (confidence >= 0.50f) {
            return "medium";
        } else {
            return "low";
        }
    }
}
