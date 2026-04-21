package com.healthlens.api.config;

import com.healthlens.api.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StorageBucketInitializer {

    private final StorageService storageService;
    private final Environment environment;
    private final boolean checkOnStartup;
    private final boolean autoCreateBucket;
    private final boolean configureCors;
    private final List<String> corsAllowedOrigins;

    public StorageBucketInitializer(
            StorageService storageService,
            Environment environment,
            @Value("${app.storage.bucket-check-on-startup:true}") boolean checkOnStartup,
            @Value("${app.storage.bucket-auto-create:false}") boolean autoCreateBucket,
            @Value("${app.storage.cors-configure:false}") boolean configureCors,
            @Value("${app.storage.cors-allowed-origins:http://localhost:3000}") String corsAllowedOrigins) {
        this.storageService = storageService;
        this.environment = environment;
        this.checkOnStartup = checkOnStartup;
        this.autoCreateBucket = autoCreateBucket;
        this.configureCors = configureCors;
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStorageBucket() {
        if (!checkOnStartup) {
            return;
        }

        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "production".equalsIgnoreCase(profile));

        if (isProduction && autoCreateBucket) {
            throw new IllegalStateException("app.storage.bucket-auto-create must be disabled in production");
        }

        storageService.ensureBucketExists(autoCreateBucket);
        if (configureCors) {
            storageService.configureBucketCors(corsAllowedOrigins);
        }
    }
}
