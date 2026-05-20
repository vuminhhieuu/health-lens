package com.healthlens.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.ReferenceRangeDto;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM Service — Tích hợp AI chat provider để generate giải thích kết quả xét nghiệm
 *
 * <p>Service này sử dụng Spring AI {@link ChatClient} để giao tiếp với provider
 * OpenAI-compatible được cấu hình qua AI_CHAT_*.
 *
 * <p>Retry strategy: 3 lần với exponential backoff (1s → 2s → 4s)
 * <p>Caching: Redis cache với key = metricName:value:unit:status
 * <p>Fallback: explanation tĩnh khi tất cả attempts thất bại
 */
@Slf4j
@Service
public class LlmService {

    private static final Duration EXPLANATION_CACHE_TTL = Duration.ofDays(7);
    private static final String EXPLANATION_CACHE_PREFIX = "llm:explanation:";
    private static final Duration RECOMMENDATIONS_CACHE_TTL = Duration.ofDays(7);
    private static final String RECOMMENDATIONS_CACHE_PREFIX = "llm:recommendations:";
    public static final String MEDICAL_RECOMMENDATIONS_DISCLAIMER =
            "Thông tin do HealthLens cung cấp chỉ mang tính tham khảo, không thay thế tư vấn, chẩn đoán hoặc điều trị từ bác sĩ.";
    static final String EXPLANATION_OUTPUT_SCHEMA_JSON = """
            {
              "type": "object",
              "required": ["explanation", "referenceRange", "disclaimer"],
              "properties": {
                "explanation": {"type": "string", "minLength": 20},
                "referenceRange": {
                  "type": "object",
                  "required": ["min", "max", "unit"],
                  "properties": {
                    "min": {"type": ["string", "null"]},
                    "max": {"type": ["string", "null"]},
                    "unit": {"type": ["string", "null"]}
                  },
                  "additionalProperties": false
                },
                "disclaimer": {"type": "string", "minLength": 20}
              },
              "additionalProperties": false
            }
            """;
    static final String RECOMMENDATIONS_OUTPUT_SCHEMA_JSON = """
            {
              "type": "array",
              "minItems": 2,
              "maxItems": 3,
              "items": {"type": "string", "minLength": 20}
            }
            """;
    private static final String EXPLANATION_OUTPUT_CONTRACT_VERSION = "schema-v1";
    private static final String RECOMMENDATIONS_PROMPT_VERSION = "v8-medical-disclaimer-vi";
    private static final String CACHE_VALUE_SEPARATOR = "||";
    private static final Pattern REFERENCE_RANGE_IN_TEXT_PATTERN =
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:-|–|đến|to)\\s*(\\d+(?:[.,]\\d+)?)");

    private final ChatClient aiChatClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final PromptTemplateRenderer promptTemplateRenderer;
    private AuditEventRecorder auditEventRecorder;

    @Value("${app.ai.fallback.explanation:Kết quả cần được bác sĩ chuyên khoa giải thích thêm.}")
    private String defaultFallbackExplanation;

    @Value("${app.ai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.ai.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${app.ai.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.ai.retry.max-total-delay-ms:4500}")
    private long maxTotalDelayMs;

    @Value("${app.ai.explanation.prompt-version:v3}")
    private String promptVersion = "v3";

    @Value("${app.ai.explanation.retrieval-version:v1}")
    private String retrievalVersion = "v1";

    @Value("${app.ai.explanation.prompt-template:ai/prompts/metric-explanation.v3.txt}")
    private String metricExplanationPromptTemplate = "ai/prompts/metric-explanation.v3.txt";

    @Value("${app.ai.recommendations.prompt-template:ai/prompts/recommendations.v8-medical-disclaimer-vi.txt}")
    private String recommendationsPromptTemplate = "ai/prompts/recommendations.v8-medical-disclaimer-vi.txt";

    @Value("${app.ai.recommendations.prompt-version:v8-medical-disclaimer-vi}")
    private String recommendationsPromptVersion = RECOMMENDATIONS_PROMPT_VERSION;

    @Value("${app.ai.chat.model:unknown}")
    private String aiModelVersion = "unknown";

    private static final Map<String, String> FALLBACK_EXPLANATIONS = Map.of(
            "Glucose", "Đây là lượng đường trong máu của bạn tại thời điểm xét nghiệm. Bạn nên gặp bác sĩ để được tư vấn phù hợp với tình trạng cơ thể.",
            "HbA1c", "Đây là chỉ số cho biết mức đường huyết trung bình trong khoảng 3 tháng gần đây. Bạn nên trao đổi với bác sĩ để hiểu rõ ý nghĩa kết quả.",
            "Cholesterol", "Đây là tổng lượng mỡ trong máu. Bác sĩ sẽ giúp bạn đánh giá kết quả này cùng các chỉ số khác.",
            "HDL", "Đây là một loại mỡ máu có vai trò bảo vệ mạch máu. Bạn nên hỏi bác sĩ để biết kết quả của mình có phù hợp không.",
            "LDL", "Đây là một loại mỡ máu có thể tăng nguy cơ bệnh tim mạch khi quá cao. Bạn nên gặp bác sĩ để được hướng dẫn cụ thể."
    );

    private static final Map<String, String> METRIC_CONTEXTS = Map.ofEntries(
            Map.entry("EO%", "Đây là tỷ lệ bạch cầu ái toan (eosinophil), thường liên quan dị ứng hoặc ký sinh trùng."),
            Map.entry("EOS%", "Đây là tỷ lệ bạch cầu ái toan (eosinophil), thường liên quan dị ứng hoặc ký sinh trùng."),
            Map.entry("BASO%", "Đây là tỷ lệ bạch cầu ái kiềm (basophil), thường liên quan phản ứng viêm hoặc dị ứng."),
            Map.entry("BAS%", "Đây là tỷ lệ bạch cầu ái kiềm (basophil), thường liên quan phản ứng viêm hoặc dị ứng."),
            Map.entry("WBC", "Đây là tổng số bạch cầu, giúp phản ánh hoạt động của hệ miễn dịch."),
            Map.entry("RBC", "Đây là số lượng hồng cầu, liên quan khả năng vận chuyển oxy của máu."),
            Map.entry("HGB", "Đây là huyết sắc tố trong hồng cầu, giúp đưa oxy đi nuôi cơ thể."),
            Map.entry("HCT", "Đây là tỷ lệ thể tích hồng cầu trong máu, liên quan tình trạng cô đặc hoặc thiếu máu."),
            Map.entry("PLT", "Đây là số lượng tiểu cầu, liên quan khả năng cầm máu và đông máu."),
            Map.entry("NEUT%", "Đây là tỷ lệ bạch cầu trung tính, thường thay đổi khi cơ thể có nhiễm khuẩn."),
            Map.entry("LYM%", "Đây là tỷ lệ bạch cầu lympho, liên quan đáp ứng miễn dịch của cơ thể."),
            Map.entry("MONO%", "Đây là tỷ lệ bạch cầu mono, liên quan quá trình viêm và dọn dẹp tế bào tổn thương."),
            Map.entry("GLUCOSE", "Đây là đường huyết tại thời điểm xét nghiệm."),
            Map.entry("HBA1C", "Đây là đường huyết trung bình trong khoảng 3 tháng gần đây."),
            Map.entry("HDLC", "Đây là HDL-C (cholesterol tốt), có vai trò bảo vệ tim mạch."),
            Map.entry("LDLC", "Đây là LDL-C (cholesterol xấu), có thể tăng nguy cơ xơ vữa mạch khi cao."),
            Map.entry("TRIGLYCERIDE", "Đây là triglyceride, một dạng mỡ máu liên quan chuyển hóa năng lượng."),
            Map.entry("TRIG", "Đây là triglyceride, một dạng mỡ máu liên quan chuyển hóa năng lượng."),
            Map.entry("CHOL", "Đây là cholesterol toàn phần, phản ánh tổng lượng mỡ trong máu."),
            Map.entry("HBSAG", "Đây là HBsAg, dấu ấn sàng lọc liên quan virus viêm gan B.")
    );

    private static final Map<String, String> FALLBACK_EXPLANATIONS_BY_NORMALIZED_METRIC = Map.ofEntries(
            Map.entry("GLUCOSE", FALLBACK_EXPLANATIONS.get("Glucose")),
            Map.entry("HBA1C", FALLBACK_EXPLANATIONS.get("HbA1c")),
            Map.entry("HDL", FALLBACK_EXPLANATIONS.get("HDL")),
            Map.entry("HDLC", FALLBACK_EXPLANATIONS.get("HDL")),
            Map.entry("LDL", FALLBACK_EXPLANATIONS.get("LDL")),
            Map.entry("LDLC", FALLBACK_EXPLANATIONS.get("LDL")),
            Map.entry("CHOLESTEROL", FALLBACK_EXPLANATIONS.get("Cholesterol")),
            Map.entry("CHOL", FALLBACK_EXPLANATIONS.get("Cholesterol")),
            Map.entry("TRIGLYCERIDE", "Khi chỉ số này tăng kéo dài, nguy cơ rối loạn mỡ máu và vấn đề tim mạch có thể tăng."),
            Map.entry("TRIG", "Khi chỉ số này tăng kéo dài, nguy cơ rối loạn mỡ máu và vấn đề tim mạch có thể tăng.")
    );

    private static final Map<String, String> METRIC_RELATIONS = Map.ofEntries(
            Map.entry("EO%", "phản ứng dị ứng, hen phế quản và một số bệnh ký sinh trùng"),
            Map.entry("EOS%", "phản ứng dị ứng, hen phế quản và một số bệnh ký sinh trùng"),
            Map.entry("BASO%", "phản ứng viêm, dị ứng và hoạt động miễn dịch"),
            Map.entry("BAS%", "phản ứng viêm, dị ứng và hoạt động miễn dịch"),
            Map.entry("WBC", "nguy cơ nhiễm trùng và tình trạng viêm"),
            Map.entry("RBC", "tình trạng thiếu máu hoặc cô đặc máu"),
            Map.entry("HGB", "khả năng vận chuyển oxy và dấu hiệu thiếu máu"),
            Map.entry("HCT", "mức độ cô đặc máu hoặc thiếu máu"),
            Map.entry("PLT", "nguy cơ chảy máu hoặc hình thành cục máu đông"),
            Map.entry("GLUCOSE", "nguy cơ rối loạn đường huyết và đái tháo đường"),
            Map.entry("HBA1C", "kiểm soát đường huyết dài hạn"),
            Map.entry("HDLC", "mức bảo vệ tim mạch"),
            Map.entry("LDLC", "nguy cơ xơ vữa mạch và bệnh tim mạch"),
            Map.entry("TRIGLYCERIDE", "rối loạn mỡ máu và nguy cơ tim mạch"),
            Map.entry("TRIG", "rối loạn mỡ máu và nguy cơ tim mạch"),
            Map.entry("CHOL", "nguy cơ rối loạn mỡ máu và bệnh tim mạch"),
            Map.entry("HBSAG", "tình trạng nhiễm virus viêm gan B")
    );

    public LlmService(ChatClient aiChatClient, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this(aiChatClient, redisTemplate, objectMapper, Metrics.globalRegistry);
    }

    @Autowired
    public LlmService(
            ChatClient aiChatClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.aiChatClient = aiChatClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.promptTemplateRenderer = new PromptTemplateRenderer();
    }

    @Autowired(required = false)
    void setAuditEventRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    public String generateExplanation(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang
    ) {
        return generateExplanationResult(metricName, value, status, referenceRange, lang, null).explanation();
    }

    public ExplanationResult generateExplanationResult(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang
    ) {
        return generateExplanationResult(metricName, value, status, referenceRange, lang, null);
    }

    public ExplanationResult generateExplanationResult(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang,
            String knowledgeSnippet
    ) {
        return generateExplanationResult(metricName, value, status, referenceRange, lang, knowledgeSnippet, null);
    }

    public ExplanationResult generateExplanationResult(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang,
            String knowledgeSnippet,
            UUID actorId
    ) {
        String normalizedLang = (lang == null || lang.isBlank()) ? "vi" : lang.trim().toLowerCase();
        String normalizedStatus = (status == null || status.isBlank()) ? "unknown" : status;
        String cacheKey = buildCacheKey(metricName, value, normalizedStatus, referenceRange, normalizedLang, knowledgeSnippet);
        String cachedExplanation = safeGetCachedExplanation(cacheKey);
        if (cachedExplanation != null && !cachedExplanation.isBlank()) {
            return parseCachedExplanation(cachedExplanation);
        }

        String prompt;
        try {
            prompt = buildMedicalPrompt(metricName, value, normalizedStatus, referenceRange, normalizedLang, knowledgeSnippet);
        } catch (RuntimeException ex) {
            recordPromptRenderingFailure("metric_explanation");
            log.warn("Prompt rendering failed for metric '{}'. Using fallback: {}", metricName, ex.getMessage());
            return new ExplanationResult(
                    getFallbackExplanation(metricName, normalizedStatus),
                    "fallback",
                    effectivePromptVersion(),
                    effectiveModelVersion()
            );
        }

        ExplanationResult result = callWithRetry(prompt, metricName, normalizedStatus, referenceRange, actorId);
        safeCacheExplanation(cacheKey, result);
        return result;
    }

    public List<String> generateRecommendations(List<RecommendationMetricInput> metrics, Integer profileAge, String gender) {
        return generateRecommendationsResult(metrics, profileAge, gender, "").recommendations();
    }

    public RecommendationResult generateRecommendationsResult(
            List<RecommendationMetricInput> metrics,
            Integer profileAge,
            String gender
    ) {
        return generateRecommendationsResult(metrics, profileAge, gender, "");
    }

    /**
     * @param examContext ngữ cảnh phiếu (loại xét nghiệm, kết luận ngắn) — giúp LLM tránh gợi ý chung chung.
     */
    public List<String> generateRecommendations(
            List<RecommendationMetricInput> metrics,
            Integer profileAge,
            String gender,
            String examContext
    ) {
        return generateRecommendationsResult(metrics, profileAge, gender, examContext).recommendations();
    }

    public RecommendationResult generateRecommendationsResult(
            List<RecommendationMetricInput> metrics,
            Integer profileAge,
            String gender,
            String examContext
    ) {
        return generateRecommendationsResult(metrics, profileAge, gender, examContext, null);
    }

    public RecommendationResult generateRecommendationsResult(
            List<RecommendationMetricInput> metrics,
            Integer profileAge,
            String gender,
            String examContext,
            UUID actorId
    ) {
        if (metrics == null || metrics.isEmpty()) {
            return new RecommendationResult(List.of(), "fallback", effectiveRecommendationsPromptVersion(), effectiveModelVersion());
        }

        List<RecommendationMetricInput> riskyMetrics = metrics.stream()
                .filter(Objects::nonNull)
                .filter(m -> {
                    String normalizedStatus = normalizeStatus(m.status());
                    return "attention".equals(normalizedStatus)
                            || "warning".equals(normalizedStatus)
                            || "abnormal".equals(normalizedStatus);
                })
                .toList();
        if (riskyMetrics.isEmpty()) {
            return new RecommendationResult(List.of(), "fallback", effectiveRecommendationsPromptVersion(), effectiveModelVersion());
        }

        String cacheKey = buildRecommendationsCacheKey(riskyMetrics, profileAge, gender, examContext);
        String cached = safeGetCachedExplanation(cacheKey);
        if (cached != null && !cached.isBlank()) {
            List<String> cachedRecommendations = parseRecommendations(cached);
            if (!cachedRecommendations.isEmpty()) {
                return new RecommendationResult(
                        ensureRequiredRecommendationModes(cachedRecommendations, riskyMetrics),
                        "cache",
                        effectiveRecommendationsPromptVersion(),
                        effectiveModelVersion()
                );
            }
            // Cache value may be corrupted or stale format; continue with regeneration path.
            log.warn("Recommendations cache malformed for key '{}', regenerating.", cacheKey);
        }

        String prompt;
        try {
            prompt = buildRecommendationsPrompt(riskyMetrics, profileAge, gender, examContext);
        } catch (RuntimeException ex) {
            recordPromptRenderingFailure("recommendations");
            log.warn("Recommendations prompt rendering failed, using fallback: {}", ex.getMessage());
            List<String> fallbackRecommendations = buildFallbackRecommendations(riskyMetrics);
            fallbackRecommendations = ensureRequiredRecommendationModes(fallbackRecommendations, riskyMetrics);
            return new RecommendationResult(
                    fallbackRecommendations,
                    "fallback",
                    effectiveRecommendationsPromptVersion(),
                    effectiveModelVersion()
            );
        }
        long startedNanos = System.nanoTime();
        String llmOutput;
        try {
            llmOutput = aiChatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.warn("Recommendations generation failed, using fallback: {}", ex.getMessage());
            List<String> fallbackRecommendations = buildFallbackRecommendations(riskyMetrics);
            fallbackRecommendations = ensureRequiredRecommendationModes(fallbackRecommendations, riskyMetrics);
            safeCacheRecommendations(cacheKey, fallbackRecommendations);
            recordAiGenerationMetrics(
                    "recommendations",
                    "fallback",
                    "error",
                    0,
                    estimateTokenUsage(prompt, ""),
                    startedNanos
            );
            recordLlmAudit(actorId, AuditActions.LLM_CALL_FAILED, "recommendations", "recommendations", "fallback", 0);
            return new RecommendationResult(
                    fallbackRecommendations,
                    "fallback",
                    effectiveRecommendationsPromptVersion(),
                    effectiveModelVersion()
            );
        }

        List<String> recommendations = parseRecommendations(llmOutput);
        boolean usedFallback = false;
        if (recommendations.isEmpty() || isLowQualityRecommendations(recommendations, riskyMetrics)) {
            recordSchemaValidationFailure("recommendations");
            recommendations = buildFallbackRecommendations(riskyMetrics);
            usedFallback = true;
        }
        recommendations = ensureRequiredRecommendationModes(recommendations, riskyMetrics);
        safeCacheRecommendations(cacheKey, recommendations);
        recordAiGenerationMetrics(
                "recommendations",
                usedFallback ? "fallback" : "llm",
                usedFallback ? "invalid" : "valid",
                0,
                estimateTokenUsage(prompt, llmOutput),
                startedNanos
        );
        recordLlmAudit(
                actorId,
                usedFallback ? AuditActions.LLM_CALL_FAILED : AuditActions.LLM_CALL_SUCCEEDED,
                "recommendations",
                "recommendations",
                usedFallback ? "fallback" : "valid",
                0
        );
        return new RecommendationResult(
                recommendations,
                usedFallback ? "fallback" : "llm",
                effectiveRecommendationsPromptVersion(),
                effectiveModelVersion()
        );
    }

    private ExplanationResult callWithRetry(
            String prompt,
            String metricName,
            String status,
            ReferenceRangeDto referenceRange,
            UUID actorId
    ) {
        long startTime = System.currentTimeMillis();
        long startedNanos = System.nanoTime();
        long currentDelayMs = initialDelayMs;
        int retryCount = 0;
        int lastTokenEstimate = estimateTokenUsage(prompt, "");

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                String rawResult = aiChatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
                lastTokenEstimate = estimateTokenUsage(prompt, rawResult);
                String result = parseValidatedExplanation(rawResult, referenceRange, status);

                if (attempt > 1) {
                    log.info("AI chat provider succeeded on attempt {}/{} for metric '{}'",
                            attempt, maxRetryAttempts, metricName);
                }
                recordAiGenerationMetrics("metric_explanation", "llm", "valid", retryCount, lastTokenEstimate, startedNanos);
                recordLlmAudit(actorId, AuditActions.LLM_CALL_SUCCEEDED, "metric_explanation", metricName, "valid", retryCount);
                return new ExplanationResult(result, "llm", effectivePromptVersion(), effectiveModelVersion());

            } catch (SchemaValidationException e) {
                recordSchemaValidationFailure("metric_explanation");
                log.warn("AI chat provider returned invalid schema on attempt {}/{} for metric '{}': {}",
                        attempt, maxRetryAttempts, metricName, e.getMessage());
                if (attempt < maxRetryAttempts) {
                    retryCount++;
                    if (!sleepBeforeRetry(startTime, currentDelayMs, metricName)) {
                        break;
                    }
                    currentDelayMs = (long) (currentDelayMs * retryMultiplier);
                }
            } catch (Exception e) {
                log.warn("AI chat provider attempt {}/{} failed for metric '{}': {}",
                        attempt, maxRetryAttempts, metricName, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    retryCount++;
                    if (!sleepBeforeRetry(startTime, currentDelayMs, metricName)) {
                        break;
                    }
                    currentDelayMs = (long) (currentDelayMs * retryMultiplier);
                }
            }
        }

        log.warn("All {} attempts failed for metric '{}'. Using fallback.",
                maxRetryAttempts, metricName);
        recordAiGenerationMetrics("metric_explanation", "fallback", "invalid", retryCount, lastTokenEstimate, startedNanos);
        recordLlmAudit(actorId, AuditActions.LLM_CALL_FAILED, "metric_explanation", metricName, "fallback", retryCount);
        return new ExplanationResult(
                getFallbackExplanation(metricName, status),
                "fallback",
                effectivePromptVersion(),
                effectiveModelVersion()
        );
    }

    private void recordLlmAudit(UUID actorId, String action, String purpose, String metricName, String outcome, int retryCount) {
        if (auditEventRecorder == null) {
            return;
        }
        Map<String, ?> details = Map.of(
                "purpose", purpose,
                "metricName", safeMetric(metricName),
                "outcome", outcome,
                "retryCount", retryCount,
                "promptVersion", effectivePromptVersion(),
                "modelVersion", effectiveModelVersion()
        );
        if (actorId != null) {
            auditEventRecorder.recordEvent(actorId, action, AuditResourceTypes.LLM_CALL, null, details);
            return;
        }
        auditEventRecorder.recordAnonymous(action, AuditResourceTypes.LLM_CALL, null, details);
    }

    String buildMedicalPrompt(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang,
            String knowledgeSnippet
    ) {
        String safeStatus = (status != null) ? status : "unknown";
        String rangeText = formatReferenceRange(referenceRange);
        String promptLanguage = "vi".equalsIgnoreCase(lang) ? "Vietnamese (tiếng Việt đơn giản)" : normalizeLang(lang);
        String metricContext = resolveMetricContext(metricName);
        String metricRelation = resolveMetricRelation(metricName);
        String effectiveKnowledgeSnippet = (knowledgeSnippet == null || knowledgeSnippet.isBlank())
                ? "Metric identity: " + metricContext + "\nClinical relation: " + metricRelation + "."
                : knowledgeSnippet;
        String statusExplanation = switch (safeStatus.toLowerCase()) {
            case "normal" -> "nằm trong ngưỡng tham chiếu";
            case "attention" -> "gần hoặc hơi lệch ngưỡng tham chiếu";
            case "abnormal" -> "đang lệch khỏi ngưỡng tham chiếu";
            default -> "cần được đối chiếu thêm với ngưỡng tham chiếu";
        };

        String basePrompt = promptTemplateRenderer.render(metricExplanationPromptTemplate, Map.of(
                "promptLanguage", promptLanguage,
                "metricName", safeMetric(metricName),
                "value", safeValue(value),
                "safeStatus", safeStatus.toLowerCase(),
                "rangeText", rangeText,
                "metricContext", metricContext,
                "metricRelation", metricRelation,
                "knowledgeSnippet", effectiveKnowledgeSnippet,
                "statusExplanation", statusExplanation
        ));
        return basePrompt + "\n\n"
                + "Output contract: return only a JSON object matching this schema, no markdown:\n"
                + EXPLANATION_OUTPUT_SCHEMA_JSON
                + "\nUse exactly this referenceRange object: "
                + expectedReferenceRangeJson(referenceRange)
                + "\nThe explanation field must contain the 3 required Vietnamese lines. "
                + "The disclaimer field must include: \"" + MEDICAL_RECOMMENDATIONS_DISCLAIMER + "\"";
    }

    String getFallbackExplanation(String metricName) {
        return getFallbackExplanation(metricName, "unknown");
    }

    String getFallbackExplanation(String metricName, String status) {
        String metricContext = resolveMetricContext(metricName);
        String metricFallback = resolveFallbackByMetric(metricName);
        String followUp = abnormalFollowUpSuffix(status);
        if (metricFallback != null) {
            return "Chỉ số này là gì: " + metricContext + "\n"
                    + "Chỉ số này liên quan đến: " + resolveMetricRelation(metricName) + ".\n"
                    + "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: " + metricFallback + followUp + " " + MEDICAL_RECOMMENDATIONS_DISCLAIMER;
        }
        return "Chỉ số này là gì: " + metricContext + "\n"
                + "Chỉ số này liên quan đến: cân bằng miễn dịch, chuyển hóa và chức năng cơ quan tùy từng xét nghiệm.\n"
                + "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: " + defaultFallbackExplanation + followUp + " " + MEDICAL_RECOMMENDATIONS_DISCLAIMER;
    }

    private boolean sleepBeforeRetry(long startTime, long currentDelayMs, String metricName) {
        long elapsedMs = System.currentTimeMillis() - startTime;
        if (elapsedMs + currentDelayMs > maxTotalDelayMs) {
            log.warn("Stop retry for metric '{}' due to delay budget exceeded (elapsed={}ms, nextDelay={}ms, budget={}ms)",
                    metricName, elapsedMs, currentDelayMs, maxTotalDelayMs);
            return false;
        }
        try {
            Thread.sleep(currentDelayMs);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Retry interrupted for metric '{}'", metricName);
            return false;
        }
    }

    private String parseValidatedExplanation(String rawOutput, ReferenceRangeDto referenceRange, String status) {
        if (rawOutput == null || rawOutput.isBlank()) {
            throw new SchemaValidationException("empty output");
        }
        String cleaned = stripJsonFence(rawOutput);
        JsonNode root;
        try {
            root = objectMapper.readTree(cleaned);
        } catch (Exception ex) {
            throw new SchemaValidationException("invalid json");
        }
        if (!root.isObject()) {
            throw new SchemaValidationException("root must be object");
        }
        if (root.size() != 3 || !root.has("explanation") || !root.has("referenceRange") || !root.has("disclaimer")) {
            throw new SchemaValidationException("unexpected explanation schema");
        }
        String explanation = requiredText(root.get("explanation"), "explanation");
        String disclaimer = requiredText(root.get("disclaimer"), "disclaimer");
        if (explanation.length() < 20) {
            throw new SchemaValidationException("explanation too short");
        }
        if (!MEDICAL_RECOMMENDATIONS_DISCLAIMER.equals(disclaimer)) {
            throw new SchemaValidationException("missing medical disclaimer");
        }
        validateReferenceRangeNode(root.get("referenceRange"), referenceRange);
        validateExplanationFormat(explanation, status);
        validateExplanationDoesNotInventReferenceRange(explanation, referenceRange);
        return appendDisclaimer(explanation);
    }

    private String requiredText(JsonNode node, String field) {
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new SchemaValidationException(field + " must be a non-empty string");
        }
        return node.asText().trim();
    }

    private void validateReferenceRangeNode(JsonNode node, ReferenceRangeDto referenceRange) {
        if (node == null || !node.isObject()) {
            throw new SchemaValidationException("referenceRange must be object");
        }
        if (node.size() != 3 || !node.has("min") || !node.has("max") || !node.has("unit")) {
            throw new SchemaValidationException("referenceRange schema mismatch");
        }
        assertRangeField(node.get("min"), referenceRange == null || referenceRange.min() == null
                ? null
                : referenceRange.min().toPlainString(), "referenceRange.min");
        assertRangeField(node.get("max"), referenceRange == null || referenceRange.max() == null
                ? null
                : referenceRange.max().toPlainString(), "referenceRange.max");
        assertRangeField(node.get("unit"), referenceRange == null ? null : referenceRange.unit(), "referenceRange.unit");
    }

    private void validateExplanationFormat(String explanation, String status) {
        String[] lines = explanation.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);
        if (lines.length != 3
                || !lines[0].startsWith("Chỉ số này là gì:")
                || !lines[1].startsWith("Chỉ số này liên quan đến:")
                || !lines[2].startsWith("Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng:")) {
            throw new SchemaValidationException("explanation must follow required 3-line format");
        }
        if ("abnormal".equals(normalizeStatus(status))) {
            String lower = lines[2].toLowerCase(Locale.ROOT);
            if (!lower.contains("không phải chẩn đoán")
                    || (!lower.contains("bác sĩ") && !lower.contains("tái khám"))) {
                throw new SchemaValidationException("abnormal explanation missing safety follow-up language");
            }
        }
    }

    private void validateExplanationDoesNotInventReferenceRange(String explanation, ReferenceRangeDto referenceRange) {
        if (referenceRange == null || referenceRange.min() == null || referenceRange.max() == null) {
            return;
        }
        String expectedMin = normalizeDecimalText(referenceRange.min().toPlainString());
        String expectedMax = normalizeDecimalText(referenceRange.max().toPlainString());
        Matcher matcher = REFERENCE_RANGE_IN_TEXT_PATTERN.matcher(explanation);
        while (matcher.find()) {
            String actualMin = normalizeDecimalText(matcher.group(1));
            String actualMax = normalizeDecimalText(matcher.group(2));
            if (!Objects.equals(expectedMin, actualMin) || !Objects.equals(expectedMax, actualMax)) {
                throw new SchemaValidationException("explanation text contains invented reference range");
            }
        }
    }

    private String appendDisclaimer(String explanation) {
        if (explanation.contains(MEDICAL_RECOMMENDATIONS_DISCLAIMER)) {
            return explanation;
        }
        return explanation + "\n" + MEDICAL_RECOMMENDATIONS_DISCLAIMER;
    }

    private void assertRangeField(JsonNode actualNode, String expected, String fieldName) {
        String actual = actualNode == null || actualNode.isNull() ? null : actualNode.asText();
        String normalizedExpected = expected == null || expected.isBlank() ? null : expected.trim();
        String normalizedActual = actual == null || actual.isBlank() ? null : actual.trim();
        if (!Objects.equals(normalizedExpected, normalizedActual)) {
            throw new SchemaValidationException(fieldName + " differs from structured reference data");
        }
    }

    private String expectedReferenceRangeJson(ReferenceRangeDto referenceRange) {
        ObjectNode range = objectMapper.createObjectNode();
        putNullableRangeField(range, "min", referenceRange == null || referenceRange.min() == null
                ? null
                : referenceRange.min().toPlainString());
        putNullableRangeField(range, "max", referenceRange == null || referenceRange.max() == null
                ? null
                : referenceRange.max().toPlainString());
        putNullableRangeField(range, "unit", referenceRange == null ? null : referenceRange.unit());
        try {
            return objectMapper.writeValueAsString(range);
        } catch (Exception ex) {
            return "{\"min\":null,\"max\":null,\"unit\":null}";
        }
    }

    private void putNullableRangeField(ObjectNode node, String fieldName, String value) {
        if (value == null || value.isBlank()) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value.trim());
        }
    }

    private String abnormalFollowUpSuffix(String status) {
        if (!"abnormal".equals(normalizeStatus(status))) {
            return "";
        }
        return " Đây không phải chẩn đoán; bạn nên trao đổi với bác sĩ hoặc tái khám để được đánh giá phù hợp.";
    }

    private String toCachedValue(ExplanationResult result) {
        return result.source()
                + CACHE_VALUE_SEPARATOR + result.promptVersion()
                + CACHE_VALUE_SEPARATOR + result.modelVersion()
                + CACHE_VALUE_SEPARATOR + result.explanation();
    }

    private ExplanationResult parseCachedExplanation(String rawCacheValue) {
        String[] parts = rawCacheValue.split("\\Q" + CACHE_VALUE_SEPARATOR + "\\E", 4);
        if (parts.length == 1) {
            // Backward-compatible with old cache format (plain explanation text only).
            return new ExplanationResult(rawCacheValue, "fallback");
        }
        if (parts.length == 2) {
            return new ExplanationResult(parts[1], parts[0], effectivePromptVersion(), "unknown");
        }
        if (parts.length < 4) {
            return new ExplanationResult(rawCacheValue, "fallback", effectivePromptVersion(), "unknown");
        }
        return new ExplanationResult(parts[3], parts[0], parts[1], parts[2]);
    }

    private String buildCacheKey(
            String metricName,
            String value,
            String status,
            ReferenceRangeDto referenceRange,
            String lang,
            String knowledgeSnippet
    ) {
        String payload = String.join("|",
                safeMetric(metricName),
                normalizeValue(value),
                normalizeStatus(status),
                normalizeLang(lang),
                promptVersion == null ? "v3" : promptVersion.trim(),
                metricExplanationPromptTemplate == null ? "" : metricExplanationPromptTemplate.trim(),
                EXPLANATION_OUTPUT_CONTRACT_VERSION,
                retrievalVersion == null ? "v1" : retrievalVersion.trim(),
                referenceRange == null || referenceRange.min() == null ? "" : referenceRange.min().toPlainString(),
                referenceRange == null || referenceRange.max() == null ? "" : referenceRange.max().toPlainString(),
                referenceRange == null || referenceRange.unit() == null ? "" : referenceRange.unit().trim(),
                sha256Hex(normalizeKnowledgeSnippet(knowledgeSnippet)));
        return EXPLANATION_CACHE_PREFIX + sha256Hex(payload);
    }

    private String normalizeStatus(String status) {
        return status == null ? "unknown" : status.trim().toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.replace(",", ".").trim();
    }

    private String normalizeLang(String lang) {
        return lang == null ? "vi" : lang.trim().toLowerCase();
    }

    private String normalizeKnowledgeSnippet(String knowledgeSnippet) {
        return knowledgeSnippet == null ? "" : knowledgeSnippet.trim();
    }

    private String safeMetric(String metricName) {
        return metricName == null ? "unknown_metric" : metricName.trim();
    }

    private String safeGetCachedExplanation(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ex) {
            log.warn("Redis cache get failed for key '{}': {}. Continue as cache miss.", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void safeCacheExplanation(String cacheKey, ExplanationResult result) {
        try {
            redisTemplate.opsForValue().set(cacheKey, toCachedValue(result), EXPLANATION_CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Redis cache set failed for key '{}': {}. Continue without caching.", cacheKey, ex.getMessage());
        }
    }

    private String resolveMetricContext(String metricName) {
        String safeMetric = safeMetric(metricName);
        String normalized = normalizeMetricKey(safeMetric);
        String context = METRIC_CONTEXTS.get(normalized);
        if (context != null) {
            return context;
        }
        return "Đây là một chỉ số xét nghiệm máu dùng để phản ánh một phần tình trạng sức khỏe hiện tại.";
    }

    private String resolveFallbackByMetric(String metricName) {
        String direct = FALLBACK_EXPLANATIONS.get(metricName);
        if (direct != null) {
            return direct;
        }
        return FALLBACK_EXPLANATIONS_BY_NORMALIZED_METRIC.get(normalizeMetricKey(metricName));
    }

    private String resolveMetricRelation(String metricName) {
        String normalized = normalizeMetricKey(metricName);
        String relation = METRIC_RELATIONS.get(normalized);
        if (relation != null) {
            return relation;
        }
        return "mức cân bằng chung của cơ thể và cần được đọc cùng các chỉ số liên quan";
    }

    private String normalizeMetricKey(String metric) {
        if (metric == null) {
            return "";
        }
        return metric.replaceAll("[^A-Za-z0-9%]", "").toUpperCase();
    }

    private String normalizeDecimalText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replace(',', '.');
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    private String formatReferenceRange(ReferenceRangeDto referenceRange) {
        if (referenceRange == null) {
            return "N/A";
        }
        String min = referenceRange.min() == null ? "N/A" : referenceRange.min().toPlainString();
        String max = referenceRange.max() == null ? "N/A" : referenceRange.max().toPlainString();
        String unit = referenceRange.unit() == null ? "" : " " + referenceRange.unit();
        return min + " - " + max + unit;
    }

    private String buildRecommendationsCacheKey(
            List<RecommendationMetricInput> riskyMetrics,
            Integer profileAge,
            String gender,
            String examContext
    ) 
    {
        List<String> normalized = riskyMetrics.stream()
                .map(m -> String.join(
                        CACHE_VALUE_SEPARATOR,
                        normalizeMetricKey(m.name()),
                        normalizeStatus(m.status()),
                        normalizeValue(m.value()),
                        m.unit() == null ? "" : m.unit().trim().toLowerCase(Locale.ROOT),
                        displayLabelFingerprint(m.displayLabelVi())))
                .sorted()
                .toList();
        String examFingerprint = (examContext == null || examContext.isBlank())
                ? ""
                : sha256Hex(examContext.trim()).substring(0, 24);
        String payload = String.join("|",
                String.join(",", normalized),
                ageGroupFromAge(profileAge),
                normalizeGender(gender),
                examFingerprint,
                effectiveRecommendationsPromptVersion(),
                recommendationsPromptTemplate == null ? "" : recommendationsPromptTemplate.trim(),
                effectiveModelVersion());
        return RECOMMENDATIONS_CACHE_PREFIX + sha256Hex(payload);
    }

    private static String displayLabelFingerprint(String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        return label.trim().toLowerCase(Locale.ROOT);
    }

    private String buildRecommendationsPrompt(
            List<RecommendationMetricInput> riskyMetrics,
            Integer profileAge,
            String gender,
            String examContext
    ) {
        String ageGroupVi = promptAgeGroupVi(profileAge);
        String genderVi = promptGenderVi(gender);
        String metricsText = riskyMetrics.stream()
                .map(m -> {
                    String labelVi = recommendationDisplayLabel(m);
                    String relation = resolveMetricRelation(m.name());
                    return "- Tên hiển thị (tiếng Việt): %s | Mã chỉ số: %s | Giá trị: %s %s | Phân loại: %s | Gợi ý phạm vi sinh học: %s"
                            .formatted(
                                    labelVi,
                                    safeMetric(m.name()),
                                    safeValue(m.value()),
                                    m.unit() == null ? "" : m.unit().trim(),
                                    statusLabelVi(normalizeStatus(m.status())),
                                    relation);
                })
                .reduce("", (left, right) -> left + right + "\n");

        String contextBlock = (examContext == null || examContext.isBlank())
                ? "(Không có thêm ngữ cảnh phiếu.)"
                : examContext.trim();

        String basePrompt = promptTemplateRenderer.render(recommendationsPromptTemplate, Map.of(
                "contextBlock", contextBlock,
                "metricsText", metricsText,
                "ageGroupVi", ageGroupVi,
                "genderVi", genderVi,
                "disclaimer", MEDICAL_RECOMMENDATIONS_DISCLAIMER
        ));
        return basePrompt + "\n\n"
                + "Output contract: return only a JSON array matching this schema, no markdown:\n"
                + RECOMMENDATIONS_OUTPUT_SCHEMA_JSON;
    }

    private String recommendationDisplayLabel(RecommendationMetricInput m) {
        if (m.displayLabelVi() != null && !m.displayLabelVi().isBlank()) {
            return m.displayLabelVi().trim();
        }
        return safeMetric(m.name());
    }

    private static String statusLabelVi(String normalizedStatus) {
        return switch (normalizedStatus) {
            case "attention" -> "cần chú ý";
            case "abnormal" -> "bất thường";
            case "warning" -> "cảnh báo";
            default -> normalizedStatus == null || normalizedStatus.isBlank() ? "không rõ" : normalizedStatus;
        };
    }

    private List<String> parseRecommendations(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return List.of();
        }
        String cleaned = stripJsonFence(rawOutput);
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isArray() || root.size() < 2 || root.size() > 3) {
                return List.of();
            }
            List<String> parsed = new ArrayList<>();
            for (JsonNode item : root) {
                if (!item.isTextual()) {
                    return List.of();
                }
                String recommendation = item.asText().trim();
                if (recommendation.length() < 20) {
                    return List.of();
                }
                parsed.add(recommendation);
            }
            return parsed;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String stripJsonFence(String rawOutput) {
        String cleaned = rawOutput.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        return cleaned;
    }

    private List<String> buildFallbackRecommendations(List<RecommendationMetricInput> riskyMetrics) {
        RecommendationMetricInput metric = riskyMetrics.stream()
                .filter(Objects::nonNull)
                .max((left, right) -> Integer.compare(
                        severityRank(normalizeStatus(left.status())),
                        severityRank(normalizeStatus(right.status()))
                ))
                .orElse(riskyMetrics.get(0));
        String labelVi = recommendationDisplayLabel(metric);
        String relation = resolveMetricRelation(metric.name());
        String normalizedStatus = normalizeStatus(metric.status());
        String valuePart = safeValue(metric.value());
        if (metric.unit() != null && !metric.unit().isBlank()) {
            valuePart = valuePart + " " + metric.unit().trim();
        }
        List<String> fallback = new ArrayList<>();
        if ("abnormal".equals(normalizedStatus)) {
            fallback.add(
                    "Với \"%s\" (%s), kết quả đang bất thường so với ngưỡng tham chiếu nhưng đây không phải chẩn đoán. Ưu tiên thói quen phù hợp với %s, không tự chẩn đoán và trao đổi với bác sĩ để được hướng dẫn cụ thể."
                            .formatted(labelVi, valuePart, relation));
        } else {
            fallback.add(
                    "Với \"%s\" (%s), chỉ số đang cần chú ý theo ngưỡng. Tiếp tục theo dõi và điều chỉnh lối sống phù hợp với %s; nhờ bác sĩ đánh giá khi tái khám."
                            .formatted(labelVi, valuePart, relation));
        }
        fallback.add(metricSpecificLifestyleAdvice(metric));
        fallback.add(
                "Theo dõi lại chỉ số theo lịch tái khám; giữ tinh thần thoải mái và tránh tự ý thay đổi thuốc hoặc liệu pháp đang được chỉ định.");
        return fallback;
    }

    private List<String> ensureRequiredRecommendationModes(
            List<String> recommendations,
            List<RecommendationMetricInput> riskyMetrics
    ) {
        List<String> normalized = recommendations == null
                ? new ArrayList<>()
                : recommendations.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        boolean hasNutrition = normalized.stream().anyMatch(this::isNutritionRecommendation);
        boolean hasLifestyle = normalized.stream().anyMatch(this::isLifestyleRecommendation);
        RecommendationMetricInput anchor = pickAnchorMetric(riskyMetrics);
        String labelVi = recommendationDisplayLabel(anchor);
        String relation = resolveMetricRelation(anchor.name());

        List<String> base = normalized.stream().distinct().toList();
        List<String> prioritized = new ArrayList<>();
        if (!base.isEmpty()) {
            prioritized.add(base.get(0));
        }
        if (!hasNutrition) {
            prioritized.add(
                    "Chế độ dinh dưỡng: Với \"%s\", ưu tiên khẩu phần cân bằng, tăng rau xanh - đạm nạc - ngũ cốc nguyên hạt và hạn chế đồ ngọt/chiên rán để hỗ trợ %s."
                            .formatted(labelVi, relation)
            );
        }
        if (!hasLifestyle) {
            prioritized.add(
                    "Chế độ sinh hoạt: Với \"%s\", duy trì vận động đều, ngủ đủ giấc, giảm căng thẳng và theo dõi triệu chứng hằng ngày để hỗ trợ ổn định %s."
                            .formatted(labelVi, relation)
            );
        }

        for (int i = base.isEmpty() ? 0 : 1; i < base.size() && prioritized.size() < 3; i++) {
            prioritized.add(base.get(i));
        }

        List<String> deduplicated = prioritized.stream().distinct().toList();
        if (deduplicated.size() <= 3) {
            return deduplicated;
        }
        return deduplicated.subList(0, 3);
    }

    private boolean isLowQualityRecommendations(
            List<String> recommendations,
            List<RecommendationMetricInput> riskyMetrics
    ) {
        if (recommendations == null || recommendations.isEmpty()) {
            return true;
        }
        long mentionedCount = recommendations.stream()
                .filter(line -> mentionsRiskMetric(line, riskyMetrics))
                .count();
        if (riskyMetrics.size() >= 2 && mentionedCount < 2) {
            return true;
        }
        if (riskyMetrics.size() == 1 && mentionedCount < 1) {
            return true;
        }
        return recommendations.stream().allMatch(line -> looksGenericLine(line, riskyMetrics));
    }

    private boolean mentionsRiskMetric(String line, List<RecommendationMetricInput> riskyMetrics) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return riskyMetrics.stream()
                .filter(Objects::nonNull)
                .map(this::metricMentions)
                .flatMap(Set::stream)
                .anyMatch(token -> containsMetricToken(lower, token));
    }

    private Set<String> metricMentions(RecommendationMetricInput metric) {
        String code = safeMetric(metric.name()).toLowerCase(Locale.ROOT);
        String display = recommendationDisplayLabel(metric).toLowerCase(Locale.ROOT);
        Set<String> mentions = new LinkedHashSet<>();
        if (!code.isBlank() && !"unknown_metric".equals(code)) {
            mentions.add(code);
        }
        if (!display.isBlank() && !"unknown_metric".equals(display)) {
            mentions.add(display);
        }
        return mentions;
    }

    private boolean containsMetricToken(String lineLower, String tokenLower) {
        if (tokenLower == null || tokenLower.isBlank()) {
            return false;
        }
        // Prevent false positives for short metric codes like "Na"/"K".
        if (tokenLower.length() <= 2 && !tokenLower.contains("%")) {
            return containsStandalone(lineLower, tokenLower);
        }
        return lineLower.contains(tokenLower);
    }

    private boolean containsStandalone(String text, String token) {
        int fromIndex = 0;
        while (true) {
            int idx = text.indexOf(token, fromIndex);
            if (idx < 0) {
                return false;
            }
            int before = idx - 1;
            int after = idx + token.length();
            boolean leftOk = before < 0 || !Character.isLetterOrDigit(text.charAt(before));
            boolean rightOk = after >= text.length() || !Character.isLetterOrDigit(text.charAt(after));
            if (leftOk && rightOk) {
                return true;
            }
            fromIndex = idx + 1;
        }
    }

    private boolean looksGenericLine(String line, List<RecommendationMetricInput> riskyMetrics) {
        if (line == null || line.isBlank()) {
            return true;
        }
        if (mentionsRiskMetric(line, riskyMetrics)) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.contains("lối sống lành mạnh")
                || lower.contains("ăn uống lành mạnh")
                || lower.contains("tập thể dục đều đặn")
                || lower.contains("ngủ đủ giấc")
                || lower.contains("duy trì vận động")
                || lower.contains("theo dõi định kỳ");
    }

    private String metricSpecificLifestyleAdvice(RecommendationMetricInput metric) {
        String normalizedMetric = normalizeMetricKey(metric.name());
        String labelVi = recommendationDisplayLabel(metric);
        return switch (normalizedMetric) {
            case "GLUCOSE", "HBA1C" ->
                    "Với \"%s\", ưu tiên bữa ăn chia đều trong ngày, giảm đồ ngọt hấp thu nhanh và đi bộ sau ăn để hỗ trợ kiểm soát đường huyết."
                            .formatted(labelVi);
            case "LDLC", "HDLC", "TRIGLYCERIDE", "TRIG", "CHOL", "CHOLESTEROL" ->
                    "Với \"%s\", nên giảm đồ chiên/rán và mỡ động vật, tăng cá - rau - chất xơ hòa tan, đồng thời duy trì vận động nhịp tim vừa để hỗ trợ mỡ máu."
                            .formatted(labelVi);
            case "RBC", "HGB", "HCT" ->
                    "Với \"%s\", chú ý bữa ăn giàu sắt, vitamin B12 và folate (thịt nạc, trứng, rau lá xanh), tránh thức khuya kéo dài để hỗ trợ tạo máu."
                            .formatted(labelVi);
            case "WBC", "NEUT%", "LYM%", "MONO%", "EO%", "EOS%", "BASO%", "BAS%" ->
                    "Với \"%s\", ưu tiên nghỉ ngơi, uống đủ nước, tránh rượu bia và theo dõi dấu hiệu nhiễm trùng/dị ứng để bác sĩ đánh giá thêm khi cần."
                            .formatted(labelVi);
            case "ALT", "AST", "GGT", "HBSAG" ->
                    "Với \"%s\", nên hạn chế rượu bia, thuốc lá và đồ ăn nhiều mỡ; tái khám đúng hẹn để bác sĩ theo dõi chức năng gan theo diễn tiến."
                            .formatted(labelVi);
            default ->
                    "Với \"%s\", tập trung điều chỉnh sinh hoạt liên quan trực tiếp đến %s thay vì chỉ áp dụng lời khuyên chung; trao đổi bác sĩ để cá thể hóa theo bệnh sử."
                            .formatted(labelVi, resolveMetricRelation(metric.name()));
        };
    }

    private int severityRank(String normalizedStatus) {
        return switch (normalizedStatus) {
            case "abnormal" -> 3;
            case "warning" -> 2;
            case "attention" -> 1;
            default -> 0;
        };
    }

    private RecommendationMetricInput pickAnchorMetric(List<RecommendationMetricInput> riskyMetrics) {
        if (riskyMetrics == null || riskyMetrics.isEmpty()) {
            return new RecommendationMetricInput("Chỉ số sức khỏe", "N/A", "", "attention");
        }
        return riskyMetrics.stream()
                .filter(Objects::nonNull)
                .max((left, right) -> Integer.compare(
                        severityRank(normalizeStatus(left.status())),
                        severityRank(normalizeStatus(right.status()))
                ))
                .orElse(riskyMetrics.get(0));
    }

    private boolean isNutritionRecommendation(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("chế độ dinh dưỡng:")
                || lower.startsWith("dinh dưỡng:");
    }

    private boolean isLifestyleRecommendation(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("chế độ sinh hoạt:")
                || lower.startsWith("sinh hoạt:");
    }

    private void safeCacheRecommendations(String cacheKey, List<String> recommendations) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(recommendations),
                    RECOMMENDATIONS_CACHE_TTL
            );
        } catch (Exception ex) {
            log.warn("Redis cache set failed for recommendations key '{}': {}", cacheKey, ex.getMessage());
        }
    }

    private String ageGroupFromAge(Integer age) {
        if (age == null || age < 0) {
            return "khong ro tuoi";
        }
        if (age < 18) {
            return "duoi 18 tuoi";
        }
        if (age < 40) {
            return "18-39 tuoi";
        }
        if (age < 60) {
            return "40-59 tuoi";
        }
        return "tu 60 tuoi tro len";
    }

    /** Nhãn tuổi có dấu — chỉ dùng trong prompt LLM (khớp logic {@link #ageGroupFromAge}). */
    private static String promptAgeGroupVi(Integer age) {
        if (age == null || age < 0) {
            return "không rõ tuổi";
        }
        if (age < 18) {
            return "dưới 18 tuổi";
        }
        if (age < 40) {
            return "18–39 tuổi";
        }
        if (age < 60) {
            return "40–59 tuổi";
        }
        return "từ 60 tuổi trở lên";
    }

    /** Giới tính có dấu — chỉ dùng trong prompt LLM (khớp logic {@link #normalizeGender}). */
    private static String promptGenderVi(String gender) {
        if (gender == null || gender.isBlank()) {
            return "không rõ giới tính";
        }
        String normalized = gender.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "male", "nam" -> "nam";
            case "female", "nu", "nữ" -> "nữ";
            default -> "không rõ giới tính";
        };
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return "khong ro gioi tinh";
        }
        String normalized = gender.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "male", "nam" -> "nam";
            case "female", "nu", "nữ" -> "nu";
            default -> normalized;
        };
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    private void recordPromptRenderingFailure(String promptType) {
        meterRegistry.counter("healthlens.ai.prompt.render.failures", "prompt_type", promptType).increment();
    }

    private void recordSchemaValidationFailure(String outputType) {
        meterRegistry.counter("healthlens.ai.schema.validation.failures", "output_type", outputType).increment();
    }

    private void recordAiGenerationMetrics(
            String outputType,
            String outcome,
            String validationStatus,
            int retryCount,
            int tokenUsage,
            long startedNanos
    ) {
        String model = effectiveModelVersion();
        String[] tags = {
                "output_type", outputType,
                "model", model,
                "outcome", outcome,
                "validation_status", validationStatus
        };
        meterRegistry.counter("healthlens.ai.generation.requests", tags).increment();
        meterRegistry.timer("healthlens.ai.generation.latency", tags)
                .record(Duration.ofNanos(Math.max(0L, System.nanoTime() - startedNanos)));
        meterRegistry.summary(
                "healthlens.ai.generation.token.usage",
                "output_type", outputType,
                "model", model,
                "outcome", outcome,
                "validation_status", validationStatus,
                "token_type", "estimated_total"
        ).record(Math.max(0, tokenUsage));
        meterRegistry.summary(
                "healthlens.ai.generation.retry.count",
                "output_type", outputType,
                "model", model,
                "outcome", outcome,
                "validation_status", validationStatus
        ).record(Math.max(0, retryCount));
    }

    private int estimateTokenUsage(String prompt, String output) {
        int charCount = (prompt == null ? 0 : prompt.length()) + (output == null ? 0 : output.length());
        if (charCount == 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(charCount / 4.0));
    }

    private String effectivePromptVersion() {
        return promptVersion == null || promptVersion.isBlank() ? "v3" : promptVersion.trim();
    }

    private String effectiveModelVersion() {
        return aiModelVersion == null || aiModelVersion.isBlank() ? "unknown" : aiModelVersion.trim();
    }

    private String effectiveRecommendationsPromptVersion() {
        String configuredVersion = recommendationsPromptVersion == null || recommendationsPromptVersion.isBlank()
                ? RECOMMENDATIONS_PROMPT_VERSION
                : recommendationsPromptVersion.trim();
        String templateVersion = versionFromTemplatePath(recommendationsPromptTemplate);
        if (templateVersion != null
                && RECOMMENDATIONS_PROMPT_VERSION.equals(configuredVersion)
                && !RECOMMENDATIONS_PROMPT_VERSION.equals(templateVersion)) {
            return templateVersion;
        }
        return configuredVersion;
    }

    private String versionFromTemplatePath(String templatePath) {
        if (templatePath == null || templatePath.isBlank()) {
            return null;
        }
        String fileName = templatePath.substring(templatePath.lastIndexOf('/') + 1);
        int firstDot = fileName.indexOf('.');
        int lastDot = fileName.lastIndexOf('.');
        if (firstDot < 0 || lastDot <= firstDot + 1) {
            return null;
        }
        String candidate = fileName.substring(firstDot + 1, lastDot);
        return candidate.startsWith("v") ? candidate : null;
    }

    public record ExplanationResult(String explanation, String source, String promptVersion, String modelVersion) {
        public ExplanationResult(String explanation, String source) {
            this(explanation, source, "unknown", "unknown");
        }

        public ExplanationResult {
            explanation = Objects.requireNonNullElse(explanation, "");
            source = Objects.requireNonNullElse(source, "fallback");
            promptVersion = Objects.requireNonNullElse(promptVersion, "unknown");
            modelVersion = Objects.requireNonNullElse(modelVersion, "unknown");
        }
    }

    public record RecommendationResult(
            List<String> recommendations,
            String source,
            String promptVersion,
            String modelVersion
    ) {
        public RecommendationResult {
            recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
            source = Objects.requireNonNullElse(source, "fallback");
            promptVersion = Objects.requireNonNullElse(promptVersion, "unknown");
            modelVersion = Objects.requireNonNullElse(modelVersion, "unknown");
        }
    }

    public record RecommendationMetricInput(
            String name,
            String value,
            String unit,
            String status,
            String displayLabelVi
    ) {
        public RecommendationMetricInput(String name, String value, String unit, String status) {
            this(name, value, unit, status, null);
        }
    }

    private static final class SchemaValidationException extends RuntimeException {
        private SchemaValidationException(String message) {
            super(message);
        }
    }
}
