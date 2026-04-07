package com.healthlens.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

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

    private final ChatClient groqChatClient;

    @Value("${app.ai.fallback.explanation:Kết quả cần được bác sĩ chuyên khoa giải thích thêm.}")
    private String defaultFallbackExplanation;

    @Value("${app.ai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.ai.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${app.ai.retry.multiplier:2.0}")
    private double retryMultiplier;

    /**
     * Fallback explanations tĩnh theo metric name.
     * Được dùng khi tất cả retry attempts thất bại.
     */
    private static final Map<String, String> FALLBACK_EXPLANATIONS = Map.of(
            "Glucose", "Đây là chỉ số đường huyết lúc đói. Hãy tham khảo ý kiến bác sĩ để hiểu rõ hơn về kết quả của bạn.",
            "HbA1c", "Chỉ số HbA1c phản ánh mức đường huyết trung bình trong 3 tháng qua. Bác sĩ sẽ giúp bạn phân tích kết quả.",
            "Cholesterol", "Đây là chỉ số mỡ máu tổng. Kết quả cần được đánh giá cùng với các chỉ số khác bởi bác sĩ.",
            "HDL", "HDL là cholesterol tốt, giúp bảo vệ tim mạch. Tham khảo bác sĩ để hiểu giá trị của bạn.",
            "LDL", "LDL là cholesterol xấu, có thể gây nguy cơ tim mạch khi cao. Bác sĩ sẽ tư vấn phù hợp."
    );

    public LlmService(ChatClient groqChatClient) {
        this.groqChatClient = groqChatClient;
    }

    /**
     * Generate giải thích y tế bằng tiếng Việt cho một chỉ số xét nghiệm.
     *
     * <p>Kết quả được cache trong Redis với TTL mặc định. Cache key dựa trên
     * tất cả 4 parameters để đảm bảo uniqueness.
     *
     * @param metricName  tên chỉ số (ví dụ: "Glucose", "HbA1c")
     * @param value       giá trị đo được (ví dụ: "5.4")
     * @param unit        đơn vị đo (ví dụ: "mmol/L", "g/dL")
     * @param status      trạng thái kết quả: "normal", "high", "low", "critical"
     * @return            giải thích bằng tiếng Việt (từ Groq hoặc fallback)
     */
    @Cacheable(value = "metric-explanations",
               key = "#metricName + ':' + #value + ':' + #unit + ':' + (#status ?: 'unknown')")
    public String generateExplanation(String metricName, String value, String unit, String status) {
        String normalizedStatus = status != null ? status : "unknown";
        String prompt = buildMedicalPrompt(metricName, value, unit, normalizedStatus);
        log.debug("Calling Groq API for metric: {}, value: {} {}, status: {}",
                metricName, value, unit, normalizedStatus);
        return callWithRetry(prompt, metricName);
    }

    /**
     * Gọi Groq API với exponential backoff retry.
     *
     * @param prompt      prompt đã format
     * @param metricName  tên chỉ số (cho logging và fallback lookup)
     * @return            response từ Groq hoặc fallback explanation
     */
    private String callWithRetry(String prompt, String metricName) {
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
                return result;

            } catch (Exception e) {
                log.warn("Groq API attempt {}/{} failed for metric '{}': {}",
                        attempt, maxRetryAttempts, metricName, e.getMessage());

                if (attempt < maxRetryAttempts) {
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
        return getFallbackExplanation(metricName);
    }

    /**
     * Build medical prompt theo chuẩn tiếng Việt.
     *
     * @param metricName  tên chỉ số xét nghiệm
     * @param value       giá trị
     * @param unit        đơn vị
     * @param status      trạng thái (normal/high/low/critical) — nullable
     * @return            prompt string đã format
     */
    String buildMedicalPrompt(String metricName, String value, String unit, String status) {
        // F4 fix: null-safe status handling
        String safeStatus = (status != null) ? status : "unknown";
        String statusVietnamese = switch (safeStatus.toLowerCase()) {
            case "normal" -> "Bình thường";
            case "high" -> "Cao hơn mức bình thường";
            case "low" -> "Thấp hơn mức bình thường";
            case "critical" -> "Cần chú ý đặc biệt";
            default -> safeStatus;
        };

        return String.format("""
                Hãy giải thích kết quả xét nghiệm sau bằng tiếng Việt đơn giản (tối đa 3 câu):
                
                Chỉ số: %s
                Giá trị đo được: %s %s
                Đánh giá: %s
                
                Yêu cầu: Giải thích ý nghĩa, nguyên nhân có thể và lời khuyên ngắn gọn. Không chẩn đoán bệnh.
                """, metricName, value, unit, statusVietnamese);
    }

    /**
     * Lấy fallback explanation khi Groq API không khả dụng.
     *
     * @param metricName  tên chỉ số
     * @return            explanation tĩnh hoặc default message
     */
    String getFallbackExplanation(String metricName) {
        return FALLBACK_EXPLANATIONS.getOrDefault(metricName, defaultFallbackExplanation);
    }
}
