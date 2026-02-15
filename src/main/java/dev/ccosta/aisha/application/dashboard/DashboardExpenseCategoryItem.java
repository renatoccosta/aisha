package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;

public record DashboardExpenseCategoryItem(Long categoryId, String categoryName, BigDecimal amount, boolean hasChildren) {
}
