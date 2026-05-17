package com.healthlens.api.repository.projection;

import java.time.LocalDate;

public interface UploadFailureBreakdownProjection {
    LocalDate getBucketDate();

    String getFailureReason();

    long getFailureCount();
}
