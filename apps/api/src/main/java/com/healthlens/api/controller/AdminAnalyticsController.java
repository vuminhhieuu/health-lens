package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.response.UploadHistoryPageResponse;
import com.healthlens.api.dto.response.UploadQualityResponse;
import com.healthlens.api.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.ADMIN_ANALYTICS_BASE)
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping(ApiRoutes.ADMIN_ANALYTICS_UPLOAD_QUALITY_REL)
    public ResponseEntity<Map<String, Object>> getUploadQuality(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        Instant fromInstant = from != null
                ? from.atStartOfDay().toInstant(ZoneOffset.UTC)
                : AnalyticsService.defaultFrom();
        Instant toExclusive = to != null
                ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : AnalyticsService.defaultToExclusive();

        if (!fromInstant.isBefore(toExclusive)) {
            throw new IllegalArgumentException("Thời điểm bắt đầu phải nhỏ hơn thời điểm kết thúc");
        }

        UploadQualityResponse response = analyticsService.getUploadQuality(fromInstant, toExclusive, granularity);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping(ApiRoutes.ADMIN_ANALYTICS_UPLOAD_HISTORY_REL)
    public ResponseEntity<Map<String, Object>> getUploadHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String failureReason,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        Instant fromInstant = from != null
                ? from.atStartOfDay().toInstant(ZoneOffset.UTC)
                : AnalyticsService.defaultFrom();
        Instant toExclusive = to != null
                ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : AnalyticsService.defaultToExclusive();

        if (!fromInstant.isBefore(toExclusive)) {
            throw new IllegalArgumentException("Thời điểm bắt đầu phải nhỏ hơn thời điểm kết thúc");
        }

        UploadHistoryPageResponse response = analyticsService.getUploadHistory(
                fromInstant, toExclusive, status, failureReason, page, limit);
        return ResponseEntity.ok(Map.of("data", response));
    }
}
