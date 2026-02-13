package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardBalanceEvolution(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    BigDecimal openingBalance,
    List<DashboardBalancePoint> points
) {
}
