package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.service.DataDeletionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * * Public endpoint for account deletion cancellation via email link. * Does
 * NOT require authentication - token is the only credential needed. * Addresses
 * Story 1.6 AC #5: User can cancel deletion request via email link within 72h
 * grace period.
 */
@RestController
@RequestMapping(ApiRoutes.DELETION_CANCELLATION_BASE)
public class DeletionCancellationController {

    private final DataDeletionService dataDeletionService;

    public DeletionCancellationController(DataDeletionService dataDeletionService) {
        this.dataDeletionService = dataDeletionService;
    }

    /**
     * * AC #5: Cancel a deletion request using a public endpoint (no
     * authentication needed). * Called from email link with cancellation token.
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelDeletionViaToken(@RequestParam(name = "token") String cancellationToken) {
        dataDeletionService.cancelDeletionRequest(cancellationToken);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Yêu cầu xóa tài khoản đã được hủy. Tài khoản của bạn đã được khôi phục.")));
    }

    private Map<String, Object> buildResponseBody(Object data) {
        return Map.of("data", data, "meta", Map.of("timestamp", Instant.now().toString(), "requestId", UUID.randomUUID().toString()));
    }
}
