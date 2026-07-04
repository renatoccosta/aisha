package dev.ccosta.aisha.application.asset;

import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates the current position and calculation memory for investment assets.
 */
@Service
public class AssetPositionService {

    private static final int MONEY_SCALE = 2;
    private static final int AVERAGE_COST_SCALE = 8;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    private final AssetService assetService;
    private final InvestmentOperationRepository investmentOperationRepository;

    public AssetPositionService(
        AssetService assetService,
        InvestmentOperationRepository investmentOperationRepository
    ) {
        this.assetService = assetService;
        this.investmentOperationRepository = investmentOperationRepository;
    }

    /**
     * Builds the full details payload for one asset, including its current position and calculation memory.
     *
     * @param assetId asset identifier
     * @return details for the requested asset
     */
    @Transactional(readOnly = true)
    public AssetPositionDetails buildDetails(Long assetId) {
        Asset asset = assetService.findById(assetId);
        List<InvestmentOperation> operations = investmentOperationRepository.findAllByAssetIdOrdered(assetId);

        BigDecimal runningQuantity = defaultQuantity(asset.getOpeningPositionQuantity());
        BigDecimal runningTotalCost = defaultMoney(asset.getOpeningPositionTotalCost());
        LocalDate baseDate = asset.getOpeningPositionDate();
        List<AssetCalculationEntry> quantityMovements = new ArrayList<>();
        List<AssetCalculationEntry> cashEvents = new ArrayList<>();

        for (InvestmentOperation operation : operations) {
            CalculationImpact impact = calculateImpact(operation, runningQuantity, runningTotalCost);
            runningQuantity = impact.resultingQuantity();
            runningTotalCost = impact.resultingTotalCost();
            baseDate = operation.getTradeDate();

            AssetCalculationEntry entry = new AssetCalculationEntry(
                operation.getTradeDate(),
                operation.getSettlementDate(),
                operation.getOperationType(),
                impact.quantityDelta(),
                impact.cashAmount(),
                impact.costDelta(),
                runningQuantity,
                runningTotalCost,
                averageCost(runningQuantity, runningTotalCost),
                operation.getCurrency(),
                operation.getNotes()
            );

            if (changesQuantity(operation.getOperationType())) {
                quantityMovements.add(entry);
            } else {
                cashEvents.add(entry);
            }
        }

        AssetCurrentPosition currentPosition = new AssetCurrentPosition(
            runningQuantity,
            runningTotalCost,
            averageCost(runningQuantity, runningTotalCost),
            asset.getCurrency(),
            baseDate
        );

        return new AssetPositionDetails(asset, currentPosition, List.copyOf(quantityMovements), List.copyOf(cashEvents));
    }

    private CalculationImpact calculateImpact(
        InvestmentOperation operation,
        BigDecimal runningQuantity,
        BigDecimal runningTotalCost
    ) {
        InvestmentOperationType type = operation.getOperationType();
        BigDecimal quantity = defaultQuantity(operation.getQuantity());
        BigDecimal quantityDelta = BigDecimal.ZERO;
        BigDecimal cashAmount = signedCashAmount(operation);
        BigDecimal costDelta = ZERO_MONEY;
        BigDecimal resultingQuantity = runningQuantity;
        BigDecimal resultingTotalCost = runningTotalCost;

        if (isQuantityIncrease(type)) {
            quantityDelta = quantity;
            costDelta = switch (type) {
                case BUY, SUBSCRIPTION, TRANSFER_IN -> acquisitionCost(operation);
                case BONUS, SPLIT -> ZERO_MONEY;
                default -> ZERO_MONEY;
            };
            resultingQuantity = runningQuantity.add(quantityDelta);
            resultingTotalCost = runningTotalCost.add(costDelta);
        } else if (isQuantityDecrease(type)) {
            BigDecimal quantityToRemove = quantity.max(BigDecimal.ZERO);
            BigDecimal removableQuantity = quantityToRemove.min(runningQuantity.max(BigDecimal.ZERO));
            quantityDelta = removableQuantity.negate();
            BigDecimal allocatedCost = allocatedCost(removableQuantity, runningQuantity, runningTotalCost);
            costDelta = allocatedCost.negate();
            resultingQuantity = sanitizeQuantity(runningQuantity.add(quantityDelta));
            resultingTotalCost = sanitizeMoney(runningTotalCost.add(costDelta));
            if (resultingQuantity.compareTo(BigDecimal.ZERO) == 0) {
                resultingTotalCost = ZERO_MONEY;
            }
        } else if (type == InvestmentOperationType.AMORTIZATION) {
            BigDecimal reduction = unsignedMoneyAmount(operation);
            costDelta = reduction.negate();
            resultingTotalCost = sanitizeMoney(runningTotalCost.add(costDelta));
        }

        return new CalculationImpact(quantityDelta, cashAmount, costDelta, resultingQuantity, resultingTotalCost);
    }

