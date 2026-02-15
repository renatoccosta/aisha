package dev.ccosta.aisha.application.dashboard;

import java.time.LocalDate;
import java.util.List;

public record DashboardCategoryTotalsEvolution(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    Long currentParentCategoryId,
    String currentParentCategoryName,
    Long drillUpParentCategoryId,
    List<LocalDate> buckets,
    List<DashboardCategoryTotalsSeries> series
) {
}
