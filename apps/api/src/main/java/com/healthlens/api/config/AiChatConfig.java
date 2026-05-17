package com.healthlens.api.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Provider-neutral AI chat configuration.
 *
 * <p>HealthLens uses Spring AI's OpenAI client path for OpenAI-compatible chat
 * providers. Native providers require an explicit adapter before they can be
 * selected through configuration.
 */
@Configuration
@EnableCaching
public class AiChatConfig {

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

    @Value("${app.ai.chat.provider:openai-compatible}")
    private String primaryProvider;

    @Bean
    public RestClientCustomizer aiChatTimeoutRestClientCustomizer(
            @Value("${app.ai.chat.timeout-ms:30000}") long timeoutMs
    ) {
        Duration timeout = Duration.ofMillis(timeoutMs);
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(timeout, timeout);
        return restClientBuilder -> restClientBuilder.requestFactory(
                ClientHttpRequestFactoryBuilder.detect().build(settings)
        );
    }

    @Bean
    public ChatClient aiChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(DEFAULT_SYSTEM_PROMPT)
                .build();
    }

    public String getPrimaryProvider() {
        return primaryProvider;
    }
}
