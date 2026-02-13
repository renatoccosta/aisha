package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardSeriesGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardBalanceEvolutionResponse(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    BigDecimal openingBalance,
    List<DashboardBalancePointResponse> points
) {
    public record DashboardBalancePointResponse(LocalDate date, BigDecimal periodAmount, BigDecimal accumulatedBalance) {
    }
}
