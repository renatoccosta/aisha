package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;

public record DashboardMetric(BigDecimal currentValue, BigDecimal previousValue, BigDecimal variationPercent) {
}
