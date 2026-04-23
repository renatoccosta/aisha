package dev.ccosta.aisha.application.investment;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLinkRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates investment operations and their optional links to financial entries.
 */
@Service
public class InvestmentOperationService {

    private final InvestmentOperationRepository investmentOperationRepository;
    private final InvestmentOperationEntryLinkRepository linkRepository;
    private final AssetRepository assetRepository;
    private final EntryService entryService;

    public InvestmentOperationService(
        InvestmentOperationRepository investmentOperationRepository,
        InvestmentOperationEntryLinkRepository linkRepository,
        AssetRepository assetRepository,
        EntryService entryService
    ) {
        this.investmentOperationRepository = investmentOperationRepository;
        this.linkRepository = linkRepository;
        this.assetRepository = assetRepository;
        this.entryService = entryService;
    }

    /**
     * Lists investment operations ordered by trade date and id using database pagination.
     *
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a page of operations
     */
    @Transactional(readOnly = true)
    public PagedResult<InvestmentOperation> listPageOrdered(int page, int pageSize) {
        return investmentOperationRepository.findPageOrdered(page, pageSize);
    }

    /**
     * Lists investment operations filtered by asset text, account, and operation type.
     *
     * @param assetFilter optional text matched against asset name or ticker
     * @param accountId optional account identifier
     * @param operationType optional operation type
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a filtered page of operations
     */
    @Transactional(readOnly = true)
    public PagedResult<InvestmentOperation> listPageOrdered(
        LocalDate startDate,
        LocalDate endDate,
        String assetFilter,
        Long accountId,
        InvestmentOperationType operationType,
        int page,
        int pageSize
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }
        return investmentOperationRepository.findPageOrdered(startDate, endDate, assetFilter, accountId, operationType, page, pageSize);
    }

    /**
     * Finds an investment operation by id.
     *
     * @param id operation identifier
     * @return the matching operation
     */
    @Transactional(readOnly = true)
    public InvestmentOperation findById(Long id) {
        return investmentOperationRepository.findById(id)
            .orElseThrow(() -> new InvestmentOperationNotFoundException(id));
    }

    /**
     * Creates an investment operation and optional links to financial entries.
     *
     * @param operation operation data
     * @param assetId related asset identifier
     * @param links optional financial entry links
     * @return persisted operation
     */
    @Transactional
    public InvestmentOperation create(
        InvestmentOperation operation,
        Long assetId,
        Collection<InvestmentOperationEntryLinkRequest> links
    ) {
        operation.setAsset(resolveAsset(assetId));
        applyDefaults(operation);
        validate(operation);
        InvestmentOperation created = investmentOperationRepository.save(operation);
        replaceLinks(created, links);
        return created;
    }

    /**
     * Updates an investment operation and replaces its financial entry links.
     *
     * @param id operation identifier
     * @param updatedData replacement operation data
     * @param assetId related asset identifier
     * @param links replacement financial entry links
     * @return updated operation
     */
    @Transactional
    public InvestmentOperation update(
        Long id,
        InvestmentOperation updatedData,
        Long assetId,
        Collection<InvestmentOperationEntryLinkRequest> links
    ) {
        InvestmentOperation existing = findById(id);
        existing.setAsset(resolveAsset(assetId));
        existing.setOperationType(updatedData.getOperationType());
        existing.setTradeDate(updatedData.getTradeDate());
        existing.setSettlementDate(updatedData.getSettlementDate());
        existing.setQuantity(updatedData.getQuantity());
        existing.setUnitPrice(updatedData.getUnitPrice());
        existing.setGrossAmount(updatedData.getGrossAmount());
        existing.setNetAmount(updatedData.getNetAmount());
        existing.setFees(updatedData.getFees());
        existing.setTaxes(updatedData.getTaxes());
        existing.setCurrency(updatedData.getCurrency());
        existing.setNotes(updatedData.getNotes());
        existing.setSourceType(updatedData.getSourceType());
        applyDefaults(existing);
        validate(existing);
        InvestmentOperation updated = investmentOperationRepository.save(existing);
        replaceLinks(updated, links);
        return updated;
    }

    /**
     * Lists financial entry links for an operation.
     *
     * @param operationId operation identifier
     * @return links ordered by id
     */
    @Transactional(readOnly = true)
    public List<InvestmentOperationEntryLink> listLinksByOperationId(Long operationId) {
        return linkRepository.findAllByOperationId(operationId);
    }

    /**
     * Deletes an operation and its financial entry associations.
     *
     * @param id operation identifier
     */
    @Transactional
    public void deleteById(Long id) {
        findById(id);
        linkRepository.deleteByOperationId(id);
        investmentOperationRepository.deleteById(id);
    }

    /**
     * Deletes distinct operations and their financial entry associations.
     *
     * @param ids operation identifiers
     */
    @Transactional
    public void bulkDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        for (Long id : uniqueIds) {
            findById(id);
        }
        linkRepository.deleteByOperationIds(uniqueIds);
        investmentOperationRepository.deleteByIds(uniqueIds);
    }

    private Asset resolveAsset(Long assetId) {
        if (assetId == null) {
            throw new IllegalArgumentException("Asset must be informed");
        }
        return assetRepository.findById(assetId)
            .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    private void applyDefaults(InvestmentOperation operation) {
        operation.setSourceType(operation.getSourceType() == null ? InvestmentOperationSourceType.MANUAL : operation.getSourceType());
        if (!StringUtils.hasText(operation.getCurrency())) {
            operation.setCurrency("BRL");
        } else {
            operation.setCurrency(operation.getCurrency().trim().toUpperCase());
        }
    }

    private void validate(InvestmentOperation operation) {
        if (operation.getOperationType() == null) {
            throw new IllegalArgumentException("Operation type must be informed");
        }
        if (operation.getTradeDate() == null) {
            throw new IllegalArgumentException("Trade date must be informed");
        }
        if (operation.getCurrency().length() != 3) {
            throw new IllegalArgumentException("Operation currency must use a 3-letter ISO code");
        }
    }

    private void replaceLinks(InvestmentOperation operation, Collection<InvestmentOperationEntryLinkRequest> links) {
        linkRepository.deleteByOperationId(operation.getId());
        if (links == null || links.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> linkedEntryIds = new LinkedHashSet<>();
        for (InvestmentOperationEntryLinkRequest request : links) {
            if (request == null || request.entryId() == null || !linkedEntryIds.add(request.entryId())) {
                continue;
            }
            Entry entry = entryService.findById(request.entryId());
            InvestmentOperationEntryLink link = new InvestmentOperationEntryLink();
            link.setOperation(operation);
            link.setEntry(entry);
            link.setAllocatedAmount(request.allocatedAmount());
            linkRepository.save(link);
        }
    }
}
