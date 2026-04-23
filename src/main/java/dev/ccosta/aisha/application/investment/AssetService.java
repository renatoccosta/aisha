package dev.ccosta.aisha.application.investment;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates asset lifecycle operations and validates account ownership for investment instruments.
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;
    private final AccountService accountService;

    public AssetService(
        AssetRepository assetRepository,
        InvestmentOperationRepository investmentOperationRepository,
        AccountService accountService
    ) {
        this.assetRepository = assetRepository;
        this.investmentOperationRepository = investmentOperationRepository;
        this.accountService = accountService;
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
     * Lists assets filtered by account, type, and descriptive text using database pagination.
     *
     * @param accountId optional account identifier
     * @param type optional asset type
     * @param descriptionFilter optional text matched against name, ticker, or issuer
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a filtered page of assets
     */
    @Transactional(readOnly = true)
    public PagedResult<Asset> listPageOrdered(Long accountId, AssetType type, String descriptionFilter, int page, int pageSize) {
        return assetRepository.findPageOrdered(accountId, type, descriptionFilter, page, pageSize);
    }

    /**
     * Lists all assets attached to one account.
     *
     * @param accountId account identifier
     * @return assets ordered by name, ticker, and id
     */
    @Transactional(readOnly = true)
    public List<Asset> listAllByAccountIdOrdered(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account must be informed");
        }
        return assetRepository.findAllByAccountIdOrdered(accountId);
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
     * Creates an asset associated with an existing account.
     *
     * @param asset asset data to persist
     * @param accountId owner investment account identifier
     * @return persisted asset
     */
    @Transactional
    public Asset create(Asset asset, Long accountId) {
        asset.setAccount(resolveAccount(accountId));
        applyDefaults(asset);
        validate(asset);
        return assetRepository.save(asset);
    }

    /**
     * Updates asset descriptive data and account association.
     *
     * @param id asset identifier
     * @param updatedData replacement asset data
     * @param accountId owner investment account identifier
     * @return updated asset
     */
    @Transactional
    public Asset update(Long id, Asset updatedData, Long accountId) {
        Asset existing = findById(id);
        existing.setAccount(resolveAccount(accountId));
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

    private Account resolveAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account must be informed");
        }
        return accountService.findById(accountId);
    }

    private void applyDefaults(Asset asset) {
        asset.setType(asset.getType() == null ? AssetType.OTHER : asset.getType());
        asset.setIndexerType(asset.getIndexerType() == null ? AssetIndexerType.NONE : asset.getIndexerType());
        if (!StringUtils.hasText(asset.getCurrency())) {
            asset.setCurrency("BRL");
        } else {
            asset.setCurrency(asset.getCurrency().trim().toUpperCase());
        }
    }

    private void validate(Asset asset) {
        if (!StringUtils.hasText(asset.getName())) {
            throw new IllegalArgumentException("Asset name must not be blank");
        }
        if (asset.getCurrency().length() != 3) {
            throw new IllegalArgumentException("Asset currency must use a 3-letter ISO code");
        }
    }

    private void ensureAssetIsNotInUse(Long id) {
        if (investmentOperationRepository.existsByAssetId(id)) {
            throw new AssetInUseException(id);
        }
    }
}
