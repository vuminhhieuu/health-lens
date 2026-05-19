package com.healthlens.api.dto.response;

import java.util.List;

public record UserAnalyticsResponse(
        long totalUsers,
        List<MonthlyGrowthPoint> monthlyGrowth
) {
    public record MonthlyGrowthPoint(String month, long newUsers) {}
}
