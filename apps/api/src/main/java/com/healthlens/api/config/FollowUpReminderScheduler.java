package com.healthlens.api.config;

import com.healthlens.api.service.FollowUpReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@Configuration
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class FollowUpReminderScheduler {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FollowUpReminderService followUpReminderService;

    public FollowUpReminderScheduler(FollowUpReminderService followUpReminderService) {
        this.followUpReminderService = followUpReminderService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendDueReminderEmails() {
        LocalDate today = LocalDate.now(VN_ZONE);
        log.info("Starting follow-up reminder email job for {}", today);
        int sentCount = followUpReminderService.sendDueReminderEmails(today);
        log.info("Follow-up reminder email job completed, sentCount={}", sentCount);
    }
}
