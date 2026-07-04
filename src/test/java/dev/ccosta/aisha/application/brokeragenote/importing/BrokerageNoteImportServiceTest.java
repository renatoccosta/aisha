package dev.ccosta.aisha.application.brokeragenote.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryEffect;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.asset.AssetType;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrokerageNoteImportServiceTest {

    @Mock
    private BrokerageNoteProcessor processor;

    @Mock
    private BrokerageNoteRepository brokerageNoteRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private EntryCategorySuggestionService entryCategorySuggestionService;

    private BrokerageNoteImportService importService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        importService = new BrokerageNoteImportService(
            List.of(processor),
            brokerageNoteRepository,
            assetRepository,
            investmentOperationRepository,
            entryRepository,
            accountService,
            entryCategorySuggestionService
        );
    }

    @Test
    void shouldIgnoreAlreadyImportedNote() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(operation(asset("PETR4"))))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(true);

        BrokerageNoteImportSummary summary = importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        assertThat(summary.importedNotes()).isZero();
        assertThat(summary.importedOperations()).isZero();
        assertThat(summary.skippedDuplicateNotes()).isEqualTo(1);
        verify(entryRepository, never()).save(any(Entry.class));
        verify(investmentOperationRepository, never()).save(any(InvestmentOperation.class));
    }

    @Test
    void shouldResolveAssetByIsinTickerThenNameAndPersistBrokerNoteOperations() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        Entry entry = note.getNetEntry();
        Asset existingByIsin = asset("PETR4");
        existingByIsin.setIsin("BRPETRACNPR6");
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(operation(asset("PETR4"))))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(false);
        when(entryRepository.save(entry)).thenReturn(entry);
        when(brokerageNoteRepository.save(note)).thenReturn(note);
        when(assetRepository.findByIsinIgnoreCase("BRPETRACNPR6")).thenReturn(Optional.of(existingByIsin));

        BrokerageNoteImportSummary summary = importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        assertThat(summary.importedNotes()).isEqualTo(1);
        assertThat(summary.importedOperations()).isEqualTo(1);
        assertThat(entry.getEntryEffect()).isEqualTo(EntryEffect.EQUITY);
        assertThat(entry.getCategory()).isNull();
        ArgumentCaptor<InvestmentOperation> operationCaptor = ArgumentCaptor.forClass(InvestmentOperation.class);
        verify(investmentOperationRepository).save(operationCaptor.capture());
        assertThat(operationCaptor.getValue().getAsset()).isSameAs(existingByIsin);
        assertThat(operationCaptor.getValue().getAccount()).isSameAs(account);
        assertThat(operationCaptor.getValue().getBrokerageNote()).isSameAs(note);
        assertThat(operationCaptor.getValue().getSourceType()).isEqualTo(InvestmentOperationSourceType.BROKER_NOTE);
    }

    @Test
    void shouldApplySuggestedCategoryToImportedBrokerageNoteNetEntry() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        Entry entry = note.getNetEntry();
        entry.setDescription("Nota de corretagem RICO");
        entry.setAmount(new BigDecimal("-100.00"));
        Category category = new Category();
        category.setTitle("Investimentos");
        Asset existingAsset = asset("PETR4");
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(operation(asset("PETR4"))))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(false);
        when(entryCategorySuggestionService.suggest(any(EntryCategorySuggestionRequest.class)))
            .thenReturn(Optional.of(new EntryCategorySuggestion(category, 0.82d, "model-v1")));
        when(entryRepository.save(entry)).thenReturn(entry);
        when(brokerageNoteRepository.save(note)).thenReturn(note);
        when(assetRepository.findByIsinIgnoreCase("BRPETRACNPR6")).thenReturn(Optional.of(existingAsset));

        importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        ArgumentCaptor<EntryCategorySuggestionRequest> requestCaptor = ArgumentCaptor.forClass(EntryCategorySuggestionRequest.class);
        verify(entryCategorySuggestionService).suggest(requestCaptor.capture());
        assertThat(requestCaptor.getValue().accountId()).isEqualTo(10L);
        assertThat(requestCaptor.getValue().description()).isEqualTo("Nota de corretagem RICO");
        assertThat(requestCaptor.getValue().amount()).isEqualByComparingTo("-100.00");
        assertThat(entry.getCategory()).isSameAs(category);
        assertThat(entry.getSuggestedCategory()).isSameAs(category);
        assertThat(entry.getCategorySuggestionConfidence()).isEqualTo(0.82d);
        assertThat(entry.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.PENDING);
    }

    @Test
    void shouldCreateAssetWhenNoMatchingAssetExists() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        Entry entry = note.getNetEntry();
        Asset candidate = asset("XPML11");
        candidate.setType(AssetType.FII);
        candidate.setIsin(null);
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(operation(candidate)))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(false);
        when(entryRepository.save(entry)).thenReturn(entry);
        when(brokerageNoteRepository.save(note)).thenReturn(note);
        when(assetRepository.findByTickerIgnoreCase("XPML11")).thenReturn(Optional.empty());
        when(assetRepository.findByNameIgnoreCase("XPML11")).thenReturn(Optional.empty());
        when(assetRepository.save(candidate)).thenReturn(candidate);

        importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getType()).isEqualTo(AssetType.FII);
        assertThat(assetCaptor.getValue().getCurrency()).isEqualTo("BRL");
    }

    @Test
    void shouldAllocateMissingCostsProportionally() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        note.setTotalCosts(new BigDecimal("30.00"));
        Entry entry = note.getNetEntry();
        Asset existingAsset = asset("PETR4");
        InvestmentOperation first = operation(asset("PETR4"));
        first.setGrossAmount(new BigDecimal("100.00"));
        InvestmentOperation second = operation(asset("PETR4"));
        second.setGrossAmount(new BigDecimal("200.00"));
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(first, second))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(false);
        when(entryRepository.save(entry)).thenReturn(entry);
        when(brokerageNoteRepository.save(note)).thenReturn(note);
        when(assetRepository.findByIsinIgnoreCase("BRPETRACNPR6")).thenReturn(Optional.of(existingAsset));

        importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        assertThat(first.getFees()).isEqualByComparingTo("10.00");
        assertThat(first.getTaxes()).isEqualByComparingTo("0.00");
        assertThat(first.getNetAmount()).isEqualByComparingTo("110.00");
        assertThat(second.getFees()).isEqualByComparingTo("20.00");
        assertThat(second.getTaxes()).isEqualByComparingTo("0.00");
        assertThat(second.getNetAmount()).isEqualByComparingTo("220.00");
    }

    @Test
    void shouldSubtractAllocatedCostsFromSellOperationNetAmount() {
        Account account = account();
        BrokerageNote note = brokerageNote();
        note.setTotalCosts(new BigDecimal("3.00"));
        Entry entry = note.getNetEntry();
        Asset existingAsset = asset("PETR4");
        InvestmentOperation operation = operation(asset("PETR4"));
        operation.setOperationType(InvestmentOperationType.SELL);
        operation.setGrossAmount(new BigDecimal("100.00"));
        when(accountService.findById(10L)).thenReturn(account);
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(true);
        when(processor.process(any(BrokerageNoteProcessingRequest.class))).thenReturn(List.of(new ParsedBrokerageNote(note, List.of(operation))));
        when(brokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate("13.434.335/0001-60", "123", LocalDate.of(2026, 1, 5)))
            .thenReturn(false);
        when(entryRepository.save(entry)).thenReturn(entry);
        when(brokerageNoteRepository.save(note)).thenReturn(note);
        when(assetRepository.findByIsinIgnoreCase("BRPETRACNPR6")).thenReturn(Optional.of(existingAsset));

        importService.importFile(10L, "nota.pdf", "hash", new byte[] {1});

        assertThat(operation.getFees()).isEqualByComparingTo("3.00");
        assertThat(operation.getTaxes()).isEqualByComparingTo("0.00");
        assertThat(operation.getNetAmount()).isEqualByComparingTo("97.00");
    }

    @Test
    void shouldRejectFileWhenNoProcessorSupportsIt() {
        when(accountService.findById(10L)).thenReturn(account());
        when(processor.supports(any(BrokerageNoteProcessingRequest.class))).thenReturn(false);

        assertThatThrownBy(() -> importService.importFile(10L, "nota.ofx", "hash", new byte[] {1}))
            .isInstanceOf(UnsupportedBrokerageNoteFormatException.class)
            .hasMessage("Unsupported brokerage note file format: nota.ofx");
        verify(entryRepository, never()).save(any(Entry.class));
        verify(investmentOperationRepository, never()).save(any(InvestmentOperation.class));
    }

    private Account account() {
        Account account = new Account();
        setId(account, 10L);
        account.setTitle("Investimentos");
        return account;
    }

    private BrokerageNote brokerageNote() {
        BrokerageNote note = new BrokerageNote();
        note.setBrokerName("RICO");
        note.setBrokerCnpj("13.434.335/0001-60");
        note.setNoteNumber("123");
        note.setTradeDate(LocalDate.of(2026, 1, 5));
        note.setSettlementDate(LocalDate.of(2026, 1, 7));
        note.setTotalCosts(BigDecimal.ZERO);
        note.setNetAmount(new BigDecimal("100.00"));
        note.setNetEntry(new Entry());
        return note;
    }

    private InvestmentOperation operation(Asset asset) {
        InvestmentOperation operation = new InvestmentOperation();
        operation.setAsset(asset);
        operation.setOperationType(InvestmentOperationType.BUY);
        operation.setQuantity(BigDecimal.ONE);
        operation.setUnitPrice(new BigDecimal("100.00"));
        operation.setGrossAmount(new BigDecimal("100.00"));
        operation.setCurrency("BRL");
        return operation;
    }

    private Asset asset(String ticker) {
        Asset asset = new Asset();
        asset.setName(ticker);
        asset.setTicker(ticker);
        asset.setIsin("BRPETRACNPR6");
        asset.setCurrency("BRL");
        return asset;
    }

    private void setId(Account account, Long id) {
        try {
            var idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
