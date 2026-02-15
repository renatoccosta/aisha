package dev.ccosta.aisha.web.dashboard.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardExpenseCategoryBreakdownResponse(
    LocalDate startDate,
    LocalDate endDate,
    Long currentParentCategoryId,
    String currentParentCategoryName,
    Long drillUpParentCategoryId,
    List<DashboardExpenseCategoryItemResponse> items
) {
    public record DashboardExpenseCategoryItemResponse(
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        boolean hasChildren
    ) {
    }
}
