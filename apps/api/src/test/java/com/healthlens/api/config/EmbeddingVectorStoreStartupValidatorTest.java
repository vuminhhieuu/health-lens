package com.healthlens.api.config;

import com.google.common.util.concurrent.Futures;
import com.healthlens.api.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmbeddingVectorStoreStartupValidatorTest {

    @Test
    void run_failsStrictStartupWhenEmbeddingDimensionDiffersFromConfiguredVectorDimension() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        MockEnvironment environment = strictEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1024");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        assertThatThrownBy(() -> validator.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Embedding/vector dimension mismatch")
                .hasMessageContaining("embedding dimension 1536")
                .hasMessageContaining("configured Qdrant vector dimension 1024")
                .hasMessageContaining("reindex");
    }

    @Test
    void run_failsStrictStartupWhenExistingQdrantCollectionDimensionDiffers() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        MockEnvironment environment = strictEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1536");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);
        when(qdrantClient.getCollectionInfoAsync("healthlens"))
                .thenReturn(Futures.immediateFuture(collectionInfo(1024)));

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        assertThatThrownBy(() -> validator.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Embedding/vector dimension mismatch")
                .hasMessageContaining("existing Qdrant collection dimension 1024")
                .hasMessageContaining("embedding dimension 1536")
                .hasMessageContaining("reindex");
    }

    @Test
    void run_allowsLocalStartupWhenQdrantIsUnavailable() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1536")
                .withProperty("app.ai.rag.validation.local-enabled", "true");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);
        when(qdrantClient.getCollectionInfoAsync("healthlens"))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("qdrant unavailable")));

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        assertThatCode(() -> validator.run()).doesNotThrowAnyException();
    }

    @Test
    void run_failsStrictStartupWhenCollectionDimensionCannotBeExtracted() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        MockEnvironment environment = strictEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1536");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);
        when(qdrantClient.getCollectionInfoAsync("healthlens"))
                .thenReturn(Futures.immediateFuture(Collections.CollectionInfo.newBuilder().build()));

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        assertThatThrownBy(() -> validator.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to verify Qdrant collection dimension")
                .hasMessageContaining("reindex");
    }

    @Test
    void run_failsStrictStartupWhenNamedVectorDimensionsDiffer() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        MockEnvironment environment = strictEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1536");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);
        when(qdrantClient.getCollectionInfoAsync("healthlens"))
                .thenReturn(Futures.immediateFuture(namedVectorCollectionInfo(1536, 1024)));

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        assertThatThrownBy(() -> validator.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unable to verify Qdrant collection dimension");
    }

    @Test
    void run_restoresInterruptWhenCollectionValidationIsInterrupted() throws Exception {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        QdrantClient qdrantClient = mock(QdrantClient.class);
        @SuppressWarnings("unchecked")
        com.google.common.util.concurrent.ListenableFuture<Collections.CollectionInfo> future =
                mock(com.google.common.util.concurrent.ListenableFuture.class);
        MockEnvironment environment = strictEnvironment()
                .withProperty("spring.ai.vectorstore.qdrant.collection-name", "healthlens")
                .withProperty("spring.ai.vectorstore.qdrant.vector-dimension", "1536");

        when(embeddingService.getEmbeddingDimensions()).thenReturn(1536);
        when(qdrantClient.getCollectionInfoAsync("healthlens")).thenReturn(future);
        when(future.get(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        EmbeddingVectorStoreStartupValidator validator =
                new EmbeddingVectorStoreStartupValidator(environment, embeddingService, qdrantClient);

        try {
            assertThatThrownBy(() -> validator.run())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unable to validate Qdrant collection");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private static MockEnvironment strictEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");
        return environment;
    }

    private static Collections.CollectionInfo collectionInfo(int dimension) {
        return Collections.CollectionInfo.newBuilder()
                .setConfig(Collections.CollectionConfig.newBuilder()
                        .setParams(Collections.CollectionParams.newBuilder()
                                .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                                        .setParams(Collections.VectorParams.newBuilder()
                                                .setSize(dimension)))))
                .build();
    }

    private static Collections.CollectionInfo namedVectorCollectionInfo(int firstDimension, int secondDimension) {
        return Collections.CollectionInfo.newBuilder()
                .setConfig(Collections.CollectionConfig.newBuilder()
                        .setParams(Collections.CollectionParams.newBuilder()
                                .setVectorsConfig(Collections.VectorsConfig.newBuilder()
                                        .setParamsMap(Collections.VectorParamsMap.newBuilder()
                                                .putMap("default", vectorParams(firstDimension))
                                                .putMap("legacy", vectorParams(secondDimension))))))
                .build();
    }

    private static Collections.VectorParams vectorParams(int dimension) {
        return Collections.VectorParams.newBuilder()
                .setSize(dimension)
                .build();
    }
}
