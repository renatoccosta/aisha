package dev.ccosta.aisha.application.dashboard;

import java.time.LocalDate;
import java.util.List;

public record DashboardRevenueExpenseEvolution(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    List<DashboardRevenueExpensePoint> points
) {
}
