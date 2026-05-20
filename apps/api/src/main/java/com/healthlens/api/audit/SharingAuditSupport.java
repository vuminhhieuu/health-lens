package com.healthlens.api.audit;

import com.healthlens.api.correlation.CorrelationContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Canonical audit payloads for profile sharing lifecycle events (invite, accept, revoke, failures).
 */
@Component
@RequiredArgsConstructor
public class SharingAuditSupport {

    private final AuditEventRecorder auditEventRecorder;

    public void recordSuccess(UUID actorId, String action, UUID profileId, SharingAuditFields fields) {
        record(actorId, action, profileId, fields, AuditOutcome.SUCCESS);
    }

    public void recordAccessDenied(
            UUID actorId,
            UUID profileId,
            UUID ownerId,
            UUID viewerId,
            UUID invitationId,
            String inviteeEmail,
            String reason
    ) {
        SharingAuditFields fields = SharingAuditFields.builder()
                .ownerId(ownerId)
                .viewerId(viewerId)
                .invitationId(invitationId)
                .inviteeEmail(inviteeEmail)
                .reason(reason)
                .build();
        recordAccessDeniedEvent(actorId, profileId, fields);
    }

    public void recordAnonymousAccessDenied(UUID profileId, String reason) {
        Map<String, Object> payload = basePayload(null, null, null, null, null, reason, AuditOutcome.FAILURE);
        auditEventRecorder.recordAnonymous(
                AuditActions.PROFILE_SHARE_ACCESS_DENIED_FAILED,
                AuditResourceTypes.PROFILE,
                profileId,
                payload
        );
    }

    private void record(UUID actorId, String action, UUID profileId, SharingAuditFields fields, String outcome) {
        Map<String, Object> payload = sharingPayload(actorId, fields, outcome);
        auditEventRecorder.recordEvent(actorId, action, AuditResourceTypes.PROFILE, profileId, payload);
    }

    private void recordAccessDeniedEvent(UUID actorId, UUID profileId, SharingAuditFields fields) {
        Map<String, Object> payload = sharingPayload(actorId, fields, AuditOutcome.FAILURE);
        auditEventRecorder.recordEventRequiresNew(
                actorId,
                AuditActions.PROFILE_SHARE_ACCESS_DENIED_FAILED,
                AuditResourceTypes.PROFILE,
                profileId,
                payload
        );
    }

    private Map<String, Object> sharingPayload(UUID actorId, SharingAuditFields fields, String outcome) {
        Map<String, Object> payload = basePayload(
                actorId,
                fields.ownerId(),
                fields.viewerId(),
                fields.invitationId(),
                fields.inviteeEmail(),
                fields.reason(),
                outcome
        );
        mergeExtras(payload, fields.extras());
        return payload;
    }

    private static Map<String, Object> basePayload(
            UUID actorId,
            UUID ownerId,
            UUID viewerId,
            UUID invitationId,
            String inviteeEmail,
            String reason,
            String outcome
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (actorId != null) {
            payload.put("actorId", actorId.toString());
        }
        if (ownerId != null) {
            payload.put("ownerId", ownerId.toString());
        }
        if (viewerId != null) {
            payload.put("viewerId", viewerId.toString());
        }
        if (invitationId != null) {
            payload.put("invitationId", invitationId.toString());
        }
        String maskedEmail = AuditPiiMasker.maskEmail(inviteeEmail);
        if (maskedEmail != null) {
            payload.put("inviteeEmailMasked", maskedEmail);
        }
        if (reason != null && !reason.isBlank()) {
            payload.put("reason", reason);
        }
        payload.put("outcome", outcome);
        String correlationId = CorrelationContext.getCorrelationId();
        if (correlationId != null && !correlationId.isBlank()) {
            payload.put("correlationId", correlationId);
        }
        return payload;
    }

    private static void mergeExtras(Map<String, Object> payload, Map<String, Object> extras) {
        if (extras == null || extras.isEmpty()) {
            return;
        }
        extras.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if ("inviteeEmail".equals(key) || "token".equals(key) || "invitationToken".equals(key)) {
                return;
            }
            payload.put(key, value);
        });
    }

    public record SharingAuditFields(
            UUID ownerId,
            UUID viewerId,
            UUID invitationId,
            String inviteeEmail,
            String reason,
            Map<String, Object> extras
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private UUID ownerId;
            private UUID viewerId;
            private UUID invitationId;
            private String inviteeEmail;
            private String reason;
            private final Map<String, Object> extras = new LinkedHashMap<>();

            public Builder ownerId(UUID ownerId) {
                this.ownerId = ownerId;
                return this;
            }

            public Builder viewerId(UUID viewerId) {
                this.viewerId = viewerId;
                return this;
            }

            public Builder invitationId(UUID invitationId) {
                this.invitationId = invitationId;
                return this;
            }

            public Builder inviteeEmail(String inviteeEmail) {
                this.inviteeEmail = inviteeEmail;
                return this;
            }

            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }

            public Builder extra(String key, Object value) {
                if (key != null && value != null) {
                    extras.put(key, value);
                }
                return this;
            }

            public SharingAuditFields build() {
                return new SharingAuditFields(ownerId, viewerId, invitationId, inviteeEmail, reason, Map.copyOf(extras));
            }
        }
    }
}
