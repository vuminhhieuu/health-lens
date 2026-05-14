package com.healthlens.api.dto.response;

import java.util.UUID;

public record AcceptHealthRecordInvitationResultResponse(
        String outcome,
        String redirectUrl,
        UUID recordId,
        UUID profileId
) {
}
