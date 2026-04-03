package com.healthlens.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding Service — Tạo vector embeddings cho RAG (Retrieval Augmented Generation)
 *
 * <p>Service này sử dụng Spring AI {@link EmbeddingModel} để tạo vector representations
 * từ văn bản. Embedding vectors được dùng để:
 * <ul>
 *   <li>Lưu trữ vào Qdrant Cloud vector database</li>
 *   <li>Tìm kiếm semantic similarity cho RAG pipeline</li>
 * </ul>
 *
 * <p>Provider mặc định: OpenAI-compatible API (có thể cấu hình qua environment variables)
 * Default model: text-embedding-3-small (dimensions: 1536)
 *
 * <p>Lưu ý: Groq KHÔNG hỗ trợ embedding API. Cần dùng provider riêng cho embeddings
 * (OpenAI, Jina AI, hoặc self-hosted). Cấu hình qua:
 * - EMBEDDING_BASE_URL (nếu dùng provider khác OpenAI)
 * - EMBEDDING_API_KEY
 * - EMBEDDING_MODEL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    /**
     * Tạo vector embedding từ đoạn văn bản.
     *
     * @param text  đoạn văn bản cần embed
     * @return      float array đại diện cho vector embedding
     * @throws RuntimeException nếu embedding thất bại
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed cannot be null or empty");
        }

        try {
            log.debug("Generating embedding for text of length: {}", text.length());

            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null)
            );

            // F5 fix: null-guard trước khi access result
            if (response.getResult() == null) {
                throw new RuntimeException("Embedding model returned null result for text of length: " + text.length());
            }

            float[] embedding = response.getResult().getOutput();
            log.debug("Embedding generated, dimensions: {}", embedding.length);

            return embedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", e.getMessage());
            throw new RuntimeException("Failed to generate text embedding", e);
        }
    }

    /**
     * Tạo vector embeddings cho nhiều đoạn văn bản (batch).
     *
     * @param texts  danh sách văn bản cần embed
     * @return       danh sách float arrays tương ứng
     * @throws RuntimeException nếu batch embedding thất bại
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Text list cannot be null or empty");
        }

        try {
            log.debug("Generating batch embeddings for {} texts", texts.size());

            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(texts, null)
            );

            List<float[]> embeddings = response.getResults().stream()
                    .map(result -> result.getOutput())
                    .toList();

            log.debug("Batch embeddings generated: {} vectors, dimensions: {}",
                    embeddings.size(),
                    embeddings.isEmpty() ? 0 : embeddings.get(0).length);

            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate batch embeddings: {}", e.getMessage());
            throw new RuntimeException("Failed to generate batch text embeddings", e);
        }
    }

    /**
     * Lấy số chiều (dimensions) của embedding model hiện tại.
     *
     * @return số chiều vector embedding
     */
    public int getEmbeddingDimensions() {
        return embeddingModel.dimensions();
    }
}
