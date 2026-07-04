package dev.ccosta.aisha.application.asset;

import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents one calculated step in the asset position memory.
 *
 * @param tradeDate operation trade date used to order the memory
 * @param settlementDate optional settlement date associated with the operation
 * @param operationType operation type applied in this step
 * @param quantityDelta quantity variation caused by the operation, or zero when it does not affect quantity
 * @param cashAmount signed cash effect used for display purposes, or {@code null} when unavailable
 * @param costDelta cost basis variation caused by the operation
 * @param resultingQuantity running quantity after the operation is applied
 * @param resultingTotalCost running total cost after the operation is applied
 * @param resultingAverageCost running average cost after the operation is applied, or {@code null} when quantity is zero
 * @param currency operation currency code
 * @param notes optional operation notes
 */
public record AssetCalculationEntry(
    LocalDate tradeDate,
    LocalDate settlementDate,
    InvestmentOperationType operationType,
    BigDecimal quantityDelta,
    BigDecimal cashAmount,
    BigDecimal costDelta,
    BigDecimal resultingQuantity,
    BigDecimal resultingTotalCost,
    BigDecimal resultingAverageCost,
    String currency,
    String notes
) {
}
