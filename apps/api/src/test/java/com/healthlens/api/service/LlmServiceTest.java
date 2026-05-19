package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.ReferenceRangeDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho LlmService
 *
 * <p>Sử dụng Mockito để mock ChatClient, tránh phụ thuộc vào AI chat provider thật.
 * Test covers: generation thành công, retry logic, fallback, caching key, null-safety.
 *
 * <p>Lưu ý: maxRetryAttempts và initialDelayMs được set = 1 và 0 trong setUp
 * để tránh sleep delays dài trong unit tests.
 */
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private ChatClient aiChatClient;
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
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        meterRegistry = new SimpleMeterRegistry();
        llmService = new LlmService(aiChatClient, redisTemplate, objectMapper, meterRegistry);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(llmService, "defaultFallbackExplanation",
                "Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 1);
        ReflectionTestUtils.setField(llmService, "initialDelayMs", 0L);
        ReflectionTestUtils.setField(llmService, "retryMultiplier", 1.0);
        ReflectionTestUtils.setField(llmService, "maxTotalDelayMs", 4500L);
        ReflectionTestUtils.setField(llmService, "retrievalVersion", "v1");
        ReflectionTestUtils.setField(llmService, "aiModelVersion", "qwen-test");
    }

    // =========================================================
    // generateExplanation() Tests
    // =========================================================

    @Test
    @DisplayName("Thành công: AI chat provider trả về giải thích cho chỉ số Glucose")
    void generateExplanation_whenAiChatAvailable_returnsApiResponse() {
        String expectedExplanation = validExplanation();
        mockAiChatSuccess(explanationJson(expectedExplanation, referenceRange()));
        when(valueOperations.get(anyString())).thenReturn(null);

        String result = llmService.generateExplanation("Glucose", "5.4", "normal", referenceRange(), "vi");

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedExplanation + "\n" + LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER);
        verify(aiChatClient).prompt();
    }

    @Test
    @DisplayName("Fallback: AI chat provider thất bại → trả về fallback explanation cho Glucose")
    void generateExplanation_whenAiChatFails_returnsKnownFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("Connection timeout"));

        String result = llmService.generateExplanation("Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result).isNotNull();
        assertThat(result).contains("Chỉ số này là gì:");
        assertThat(result).contains("Chỉ số này liên quan đến:");
        assertThat(result).contains("Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng:");
    }

    @Test
    @DisplayName("Fallback: AI chat provider thất bại với metric không biết → trả về default message")
    void generateExplanation_whenAiChatFailsUnknownMetric_returnsDefaultFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("Rate limit exceeded"));

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
        String expectedExplanation = validExplanation();

        when(aiChatClient.prompt())
                .thenThrow(new RuntimeException("Transient error"))
                .thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(explanationJson(expectedExplanation, referenceRange()));

        String result = llmService.generateExplanation("HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result).isEqualTo(expectedExplanation + "\n" + LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER);
        verify(aiChatClient, times(2)).prompt();
    }

    @Test
    @DisplayName("Cache hit: không gọi LLM API khi explanation đã có trong Redis")
    void generateExplanation_whenCacheHit_doesNotCallLlm() {
        when(valueOperations.get(anyString())).thenReturn("llm||v-test||qwen-test||cached explanation");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result.explanation()).isEqualTo("cached explanation");
        assertThat(result.source()).isEqualTo("llm");
        assertThat(result.promptVersion()).isEqualTo("v-test");
        assertThat(result.modelVersion()).isEqualTo("qwen-test");
        verify(aiChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Cache hit legacy value: fallback để tránh gắn nhãn sai source")
    void generateExplanation_whenCacheHitLegacyFormat_marksAsFallback() {
        when(valueOperations.get(anyString())).thenReturn("legacy cached explanation");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result.explanation()).isEqualTo("legacy cached explanation");
        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.modelVersion()).isEqualTo("unknown");
        verify(aiChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Cache hit malformed 3-part value: không ném lỗi và fallback an toàn")
    void generateExplanation_whenCacheHitMalformedThreePartValue_marksAsFallback() {
        when(valueOperations.get(anyString())).thenReturn("llm||v3||cached explanation");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "HbA1c", "6.2", "normal", referenceRange(), "vi");

        assertThat(result.explanation()).isEqualTo("llm||v3||cached explanation");
        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.modelVersion()).isEqualTo("unknown");
        verify(aiChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Cache miss: lưu explanation vào Redis với TTL 7 ngày")
    void generateExplanation_whenCacheMiss_storesWithSevenDaysTtl() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));
        ReflectionTestUtils.setField(llmService, "promptVersion", "v-test");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "5.6", "normal", referenceRange(), "vi");

        assertThat(result.promptVersion()).isEqualTo("v-test");
        assertThat(result.modelVersion()).isEqualTo("qwen-test");
        verify(valueOperations).set(
                anyString(),
                eq("llm||v-test||qwen-test||" + validExplanation() + "\n" + LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER),
                eq(Duration.ofDays(7))
        );
    }

    @Test
    @DisplayName("Cache key thay đổi khi retrieval-version thay đổi")
    void generateExplanation_cacheKeyChangesWhenRetrievalVersionChanges() {
        List<String> requestedKeys = new ArrayList<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            requestedKeys.add(invocation.getArgument(0));
            return null;
        });
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));

        llmService.generateExplanation("Glucose", "5.6", "normal", referenceRange(), "vi");
        ReflectionTestUtils.setField(llmService, "retrievalVersion", "v2");
        llmService.generateExplanation("Glucose", "5.6", "normal", referenceRange(), "vi");

        assertThat(requestedKeys).hasSize(2);
        assertThat(requestedKeys.get(0)).isNotEqualTo(requestedKeys.get(1));
    }

    @Test
    @DisplayName("Cache key thay đổi khi prompt-version thay đổi")
    void generateExplanation_cacheKeyChangesWhenPromptVersionChanges() {
        List<String> requestedKeys = new ArrayList<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            requestedKeys.add(invocation.getArgument(0));
            return null;
        });
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));

        ReflectionTestUtils.setField(llmService, "promptVersion", "v2");
        llmService.generateExplanation("Glucose", "8.9", "normal", referenceRange(), "vi");
        ReflectionTestUtils.setField(llmService, "promptVersion", "v3");
        llmService.generateExplanation("Glucose", "8.9", "normal", referenceRange(), "vi");

        assertThat(requestedKeys).hasSize(2);
        assertThat(requestedKeys.get(0)).isNotEqualTo(requestedKeys.get(1));
    }

    @Test
    @DisplayName("Cache key thay đổi khi metric explanation template path thay đổi")
    void generateExplanation_cacheKeyChangesWhenPromptTemplateChanges() {
        List<String> requestedKeys = new ArrayList<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            requestedKeys.add(invocation.getArgument(0));
            return "legacy cached explanation";
        });

        ReflectionTestUtils.setField(llmService, "metricExplanationPromptTemplate", "ai/prompts/metric-explanation.v3.txt");
        llmService.generateExplanation("Glucose", "8.9", "abnormal", referenceRange(), "vi");
        ReflectionTestUtils.setField(llmService, "metricExplanationPromptTemplate", "ai/prompts/metric-explanation.v4.txt");
        llmService.generateExplanation("Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(requestedKeys).hasSize(2);
        assertThat(requestedKeys.get(0)).isNotEqualTo(requestedKeys.get(1));
    }

    @Test
    @DisplayName("Cache key: structured output contract version tách cache cũ của v3 prompt")
    void generateExplanation_cacheKeyIncludesStructuredOutputContractVersion() {
        List<String> requestedKeys = new ArrayList<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            requestedKeys.add(invocation.getArgument(0));
            return null;
        });
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));

        ReflectionTestUtils.setField(llmService, "promptVersion", "v3");
        ReflectionTestUtils.setField(llmService, "metricExplanationPromptTemplate", "ai/prompts/metric-explanation.v3.txt");

        llmService.generateExplanation("Glucose", "5.6", "normal", referenceRange(), "vi");

        String legacyPayload = String.join("|",
                "Glucose",
                "5.6",
                "normal",
                "vi",
                "v3",
                "ai/prompts/metric-explanation.v3.txt",
                "v1",
                "3.9",
                "6.4",
                "mmol/L",
                sha256HexForTest(""));
        String legacyKey = "llm:explanation:" + sha256HexForTest(legacyPayload);
        assertThat(requestedKeys).hasSize(1);
        assertThat(requestedKeys.get(0)).isNotEqualTo(legacyKey);
    }

    @Test
    @DisplayName("Fallback sau 3 retries: source phải là fallback")
    void generateExplanation_afterMaxRetries_returnsFallbackSource() {
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 3);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("API error"));

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "5.4", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        verify(aiChatClient, times(3)).prompt();
    }

    @Test
    @DisplayName("Schema validation: invalid JSON thì retry rồi fallback an toàn")
    void generateExplanation_whenInvalidJsonRetriesAndReturnsSafeFallback() {
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 2);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{bad json");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.explanation())
                .contains("Chỉ số này là gì:")
                .contains("không thay thế tư vấn");
        verify(aiChatClient, times(2)).prompt();
        assertThat(meterRegistry.counter(
                "healthlens.ai.schema.validation.failures",
                "output_type", "metric_explanation"
        ).count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("Schema validation: LLM bịa reference range thì reject và không trả raw output")
    void generateExplanation_whenLlmInventsReferenceRange_returnsFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        String hallucinated = """
                {"explanation":"Chỉ số này là gì: Glucose.\\nChỉ số này liên quan đến: đường huyết.\\nẢnh hưởng thường gặp nếu chỉ số lệch ngưỡng: cần theo dõi.",
                 "referenceRange":{"min":"1.0","max":"99.0","unit":"mmol/L"},
                 "disclaimer":"%s"}
                """.formatted(LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER);
        mockAiChatSuccess(hallucinated);

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.explanation()).doesNotContain("1.0").doesNotContain("99.0");
        assertThat(meterRegistry.counter(
                "healthlens.ai.schema.validation.failures",
                "output_type", "metric_explanation"
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Schema validation: explanation text bịa reference range thì reject")
    void generateExplanation_whenExplanationTextInventsReferenceRange_returnsFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        String hallucinatedText = """
                {"explanation":"Chỉ số này là gì: Glucose là đường huyết.\\nChỉ số này liên quan đến: kiểm soát chuyển hóa.\\nẢnh hưởng thường gặp nếu chỉ số lệch ngưỡng: ngưỡng 1.0 - 99.0 mmol/L cần theo dõi.",
                 "referenceRange":{"min":"3.9","max":"6.4","unit":"mmol/L"},
                 "disclaimer":"%s"}
                """.formatted(LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER);
        mockAiChatSuccess(hallucinatedText);

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "8.9", "normal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.explanation()).doesNotContain("1.0 - 99.0");
    }

    @Test
    @DisplayName("Schema validation: disclaimer không đúng canonical thì reject")
    void generateExplanation_whenDisclaimerIsNotCanonical_returnsFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        String nonCanonicalDisclaimer = """
                {"explanation":"Chỉ số này là gì: Glucose là đường huyết.\\nChỉ số này liên quan đến: kiểm soát chuyển hóa.\\nẢnh hưởng thường gặp nếu chỉ số lệch ngưỡng: cần theo dõi cùng bác sĩ khi tái khám.",
                 "referenceRange":{"min":"3.9","max":"6.4","unit":"mmol/L"},
                 "disclaimer":"Thông tin này không thay thế bác sĩ."}
                """;
        mockAiChatSuccess(nonCanonicalDisclaimer);

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "5.6", "normal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(meterRegistry.counter(
                "healthlens.ai.schema.validation.failures",
                "output_type", "metric_explanation"
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Schema validation: successful output luôn trả canonical disclaimer")
    void generateExplanation_whenValidStructuredOutput_returnsCanonicalDisclaimer() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));

        String result = llmService.generateExplanation("Glucose", "5.6", "normal", referenceRange(), "vi");

        assertThat(result).endsWith(LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER);
    }

    @Test
    @DisplayName("Schema validation: abnormal explanation thiếu safety follow-up thì reject")
    void generateExplanation_whenAbnormalMissingSafetyLanguage_returnsFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess(explanationJson(validExplanation(), referenceRange()));

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.explanation())
                .contains("Đây không phải chẩn đoán")
                .contains("trao đổi với bác sĩ");
    }

    @Test
    @DisplayName("Metrics: AI generation ghi latency, model, token usage, outcome và validation status")
    void generateExplanation_recordsGenerationMetrics() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess(explanationJson(
                validExplanation(),
                referenceRange()
        ));

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "5.4", "normal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("llm");
        assertThat(meterRegistry.counter(
                "healthlens.ai.generation.requests",
                "output_type", "metric_explanation",
                "model", "qwen-test",
                "outcome", "llm",
                "validation_status", "valid"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("healthlens.ai.generation.latency").timer()).isNotNull();
        assertThat(meterRegistry.find("healthlens.ai.generation.token.usage").summary()).isNotNull();
        assertThat(meterRegistry.find("healthlens.ai.generation.retry.count").summary()).isNotNull();
    }

    @Test
    @DisplayName("Prompt builder: tiếng Anh nhưng yêu cầu output tiếng Việt đơn giản")
    void buildMedicalPrompt_containsVietnameseSimpleInstruction() {
        String prompt = llmService.buildMedicalPrompt("Glucose", "5.4", "normal", referenceRange(), "vi", "knowledge");

        assertThat(prompt).contains("Explain this health metric result in simple Vietnamese");
        assertThat(prompt).contains("Do not use complex medical terms without explanation.");
        assertThat(prompt).contains("Do not invent, infer, or change reference ranges.");
        assertThat(prompt).contains("Required explanation field format (exactly 3 short lines in Vietnamese):");
        assertThat(prompt).contains("Chỉ số này liên quan đến:");
        assertThat(prompt).contains("Metric context:");
        assertThat(prompt).contains("Metric relation:");
        assertThat(prompt).contains("Knowledge snippet:");
        assertThat(prompt).contains("Reference range: 3.9 - 6.4 mmol/L");
    }

    @Test
    @DisplayName("Prompt builder: chỉ số abnormal phải yêu cầu follow-up và tránh chẩn đoán")
    void buildMedicalPrompt_abnormalRequiresFollowUpAndNoDiagnosisLanguage() {
        String prompt = llmService.buildMedicalPrompt("Glucose", "8.9", "abnormal", referenceRange(), "vi", "knowledge");

        assertThat(prompt)
                .contains("If Status is abnormal")
                .contains("not a diagnosis")
                .contains("follow-up with a doctor")
                .contains("Status: abnormal");
    }

    @Test
    @DisplayName("Prompt template resource: giữ disclaimer và quy tắc không tự bịa reference range")
    void promptTemplateResource_containsRequiredMedicalGuardrails() throws IOException {
        ClassPathResource template = new ClassPathResource("ai/prompts/metric-explanation.v3.txt");

        String content = template.getContentAsString(StandardCharsets.UTF_8);

        assertThat(content)
                .contains("Do not provide treatment plan or disease diagnosis.")
                .contains("Do not invent, infer, or change reference ranges.")
                .contains("this is not a diagnosis")
                .contains("follow-up with a doctor");
    }

    @Test
    @DisplayName("Prompt rendering failure: trả fallback an toàn và tăng failure metric")
    void generateExplanation_whenPromptRenderingFails_returnsFallbackAndRecordsMetric() {
        when(valueOperations.get(anyString())).thenReturn(null);
        ReflectionTestUtils.setField(llmService, "metricExplanationPromptTemplate", "ai/prompts/missing-template.txt");

        LlmService.ExplanationResult result = llmService.generateExplanationResult(
                "Glucose", "8.9", "abnormal", referenceRange(), "vi");

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.explanation())
                .contains("Chỉ số này là gì:")
                .contains("Đây không phải chẩn đoán");
        assertThat(meterRegistry.counter(
                "healthlens.ai.prompt.render.failures",
                "prompt_type", "metric_explanation"
        ).count()).isEqualTo(1.0);
        verify(aiChatClient, never()).prompt();
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
    @DisplayName("Fallback explanation abnormal: phải nhắc không chẩn đoán và follow-up")
    void getFallbackExplanation_abnormalStatus_containsFollowUpSafetyCopy() {
        String result = llmService.getFallbackExplanation("Glucose", "abnormal");

        assertThat(result)
                .contains("Đây không phải chẩn đoán")
                .contains("trao đổi với bác sĩ")
                .contains("tái khám");
    }

    @Test
    @DisplayName("Recommendations: cache hit thì không gọi LLM")
    void generateRecommendations_cacheHit_returnsCachedData() {
        when(valueOperations.get(anyString())).thenReturn("[\"Chế độ dinh dưỡng: Với Glucose, ưu tiên bữa ăn cân bằng và hạn chế đồ ngọt.\", \"Chế độ sinh hoạt: Với Glucose, duy trì đi bộ nhẹ sau ăn và ngủ đủ giấc.\"]");

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).hasSizeBetween(2, 3);
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("dinh dưỡng"))).isTrue();
        assertThat(result.stream().anyMatch(line -> line.toLowerCase().contains("sinh hoạt"))).isTrue();
        verify(aiChatClient, never()).prompt();
    }

    @Test
    @DisplayName("Recommendations: cache miss thì gọi LLM và parse JSON output")
    void generateRecommendations_cacheMiss_callsLlmAndParseJson() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess("[\"Chế độ dinh dưỡng: Với Đường huyết, bạn nên giảm đồ ngọt\", \"Chế độ sinh hoạt: Với Glucose, đi bộ sau ăn 20-30 phút\"]");

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
    @DisplayName("Recommendations prompt: bắt buộc có disclaimer chuẩn và tránh ngôn ngữ chẩn đoán")
    void generateRecommendations_promptContainsRequiredDisclaimerAndNoDiagnosisRules() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess("[\"Chế độ dinh dưỡng: Với Đường huyết, giảm đồ ngọt\", \"Chế độ sinh hoạt: Với Glucose, đi bộ nhẹ sau ăn\"]");

        llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains(LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER)
                .contains(LlmService.RECOMMENDATIONS_OUTPUT_SCHEMA_JSON)
                .contains("không thay thế tư vấn, chẩn đoán hoặc điều trị")
                .contains("không được viết như kết luận chẩn đoán")
                .contains("khuyến nghị người dùng trao đổi với bác sĩ");
    }

    @Test
    @DisplayName("Recommendations metadata: trả prompt/model version cho audit")
    void generateRecommendationsResult_returnsPromptAndModelVersion() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess("[\"Chế độ dinh dưỡng: Với Đường huyết, giảm đồ ngọt\", \"Chế độ sinh hoạt: Với Glucose, đi bộ nhẹ sau ăn\"]");
        ReflectionTestUtils.setField(llmService, "recommendationsPromptVersion", "v9-test");

        LlmService.RecommendationResult result = llmService.generateRecommendationsResult(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result.recommendations()).hasSize(2);
        assertThat(result.source()).isEqualTo("llm");
        assertThat(result.promptVersion()).isEqualTo("v9-test");
        assertThat(result.modelVersion()).isEqualTo("qwen-test");
    }

    @Test
    @DisplayName("Recommendations prompt rendering failure: trả fallback và tăng failure metric")
    void generateRecommendations_whenPromptRenderingFails_returnsFallbackAndRecordsMetric() {
        when(valueOperations.get(anyString())).thenReturn(null);
        ReflectionTestUtils.setField(llmService, "recommendationsPromptTemplate", "ai/prompts/missing-recommendations-template.txt");

        LlmService.RecommendationResult result = llmService.generateRecommendationsResult(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.recommendations()).isNotEmpty();
        assertThat(meterRegistry.counter(
                "healthlens.ai.prompt.render.failures",
                "prompt_type", "recommendations"
        ).count()).isEqualTo(1.0);
        verify(aiChatClient, never()).prompt();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Recommendations: cache key thay đổi khi model version thay đổi")
    void generateRecommendations_cacheKeyChangesWhenModelVersionChanges() {
        List<String> requestedKeys = new ArrayList<>();
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            requestedKeys.add(invocation.getArgument(0));
            return null;
        });
        mockAiChatSuccess("[\"Chế độ dinh dưỡng: Với Đường huyết, giảm đồ ngọt\", \"Chế độ sinh hoạt: Với Glucose, đi bộ nhẹ sau ăn\"]");

        ReflectionTestUtils.setField(llmService, "aiModelVersion", "qwen-v1");
        llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );
        ReflectionTestUtils.setField(llmService, "aiModelVersion", "qwen-v2");
        llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(requestedKeys).hasSize(2);
        assertThat(requestedKeys.get(0)).isNotEqualTo(requestedKeys.get(1));
    }

    @Test
    @DisplayName("Recommendations: output LLM low-quality thì source là fallback")
    void generateRecommendationsResult_whenLlmOutputLowQuality_returnsFallbackSource() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess("[]");

        LlmService.RecommendationResult result = llmService.generateRecommendationsResult(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(result.recommendations()).isNotEmpty();
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Recommendations schema: item quá ngắn thì fallback")
    void generateRecommendationsResult_whenRecommendationItemTooShort_returnsFallbackSource() {
        when(valueOperations.get(anyString())).thenReturn(null);
        mockAiChatSuccess("[\"ngắn\", \"Chế độ sinh hoạt: Với Glucose, đi bộ nhẹ sau ăn và ngủ đủ giấc.\"]");

        LlmService.RecommendationResult result = llmService.generateRecommendationsResult(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result.source()).isEqualTo("fallback");
        assertThat(meterRegistry.counter(
                "healthlens.ai.schema.validation.failures",
                "output_type", "recommendations"
        ).count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Recommendations: cache malformed thì bỏ qua cache và fallback khi LLM lỗi")
    void generateRecommendations_cacheMalformed_thenFallbackWhenLlmFails() {
        when(valueOperations.get(anyString())).thenReturn("malformed-cache-value");
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("LLM down"));

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).isNotEmpty();
        verify(aiChatClient).prompt();
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Recommendations: LLM exception thì fallback được cache cho lần sau")
    void generateRecommendations_llmException_cachesFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("Rate limit"));

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
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("LLM down"));

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

    @Test
    @DisplayName("Recommendations fallback: chỉ số bất thường phải có theo dõi y tế và không tự chẩn đoán")
    void generateRecommendations_abnormalFallbackIncludesFollowUpAndNoSelfDiagnosisLanguage() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(aiChatClient.prompt()).thenThrow(new RuntimeException("LLM down"));

        List<String> result = llmService.generateRecommendations(
                List.of(new LlmService.RecommendationMetricInput("Glucose", "8.1", "mmol/L", "abnormal")),
                45,
                "female"
        );

        assertThat(result).isNotEmpty();
        assertThat(String.join(" ", result))
                .contains("không tự chẩn đoán")
                .contains("trao đổi với bác sĩ");
    }

    private void mockAiChatSuccess(String responseContent) {
        when(aiChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }

    private String explanationJson(String explanation, ReferenceRangeDto range) {
        return """
                {"explanation":"%s",
                 "referenceRange":{"min":"%s","max":"%s","unit":"%s"},
                 "disclaimer":"%s"}
                """.formatted(
                explanation.replace("\n", "\\n").replace("\"", "\\\""),
                range.min().toPlainString(),
                range.max().toPlainString(),
                range.unit(),
                LlmService.MEDICAL_RECOMMENDATIONS_DISCLAIMER
        );
    }

    private String validExplanation() {
        return "Chỉ số này là gì: Glucose là đường huyết tại thời điểm xét nghiệm.\n"
                + "Chỉ số này liên quan đến: kiểm soát chuyển hóa và năng lượng của cơ thể.\n"
                + "Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng: cần theo dõi xu hướng cùng bác sĩ khi tái khám.";
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

    private String sha256HexForTest(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
