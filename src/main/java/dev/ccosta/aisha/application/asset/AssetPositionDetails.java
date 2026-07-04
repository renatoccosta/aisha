package dev.ccosta.aisha.application.asset;

import dev.ccosta.aisha.domain.asset.Asset;
import java.util.List;

/**
 * Aggregates the information required by the asset details screen.
 *
 * @param asset asset master data shown on the screen
 * @param currentPosition current calculated position for the asset
 * @param quantityMovements ordered operations that affect asset quantity
 * @param cashEvents ordered operations that only affect cash or cost basis without changing quantity
 */
public record AssetPositionDetails(
    Asset asset,
    AssetCurrentPosition currentPosition,
    List<AssetCalculationEntry> quantityMovements,
    List<AssetCalculationEntry> cashEvents
) {
}
