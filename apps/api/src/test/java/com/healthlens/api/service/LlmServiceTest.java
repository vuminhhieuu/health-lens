package com.healthlens.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

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
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private LlmService llmService;

    @BeforeEach
    void setUp() {
        llmService = new LlmService(groqChatClient);
        ReflectionTestUtils.setField(llmService, "defaultFallbackExplanation",
                "Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
        // Tắt retry delay để unit tests chạy nhanh
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 1);
        ReflectionTestUtils.setField(llmService, "initialDelayMs", 0L);
        ReflectionTestUtils.setField(llmService, "retryMultiplier", 1.0);
    }

    // =========================================================
    // generateExplanation() Tests
    // =========================================================

    @Test
    @DisplayName("Thành công: Groq API trả về giải thích cho chỉ số Glucose")
    void generateExplanation_whenGroqAvailable_returnsApiResponse() {
        // Arrange
        String expectedExplanation = "Chỉ số đường huyết của bạn ở mức bình thường (5.4 mmol/L). "
                + "Duy trì chế độ ăn lành mạnh và tập thể dục thường xuyên để giữ mức này. "
                + "Hãy kiểm tra định kỳ theo khuyến nghị của bác sĩ.";
        mockGroqApiSuccess(expectedExplanation);

        // Act
        String result = llmService.generateExplanation("Glucose", "5.4", "mmol/L", "normal");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedExplanation);
        verify(groqChatClient).prompt();
    }

    @Test
    @DisplayName("Fallback: Groq API thất bại → trả về fallback explanation cho Glucose")
    void generateExplanation_whenGroqFails_returnsKnownFallback() {
        // Arrange: Mock Groq API ném exception (1 attempt vì maxRetryAttempts=1)
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("Connection timeout"));

        // Act
        String result = llmService.generateExplanation("Glucose", "8.9", "mmol/L", "high");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("đường huyết");
        assertThat(result).contains("bác sĩ");
    }

    @Test
    @DisplayName("Fallback: Groq API thất bại với metric không biết → trả về default message")
    void generateExplanation_whenGroqFailsUnknownMetric_returnsDefaultFallback() {
        // Arrange
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("Rate limit exceeded"));

        // Act
        String result = llmService.generateExplanation("UnknownMetric999", "42", "unit", "unknown");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
    }

    @Test
    @DisplayName("Retry: Thất bại lần 1, thành công lần 2")
    void generateExplanation_failsThenSucceeds_returnsApiResponse() {
        // Arrange: 3 attempts cho test này
        ReflectionTestUtils.setField(llmService, "maxRetryAttempts", 3);
        String expectedExplanation = "HbA1c bình thường, tốt lắm!";

        // Lần 1 thất bại, lần 2 thành công
        when(groqChatClient.prompt())
                .thenThrow(new RuntimeException("Transient error"))
                .thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(expectedExplanation);

        // Act
        String result = llmService.generateExplanation("HbA1c", "6.2", "%", "normal");

        // Assert
        assertThat(result).isEqualTo(expectedExplanation);
        verify(groqChatClient, times(2)).prompt(); // 1 fail + 1 success
    }

    @Test
    @DisplayName("Thành công: Generate explanation cho HbA1c - metric quan trọng")
    void generateExplanation_HbA1c_callsGroqWithCorrectMetric() {
        // Arrange
        String expectedExplanation = "Chỉ số HbA1c của bạn là 6.2%, ở mức bình thường.";
        mockGroqApiSuccess(expectedExplanation);

        // Act
        String result = llmService.generateExplanation("HbA1c", "6.2", "%", "normal");

        // Assert
        assertThat(result).isEqualTo(expectedExplanation);
    }

    // =========================================================
    // buildMedicalPrompt() Tests
    // =========================================================

    @Test
    @DisplayName("Prompt builder: Chứa tên chỉ số, giá trị, đơn vị")
    void buildMedicalPrompt_containsMetricInfo() {
        // Act
        String prompt = llmService.buildMedicalPrompt("Glucose", "5.4", "mmol/L", "normal");

        // Assert
        assertThat(prompt).contains("Glucose");
        assertThat(prompt).contains("5.4");
        assertThat(prompt).contains("mmol/L");
        assertThat(prompt).contains("Bình thường");
    }

    @Test
    @DisplayName("Prompt builder: Status 'high' → tiếng Việt 'Cao hơn mức bình thường'")
    void buildMedicalPrompt_highStatus_translatedToVietnamese() {
        // Act
        String prompt = llmService.buildMedicalPrompt("Cholesterol", "6.5", "mmol/L", "high");

        // Assert
        assertThat(prompt).contains("Cao hơn mức bình thường");
    }

    @Test
    @DisplayName("Prompt builder: Status 'low' → tiếng Việt 'Thấp hơn mức bình thường'")
    void buildMedicalPrompt_lowStatus_translatedToVietnamese() {
        // Act
        String prompt = llmService.buildMedicalPrompt("Hemoglobin", "9.5", "g/dL", "low");

        // Assert
        assertThat(prompt).contains("Thấp hơn mức bình thường");
    }

    @Test
    @DisplayName("Prompt builder: Status 'critical' → tiếng Việt 'Cần chú ý đặc biệt'")
    void buildMedicalPrompt_criticalStatus_translatedToVietnamese() {
        // Act
        String prompt = llmService.buildMedicalPrompt("Potassium", "6.8", "mEq/L", "critical");

        // Assert
        assertThat(prompt).contains("Cần chú ý đặc biệt");
    }

    @Test
    @DisplayName("F4 fix: Prompt builder null-safe khi status = null → không NullPointerException")
    void buildMedicalPrompt_nullStatus_doesNotThrowNPE() {
        // Act — không nên NPE
        String prompt = llmService.buildMedicalPrompt("Glucose", "5.4", "mmol/L", null);

        // Assert
        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("Glucose");
        assertThat(prompt).contains("5.4");
    }

    @Test
    @DisplayName("F4 fix: generateExplanation null-safe khi status = null → trả về kết quả hoặc fallback")
    void generateExplanation_nullStatus_returnsWithoutNPE() {
        // Arrange
        when(groqChatClient.prompt()).thenThrow(new RuntimeException("API error"));

        // Act — không nên NPE
        String result = llmService.generateExplanation("Glucose", "5.4", "mmol/L", null);

        // Assert
        assertThat(result).isNotNull();
    }

    // =========================================================
    // getFallbackExplanation() Tests
    // =========================================================

    @Test
    @DisplayName("Fallback: Glucose → có explanation cụ thể")
    void getFallbackExplanation_glucose_returnsSpecificMessage() {
        // Act
        String result = llmService.getFallbackExplanation("Glucose");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("đường huyết");
    }

    @Test
    @DisplayName("Fallback: HbA1c → có explanation cụ thể")
    void getFallbackExplanation_hba1c_returnsSpecificMessage() {
        // Act
        String result = llmService.getFallbackExplanation("HbA1c");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).contains("3 tháng");
    }

    @Test
    @DisplayName("Fallback: Metric không biết → trả về default explanation")
    void getFallbackExplanation_unknownMetric_returnsDefault() {
        // Act
        String result = llmService.getFallbackExplanation("XYZ_Unknown_Metric");

        // Assert
        assertThat(result).isEqualTo("Kết quả cần được bác sĩ chuyên khoa giải thích thêm.");
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    private void mockGroqApiSuccess(String responseContent) {
        when(groqChatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseContent);
    }
}
