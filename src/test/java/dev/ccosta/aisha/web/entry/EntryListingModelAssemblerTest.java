package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class EntryListingModelAssemblerTest {

    @Mock
    private EntryService entryService;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EntryTransferService entryTransferService;

    @Test
    void shouldShowUnfilteredEntriesAfterMutationEmptiesCurrentFilter() {
        DateFilterState dateFilter = baseDateFilter();
        Entry fallbackEntry = entryWithId(10L);
        EntryListingModelAssembler assembler = new EntryListingModelAssembler(
            entryService,
            accountService,
            categoryService,
            entryTransferService
        );
        when(accountService.listVisibleForEntryFilter(dateFilter.getStartDate())).thenReturn(List.of());
        when(categoryService.listHierarchyOptions()).thenReturn(List.of());
        when(entryTransferService.findTransferViewByEntryId(10L)).thenReturn(Optional.empty());
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(
            dateFilter.getStartDate(),
            dateFilter.getEndDate(),
            null,
            null,
            "mercado",
            false,
            false,
            0,
            25
        )).thenReturn(new PagedResult<>(List.of(), 0, 25, 0, 0));
        when(entryService.listMostRecentBySettlementDateBetweenAndFilters(
            dateFilter.getStartDate(),
            dateFilter.getEndDate(),
            null,
            null,
            null,
            false,
            false,
            0,
            25
        )).thenReturn(new PagedResult<>(List.of(fallbackEntry), 0, 25, 1, 1));

        ConcurrentModel model = new ConcurrentModel();
        assembler.fillListingAfterMutation(model, dateFilter, null, null, "mercado", false, 0, 25);

        assertThat(model.getAttribute("entries")).isEqualTo(List.of(fallbackEntry));
        assertThat(model.getAttribute("selectedDescription")).isNull();
    }

    private DateFilterState baseDateFilter() {
        DateFilterState state = new DateFilterState();
        state.applyCustom(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        return state;
    }

    private Entry entryWithId(Long id) {
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
