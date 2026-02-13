package dev.ccosta.aisha.web.dashboard.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardExpenseCategoryBreakdownResponse(
    LocalDate startDate,
    LocalDate endDate,
    List<DashboardExpenseCategoryItemResponse> items
) {
    public record DashboardExpenseCategoryItemResponse(String categoryName, BigDecimal amount, boolean others) {
    }
}
