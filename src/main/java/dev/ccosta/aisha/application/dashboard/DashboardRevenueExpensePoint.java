package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardRevenueExpensePoint(LocalDate date, BigDecimal revenues, BigDecimal expenses) {
}
