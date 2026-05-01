package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.domain.account.AccountType;
import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    DashboardMetricResponse currentBalance,
    DashboardMetricResponse totalExpenses,
    DashboardMetricResponse totalRevenues,
    DashboardInvestmentOverviewResponse investmentOverview,
    List<DashboardAccountTypeBalanceResponse> accountTypeBalances,
    List<DashboardAccountBalanceResponse> accountBalances
) {
    public record DashboardMetricResponse(BigDecimal currentValue, BigDecimal previousValue, BigDecimal variationPercent) {
    }

    public record DashboardAccountTypeBalanceResponse(AccountType accountType, BigDecimal balance) {
    }

    public record DashboardAccountBalanceResponse(
        Long accountId,
        String accountTitle,
        AccountType accountType,
        BigDecimal balance
    ) {
    }

    public record DashboardInvestmentOverviewResponse(
        DashboardMetricResponse positionCost,
        DashboardMetricResponse periodNetFlow,
        DashboardMetricResponse periodIncome,
        int openAssetCount,
        int excludedAssetCount,
        int excludedOperationCount,
        List<DashboardInvestmentAllocationResponse> allocationsByAssetType
    ) {
    }

    public record DashboardInvestmentAllocationResponse(
        String key,
        String label,
        BigDecimal amount
    ) {
    }
}
