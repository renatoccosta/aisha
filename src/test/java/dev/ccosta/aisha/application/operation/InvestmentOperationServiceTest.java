package dev.ccosta.aisha.application.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryEffect;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLinkRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvestmentOperationServiceTest {

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;

    @Mock
    private InvestmentOperationEntryLinkRepository linkRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private BrokerageNoteRepository brokerageNoteRepository;

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private EntryService entryService;

    @InjectMocks
    private InvestmentOperationService investmentOperationService;

    @Test
    void shouldCreateOperationAndLinkEntries() {
        Asset asset = new Asset();
        Account account = new Account();
        Entry entry = new Entry();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.BUY);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        operation.setCurrency("usd");
        setId(operation, 30L);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(operation)).thenReturn(operation);
        when(entryService.findById(40L)).thenReturn(entry);

        InvestmentOperation created = investmentOperationService.create(
            operation,
            10L,
            20L,
            List.of(new InvestmentOperationEntryLinkRequest(40L, new BigDecimal("125.50")))
        );

        assertThat(created.getAsset()).isSameAs(asset);
        assertThat(created.getAccount()).isSameAs(account);
        assertThat(created.getCurrency()).isEqualTo("USD");
        assertThat(created.getSourceType()).isEqualTo(InvestmentOperationSourceType.MANUAL);
        verify(linkRepository).deleteByOperationId(30L);
        ArgumentCaptor<InvestmentOperationEntryLink> linkCaptor = ArgumentCaptor.forClass(InvestmentOperationEntryLink.class);
        verify(linkRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getOperation()).isSameAs(operation);
        assertThat(linkCaptor.getValue().getEntry()).isSameAs(entry);
        assertThat(linkCaptor.getValue().getAllocatedAmount()).isEqualByComparingTo("125.50");
    }

    @Test
    void shouldSkipDuplicateEntryLinks() {
        Asset asset = new Asset();
        Account account = new Account();
        Entry entry = new Entry();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.DIVIDEND);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        setId(operation, 30L);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(operation)).thenReturn(operation);
        when(entryService.findById(40L)).thenReturn(entry);
        when(linkRepository.save(any(InvestmentOperationEntryLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        investmentOperationService.create(
            operation,
            10L,
            20L,
            List.of(
                new InvestmentOperationEntryLinkRequest(40L, BigDecimal.ONE),
                new InvestmentOperationEntryLinkRequest(40L, BigDecimal.TEN)
            )
        );

        verify(linkRepository).save(any(InvestmentOperationEntryLink.class));
    }

    @Test
    void shouldRejectMultipleDistinctEntryLinks() {
        Asset asset = new Asset();
        Account account = new Account();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.DIVIDEND);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        setId(operation, 30L);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(operation)).thenReturn(operation);

        assertThatThrownBy(() -> investmentOperationService.create(
            operation,
            10L,
            20L,
            List.of(
                new InvestmentOperationEntryLinkRequest(40L, BigDecimal.ONE),
                new InvestmentOperationEntryLinkRequest(41L, BigDecimal.TEN)
            )
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Investment operation can be linked to only one financial entry");

        verify(linkRepository, never()).deleteByOperationId(30L);
        verify(linkRepository, never()).save(any(InvestmentOperationEntryLink.class));
    }

    @Test
    void shouldRejectEntryLinkedToAnotherOperation() {
        Asset asset = new Asset();
        Account account = new Account();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.DIVIDEND);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        setId(operation, 30L);

        InvestmentOperation linkedOperation = new InvestmentOperation();
        setId(linkedOperation, 99L);
        InvestmentOperationEntryLink existingLink = new InvestmentOperationEntryLink();
        existingLink.setOperation(linkedOperation);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(operation)).thenReturn(operation);
        when(linkRepository.findByEntryId(40L)).thenReturn(Optional.of(existingLink));

        assertThatThrownBy(() -> investmentOperationService.create(
            operation,
            10L,
            20L,
            List.of(new InvestmentOperationEntryLinkRequest(40L, BigDecimal.ONE))
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Financial entry is already linked to another investment operation");

        verify(linkRepository).deleteByOperationId(30L);
        verify(linkRepository, never()).save(any(InvestmentOperationEntryLink.class));
    }

    @Test
    void shouldAssociateBrokerageNoteWhenCreatingBrokerNoteOperation() {
        Asset asset = new Asset();
        Account account = new Account();
        BrokerageNote brokerageNote = new BrokerageNote();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.BUY);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        operation.setSourceType(InvestmentOperationSourceType.BROKER_NOTE);
        setId(operation, 30L);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(brokerageNoteRepository.findById(50L)).thenReturn(Optional.of(brokerageNote));
        when(investmentOperationRepository.save(operation)).thenReturn(operation);

        InvestmentOperation created = investmentOperationService.create(operation, 10L, 20L, 50L, List.of());

        assertThat(created.getAccount()).isSameAs(account);
        assertThat(created.getBrokerageNote()).isSameAs(brokerageNote);
        verify(linkRepository).deleteByOperationId(30L);
    }

    @Test
    void shouldUpdateOperationIndexerSpread() {
        Asset asset = new Asset();
        Account account = new Account();
        InvestmentOperation existing = new InvestmentOperation();
        existing.setOperationType(InvestmentOperationType.BUY);
        existing.setTradeDate(LocalDate.of(2026, 4, 20));
        existing.setCurrency("BRL");
        setId(existing, 30L);

        InvestmentOperation updatedData = new InvestmentOperation();
        updatedData.setOperationType(InvestmentOperationType.BUY);
        updatedData.setTradeDate(LocalDate.of(2026, 4, 21));
        updatedData.setCurrency("BRL");
        updatedData.setIndexerSpread(new BigDecimal("0.120000"));

        when(investmentOperationRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(existing)).thenReturn(existing);

        InvestmentOperation updated = investmentOperationService.update(30L, updatedData, 10L, 20L, List.of());

        assertThat(updated.getIndexerSpread()).isEqualByComparingTo("0.120000");
        verify(linkRepository).deleteByOperationId(30L);
    }

    @Test
    void shouldSynchronizeLinkedEntryWhenUpdatingOperation() {
        Asset asset = new Asset();
        asset.setName("Petróleo Brasileiro S.A. PN");
        Account account = new Account();
        Entry linkedEntry = new Entry();
        InvestmentOperation existing = new InvestmentOperation();
        existing.setAsset(asset);
        existing.setAccount(account);
        existing.setOperationType(InvestmentOperationType.SELL);
        existing.setTradeDate(LocalDate.of(2015, 2, 4));
        existing.setSettlementDate(LocalDate.of(2015, 2, 4));
        existing.setNetAmount(new BigDecimal("10007.30"));
        existing.setCurrency("BRL");
        existing.setSourceType(InvestmentOperationSourceType.MANUAL);
        setId(existing, 30L);

        InvestmentOperation updatedData = new InvestmentOperation();
        updatedData.setOperationType(InvestmentOperationType.SELL);
        updatedData.setTradeDate(LocalDate.of(2015, 2, 4));
        updatedData.setSettlementDate(LocalDate.of(2015, 2, 6));
        updatedData.setNetAmount(new BigDecimal("10009.50"));
        updatedData.setCurrency("BRL");
        updatedData.setSourceType(InvestmentOperationSourceType.MANUAL);

        InvestmentOperationEntryLink savedLink = new InvestmentOperationEntryLink();
        savedLink.setOperation(existing);
        savedLink.setEntry(linkedEntry);

        when(investmentOperationRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(existing)).thenReturn(existing);
        when(entryService.findById(40L)).thenReturn(linkedEntry);
        when(linkRepository.save(any(InvestmentOperationEntryLink.class))).thenReturn(savedLink);

        investmentOperationService.update(
            30L,
            updatedData,
            10L,
            20L,
            null,
            List.of(new InvestmentOperationEntryLinkRequest(40L, null))
        );

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository).save(entryCaptor.capture());
        Entry synchronizedEntry = entryCaptor.getValue();
        assertThat(synchronizedEntry.getAccount()).isSameAs(account);
        assertThat(synchronizedEntry.getMovementDate()).isEqualTo(LocalDate.of(2015, 2, 4));
        assertThat(synchronizedEntry.getSettlementDate()).isEqualTo(LocalDate.of(2015, 2, 6));
        assertThat(synchronizedEntry.getDescription()).isEqualTo("Investimento - Venda - Petróleo Brasileiro S.A. PN");
        assertThat(synchronizedEntry.getAmount()).isEqualByComparingTo("10009.50");
        assertThat(synchronizedEntry.getEntrySource()).isEqualTo(EntrySource.IMPORT);
        assertThat(synchronizedEntry.getEntryEffect()).isEqualTo(EntryEffect.EQUITY);
    }

    @Test
    void shouldSynchronizeLinkedIncomeEntryAsResultWhenUpdatingOperation() {
        Asset asset = new Asset();
        asset.setName("Tesouro IPCA+ com Juros Semestrais 2020");
        Account account = new Account();
        Entry linkedEntry = new Entry();
        InvestmentOperation existing = new InvestmentOperation();
        existing.setAsset(asset);
        existing.setAccount(account);
        existing.setOperationType(InvestmentOperationType.INTEREST);
        existing.setTradeDate(LocalDate.of(2015, 2, 18));
        existing.setSettlementDate(LocalDate.of(2015, 2, 18));
        existing.setNetAmount(new BigDecimal("285.97"));
        existing.setCurrency("BRL");
        existing.setSourceType(InvestmentOperationSourceType.TREASURY_DIRECT);
        setId(existing, 30L);

        InvestmentOperation updatedData = new InvestmentOperation();
        updatedData.setOperationType(InvestmentOperationType.INTEREST);
        updatedData.setTradeDate(LocalDate.of(2015, 2, 18));
        updatedData.setSettlementDate(LocalDate.of(2015, 2, 20));
        updatedData.setNetAmount(new BigDecimal("286.10"));
        updatedData.setCurrency("BRL");
        updatedData.setSourceType(InvestmentOperationSourceType.TREASURY_DIRECT);

        InvestmentOperationEntryLink savedLink = new InvestmentOperationEntryLink();
        savedLink.setOperation(existing);
        savedLink.setEntry(linkedEntry);

        when(investmentOperationRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);
        when(investmentOperationRepository.save(existing)).thenReturn(existing);
        when(entryService.findById(40L)).thenReturn(linkedEntry);
        when(linkRepository.save(any(InvestmentOperationEntryLink.class))).thenReturn(savedLink);

        investmentOperationService.update(
            30L,
            updatedData,
            10L,
            20L,
            null,
            List.of(new InvestmentOperationEntryLinkRequest(40L, null))
        );

        ArgumentCaptor<Entry> entryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository).save(entryCaptor.capture());
        Entry synchronizedEntry = entryCaptor.getValue();
        assertThat(synchronizedEntry.getDescription()).isEqualTo("Tesouro Direto - Juros - Tesouro IPCA+ com Juros Semestrais 2020");
        assertThat(synchronizedEntry.getAmount()).isEqualByComparingTo("286.10");
        assertThat(synchronizedEntry.getEntryEffect()).isEqualTo(EntryEffect.RESULT);
    }

    @Test
    void shouldRejectBrokerNoteOperationWithoutBrokerageNote() {
        Asset asset = new Asset();
        Account account = new Account();
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(InvestmentOperationType.BUY);
        operation.setTradeDate(LocalDate.of(2026, 4, 20));
        operation.setSourceType(InvestmentOperationSourceType.BROKER_NOTE);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(accountService.findById(20L)).thenReturn(account);

        assertThatThrownBy(() -> investmentOperationService.create(operation, 10L, 20L, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Brokerage note must be informed for broker note operations");
    }

    @Test
    void shouldListOperationsInsideSettlementRange() {
        LocalDate startDate = LocalDate.of(2026, 4, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 30);
        PagedResult<InvestmentOperation> page = new PagedResult<>(List.of(), 0, 25, 0, 0);

        when(investmentOperationRepository.findPageOrdered(
            startDate,
            endDate,
            "PETR4",
            20L,
            InvestmentOperationType.BUY,
            50L,
            0,
            25
        )).thenReturn(page);

        PagedResult<InvestmentOperation> result = investmentOperationService.listPageOrdered(
            startDate,
            endDate,
            "PETR4",
            20L,
            InvestmentOperationType.BUY,
            50L,
            0,
            25
        );

        assertThat(result).isSameAs(page);
        verify(investmentOperationRepository).findPageOrdered(
            startDate,
            endDate,
            "PETR4",
            20L,
            InvestmentOperationType.BUY,
            50L,
            0,
            25
        );
    }

    private void setId(InvestmentOperation operation, Long id) {
        try {
            var idField = InvestmentOperation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(operation, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
