package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardSeriesGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardRevenueExpenseEvolutionResponse(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    List<DashboardRevenueExpensePointResponse> points
) {
    public record DashboardRevenueExpensePointResponse(LocalDate date, BigDecimal revenues, BigDecimal expenses) {
    }
}
