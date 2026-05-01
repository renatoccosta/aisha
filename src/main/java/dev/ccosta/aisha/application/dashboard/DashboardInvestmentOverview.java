package dev.ccosta.aisha.application.dashboard;

import java.util.List;

/**
 * Groups the investment overview metrics shown in the main dashboard using historical cost only.
 *
 * @param positionCost historical cost still allocated to open positions
 * @param periodNetFlow net investment cash flow inside the selected period
 * @param periodIncome cash income from dividends, interest, coupons, amortizations, and redemptions
 * @param openAssetCount number of assets with open position
 * @param excludedAssetCount number of assets excluded because their currency basis is not safely aggregatable in BRL
 * @param excludedOperationCount number of operations excluded from the period flow because their currency basis is not safely aggregatable in BRL
 * @param allocationsByAssetType historical cost allocation grouped by asset type
 */
public record DashboardInvestmentOverview(
    DashboardMetric positionCost,
    DashboardMetric periodNetFlow,
    DashboardMetric periodIncome,
    int openAssetCount,
    int excludedAssetCount,
    int excludedOperationCount,
    List<DashboardInvestmentAllocation> allocationsByAssetType
) {
}
