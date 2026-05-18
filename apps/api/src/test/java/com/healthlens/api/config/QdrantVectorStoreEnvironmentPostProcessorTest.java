package com.healthlens.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QdrantVectorStoreEnvironmentPostProcessorTest {

    private final QdrantVectorStoreEnvironmentPostProcessor postProcessor =
            new QdrantVectorStoreEnvironmentPostProcessor();

    @Test
    void postProcessEnvironment_failsProductionWhenQdrantHostContainsProtocol() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "https://cluster.qdrant.io");
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT_HOST must be a hostname without protocol")
                .hasMessageContaining("cluster.qdrant.io");
    }

    @Test
    void postProcessEnvironment_normalizesProtocolHostOutsideStrictProfiles() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "https://cluster.qdrant.io:6333")
                .withProperty("spring.ai.vectorstore.qdrant.port", "6334");

        postProcessor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.ai.vectorstore.qdrant.host"))
                .isEqualTo("cluster.qdrant.io");
        assertThat(environment.getProperty("spring.ai.vectorstore.qdrant.port"))
                .isEqualTo("6333");
    }

    @Test
    void postProcessEnvironment_rejectsHostPortWithoutProtocol() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "cluster.qdrant.io:6334");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT_HOST must be a hostname without protocol, port, path");
    }

    @Test
    void postProcessEnvironment_rejectsUnsupportedProtocol() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "grpc://cluster.qdrant.io");
        environment.setActiveProfiles("production");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT_HOST must be a hostname without protocol");
    }

    @Test
    void postProcessEnvironment_rejectsInvalidQdrantPort() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "cluster.qdrant.io")
                .withProperty("QDRANT_PORT", "99999");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT_PORT must be between 1 and 65535");
    }

    @Test
    void postProcessEnvironment_rejectsOutOfRangePortFromUrlStyleLocalHost() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("QDRANT_HOST", "https://cluster.qdrant.io:99999");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("QDRANT_PORT must be between 1 and 65535");
    }

    @Test
    void springFactories_registersQdrantVectorStoreEnvironmentPostProcessor() {
        assertThat(SpringFactoriesLoader
                .loadFactoryNames(EnvironmentPostProcessor.class, getClass().getClassLoader()))
                .contains(QdrantVectorStoreEnvironmentPostProcessor.class.getName());
    }
}
