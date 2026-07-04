package dev.ccosta.aisha.application.investment.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLinkRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TreasuryDirectImportServiceTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private InvestmentOperationRepository operationRepository;

    @Mock
    private InvestmentOperationEntryLinkRepository linkRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private EntryCategorySuggestionService entryCategorySuggestionService;

    private TreasuryDirectImportService importService;

    @BeforeEach
    void setUp() {
        importService = new TreasuryDirectImportService(
            accountService,
            assetRepository,
            operationRepository,
            linkRepository,
            entryRepository,
            entryCategorySuggestionService
        );
    }

    @Test
    void shouldImportTreasuryDirectOperationsWithEntriesAndAsset() {
        Account account = new Account();
        when(accountService.findById(10L)).thenReturn(account);
        when(assetRepository.findByNameIgnoreCase("Tesouro IPCA+ com Juros Semestrais 2020")).thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operationRepository.existsByExternalId(anyString())).thenReturn(false);
        when(operationRepository.save(any(InvestmentOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TreasuryDirectImportSummary summary = importService.importFile(
            10L,
            "tesouro.json",
            "file-hash",
            sampleJson().getBytes(StandardCharsets.UTF_8)
        );

        assertThat(summary.importedOperations()).isEqualTo(3);
        assertThat(summary.importedEntries()).isEqualTo(3);
        assertThat(summary.skippedDuplicateOperations()).isZero();

        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetRepository).save(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getType()).isEqualTo(AssetType.BOND_GOV);
        assertThat(assetCaptor.getValue().getIndexerType()).isEqualTo(AssetIndexerType.IPCA);
        assertThat(assetCaptor.getValue().getMaturityDate()).isEqualTo(LocalDate.of(2020, 8, 15));

        ArgumentCaptor<InvestmentOperation> operationCaptor = ArgumentCaptor.forClass(InvestmentOperation.class);
        verify(operationRepository, org.mockito.Mockito.times(3)).save(operationCaptor.capture());
        assertThat(operationCaptor.getAllValues()).extracting(InvestmentOperation::getOperationType)
            .containsExactly(InvestmentOperationType.BUY, InvestmentOperationType.FEE, InvestmentOperationType.INTEREST);
        assertThat(operationCaptor.getAllValues().getFirst().getSourceType()).isEqualTo(InvestmentOperationSourceType.TREASURY_DIRECT);
        assertThat(operationCaptor.getAllValues().getFirst().getIndexerSpread()).isEqualByComparingTo("5.800000");
        assertThat(operationCaptor.getAllValues().getFirst().getExternalId()).startsWith("treasury-direct:v1:");

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, org.mockito.Mockito.times(3)).save(entryCaptor.capture());
        assertThat(entryCaptor.getAllValues()).extracting(Entry::getAmount)
            .containsExactly(new BigDecimal("-10007.30"), new BigDecimal("-10.01"), new BigDecimal("285.97"));
        assertThat(entryCaptor.getAllValues().getFirst().getEntryEffect()).isEqualTo(EntryEffect.EQUITY);
        assertThat(entryCaptor.getAllValues().get(1).getEntryEffect()).isEqualTo(EntryEffect.RESULT);
        assertThat(entryCaptor.getAllValues().get(2).getEntryEffect()).isEqualTo(EntryEffect.RESULT);

        verify(linkRepository, org.mockito.Mockito.times(3)).save(any(InvestmentOperationEntryLink.class));
    }

    @Test
    void shouldSkipDuplicateOperationsByExternalId() {
        Account account = new Account();
        Asset asset = new Asset();
        asset.setName("Tesouro IPCA+ com Juros Semestrais 2020");
        when(accountService.findById(10L)).thenReturn(account);
        when(assetRepository.findByNameIgnoreCase("Tesouro IPCA+ com Juros Semestrais 2020")).thenReturn(Optional.of(asset));
        when(operationRepository.existsByExternalId(anyString())).thenReturn(true);

        TreasuryDirectImportSummary summary = importService.importFile(
            10L,
            "tesouro.json",
            "file-hash",
            sampleJson().getBytes(StandardCharsets.UTF_8)
        );

        assertThat(summary.importedOperations()).isZero();
        assertThat(summary.importedEntries()).isZero();
        assertThat(summary.skippedDuplicateOperations()).isEqualTo(3);
        verify(operationRepository, never()).save(any(InvestmentOperation.class));
        verify(entryRepository, never()).save(any(Entry.class));
        verify(linkRepository, never()).save(any(InvestmentOperationEntryLink.class));
    }

    @Test
    void shouldApplySuggestedCategoryToImportedTreasuryDirectEntries() {
        Account account = new Account();
        setId(account, 10L);
        Asset asset = new Asset();
        asset.setName("Tesouro IPCA+ com Juros Semestrais 2020");
        Category category = new Category();
        category.setTitle("Investimentos");
        when(accountService.findById(10L)).thenReturn(account);
        when(assetRepository.findByNameIgnoreCase("Tesouro IPCA+ com Juros Semestrais 2020")).thenReturn(Optional.of(asset));
        when(operationRepository.existsByExternalId(anyString())).thenReturn(false);
        when(operationRepository.save(any(InvestmentOperation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryCategorySuggestionService.suggest(any(EntryCategorySuggestionRequest.class)))
            .thenReturn(Optional.of(new EntryCategorySuggestion(category, 0.91d, "model-v1")));
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        importService.importFile(
            10L,
            "tesouro.json",
            "file-hash",
            sampleJson().getBytes(StandardCharsets.UTF_8)
        );

        ArgumentCaptor<EntryCategorySuggestionRequest> requestCaptor = ArgumentCaptor.forClass(EntryCategorySuggestionRequest.class);
        verify(entryCategorySuggestionService, org.mockito.Mockito.times(3)).suggest(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().getFirst().accountId()).isEqualTo(10L);
        assertThat(requestCaptor.getAllValues().getFirst().description())
            .isEqualTo("Tesouro Direto - Compra - Tesouro IPCA+ com Juros Semestrais 2020");
        assertThat(requestCaptor.getAllValues().getFirst().amount()).isEqualByComparingTo("-10007.30");

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository, org.mockito.Mockito.times(3)).save(entryCaptor.capture());
        assertThat(entryCaptor.getAllValues()).allSatisfy(entry -> {
            assertThat(entry.getCategory()).isSameAs(category);
            assertThat(entry.getSuggestedCategory()).isSameAs(category);
            assertThat(entry.getCategorySuggestionConfidence()).isEqualTo(0.91d);
            assertThat(entry.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.PENDING);
        });
    }

    private String sampleJson() {
        return """
            {
              "formatVersion": 1,
              "source": {
                "url": "https://portalinvestidor.tesourodireto.com.br/MeusInvestimentos/Titulo/386/135/1/2020",
                "dataType": "operations"
              },
              "institution": {
                "code": 386,
                "name": "XP INVESTIMENTOS CORRETORA DE CAMBIO, TITULOS E VALORES MOBI"
              },
              "title": {
                "code": 135,
                "description": "Tesouro IPCA+ com Juros Semestrais 2020",
                "maturityDate": "2020-08-15T00:00:00-03:00"
              },
              "period": {
                "year": 2020,
                "month": 1
              },
              "operations": [
                {
                  "date": "2015-02-04T00:00:00-03:00",
                  "operationType": "purchase",
                  "type": "credit",
                  "quantity": 3.8,
                  "price": 2633.5,
                  "annualRate": 5.8,
                  "amount": 10007.3
                },
                {
                  "date": "2015-02-06T00:00:00-03:00",
                  "operationType": "fee",
                  "type": "debit",
                  "quantity": null,
                  "price": null,
                  "annualRate": null,
                  "amount": 10.01
                },
                {
                  "date": "2015-02-18T00:00:00-03:00",
                  "operationType": "interest",
                  "type": null,
                  "quantity": 3.8,
                  "price": 75.26,
                  "annualRate": null,
                  "amount": 285.97
                }
              ]
            }
            """;
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
