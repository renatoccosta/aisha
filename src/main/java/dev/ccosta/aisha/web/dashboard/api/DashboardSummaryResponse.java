package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.domain.account.AccountType;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    DashboardMetricResponse currentBalance,
    DashboardMetricResponse totalExpenses,
    DashboardMetricResponse totalRevenues,
    List<DashboardAccountTypeBalanceResponse> accountTypeBalances
) {
    public record DashboardMetricResponse(BigDecimal currentValue, BigDecimal previousValue, BigDecimal variationPercent) {
    }

    public record DashboardAccountTypeBalanceResponse(AccountType accountType, BigDecimal balance) {
    }
}
