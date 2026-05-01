package dev.ccosta.aisha.application.investment;

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
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLinkRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
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
