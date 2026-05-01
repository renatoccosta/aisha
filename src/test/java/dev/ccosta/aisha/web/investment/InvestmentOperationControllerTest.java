package dev.ccosta.aisha.web.investment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.investment.AssetService;
import dev.ccosta.aisha.application.investment.InvestmentOperationEntryLinkRequest;
import dev.ccosta.aisha.application.investment.InvestmentOperationService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
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
        assertThat(model.getAttribute("entryCandidates")).isEqualTo(List.of(entry));
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

}
