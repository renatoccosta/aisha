package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one bucket in the investment cash flow evolution for the selected period.
 *
 * @param date start date of the bucket
 * @param inflows positive investment cash events such as sales, redemptions, and income
 * @param outflows negative investment cash events such as purchases, subscriptions, fees, and taxes
 * @param netFlow net result of inflows and outflows in the bucket
 */
public record DashboardInvestmentFlowPoint(
    LocalDate date,
    BigDecimal inflows,
    BigDecimal outflows,
    BigDecimal netFlow
) {
}
