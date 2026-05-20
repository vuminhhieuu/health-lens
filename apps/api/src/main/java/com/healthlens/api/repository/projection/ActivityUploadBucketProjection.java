package com.healthlens.api.repository.projection;

import java.time.LocalDate;

public interface ActivityUploadBucketProjection {

    LocalDate getPeriodStart();

    long getUploadCount();

    long getRetryCount();
}
