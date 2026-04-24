package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardSeriesGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardInvestmentFlowEvolutionResponse(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    List<DashboardInvestmentFlowPointResponse> points
) {
    public record DashboardInvestmentFlowPointResponse(
        LocalDate date,
        BigDecimal inflows,
        BigDecimal outflows,
        BigDecimal netFlow
    ) {
    }
}
