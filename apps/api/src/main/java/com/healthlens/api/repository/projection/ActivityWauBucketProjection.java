package com.healthlens.api.repository.projection;

import java.time.LocalDate;

public interface ActivityWauBucketProjection {

    LocalDate getPeriodStart();

    long getWau();
}
