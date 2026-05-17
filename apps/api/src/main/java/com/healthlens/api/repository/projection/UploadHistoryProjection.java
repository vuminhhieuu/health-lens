package com.healthlens.api.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface UploadHistoryProjection {

    UUID getRecordId();

    UUID getUserId();

    String getUserEmail();

    String getUserFullName();

    UUID getProfileId();

    String getProfileDisplayName();

    String getStatus();

    String getFailureReason();

    Instant getCreatedAt();

    String getHospitalName();

    String getRecordType();
}
