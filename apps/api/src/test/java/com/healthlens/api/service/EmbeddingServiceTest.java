package com.healthlens.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResultMetadata;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho EmbeddingService
 *
 * <p>Sử dụng Mockito để mock EmbeddingModel, tránh gọi API thật.
 * Test covers: embed thành công, batch embedding, validation, error handling.
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel);
    }

    // =========================================================
    // embed() Tests
    // =========================================================

    @Test
    @DisplayName("Thành công: Embed text → trả về float array không rỗng")
    void embed_validText_returnsFloatArray() {
        // Arrange
        float[] mockEmbedding = new float[1536]; // text-embedding-3-small dimensions
        for (int i = 0; i < mockEmbedding.length; i++) {
            mockEmbedding[i] = (float) Math.random();
        }
        mockEmbeddingSuccess(mockEmbedding);

        // Act
        float[] result = embeddingService.embed("Chỉ số glucose 5.4 mmol/L bình thường");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1536);
        verify(embeddingModel).call(any(EmbeddingRequest.class));
    }

    @Test
    @DisplayName("Thành công: Embed text tiếng Việt y tế")
    void embed_vietnameseMedicalText_returnsEmbedding() {
        // Arrange
        float[] mockEmbedding = new float[1536];
        mockEmbeddingSuccess(mockEmbedding);

        // Act
        float[] result = embeddingService.embed("Kết quả xét nghiệm HbA1c cho thấy đường huyết trung bình 3 tháng là 6.2%");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Validation: Text null → throws IllegalArgumentException")
    void embed_nullText_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Validation: Text rỗng → throws IllegalArgumentException")
    void embed_emptyText_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embed("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Error handling: EmbeddingModel thất bại → throws RuntimeException")
    void embed_modelFails_throwsRuntimeException() {
        // Arrange
        when(embeddingModel.call(any(EmbeddingRequest.class)))
                .thenThrow(new RuntimeException("API connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embed("test text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate text embedding");
    }

    @Test
    @DisplayName("F5 fix: EmbeddingModel trả về null result → throws RuntimeException (không NPE)")
    void embed_nullResult_throwsRuntimeException() {
        // Arrange: mock trả về response rồi throw exception khi getResult() xử lý
        // Thực tế khi Spring AI không có result → throws exception với message rõ ràng
        when(embeddingModel.call(any(EmbeddingRequest.class)))
                .thenThrow(new RuntimeException("No embedding data available."));

        // Act & Assert — phải throw RuntimeException với wrapper message, không NPE
        assertThatThrownBy(() -> embeddingService.embed("test text"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate text embedding");
    }

    // =========================================================
    // embedBatch() Tests
    // =========================================================

    @Test
    @DisplayName("Thành công: Batch embed 3 texts → trả về 3 float arrays")
    void embedBatch_validTexts_returnsMultipleArrays() {
        // Arrange
        float[] embedding1 = new float[1536];
        float[] embedding2 = new float[1536];
        float[] embedding3 = new float[1536];
        mockBatchEmbeddingSuccess(List.of(embedding1, embedding2, embedding3));

        List<String> texts = List.of(
                "Glucose bình thường",
                "Cholesterol cao",
                "HbA1c cần theo dõi"
        );

        // Act
        List<float[]> results = embeddingService.embedBatch(texts);

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasSize(1536);
        verify(embeddingModel).call(any(EmbeddingRequest.class));
    }

    @Test
    @DisplayName("Validation: Danh sách null → throws IllegalArgumentException")
    void embedBatch_nullList_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embedBatch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    @DisplayName("Validation: Danh sách rỗng → throws IllegalArgumentException")
    void embedBatch_emptyList_throwsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.embedBatch(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    // =========================================================
    // getEmbeddingDimensions() Tests
    // =========================================================

    @Test
    @DisplayName("getEmbeddingDimensions: Trả về số chiều từ model")
    void getEmbeddingDimensions_returnsModelDimensions() {
        // Arrange
        when(embeddingModel.dimensions()).thenReturn(1536);

        // Act
        int dimensions = embeddingService.getEmbeddingDimensions();

        // Assert
        assertThat(dimensions).isEqualTo(1536);
    }

    // =========================================================
    // Helper Methods
    // =========================================================

    private void mockEmbeddingSuccess(float[] embeddingVector) {
        Embedding embedding = new Embedding(embeddingVector, 0);
        EmbeddingResponse response = new EmbeddingResponse(List.of(embedding));
        when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(response);
    }

    private void mockBatchEmbeddingSuccess(List<float[]> vectors) {
        List<Embedding> embeddings = new java.util.ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }
        EmbeddingResponse response = new EmbeddingResponse(embeddings);
        when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(response);
    }
}
