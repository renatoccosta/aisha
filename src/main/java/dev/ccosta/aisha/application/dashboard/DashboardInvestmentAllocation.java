package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;

/**
 * Represents one allocation slice in the investment overview based on historical cost.
 *
 * @param key stable identifier used by the UI
 * @param label user-facing label for the slice
 * @param amount allocated historical cost for the slice
 */
public record DashboardInvestmentAllocation(
    String key,
    String label,
    BigDecimal amount
) {
}
