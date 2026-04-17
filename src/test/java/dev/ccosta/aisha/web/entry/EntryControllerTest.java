package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCategorySelection;
import dev.ccosta.aisha.application.entry.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.EntryTransferService;
import dev.ccosta.aisha.application.entry.statement.EntryStatementImportService;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
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
    private EntryStatementImportService entryStatementImportService;

    @Mock
    private EntryImportJobCoordinator entryImportJobCoordinator;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private EntryController entryController;

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
    void shouldLinkTransferFromListingSelectionAndReturnSuccessToast() {
        ConcurrentModel model = new ConcurrentModel();
        stubListingDependencies();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.success"), eq(null), any()))
            .thenReturn("Transferência associada com sucesso.");

        String view = entryController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastMessage")).isEqualTo("Transferência associada com sucesso.");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("success");
        verify(entryTransferService).linkExistingEntries(1L, 2L);
    }

    @Test
    void shouldReturnErrorToastWhenNoCounterpartIsSelected() {
        ConcurrentModel model = new ConcurrentModel();
        stubListingDependencies();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.selection.single"), eq(null), any()))
            .thenReturn("Selecione exatamente um outro lançamento para associar como transferência.");

        String view = entryController.linkTransferFromSelection(
            1L,
            List.of(1L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Selecione exatamente um outro lançamento para associar como transferência.");
    }

    @Test
    void shouldReturnErrorToastWhenMoreThanOneCounterpartIsSelected() {
        ConcurrentModel model = new ConcurrentModel();
        stubListingDependencies();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.selection.single"), eq(null), any()))
            .thenReturn("Selecione exatamente um outro lançamento para associar como transferência.");

        String view = entryController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L, 3L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("Selecione exatamente um outro lançamento para associar como transferência.");
    }

    @Test
    void shouldReturnExplainedErrorToastWhenSelectedCounterpartIsIncompatible() {
        ConcurrentModel model = new ConcurrentModel();
        stubListingDependencies();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.incompatible"), any(), any()))
            .thenReturn("O lançamento selecionado não pode ser associado: os lançamentos devem ter sinais opostos");
        when(entryTransferService.linkExistingEntries(1L, 2L))
            .thenThrow(new IllegalArgumentException("Transfer entries must have opposite signs"));

        String view = entryController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("O lançamento selecionado não pode ser associado: os lançamentos devem ter sinais opostos");
    }

    @Test
    void shouldReturnExplainedErrorToastWhenSelectedCounterpartIsMissing() {
        ConcurrentModel model = new ConcurrentModel();
        stubListingDependencies();
        when(messageSource.getMessage(eq("entries.list.toast.transfer.link.incompatible"), any(), any()))
            .thenReturn("O lançamento selecionado não pode ser associado: o lançamento selecionado não foi encontrado");
        when(entryTransferService.linkExistingEntries(1L, 2L))
            .thenThrow(new EntryNotFoundException(2L));

        String view = entryController.linkTransferFromSelection(
            1L,
            List.of(1L, 2L),
            baseDateFilter(),
            null,
            null,
            null,
            false,
            2,
            25,
            htmxRequest(),
            model
        );

        assertThat(view).isEqualTo("entries/list :: listing");
        assertThat(model.getAttribute("toastLevel")).isEqualTo("error");
        assertThat(model.getAttribute("toastMessage"))
            .isEqualTo("O lançamento selecionado não pode ser associado: o lançamento selecionado não foi encontrado");
    }

    private void stubListingDependencies() {
        when(accountService.listVisibleForEntryFilter(any())).thenReturn(List.of());
        when(categoryService.listHierarchyOptions()).thenReturn(List.of());
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyInt(), anyInt()))
            .thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));
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
}
