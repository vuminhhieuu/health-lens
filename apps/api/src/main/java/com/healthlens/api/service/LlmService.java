package com.healthlens.api.service;

import com.healthlens.api.dto.ReferenceRangeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * LLM Service — Tích hợp Groq API để generate giải thích kết quả xét nghiệm
 *
 * <p>Service này sử dụng Spring AI {@link ChatClient} để giao tiếp với Groq API
 * (OpenAI-compatible endpoint: https://api.groq.com/openai).
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
    private static final String CACHE_VALUE_SEPARATOR = "||";

    private final ChatClient groqChatClient;
    private final StringRedisTemplate redisTemplate;

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

    @Value("${app.ai.explanation.prompt-version:v2}")
    private String promptVersion;

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

    public LlmService(ChatClient groqChatClient, StringRedisTemplate redisTemplate) {
        this.groqChatClient = groqChatClient;
        this.redisTemplate = redisTemplate;
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
        String normalizedLang = (lang == null || lang.isBlank()) ? "vi" : lang.trim().toLowerCase();
        String normalizedStatus = (status == null || status.isBlank()) ? "unknown" : status;
        String cacheKey = buildCacheKey(metricName, value, normalizedStatus, referenceRange, normalizedLang, knowledgeSnippet);
        String cachedExplanation = safeGetCachedExplanation(cacheKey);
        if (cachedExplanation != null && !cachedExplanation.isBlank()) {
            return parseCachedExplanation(cachedExplanation);
        }

        String prompt = buildMedicalPrompt(metricName, value, normalizedStatus, referenceRange, normalizedLang, knowledgeSnippet);

        ExplanationResult result = callWithRetry(prompt, metricName);
        safeCacheExplanation(cacheKey, result);
        return result;
    }

    private ExplanationResult callWithRetry(String prompt, String metricName) {
        long startTime = System.currentTimeMillis();
        long currentDelayMs = initialDelayMs;

        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                String result = groqChatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                if (attempt > 1) {
                    log.info("Groq API succeeded on attempt {}/{} for metric '{}'",
                            attempt, maxRetryAttempts, metricName);
                }
                return new ExplanationResult(result, "llm");

            } catch (Exception e) {
                log.warn("Groq API attempt {}/{} failed for metric '{}': {}",
                        attempt, maxRetryAttempts, metricName, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    long elapsedMs = System.currentTimeMillis() - startTime;
                    if (elapsedMs + currentDelayMs > maxTotalDelayMs) {
                        log.warn("Stop retry for metric '{}' due to delay budget exceeded (elapsed={}ms, nextDelay={}ms, budget={}ms)",
                                metricName, elapsedMs, currentDelayMs, maxTotalDelayMs);
                        break;
                    }
                    try {
                        Thread.sleep(currentDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Retry interrupted for metric '{}'", metricName);
                        break;
                    }
                    currentDelayMs = (long) (currentDelayMs * retryMultiplier);
                }
            }
        }

        log.warn("All {} attempts failed for metric '{}'. Using fallback.",
                maxRetryAttempts, metricName);
        return new ExplanationResult(getFallbackExplanation(metricName), "fallback");
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
        String promptLanguage = "vi".equalsIgnoreCase(lang) ? "Vietnamese (tiếng Việt đơn giản)" : lang;
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

        return String.format("""
                You are a health assistant.
                Explain this health metric result in simple %s.
                Do not use complex medical terms without explanation.
                Focus on helping non-medical users understand what this metric is and how it may affect health.
                Do not provide treatment plan or disease diagnosis.

                Required output format (exactly 3 short lines in Vietnamese):
                1) Chỉ số này là gì: ...
                2) Chỉ số này liên quan đến: ...
                3) Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: ...

                Metric: %s
                Value: %s
                Status: %s (normal/attention/abnormal)
                Reference range: %s
                Metric context: %s
                Metric relation: %s
                Knowledge snippet:
                %s
                Status meaning: %s
                """, promptLanguage, safeMetric(metricName), safeValue(value), safeStatus.toLowerCase(), rangeText, metricContext, metricRelation, effectiveKnowledgeSnippet, statusExplanation);
    }

    String getFallbackExplanation(String metricName) {
        String metricContext = resolveMetricContext(metricName);
        String metricFallback = resolveFallbackByMetric(metricName);
        if (metricFallback != null) {
            return "Chỉ số này là gì: " + metricContext + "\n"
                    + "Chỉ số này liên quan đến: " + resolveMetricRelation(metricName) + ".\n"
                    + "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: " + metricFallback;
        }
        return "Chỉ số này là gì: " + metricContext + "\n"
                + "Chỉ số này liên quan đến: cân bằng miễn dịch, chuyển hóa và chức năng cơ quan tùy từng xét nghiệm.\n"
                + "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: " + defaultFallbackExplanation;
    }

    private String toCachedValue(ExplanationResult result) {
        return result.source() + CACHE_VALUE_SEPARATOR + result.explanation();
    }

    private ExplanationResult parseCachedExplanation(String rawCacheValue) {
        int separatorIndex = rawCacheValue.indexOf(CACHE_VALUE_SEPARATOR);
        if (separatorIndex < 0) {
            // Backward-compatible with old cache format (plain explanation text only).
            return new ExplanationResult(rawCacheValue, "fallback");
        }
        String source = rawCacheValue.substring(0, separatorIndex);
        String explanation = rawCacheValue.substring(separatorIndex + CACHE_VALUE_SEPARATOR.length());
        return new ExplanationResult(explanation, source);
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
                promptVersion == null ? "v2" : promptVersion.trim(),
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

    public record ExplanationResult(String explanation, String source) {
        public ExplanationResult {
            explanation = Objects.requireNonNullElse(explanation, "");
            source = Objects.requireNonNullElse(source, "fallback");
        }
    }
}
