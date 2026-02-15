package dev.ccosta.aisha.web.dashboard.api;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
    DashboardMetricResponse currentBalance,
    DashboardMetricResponse totalExpenses,
    DashboardMetricResponse totalRevenues
) {
    public record DashboardMetricResponse(BigDecimal currentValue, BigDecimal previousValue, BigDecimal variationPercent) {
    }
}
