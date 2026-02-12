package dev.ccosta.aisha.application.category;

import java.time.LocalDate;
import java.util.List;

public record CategoryBalanceReport(
    LocalDate startDate,
    LocalDate endDate,
    CategoryBalanceGranularity granularity,
    List<CategoryBalanceBucket> buckets,
    List<CategoryBalanceRow> rows
) {
}
