package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardSeriesGranularity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardCategoryTotalsEvolutionResponse(
    LocalDate startDate,
    LocalDate endDate,
    DashboardSeriesGranularity granularity,
    Long currentParentCategoryId,
    String currentParentCategoryName,
    Long drillUpParentCategoryId,
    List<LocalDate> buckets,
    List<DashboardCategoryTotalsSeriesResponse> series
) {
    public record DashboardCategoryTotalsSeriesResponse(
        Long categoryId,
        String categoryName,
        boolean hasChildren,
        List<BigDecimal> values
    ) {
    }
}
