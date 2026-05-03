package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.investment.AssetService;
import dev.ccosta.aisha.application.investment.InvestmentOperationEntryLinkRequest;
import dev.ccosta.aisha.application.investment.InvestmentOperationService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class InvestmentOperationControllerTest {

    @Mock
    private InvestmentOperationService operationService;

    @Mock
    private AssetService assetService;

    @Mock
    private AccountService accountService;

    @Mock
    private EntryService entryService;

    @Mock
    private EntryLinkedOperationPrefillBuilder entryLinkedOperationPrefillBuilder;

    @InjectMocks
    private InvestmentOperationController operationController;

    @Test
    void shouldCreateOperationFromFormWithLinkedEntry() {
        InvestmentOperationForm form = baseForm();
        form.setLinkedEntryIds(List.of(30L));

        String view = operationController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "/investments/operations?page=2&size=50",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/operations?page=2&size=50");
        ArgumentCaptor<InvestmentOperation> operationCaptor = ArgumentCaptor.forClass(InvestmentOperation.class);
        ArgumentCaptor<Collection<InvestmentOperationEntryLinkRequest>> linksCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(operationService).create(operationCaptor.capture(), eq(10L), eq(20L), linksCaptor.capture());
        assertThat(operationCaptor.getValue().getOperationType()).isEqualTo(InvestmentOperationType.BUY);
        assertThat(operationCaptor.getValue().getTradeDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(operationCaptor.getValue().getNetAmount()).isEqualByComparingTo("125.50");
        assertThat(linksCaptor.getValue()).extracting(InvestmentOperationEntryLinkRequest::entryId).containsExactly(30L);
    }

    @Test
    void shouldFallbackToOperationListingWhenReturnPathIsUnsafe() {
        InvestmentOperationForm form = baseForm();

        String view = operationController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "https://evil.example/investments/operations",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/operations");
    }

    @Test
    void shouldCreateNewOtherAssetWhenNewAssetOptionIsSelected() {
        InvestmentOperationForm form = baseForm();
        form.setAssetId(-1L);
        form.setNewAssetName(" Debênture XP ");
        Asset createdAsset = assetWithId(99L);
        when(assetService.create(any(Asset.class))).thenReturn(createdAsset);

        String view = operationController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            null,
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/investments/operations");
        ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
        verify(assetService).create(assetCaptor.capture());
        assertThat(assetCaptor.getValue().getName()).isEqualTo("Debênture XP");
        assertThat(assetCaptor.getValue().getType()).isEqualTo(AssetType.OTHER);
        verify(operationService).create(any(InvestmentOperation.class), eq(99L), eq(20L), any());
    }

    @Test
    void shouldRejectBlankNewAssetName() {
        InvestmentOperationForm form = baseForm();
        form.setAssetId(-1L);
        form.setNewAssetName(" ");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 5, 20),
            20L,
            null,
            null,
            false,
            false,
            0,
            100
        )).thenReturn(new PagedResult<>(List.of(), 0, 100, 0, 0));
        when(assetService.listPageOrdered(0, 100)).thenReturn(new PagedResult<>(List.of(), 0, 100, 0, 0));

        String view = operationController.create(form, bindingResult, null, new ConcurrentModel());

        assertThat(view).isEqualTo("investments/operations/form");
        assertThat(bindingResult.getFieldError("newAssetName")).isNotNull();
        verify(assetService, never()).create(any(Asset.class));
        verify(operationService, never()).create(any(), any(), any(), any());
    }

    @Test
    void shouldRefreshEntryCandidatesFromAccountAndDate() {
        InvestmentOperationForm form = baseForm();
        Entry entry = new Entry();
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 5, 20),
            20L,
            null,
            null,
            false,
            false,
            0,
            100
        )).thenReturn(new PagedResult<>(List.of(entry), 0, 100, 1, 1));

        ConcurrentModel model = new ConcurrentModel();
        String view = operationController.entryCandidatesFragment(form, model);

        assertThat(view).isEqualTo("investments/operations/form :: entryLinkSelection");
        assertThat(model.getAttribute("form")).isSameAs(form);
        assertThat(model.getAttribute("entryCandidates")).isEqualTo(List.of(entry));
    }

    @Test
    void shouldOpenCreateFormPrefilledFromEntry() {
        Entry entry = entryWithId(30L, accountWithId(20L));
        InvestmentOperationForm form = baseForm();
        form.setLinkedEntryIds(List.of(30L));
        Asset asset = assetWithId(10L);
        when(entryService.findById(30L)).thenReturn(entry);
        when(assetService.listPageOrdered(0, 100))
            .thenReturn(new PagedResult<>(List.of(asset), 0, 100, 1, 1));
        when(entryLinkedOperationPrefillBuilder.build(entry, List.of(asset), -1L)).thenReturn(form);
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(
            LocalDate.of(2026, 3, 21),
            LocalDate.of(2026, 5, 20),
            20L,
            null,
            null,
            false,
            false,
            0,
            100
        )).thenReturn(new PagedResult<>(List.of(), 0, 100, 0, 0));

        ConcurrentModel model = new ConcurrentModel();
        String view = operationController.createFormFromEntry(30L, "/entries?page=1", model);

        assertThat(view).isEqualTo("investments/operations/form");
        assertThat(model.getAttribute("form")).isSameAs(form);
        assertThat(model.getAttribute("returnTo")).isEqualTo("/entries?page=1");
        assertThat(model.getAttribute("entryCandidates")).isEqualTo(List.of(entry));
    }

    @Test
    void shouldRedirectWhenTryingToPrefillOperationFromTransferEntry() {
        Entry entry = entryWithId(30L, accountWithId(20L));
        entry.setEntryType(dev.ccosta.aisha.domain.entry.EntryType.TRANSFER);
        when(entryService.findById(30L)).thenReturn(entry);

        String view = operationController.createFormFromEntry(30L, "/entries?page=1", new ConcurrentModel());

        assertThat(view).isEqualTo("redirect:/entries?page=1");
        verify(entryLinkedOperationPrefillBuilder, never()).build(any(), any(), any());
    }

    @Test
    void shouldBulkDeleteAndRefreshListingForHtmx() {
        DateFilterState globalDateFilter = baseDateFilter();
        when(operationService.listPageOrdered(globalDateFilter.getStartDate(), globalDateFilter.getEndDate(), null, null, null, null, 0, 25))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        String view = operationController.bulkDelete(
            globalDateFilter,
            List.of(1L, 2L),
            null,
            null,
            null,
            null,
            null,
            null,
            htmxRequest(),
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("investments/operations/list :: table");
        verify(operationService).bulkDelete(List.of(1L, 2L));
        verify(operationService).listPageOrdered(globalDateFilter.getStartDate(), globalDateFilter.getEndDate(), null, null, null, null, 0, 25);
    }

    @Test
    void shouldShowUnfilteredOperationsAfterDeleteEmptiesCurrentFilter() {
        DateFilterState globalDateFilter = baseDateFilter();
        InvestmentOperation fallbackOperation = new InvestmentOperation();
        when(operationService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            "PETR4",
            null,
            null,
            null,
            0,
            25
        )).thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));
        when(operationService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            null,
            null,
            null,
            null,
            0,
            25
        )).thenReturn(new PagedResult<>(List.of(fallbackOperation), 0, 25, 1, 1));

        ConcurrentModel model = new ConcurrentModel();
        String view = operationController.delete(
            globalDateFilter,
            1L,
            "PETR4",
            null,
            null,
            null,
            0,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("investments/operations/list :: table");
        assertThat(model.getAttribute("operations")).isEqualTo(List.of(fallbackOperation));
        assertThat(model.getAttribute("selectedAsset")).isNull();
        verify(operationService).deleteById(1L);
    }

    @Test
    void shouldApplyListingFilters() {
        DateFilterState globalDateFilter = baseDateFilter();
        when(operationService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            "Petróleo",
            20L,
            InvestmentOperationType.BUY,
            null,
            0,
            25
        ))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        ConcurrentModel model = new ConcurrentModel();
        String view = operationController.table(globalDateFilter, " Petróleo ", 20L, InvestmentOperationType.BUY, null, null, null, model);

        assertThat(view).isEqualTo("investments/operations/list :: table");
        assertThat(model.getAttribute("selectedAsset")).isEqualTo("Petróleo");
        assertThat(model.getAttribute("selectedAccountId")).isEqualTo(20L);
        assertThat(model.getAttribute("selectedOperationType")).isEqualTo(InvestmentOperationType.BUY);
        verify(operationService).listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            "Petróleo",
            20L,
            InvestmentOperationType.BUY,
            null,
            0,
            25
        );
    }

    @Test
    void shouldApplyGlobalDateFilterToOperationListing() {
        DateFilterState globalDateFilter = baseDateFilter();
        when(operationService.listPageOrdered(globalDateFilter.getStartDate(), globalDateFilter.getEndDate(), null, null, null, null, 0, 25))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));

        String view = operationController.list(globalDateFilter, null, null, null, null, null, null, new ConcurrentModel());

        assertThat(view).isEqualTo("investments/operations/list");
        verify(operationService).listPageOrdered(globalDateFilter.getStartDate(), globalDateFilter.getEndDate(), null, null, null, null, 0, 25);
    }

    private InvestmentOperationForm baseForm() {
        InvestmentOperationForm form = new InvestmentOperationForm();
        form.setAssetId(10L);
        form.setAccountId(20L);
        form.setOperationType(InvestmentOperationType.BUY);
        form.setTradeDate(LocalDate.of(2026, 4, 20));
        form.setSettlementDate(LocalDate.of(2026, 4, 20));
        form.setQuantity(new BigDecimal("10.0000000000"));
        form.setUnitPrice(new BigDecimal("12.55000000"));
        form.setNetAmount(new BigDecimal("125.50"));
        form.setCurrency("BRL");
        form.setSourceType(InvestmentOperationSourceType.MANUAL);
        return form;
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        return request;
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        return state;
    }

    private Asset assetWithId(Long id) {
        Asset asset = new Asset();
        ReflectionTestUtils.setField(asset, "id", id);
        return asset;
    }

    private Entry entryWithId(Long id, Account account) {
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setAccount(account);
        entry.setDescription("Compra PETR4 qtd 10");
        entry.setMovementDate(LocalDate.of(2026, 4, 20));
        entry.setSettlementDate(LocalDate.of(2026, 4, 20));
        entry.setAmount(new BigDecimal("-125.50"));
        return entry;
    }

    private Account accountWithId(Long id) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setTitle("Investimentos");
        return account;
    }

}