    private BigDecimal acquisitionCost(InvestmentOperation operation) {
        if (operation.getNetAmount() != null) {
            return operation.getNetAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (operation.getGrossAmount() != null) {
            BigDecimal fees = defaultMoney(operation.getFees());
            BigDecimal taxes = defaultMoney(operation.getTaxes());
            return operation.getGrossAmount()
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .add(fees)
                .add(taxes);
        }
        if (operation.getQuantity() != null && operation.getUnitPrice() != null) {
            BigDecimal baseAmount = operation.getQuantity().multiply(operation.getUnitPrice());
            return baseAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .add(defaultMoney(operation.getFees()))
                .add(defaultMoney(operation.getTaxes()));
        }
        return ZERO_MONEY;
    }

    private BigDecimal allocatedCost(BigDecimal quantity, BigDecimal runningQuantity, BigDecimal runningTotalCost) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0
            || runningQuantity.compareTo(BigDecimal.ZERO) <= 0
            || runningTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_MONEY;
        }
        if (quantity.compareTo(runningQuantity) >= 0) {
            return sanitizeMoney(runningTotalCost);
        }
        BigDecimal averageCost = averageCost(runningQuantity, runningTotalCost);
        if (averageCost == null) {
            return ZERO_MONEY;
        }
        return sanitizeMoney(averageCost.multiply(quantity));
    }

    private BigDecimal signedCashAmount(InvestmentOperation operation) {
        BigDecimal rawAmount = unsignedMoneyAmount(operation);
        if (rawAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return switch (operation.getOperationType()) {
            case BUY, SUBSCRIPTION, TAX, FEE -> rawAmount.negate();
            case SELL, DIVIDEND, INTEREST, AMORTIZATION, COUPON, REDEMPTION -> rawAmount;
            default -> operation.getNetAmount() != null
                ? operation.getNetAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : rawAmount;
        };
    }

    private BigDecimal unsignedMoneyAmount(InvestmentOperation operation) {
        if (operation.getNetAmount() != null) {
            return operation.getNetAmount().abs().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (operation.getGrossAmount() != null) {
            return operation.getGrossAmount().abs().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (operation.getQuantity() != null && operation.getUnitPrice() != null) {
            return operation.getQuantity()
                .multiply(operation.getUnitPrice())
                .abs()
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return ZERO_MONEY;
    }

    private BigDecimal averageCost(BigDecimal quantity, BigDecimal totalCost) {
        if (quantity == null || totalCost == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return totalCost.divide(quantity, AVERAGE_COST_SCALE, RoundingMode.HALF_UP);
    }

    private boolean changesQuantity(InvestmentOperationType type) {
        return isQuantityIncrease(type) || isQuantityDecrease(type);
    }

    private boolean isQuantityIncrease(InvestmentOperationType type) {
        return switch (type) {
            case BUY, BONUS, SPLIT, SUBSCRIPTION, TRANSFER_IN -> true;
            default -> false;
        };
    }

    private boolean isQuantityDecrease(InvestmentOperationType type) {
        return switch (type) {
            case SELL, REDEMPTION, REVERSE_SPLIT, TRANSFER_OUT -> true;
            default -> false;
        };
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? ZERO_MONEY : value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizeMoney(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO_MONEY;
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizeQuantity(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private record CalculationImpact(
        BigDecimal quantityDelta,
        BigDecimal cashAmount,
        BigDecimal costDelta,
        BigDecimal resultingQuantity,
        BigDecimal resultingTotalCost
    ) {
    }
}
