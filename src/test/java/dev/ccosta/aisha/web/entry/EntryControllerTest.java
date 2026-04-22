package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCategorySelection;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.domain.entry.Entry;
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
    private EntryListingModelAssembler listingModelAssembler;

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
