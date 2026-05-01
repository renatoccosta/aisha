package dev.ccosta.aisha.application.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregates the investment cash flow series displayed in the dashboard.
 *
 * @param startDate filter start date
 * @param endDate filter end date
 * @param granularity effective chart granularity
 * @param points ordered buckets in the selected period
 */
public record DashboardInvestmentFlowEvolution(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    List<DashboardInvestmentFlowPoint> points
) {
}
