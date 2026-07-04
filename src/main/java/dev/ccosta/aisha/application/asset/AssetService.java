package dev.ccosta.aisha.application.asset;

import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetIndexerType;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.asset.AssetType;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates asset lifecycle operations for investment instruments.
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;

    public AssetService(
        AssetRepository assetRepository,
        InvestmentOperationRepository investmentOperationRepository
    ) {
        this.assetRepository = assetRepository;
        this.investmentOperationRepository = investmentOperationRepository;
    }

    /**
     * Lists assets ordered by account, name, ticker, and id using database pagination.
     *
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a page of assets
     */
    @Transactional(readOnly = true)
    public PagedResult<Asset> listPageOrdered(int page, int pageSize) {
        return assetRepository.findPageOrdered(page, pageSize);
    }

    /**
     * Lists assets filtered by type and descriptive text using database pagination.
     *
     * @param type optional asset type
     * @param descriptionFilter optional text matched against name, ticker, or issuer
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a filtered page of assets
     */
    @Transactional(readOnly = true)
    public PagedResult<Asset> listPageOrdered(AssetType type, String descriptionFilter, int page, int pageSize) {
        return assetRepository.findPageOrdered(type, descriptionFilter, page, pageSize);
    }

    /**
     * Finds an asset by id.
     *
     * @param id asset identifier
     * @return the matching asset
     */
    @Transactional(readOnly = true)
    public Asset findById(Long id) {
        return assetRepository.findById(id)
            .orElseThrow(() -> new AssetNotFoundException(id));
    }

    /**
     * Creates an asset.
     *
     * @param asset asset data to persist
     * @return persisted asset
     */
    @Transactional
    public Asset create(Asset asset) {
        applyDefaults(asset);
        validate(asset);
        return assetRepository.save(asset);
    }

    /**
     * Updates asset descriptive data.
     *
     * @param id asset identifier
     * @param updatedData replacement asset data
     * @return updated asset
     */
    @Transactional
    public Asset update(Long id, Asset updatedData) {
        Asset existing = findById(id);
        existing.setType(updatedData.getType());
        existing.setName(updatedData.getName());
        existing.setTicker(updatedData.getTicker());
        existing.setIsin(updatedData.getIsin());
        existing.setIssuer(updatedData.getIssuer());
        existing.setCurrency(updatedData.getCurrency());
        existing.setMaturityDate(updatedData.getMaturityDate());
        existing.setIndexerType(updatedData.getIndexerType());
        existing.setIndexerSpread(updatedData.getIndexerSpread());
        applyDefaults(existing);
        validate(existing);
        return assetRepository.save(existing);
    }

    /**
     * Deletes an asset when it is not referenced by investment operations.
     *
     * @param id asset identifier
     */
    @Transactional
    public void deleteById(Long id) {
        findById(id);
        ensureAssetIsNotInUse(id);
        assetRepository.deleteById(id);
    }

    /**
     * Deletes distinct assets when none is referenced by investment operations.
     *
     * @param ids asset identifiers
     */
    @Transactional
    public void bulkDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        for (Long id : uniqueIds) {
            findById(id);
            ensureAssetIsNotInUse(id);
        }
        assetRepository.deleteByIds(uniqueIds);
    }

    private void applyDefaults(Asset asset) {
        asset.setType(asset.getType() == null ? AssetType.OTHER : asset.getType());
        asset.setIndexerType(asset.getIndexerType() == null ? AssetIndexerType.NONE : asset.getIndexerType());
        if (!StringUtils.hasText(asset.getCurrency())) {
            asset.setCurrency("BRL");
        } else {
            asset.setCurrency(asset.getCurrency().trim().toUpperCase());
        }
        if (hasOpeningPosition(asset)) {
            if (!StringUtils.hasText(asset.getOpeningPositionCurrency())) {
                asset.setOpeningPositionCurrency(asset.getCurrency());
            } else {
                asset.setOpeningPositionCurrency(asset.getOpeningPositionCurrency().trim().toUpperCase());
            }
        }
    }

    private void validate(Asset asset) {
        if (!StringUtils.hasText(asset.getName())) {
            throw new IllegalArgumentException("Asset name must not be blank");
        }
        if (asset.getCurrency().length() != 3) {
            throw new IllegalArgumentException("Asset currency must use a 3-letter ISO code");
        }
        validateOpeningPosition(asset);
    }

    private void ensureAssetIsNotInUse(Long id) {
        if (investmentOperationRepository.existsByAssetId(id)) {
            throw new AssetInUseException(id);
        }
    }

    private void validateOpeningPosition(Asset asset) {
        boolean hasPositionDate = asset.getOpeningPositionDate() != null;
        boolean hasQuantity = asset.getOpeningPositionQuantity() != null;
        boolean hasTotalCost = asset.getOpeningPositionTotalCost() != null;
        boolean hasCurrency = StringUtils.hasText(asset.getOpeningPositionCurrency());

        if (!hasPositionDate && !hasQuantity && !hasTotalCost && !hasCurrency) {
            return;
        }

        if (!hasPositionDate || !hasQuantity || !hasTotalCost || !hasCurrency) {
            throw new IllegalArgumentException("Opening position must include date, quantity, total cost, and currency");
        }
        if (asset.getOpeningPositionCurrency().length() != 3) {
            throw new IllegalArgumentException("Opening position currency must use a 3-letter ISO code");
        }
        if (asset.getOpeningPositionQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Opening position quantity must be greater than zero");
        }
        if (asset.getOpeningPositionTotalCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Opening position total cost must not be negative");
        }
    }

    private boolean hasOpeningPosition(Asset asset) {
        return asset.getOpeningPositionDate() != null
            || asset.getOpeningPositionQuantity() != null
            || asset.getOpeningPositionTotalCost() != null
            || StringUtils.hasText(asset.getOpeningPositionCurrency());
    }
}
