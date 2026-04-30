package com.healthlens.api.dto.response;

import java.util.UUID;

public record AcceptInvitationResultResponse(
        String outcome,
        String redirectUrl,
        UUID profileId
) {
}
