package com.healthlens.api.config;

import com.healthlens.api.service.EmbeddingService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.OptionalInt;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingVectorStoreStartupValidator implements ApplicationRunner {

    private static final Duration QDRANT_VALIDATION_TIMEOUT = Duration.ofSeconds(5);

    private final Environment environment;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        run();
    }

    void run() {
        if (!shouldValidateOnStartup()) {
            log.debug("Skipping embedding/vector startup validation outside strict profiles.");
            return;
        }

        int embeddingDimension = embeddingService.getEmbeddingDimensions();
        int configuredDimension = environment.getProperty(
                "spring.ai.vectorstore.qdrant.vector-dimension",
                Integer.class,
                embeddingDimension
        );
        String collectionName = environment.getProperty(
                "spring.ai.vectorstore.qdrant.collection-name",
                "healthlens"
        );

        if (embeddingDimension != configuredDimension) {
            handleValidationFailure("Embedding/vector dimension mismatch: embedding dimension "
                    + embeddingDimension + " does not match configured Qdrant vector dimension "
                    + configuredDimension + ". Update QDRANT_VECTOR_DIMENSION or reindex the Qdrant collection before deploying.");
            return;
        }

        OptionalInt collectionDimension;
        try {
            Collections.CollectionInfo collectionInfo = qdrantClient
                    .getCollectionInfoAsync(collectionName)
                    .get(QDRANT_VALIDATION_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            collectionDimension = extractVectorDimension(collectionInfo);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            handleValidationFailure("Unable to validate Qdrant collection '" + collectionName
                    + "' during startup: " + ex.getMessage());
            return;
        } catch (Exception ex) {
            handleValidationFailure("Unable to validate Qdrant collection '" + collectionName
                    + "' during startup: " + ex.getMessage());
            return;
        }

        if (collectionDimension.isPresent() && collectionDimension.getAsInt() != embeddingDimension) {
            handleValidationFailure("Embedding/vector dimension mismatch: existing Qdrant collection dimension "
                    + collectionDimension.getAsInt() + " does not match embedding dimension "
                    + embeddingDimension + " for collection '" + collectionName
                    + "'. reindex the collection with the current embedding model before deploying.");
            return;
        }
        if (collectionDimension.isEmpty() && requiresStrictValidation()) {
            handleValidationFailure("Unable to verify Qdrant collection dimension for collection '" + collectionName
                    + "'. Inspect the collection vector configuration or reindex before deploying.");
        }
    }

    private void handleValidationFailure(String message) {
        if (requiresStrictValidation()) {
            throw new IllegalStateException(message);
        }
        log.warn("{} Local/dev startup continues; production and staging fail fast.", message);
    }

    private boolean requiresStrictValidation() {
        return environment.acceptsProfiles(Profiles.of("production", "staging"))
                || environment.getProperty("app.ai.rag.validation.required", Boolean.class, false);
    }

    private boolean shouldValidateOnStartup() {
        return requiresStrictValidation()
                || environment.getProperty("app.ai.rag.validation.local-enabled", Boolean.class, false);
    }

    private static OptionalInt extractVectorDimension(Collections.CollectionInfo collectionInfo) {
        if (!collectionInfo.hasConfig()
                || !collectionInfo.getConfig().hasParams()
                || !collectionInfo.getConfig().getParams().hasVectorsConfig()) {
            return OptionalInt.empty();
        }

        Collections.VectorsConfig vectorsConfig = collectionInfo.getConfig().getParams().getVectorsConfig();
        if (vectorsConfig.hasParams()) {
            return OptionalInt.of(Math.toIntExact(vectorsConfig.getParams().getSize()));
        }
        if (vectorsConfig.hasParamsMap() && !vectorsConfig.getParamsMap().getMapMap().isEmpty()) {
            List<Integer> sizes = vectorsConfig.getParamsMap().getMapMap().values().stream()
                    .map(params -> Math.toIntExact(params.getSize()))
                    .distinct()
                    .toList();
            if (sizes.size() == 1) {
                return OptionalInt.of(sizes.getFirst());
            }
        }
        return OptionalInt.empty();
    }
}
