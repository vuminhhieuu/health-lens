package com.healthlens.api.repository.projection;

import java.time.LocalDate;

public interface MonthlyUserGrowthProjection {
    LocalDate getMonthStart();

    long getNewUsers();
}
