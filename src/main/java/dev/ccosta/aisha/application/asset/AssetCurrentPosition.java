package dev.ccosta.aisha.application.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents the calculated current position snapshot for one asset.
 *
 * @param quantity resulting quantity held after applying the opening position and operations
 * @param totalCost resulting total acquisition cost still allocated to the held quantity
 * @param averageCost resulting average acquisition cost per unit, or {@code null} when quantity is zero
 * @param currency currency code used to present the position values
 * @param baseDate latest reference date used in the calculation, or {@code null} when no baseline exists
 */
public record AssetCurrentPosition(
    BigDecimal quantity,
    BigDecimal totalCost,
    BigDecimal averageCost,
    String currency,
    LocalDate baseDate
) {
}
