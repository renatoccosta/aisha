package dev.ccosta.aisha.application.dashboard;

import java.time.LocalDate;
import java.util.List;

public record DashboardExpenseCategoryBreakdown(
    LocalDate startDate,
    LocalDate endDate,
    Long currentParentCategoryId,
    String currentParentCategoryName,
    Long drillUpParentCategoryId,
    List<DashboardExpenseCategoryItem> items
) {
}
