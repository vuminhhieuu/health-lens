package com.healthlens.api.repository.projection;

import java.time.LocalDate;

public interface UploadQualityBucketProjection {
    LocalDate getBucketDate();

    long getSuccessCount();

    long getFailedCount();
}
