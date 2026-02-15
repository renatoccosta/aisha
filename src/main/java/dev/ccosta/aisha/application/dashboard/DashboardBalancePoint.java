package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardBalancePoint(LocalDate date, BigDecimal periodAmount, BigDecimal accumulatedBalance) {
}
