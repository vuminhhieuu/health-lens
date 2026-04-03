package com.healthlens.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Groq AI Configuration
 *
 * <p>Cấu hình Spring AI sử dụng Groq API thông qua OpenAI-compatible endpoint.
 * Groq là cloud AI inference service hỗ trợ OpenAI API format.
 *
 * <p>Kiến trúc:
 * <ul>
 *   <li>ChatModel: Groq API (base-url: https://api.groq.com/openai)</li>
 *   <li>EmbeddingModel: OpenAI-compatible embedding provider</li>
 *   <li>ChatClient: Spring AI abstraction với system prompt mặc định</li>
 * </ul>
 *
 * <p>Config tại: application.yml → spring.ai.openai
 */
@Configuration
@EnableCaching
public class GroqAiConfig {

    /**
     * System prompt mặc định cho tất cả chat requests.
     * Hướng dẫn LLM respond bằng tiếng Việt và giữ phong cách thân thiện, dễ hiểu.
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            Bạn là trợ lý sức khỏe thông minh của HealthLens.
            Nhiệm vụ của bạn là giải thích các kết quả xét nghiệm y tế bằng tiếng Việt đơn giản, dễ hiểu.
            Luôn:
            - Sử dụng tiếng Việt thông thường, tránh thuật ngữ y khoa phức tạp
            - Giải thích ngắn gọn (tối đa 3-4 câu)
            - Khuyến khích người dùng tham khảo ý kiến bác sĩ cho kết quả bất thường
            - Không đưa ra chẩn đoán bệnh
            """;

    @Value("${app.ai.primary:groq}")
    private String primaryProvider;

    /**
     * ChatClient bean với system prompt mặc định.
     *
     * <p>ChatModel được auto-configured bởi Spring AI từ application.yml:
     * spring.ai.openai.base-url = https://api.groq.com/openai
     * spring.ai.openai.api-key = ${GROQ_API_KEY}
     *
     * @param chatModel auto-configured OpenAI-compatible chat model (points to Groq)
     * @return ChatClient với system prompt sức khỏe tiếng Việt
     */
    @Bean
    public ChatClient groqChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }

    /**
     * Expose primary provider name cho logging/monitoring.
     *
     * @return tên provider chính (mặc định: "groq")
     */
    public String getPrimaryProvider() {
        return primaryProvider;
    }
}
