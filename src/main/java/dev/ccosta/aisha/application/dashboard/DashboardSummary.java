package dev.ccosta.aisha.application.dashboard;

import java.util.List;

public record DashboardSummary(
    DashboardMetric currentBalance,
    DashboardMetric totalExpenses,
    DashboardMetric totalRevenues,
    DashboardInvestmentOverview investmentOverview,
    List<DashboardAccountTypeBalance> accountTypeBalances,
    List<DashboardAccountBalance> accountBalances
) {
}
