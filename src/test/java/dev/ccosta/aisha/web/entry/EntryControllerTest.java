package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCategorySelection;
import dev.ccosta.aisha.application.entry.EntryInUseException;
import dev.ccosta.aisha.application.entry.EntryRelationSummary;
import dev.ccosta.aisha.application.entry.EntryRelationSummaryService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryType;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class EntryControllerTest {

    @Mock
    private EntryService entryService;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EntryCategorySuggestionService entryCategorySuggestionService;

    @Mock
    private EntryTransferService entryTransferService;

    @Mock
    private EntryRelationSummaryService entryRelationSummaryService;

    @Mock
    private EntryListingModelAssembler listingModelAssembler;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private EntryController entryController;

    @Test
    void shouldRenderEntryDetailsWithExistingRelationships() {
        Entry entry = baseEntry(10L);
        EntryTransferView transferView = new EntryTransferView(40L, 11L, 2L, "Reserva", true);
        EntryRelationSummary relationSummary = EntryRelationSummary.empty(10L)
            .withTransferView(transferView)
            .withInvestmentOperationId(20L)
            .withBrokerageNoteId(30L);
        ConcurrentModel model = new ConcurrentModel();
        when(entryService.findById(10L)).thenReturn(entry);
        when(entryRelationSummaryService.summarize(entry)).thenReturn(relationSummary);
        when(messageSource.getMessage(eq("entries.details.heading"), any(Object[].class), any(Locale.class))).thenReturn("Lançamento #10");
        when(messageSource.getMessage(eq("entry.type.transfer"), any(), any(Locale.class))).thenReturn("Transferência");
        when(messageSource.getMessage(eq("entry.effect.result"), any(), any(Locale.class))).thenReturn("Resultado");
        when(messageSource.getMessage(eq("entry.categorySuggestionStatus.none"), any(), any(Locale.class))).thenReturn("Sem sugestão");

        String view = entryController.details(10L, "/entries?page=1", model);

        assertThat(view).isEqualTo("entries/details");
        assertThat(model.getAttribute("entry")).isSameAs(entry);
        assertThat(model.getAttribute("transferEntry")).isEqualTo(true);
        assertThat(model.getAttribute("transferView")).isSameAs(transferView);
        assertThat(model.getAttribute("entryRelationSummary")).isSameAs(relationSummary);
        assertThat(model.getAttribute("entryDetailsReturnPath")).isEqualTo("/entries/10");
        assertThat(model.getAttribute("entryDetailsHeading")).isEqualTo("Lançamento #10");
        assertThat(model.getAttribute("transferCounterpartEntryId")).isEqualTo(11L);
        assertThat(model.getAttribute("linkedInvestmentOperationId")).isEqualTo(20L);
        assertThat(model.getAttribute("linkedBrokerageNoteId")).isEqualTo(30L);
        assertThat(model.getAttribute("returnTo")).isEqualTo("/entries?page=1");
    }

    @Test
    void shouldRenderEntryDetailsWithBrokerageNoteNetEntryRelationship() {
        Entry entry = baseEntry(10L);
        EntryRelationSummary relationSummary = EntryRelationSummary.empty(10L).withBrokerageNoteId(30L);
        ConcurrentModel model = new ConcurrentModel();
        when(entryService.findById(10L)).thenReturn(entry);
        when(entryRelationSummaryService.summarize(entry)).thenReturn(relationSummary);
        when(messageSource.getMessage(eq("entries.details.heading"), any(Object[].class), any(Locale.class))).thenReturn("Lançamento #10");
        when(messageSource.getMessage(eq("entry.type.transfer"), any(), any(Locale.class))).thenReturn("Transferência");
        when(messageSource.getMessage(eq("entry.effect.result"), any(), any(Locale.class))).thenReturn("Resultado");
        when(messageSource.getMessage(eq("entry.categorySuggestionStatus.none"), any(), any(Locale.class))).thenReturn("Sem sugestão");

        String view = entryController.details(10L, "/entries?page=1", model);

        assertThat(view).isEqualTo("entries/details");
        assertThat(model.getAttribute("linkedInvestmentOperationId")).isNull();
        assertThat(model.getAttribute("linkedBrokerageNoteId")).isEqualTo(30L);
    }

    @Test
    void shouldShowNewCategoryFieldWhenSpecialOptionIsSelected() {
        EntryForm form = baseForm();
        form.setCategoryId(-2L);
        ConcurrentModel model = new ConcurrentModel();
        when(categoryService.listHierarchyOptions()).thenReturn(List.of(new CategoryOption(1L, "Alimentação")));

        String view = entryController.categorySuggestion(form, model);

        assertThat(view).isEqualTo("entries/form :: categorySelectionSection");
        assertThat(model.getAttribute("showNewCategoryField")).isEqualTo(true);
    }

    @Test
    void shouldKeepCategoryControlsAvailableForEquityEntry() {
        EntryForm form = baseForm();
        form.setEquityEntry(true);
        form.setCategoryId(-2L);
        ConcurrentModel model = new ConcurrentModel();
        when(categoryService.listHierarchyOptions()).thenReturn(List.of(new CategoryOption(1L, "Investimentos")));

        String view = entryController.categorySuggestion(form, model);

        assertThat(view).isEqualTo("entries/form :: categorySelectionSection");
        assertThat(model.getAttribute("showNewCategoryField")).isEqualTo(true);
    }

    @Test
    void shouldRequireNewCategoryTitleWhenSpecialOptionIsSelected() {
        EntryForm form = baseForm();
        form.setCategoryId(-2L);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        ConcurrentModel model = new ConcurrentModel();
        when(accountService.listAvailableForEntryForm(any())).thenReturn(List.of());
        when(categoryService.listHierarchyOptions()).thenReturn(List.of(new CategoryOption(1L, "Alimentação")));

        String view = entryController.create(form, bindingResult, "/entries", model);

        assertThat(view).isEqualTo("entries/form");
        assertThat(bindingResult.hasFieldErrors("newCategoryTitle")).isTrue();
        assertThat(model.getAttribute("showNewCategoryField")).isEqualTo(true);
    }

    @Test
    void shouldConvertSpecialCategoryOptionIntoNewCategorySelection() {
        EntryForm form = baseForm();
        form.setCategoryId(-2L);
        form.setNewCategoryTitle("Educação");

        String view = entryController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "/entries?page=2&size=50",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/entries?page=2&size=50");
        ArgumentCaptor<EntryCategorySelection> selectionCaptor = ArgumentCaptor.forClass(EntryCategorySelection.class);
        verify(entryService).create(any(Entry.class), any(), selectionCaptor.capture());
        assertThat(selectionCaptor.getValue().categoryId()).isNull();
        assertThat(selectionCaptor.getValue().newCategoryTitle()).isEqualTo("Educação");
    }

    @Test
    void shouldFallbackToEntriesListingWhenReturnPathIsUnsafe() {
        EntryForm form = baseForm();
        form.setCategoryId(1L);

        String view = entryController.create(
            form,
            new BeanPropertyBindingResult(form, "form"),
            "https://evil.example/entries",
            new ConcurrentModel()
        );

        assertThat(view).isEqualTo("redirect:/entries");
    }

    @Test
    void shouldConfirmCategorySuggestionsInBulkAndRefreshListingForHtmx() {
        ConcurrentModel model = new ConcurrentModel();

        String view = entryController.bulkConfirmCategorySuggestions(
            List.of(1L, 2L),
            baseDateFilter(),
            3L,
            4L,
            "mercado",
            true,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        verify(entryService).bulkConfirmCategorySuggestions(List.of(1L, 2L));
    }

    @Test
    void shouldReturnListingToastWhenDeletingEntryLinkedToInvestmentOperationForHtmx() {
        ConcurrentModel model = new ConcurrentModel();
        DateFilterState dateFilter = baseDateFilter();
        doThrow(new EntryInUseException(7L)).when(entryService).deleteById(7L);
        when(messageSource.getMessage(
            "entries.list.toast.delete.inUse",
            null,
            org.springframework.context.i18n.LocaleContextHolder.getLocale()
        )).thenReturn("Este lançamento possui uma operação de investimento associada e não pode ser excluído.");

        String view = entryController.delete(7L, dateFilter, 3L, 4L, "PETR4", false, 0, 25, htmxRequest(), model);

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Este lançamento possui uma operação de investimento associada e não pode ser excluído.");
        verify(listingModelAssembler).fillListing(model, dateFilter, 3L, 4L, "PETR4", false, 0, 25);
    }

    @Test
    void shouldReturnListingToastWhenBulkDeletingEntryLinkedToInvestmentOperationForHtmx() {
        ConcurrentModel model = new ConcurrentModel();
        DateFilterState dateFilter = baseDateFilter();
        List<Long> ids = List.of(7L, 8L);
        doThrow(new EntryInUseException(7L)).when(entryService).bulkDelete(ids);
        when(messageSource.getMessage(
            "entries.list.toast.delete.inUse",
            null,
            org.springframework.context.i18n.LocaleContextHolder.getLocale()
        )).thenReturn("Este lançamento possui uma operação de investimento associada e não pode ser excluído.");

        String view = entryController.bulkDelete(ids, dateFilter, 3L, 4L, "PETR4", false, 0, 25, htmxRequest(), model);

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Este lançamento possui uma operação de investimento associada e não pode ser excluído.");
        verify(listingModelAssembler).fillListing(model, dateFilter, 3L, 4L, "PETR4", false, 0, 25);
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        return state;
    }

    private MockHttpServletRequest htmxRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("HX-Request", "true");
        return request;
    }

    private EntryForm baseForm() {
        EntryForm form = new EntryForm();
        form.setAccountId(1L);
        form.setMovementDate(LocalDate.of(2026, 3, 10));
        form.setSettlementDate(LocalDate.of(2026, 3, 10));
        form.setDescription("Curso");
        form.setAmount(new BigDecimal("120.00"));
        return form;
    }

    private Entry baseEntry(Long id) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", 1L);
        account.setTitle("Carteira");
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setAccount(account);
        entry.setMovementDate(LocalDate.of(2026, 3, 10));
        entry.setSettlementDate(LocalDate.of(2026, 3, 10));
        entry.setDescription("Compra PETR4");
        entry.setAmount(new BigDecimal("-120.00"));
        entry.setEntryType(EntryType.TRANSFER);
        return entry;
    }

}
