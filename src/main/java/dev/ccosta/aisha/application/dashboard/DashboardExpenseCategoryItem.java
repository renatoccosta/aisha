package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;

public record DashboardExpenseCategoryItem(String categoryName, BigDecimal amount, boolean others) {
}
