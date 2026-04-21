package com.healthlens.api.config;

import com.healthlens.api.service.DataDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for data deletion processing.
 * Processes pending deletion requests that have exceeded their 72-hour grace period.
 */
@Slf4j
@Component
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class DeletionScheduler {

    private final DataDeletionService dataDeletionService;

    public DeletionScheduler(DataDeletionService dataDeletionService) {
        this.dataDeletionService = dataDeletionService;
    }

    /**
     * Process overdue deletion requests every hour.
     * Runs at the top of every hour (00:00, 01:00, 02:00, etc.)
     * AC #2: Find all pending requests > 72h old and delete their data.
     */
    @Scheduled(cron = "0 0 * * * *") // Runs every hour
    public void processDeletionRequests() {
        log.info("Starting scheduled data deletion job");
        try {
            dataDeletionService.processDeletionRequests();
            log.info("Data deletion job completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled data deletion job", e);
            // Don't rethrow - scheduler should continue on error
        }
    }
}
