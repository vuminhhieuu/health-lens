package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.ReferenceRangeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho LlmService
 *
 * <p>Sử dụng Mockito để mock ChatClient, tránh phụ thuộc vào Groq API thật.
 * Test covers: generation thành công, retry logic, fallback, caching key, null-safety.
 *
 * <p>Lưu ý: maxRetryAttempts và initialDelayMs được set = 1 và 0 trong setUp
 * để tránh sleep delays dài trong unit tests.
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private ChatClient groqChatClient;
    @Mock
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private LlmService llmService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        llmService = new LlmService(groqChatClient, redisTemplate, objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(llmService, "defaultFallbackExplanation",
                "Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 1);
        ReflectionTestUtils.setField(llmService, "initialDelayMs", 0L);
        ReflectionTestUtils.setField(llmService, "retryMultiplier", 1.0);
        ReflectionTestUtils.setField(llmService, "maxTotalDelayMs", 4500L);
    }

    // =========================================================
    // generateExplanation() Tests
    // =========================================================

    @Test
    @DisplayName("Thành công: Groq API trả về giải thích cho chỉ số Glucose")
    void generateExplanation_whenGroqAvailable_returnsApiResponse() {
        String expectedExplanation = "Chỉ số đường huyết của bạn ở mức bình thường (5.4 mmol/L). "
                + "Duy trì chế độ ăn lành mạnh và tập thể dục thường xuyên để giữ mức này. "
                + "Hãy kiểm tra định kỳ theo khuyến nghị của bác sĩ.";
        mockGroqApiSuccess(expectedExplanation);
        when(valueOperations.get(anyString())).thenReturn(null);

        String result = llmService.generateExplanation("Glucose", "5.4", "normal", referenceRange(), "vi");

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedExplanation);
        verify(groqChatClient).prompt();
    }

    @Test
    @DisplayName("Fallback: Groq API thất bại → trả về fallback explanation cho Glucose")
    void generateExplanation_whenGroqFails_returnsKnownFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("Connection timeout"));

        String result = llmService.generateExplanation("Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result).isNotNull();
        assertThat(result).contains("Chỉ số này là gì:");
        assertThat(result).contains("Chỉ số này liên quan đến:");
        assertThat(result).contains("Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng:");
    }

    @Test
    @DisplayName("Fallback: Groq API thất bại với metric không biết → trả về default message")
    void generateExplanation_whenGroqFailsUnknownMetric_returnsDefaultFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("Rate limit exceeded"));

        String result = llmService.generateExplanation("UnknownMetric999", "42", "unknown", null, "vi");

        assertThat(result).isNotNull();
        assertThat(result).contains("Chỉ số này là gì:");
        assertThat(result).contains("Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
    }

    @Test
    @DisplayName("Retry: Thất bại lần 1, thành công lần 2")
    void generateExplanation_failsThenSucceeds_returnsApiResponse() {
        when(valueOperations.get(anyString())).thenReturn(null);
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 3);
        String expectedExplanation = "HbA1c bình thường, tốt lắm!";

        when(groqChatClient.prompt())
                .thenThrow(new RuntimeException("Transient error"))
                .thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedExplanation);

        String result = llmService.generateExplanation("HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result).isEqualTo(expectedExplanation);
        verify(groqChatClient, times(2)).prompt();
    }

    @Test
    @DisplayName("Cache hit: không gọi LLM API khi explanation đã có trong Redis")
    void generateExplanation_whenCacheHit_doesNotCallLlm() {
        when(valueOperations.get(anyString())).thenReturn("llm||cached explanation");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result.explanation()).isEqualTo("cached explanation");
        assertThat(result.source()).isEqualTo("llm");
        verify(groqChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Cache hit legacy value: fallback để tránh gắn nhãn sai source")
    void generateExplanation_whenCacheHitLegacyFormat_marksAsFallback() {
        when(valueOperations.get(anyString())).thenReturn("legacy cached explanation");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result.explanation()).isEqualTo("legacy cached explanation");
        assertThat(result.source()).isEqualTo("fallback");
        verify(groqChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Cache miss: lưu explanation vào Redis với TTL 7 ngày")
    void generateExplanation_whenCacheMiss_storesWithSevenDaysTtl() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockGroqApiSuccess("llm explanation");

        llmService.generateExplanation("Glucose", "5.6", "normal", referenceRange(), "vi");

        verify(valueOperations).set(anyString(), eq("llm||llm explanation"), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Fallback sau 3 retries: source phải là fallback")
    void generateExplanation_afterMaxRetries_returnsFallbackSource() {
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 3);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("API error"));

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "5.4", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        verify(groqChatClient, times(3)).prompt();
    }

    @Test
    @DisplayName("Prompt builder: tiếng Anh nhưng yêu cầu output tiếng Việt đơn giản")
    void buildMedicalPrompt_containsVietnameseSimpleInstruction() {
        String prompt = llmService.buildMedicalPrompt("Glucose", "5.4", "normal", referenceRange(), "vi", "knowledge");

        assertThat(prompt).contains("Explain this health metric result in simple Vietnamese");
        assertThat(prompt).contains("Do not use complex medical terms without explanation.");
        assertThat(prompt).contains("Required output format (exactly 3 short lines in Vietnamese):");
        assertThat(prompt).contains("Chỉ số này liên quan đến:");
        assertThat(prompt).contains("Metric context:");
        assertThat(prompt).contains("Metric relation:");
        assertThat(prompt).contains("Knowledge snippet:");
        assertThat(prompt).contains("Reference range: 3.9 - 6.4 mmol/L");
    }

    @Test
    @DisplayName("Fallback EO%: phải có ngữ cảnh chỉ số và ảnh hưởng")
    void getFallbackExplanation_eosinophil_containsContext() {
        String result = llmService.getFallbackExplanation("EO%");

        assertThat(result).contains("bạch cầu ái toan");
        assertThat(result).contains("Chỉ số này là gì:");
        assertThat(result).contains("Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng:");
    }

    @Test
    @DisplayName("Fallback HDL-C: dùng đúng ngữ cảnh cholesterol tốt")
    void getFallbackExplanation_hdlc_containsLipidContext() {
        String result = llmService.getFallbackExplanation("HDL-C");

        assertThat(result).contains("HDL-C");
        assertThat(result).contains("cholesterol tốt");
        assertThat(result).contains("Chỉ số này liên quan đến:");
    }

    @Test
    @DisplayName("Fallback chỉ số lạ: vẫn trả đủ 3 ý theo format")
    void getFallbackExplanation_unknownMetric_hasThreeSections() {
        String result = llmService.getFallbackExplanation("Ferritin");

        assertThat(result).contains("Chỉ số này là gì:");
        assertThat(result).contains("Chỉ số này liên quan đến:");
        assertThat(result).contains("Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng:");
    }

    @Test
    @DisplayName("Recommendations: cache hit thì không gọi LLM")
    void generateRecommendations_cacheHit_returnsCachedData() {
        when(valueOperations.get(anyString())).thenReturn("[\"An nhat\", \"Tap deu\"]");

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).hasSizeBetween(2, 3);
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("dinh dưỡng"))).isTrue();
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("sinh hoạt"))).isTrue();
        verify(groqChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Recommendations: cache miss thì gọi LLM và parse JSON output")
    void generateRecommendations_cacheMiss_callsLlmAndParseJson() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockGroqApiSuccess("[\"Chế độ dinh dưỡng: Với Đường huyết, bạn nên giảm đồ ngọt\", \"Chế độ sinh hoạt: Với Glucose, đi bộ sau ăn 20-30 phút\"]");

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).containsExactly(
                "Chế độ dinh dưỡng: Với Đường huyết, bạn nên giảm đồ ngọt",
                "Chế độ sinh hoạt: Với Glucose, đi bộ sau ăn 20-30 phút"
        );
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Recommendations: cache malformed thì bỏ qua cache và fallback khi LLM lỗi")
    void generateRecommendations_cacheMalformed_thenFallbackWhenLlmFails() {
        when(valueOperations.get(anyString())).thenReturn("malformed-cache-value");
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("LLM down"));

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).isNotEmpty();
        verify(groqChatClient).prompt();
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Recommendations: LLM exception thì fallback được cache cho lần sau")
    void generateRecommendations_llmException_cachesFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("Rate limit"));

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).isNotEmpty();
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Recommendations fallback: ưu tiên metric có severity cao nhất")
    void generateRecommendations_fallbackPrioritizesMostSevereMetric() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("LLM down"));

        List<String> result = llmService.generateRecommendations(
                List.of(
                        new LlmService.RecommendationMetricInput("Glucose", "6.2", "mmol/L", "attention"),
                        new LlmService.RecommendationMetricInput("LDL-C", "4.1", "mmol/L", "abnormal"),
                        new LlmService.RecommendationMetricInput("Triglyceride", "2.5", "mmol/L", "warning")
                ),
                45,
                "female"
        );

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).contains("LDL-C");
        assertThat(result.get(0)).contains("bất thường");
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("dinh dưỡng"))).isTrue();
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("sinh hoạt"))).isTrue();
    }

    private void mockGroqApiSuccess(String responseContent) {
        when(groqChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }

    private ReferenceRangeDto referenceRange() {
        return new ReferenceRangeDto(
                BigDecimal.valueOf(3.9),
                BigDecimal.valueOf(6.4),
                BigDecimal.valueOf(3.2),
                BigDecimal.valueOf(7.1),
                "mmol/L"
        );
    }
}
