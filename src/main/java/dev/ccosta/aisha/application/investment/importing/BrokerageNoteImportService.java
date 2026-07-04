package dev.ccosta.aisha.application.investment.importing;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.EntryType;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.investment.InvestmentEntryEffectPolicy;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Imports parsed brokerage notes by persisting notes, net entries, assets, and investment operations.
 */
@Service
public class BrokerageNoteImportService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    private final List<BrokerageNoteProcessor> processors;
    private final BrokerageNoteRepository brokerageNoteRepository;
    private final AssetRepository assetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;
    private final EntryRepository entryRepository;
    private final AccountService accountService;
    private final EntryCategorySuggestionService entryCategorySuggestionService;

    public BrokerageNoteImportService(
        List<BrokerageNoteProcessor> processors,
        BrokerageNoteRepository brokerageNoteRepository,
        AssetRepository assetRepository,
        InvestmentOperationRepository investmentOperationRepository,
        EntryRepository entryRepository,
        AccountService accountService,
        EntryCategorySuggestionService entryCategorySuggestionService
    ) {
        this.processors = processors.stream()
            .sorted(Comparator.comparing(processor -> processor.getClass().getName()))
            .toList();
        this.brokerageNoteRepository = brokerageNoteRepository;
        this.assetRepository = assetRepository;
        this.investmentOperationRepository = investmentOperationRepository;
        this.entryRepository = entryRepository;
        this.accountService = accountService;
        this.entryCategorySuggestionService = entryCategorySuggestionService;
    }

    /**
     * Imports brokerage notes parsed from an uploaded file.
     *
     * @param accountId selected account used by the import routine
     * @param originalFileName uploaded file name
     * @param fileHash SHA-256 hash of the uploaded file
     * @param fileContent raw uploaded file bytes
     * @return import summary
     */
    @Transactional
    public BrokerageNoteImportSummary importFile(Long accountId, String originalFileName, String fileHash, byte[] fileContent) {
        Instant startedAt = Instant.now();
        Account account = accountService.findById(accountId);
        BrokerageNoteProcessingRequest request = new BrokerageNoteProcessingRequest(accountId, originalFileName, fileHash, fileContent);
        BrokerageNoteProcessor processor = selectProcessor(request);
        List<ParsedBrokerageNote> parsedNotes = processor.process(request);

        int importedNotes = 0;
        int importedOperations = 0;
        int skippedDuplicateNotes = 0;

        for (ParsedBrokerageNote parsedNote : parsedNotes) {
            if (parsedNote == null || parsedNote.brokerageNote() == null) {
                continue;
            }

            BrokerageNote brokerageNote = parsedNote.brokerageNote();
            applyBrokerageNoteDefaults(brokerageNote, originalFileName, fileHash);
            validateBrokerageNote(brokerageNote);
            if (alreadyImported(brokerageNote)) {
                skippedDuplicateNotes++;
                continue;
            }

            Entry netEntry = saveNetEntry(brokerageNote.getNetEntry(), account);
            brokerageNote.setNetEntry(netEntry);
            BrokerageNote savedNote = brokerageNoteRepository.save(brokerageNote);
            List<InvestmentOperation> operations = new ArrayList<>(parsedNote.operations());
            allocateMissingCosts(savedNote, operations);
            for (InvestmentOperation operation : operations) {
                persistOperation(operation, savedNote, account);
                importedOperations++;
            }
            importedNotes++;
        }

        long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();
        return new BrokerageNoteImportSummary(importedNotes, importedOperations, skippedDuplicateNotes, durationMillis);
    }

    private BrokerageNoteProcessor selectProcessor(BrokerageNoteProcessingRequest request) {
        return processors.stream()
            .filter(processor -> processor.supports(request))
            .findFirst()
            .orElseThrow(() -> new UnsupportedBrokerageNoteFormatException(request.originalFileName()));
    }

    private void applyBrokerageNoteDefaults(BrokerageNote brokerageNote, String originalFileName, String fileHash) {
        if (brokerageNote.getImportedAt() == null) {
            brokerageNote.setImportedAt(LocalDateTime.now());
        }
        if (!StringUtils.hasText(brokerageNote.getOriginalFileName())) {
            brokerageNote.setOriginalFileName(originalFileName);
        }
        if (!StringUtils.hasText(brokerageNote.getFileHash())) {
            brokerageNote.setFileHash(fileHash);
        }
        if (brokerageNote.getTotalCosts() == null) {
            brokerageNote.setTotalCosts(ZERO_MONEY);
        } else {
            brokerageNote.setTotalCosts(money(brokerageNote.getTotalCosts()));
        }
        if (brokerageNote.getNetAmount() != null) {
            brokerageNote.setNetAmount(money(brokerageNote.getNetAmount()));
        }
    }

    private void validateBrokerageNote(BrokerageNote brokerageNote) {
        if (!StringUtils.hasText(brokerageNote.getBrokerName())) {
            throw new IllegalArgumentException("Broker name must be informed");
        }
        if (!StringUtils.hasText(brokerageNote.getBrokerCnpj())) {
            throw new IllegalArgumentException("Broker CNPJ must be informed");
        }
        if (!StringUtils.hasText(brokerageNote.getNoteNumber())) {
            throw new IllegalArgumentException("Brokerage note number must be informed");
        }
        if (brokerageNote.getTradeDate() == null) {
            throw new IllegalArgumentException("Trade date must be informed");
        }
        if (brokerageNote.getSettlementDate() == null) {
            throw new IllegalArgumentException("Settlement date must be informed");
        }
        if (brokerageNote.getNetAmount() == null) {
            throw new IllegalArgumentException("Brokerage note net amount must be informed");
        }
        if (brokerageNote.getNetEntry() == null) {
            throw new IllegalArgumentException("Brokerage note net entry must be informed");
        }
    }

    private boolean alreadyImported(BrokerageNote brokerageNote) {
        return brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate(
            brokerageNote.getBrokerCnpj(),
            brokerageNote.getNoteNumber(),
            brokerageNote.getTradeDate()
        );
    }

    private Entry saveNetEntry(Entry entry, Account account) {
        if (entry.getAccount() == null) {
            entry.setAccount(account);
        }
        if (entry.getEntrySource() == null) {
            entry.setEntrySource(EntrySource.IMPORT);
        }
        if (entry.getRegistrationDate() == null) {
            entry.setRegistrationDate(LocalDate.now());
        }
        if (entry.getEntryType() == null) {
            entry.setEntryType(EntryType.REGULAR);
        }
        entry.setEntryEffect(InvestmentEntryEffectPolicy.resolveBrokerageNoteNetEntry());
        applyCategorySuggestion(entry, account);
        return entryRepository.save(entry);
    }

    private void applyCategorySuggestion(Entry entry, Account account) {
        entryCategorySuggestionService.suggest(new EntryCategorySuggestionRequest(account.getId(), entry.getDescription(), entry.getAmount()))
            .ifPresentOrElse(
                suggestion -> applySuggestedCategory(entry, suggestion),
                () -> {
                    entry.setCategory(null);
                    entry.setSuggestedCategory(null);
                    entry.setCategorySuggestionConfidence(null);
                    entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE);
                }
            );
    }

    private void applySuggestedCategory(Entry entry, EntryCategorySuggestion suggestion) {
        entry.setCategory(suggestion.category());
        entry.setSuggestedCategory(suggestion.category());
        entry.setCategorySuggestionConfidence(suggestion.confidence());
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);
    }

    private void persistOperation(InvestmentOperation operation, BrokerageNote brokerageNote, Account account) {
        Asset asset = resolveOrCreateAsset(operation.getAsset(), account);
        operation.setAsset(asset);
        operation.setAccount(account);
        operation.setBrokerageNote(brokerageNote);
        operation.setSourceType(InvestmentOperationSourceType.BROKER_NOTE);
        if (operation.getTradeDate() == null) {
            operation.setTradeDate(brokerageNote.getTradeDate());
        }
        if (operation.getSettlementDate() == null) {
            operation.setSettlementDate(brokerageNote.getSettlementDate());
        }
        if (!StringUtils.hasText(operation.getCurrency())) {
            operation.setCurrency("BRL");
        } else {
            operation.setCurrency(operation.getCurrency().trim().toUpperCase(Locale.ROOT));
        }
        normalizeImportedOperationAmounts(operation);
        investmentOperationRepository.save(operation);
    }

    private Asset resolveOrCreateAsset(Asset candidate, Account account) {
        if (candidate == null) {
            throw new IllegalArgumentException("Imported operation asset must be informed");
        }

        if (StringUtils.hasText(candidate.getIsin())) {
            return assetRepository.findByIsinIgnoreCase(candidate.getIsin().trim())
                .orElseGet(() -> findByTickerOrNameOrCreate(candidate));
        }
        return findByTickerOrNameOrCreate(candidate);
    }

    private Asset findByTickerOrNameOrCreate(Asset candidate) {
        if (StringUtils.hasText(candidate.getTicker())) {
            return assetRepository.findByTickerIgnoreCase(candidate.getTicker().trim())
                .orElseGet(() -> findByNameOrCreate(candidate));
        }
        return findByNameOrCreate(candidate);
    }

    private Asset findByNameOrCreate(Asset candidate) {
        if (StringUtils.hasText(candidate.getName())) {
            return assetRepository.findByNameIgnoreCase(candidate.getName().trim())
                .orElseGet(() -> createAsset(candidate));
        }
        return createAsset(candidate);
    }

    private Asset createAsset(Asset candidate) {
        if (!StringUtils.hasText(candidate.getName())) {
            if (StringUtils.hasText(candidate.getTicker())) {
                candidate.setName(candidate.getTicker().trim());
            } else if (StringUtils.hasText(candidate.getIsin())) {
                candidate.setName(candidate.getIsin().trim());
            } else {
                throw new IllegalArgumentException("Imported asset name must be informed");
            }
        }
        candidate.setType(candidate.getType() == null ? AssetType.OTHER : candidate.getType());
        candidate.setIndexerType(candidate.getIndexerType() == null ? AssetIndexerType.NONE : candidate.getIndexerType());
        if (!StringUtils.hasText(candidate.getCurrency())) {
            candidate.setCurrency("BRL");
        } else {
            candidate.setCurrency(candidate.getCurrency().trim().toUpperCase(Locale.ROOT));
        }
        return assetRepository.save(candidate);
    }

    private void allocateMissingCosts(BrokerageNote brokerageNote, List<InvestmentOperation> operations) {
        BigDecimal totalCosts = defaultMoney(brokerageNote.getTotalCosts());
        if (totalCosts.compareTo(ZERO_MONEY) <= 0 || operations.isEmpty()) {
            return;
        }

        BigDecimal existingCosts = operations.stream()
            .map(this::operationCosts)
            .reduce(ZERO_MONEY, BigDecimal::add);
        BigDecimal remainingCosts = totalCosts.subtract(existingCosts).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (remainingCosts.compareTo(ZERO_MONEY) <= 0) {
            return;
        }

        List<InvestmentOperation> missingCostOperations = operations.stream()
            .filter(operation -> operationCosts(operation).compareTo(ZERO_MONEY) == 0)
            .toList();
        if (missingCostOperations.isEmpty()) {
            return;
        }

        BigDecimal totalWeight = missingCostOperations.stream()
            .map(this::allocationWeight)
            .reduce(ZERO_MONEY, BigDecimal::add);
        boolean useEqualWeights = totalWeight.compareTo(ZERO_MONEY) <= 0;
        if (useEqualWeights) {
            totalWeight = BigDecimal.valueOf(missingCostOperations.size()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal allocated = ZERO_MONEY;
        for (int index = 0; index < missingCostOperations.size(); index++) {
            InvestmentOperation operation = missingCostOperations.get(index);
            BigDecimal cost;
            if (index == missingCostOperations.size() - 1) {
                cost = remainingCosts.subtract(allocated).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                BigDecimal weight = useEqualWeights ? BigDecimal.ONE : allocationWeight(operation);
                cost = remainingCosts.multiply(weight).divide(totalWeight, MONEY_SCALE, RoundingMode.HALF_UP);
                allocated = allocated.add(cost).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            operation.setFees(cost);
            operation.setTaxes(ZERO_MONEY);
        }
    }

    private void normalizeImportedOperationAmounts(InvestmentOperation operation) {
        if (operation.getFees() != null) {
            operation.setFees(money(operation.getFees()));
        }
        if (operation.getTaxes() != null) {
            operation.setTaxes(money(operation.getTaxes()));
        }
        if (operation.getGrossAmount() == null) {
            return;
        }

        BigDecimal grossAmount = money(operation.getGrossAmount());
        operation.setGrossAmount(grossAmount);
        BigDecimal costs = operationCosts(operation);
        if (operation.getOperationType() == InvestmentOperationType.BUY
            || operation.getOperationType() == InvestmentOperationType.SUBSCRIPTION
            || operation.getOperationType() == InvestmentOperationType.TRANSFER_IN) {
            operation.setNetAmount(grossAmount.add(costs).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
            return;
        }
        if (operation.getOperationType() == InvestmentOperationType.SELL
            || operation.getOperationType() == InvestmentOperationType.REDEMPTION
            || operation.getOperationType() == InvestmentOperationType.TRANSFER_OUT) {
            operation.setNetAmount(grossAmount.subtract(costs).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal operationCosts(InvestmentOperation operation) {
        return defaultMoney(operation.getFees()).add(defaultMoney(operation.getTaxes())).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal allocationWeight(InvestmentOperation operation) {
        if (operation.getGrossAmount() != null && operation.getGrossAmount().compareTo(BigDecimal.ZERO) > 0) {
            return money(operation.getGrossAmount());
        }
        if (operation.getQuantity() != null && operation.getUnitPrice() != null) {
            BigDecimal calculated = operation.getQuantity().multiply(operation.getUnitPrice()).abs();
            if (calculated.compareTo(BigDecimal.ZERO) > 0) {
                return calculated.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
        }
        return ZERO_MONEY;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? ZERO_MONEY : money(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
