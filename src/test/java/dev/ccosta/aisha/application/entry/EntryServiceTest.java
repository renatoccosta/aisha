package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransferRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private EntryTransferRepository entryTransferRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private EntryService entryService;

    @Test
    void shouldUpdateExistingEntry() {
        Entry existing = newEntry("Descricao antiga", new BigDecimal("10.00"));
        Entry updatedData = newEntry("Descricao nova", new BigDecimal("99.90"));
        Account account = newAccount("Conta nova");
        Category category = newCategory("Alimentação");

        when(entryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountService.findById(6L)).thenReturn(account);
        when(categoryService.findById(7L)).thenReturn(category);
        when(entryRepository.save(existing)).thenReturn(existing);

        Entry updated = entryService.update(1L, updatedData, 6L, selection(7L, null, null, null));

        assertThat(updated.getAccount().getTitle()).isEqualTo("Conta nova");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
        assertThat(updated.getAmount()).isEqualByComparingTo("99.90");
        assertThat(updated.getCategory().getTitle()).isEqualTo("Alimentação");
        verify(entryRepository).save(existing);
    }


    @Test
    void shouldDefineManualSourceWhenUpdatingLegacyEntry() {
        Entry existing = newEntry("Descricao antiga", new BigDecimal("10.00"));
        existing.setEntrySource(null);
        existing.setRegistrationDate(null);
        Entry updatedData = newEntry("Descricao nova", new BigDecimal("99.90"));
        Account account = newAccount("Conta nova");
        Category category = newCategory("Alimentação");

        when(entryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountService.findById(6L)).thenReturn(account);
        when(categoryService.findById(7L)).thenReturn(category);
        when(entryRepository.save(existing)).thenReturn(existing);

        Entry updated = entryService.update(1L, updatedData, 6L, selection(7L, null, null, null));

        assertThat(updated.getEntrySource()).isEqualTo(EntrySource.MANUAL);
        assertThat(updated.getRegistrationDate()).isNotNull();
    }

    @Test
    void shouldFailUpdateWhenEntryDoesNotExist() {
        Entry updatedData = newEntry("Descricao", new BigDecimal("99.90"));
        when(entryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entryService.update(999L, updatedData, 6L, selection(7L, null, null, null)))
            .isInstanceOf(EntryNotFoundException.class)
            .hasMessageContaining("999");

        verify(entryRepository, never()).save(updatedData);
    }

    @Test
    void shouldIgnoreBulkDeleteWhenNoIds() {
        entryService.bulkDelete(List.of());

        verify(entryRepository, never()).deleteByIds(List.of());
        verify(entryTransferRepository, never()).findAllByEntryIds(List.of());
    }

    @Test
    void shouldIgnoreBulkConfirmWhenNoIds() {
        entryService.bulkConfirmCategorySuggestions(List.of());

        verify(entryRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(entryRepository, never()).save(org.mockito.ArgumentMatchers.any(Entry.class));
    }

    @Test
    void shouldConfirmOnlyPendingSuggestionsInBulk() {
        Entry pendingEntry = newEntry("Mercado", new BigDecimal("15.00"));
        Category suggestedCategory = newCategory("Supermercado");
        pendingEntry.setCategory(suggestedCategory);
        pendingEntry.setSuggestedCategory(suggestedCategory);
        pendingEntry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);

        Entry acceptedEntry = newEntry("Padaria", new BigDecimal("8.00"));
        acceptedEntry.setCategory(suggestedCategory);
        acceptedEntry.setSuggestedCategory(suggestedCategory);
        acceptedEntry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.ACCEPTED);

        Entry invalidPendingEntry = newEntry("Farmácia", new BigDecimal("20.00"));
        invalidPendingEntry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);

        when(entryRepository.findById(1L)).thenReturn(Optional.of(pendingEntry));
        when(entryRepository.findById(2L)).thenReturn(Optional.of(acceptedEntry));
        when(entryRepository.findById(3L)).thenReturn(Optional.of(invalidPendingEntry));
        when(entryRepository.save(pendingEntry)).thenReturn(pendingEntry);

        entryService.bulkConfirmCategorySuggestions(List.of(1L, 2L, 1L, 3L));

        assertThat(pendingEntry.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.ACCEPTED);
        assertThat(pendingEntry.getCategorySuggestionConfidence()).isEqualTo(1.0d);
        verify(entryRepository, times(1)).findById(1L);
        verify(entryRepository, times(1)).findById(2L);
        verify(entryRepository, times(1)).findById(3L);
        verify(entryRepository).save(pendingEntry);
        verify(entryRepository, never()).save(acceptedEntry);
        verify(entryRepository, never()).save(invalidPendingEntry);
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        doReturn(List.of()).when(entryTransferRepository).findAllByEntryIds(org.mockito.ArgumentMatchers.anyCollection());

        entryService.bulkDelete(List.of(1L, 2L, 1L, 2L, 3L));

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryRepository).deleteByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void shouldDeleteBothSidesWhenBulkDeleteContainsTransferEntry() {
        Entry originEntry = newTransferEntry(10L);
        Entry destinationEntry = newTransferEntry(11L);
        EntryTransfer transfer = new EntryTransfer();
        transfer.setOriginEntry(originEntry);
        transfer.setDestinationEntry(destinationEntry);
        doReturn(List.of(transfer)).when(entryTransferRepository).findAllByEntryIds(org.mockito.ArgumentMatchers.anyCollection());

        entryService.bulkDelete(List.of(10L));

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryTransferRepository).deleteAllByEntryIds(idsCaptor.capture());
        ArgumentCaptor<Collection<Long>> entryIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryRepository).deleteByIds(entryIdsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(10L, 11L);
        assertThat(entryIdsCaptor.getValue()).containsExactly(10L, 11L);
    }

    @Test
    void shouldDeleteEachTransferOnlyOnceWhenBothSidesAreSelected() {
        Entry originEntry = newTransferEntry(10L);
        Entry destinationEntry = newTransferEntry(11L);
        EntryTransfer transfer = new EntryTransfer();
        transfer.setOriginEntry(originEntry);
        transfer.setDestinationEntry(destinationEntry);
        doReturn(List.of(transfer)).when(entryTransferRepository).findAllByEntryIds(org.mockito.ArgumentMatchers.anyCollection());

        entryService.bulkDelete(List.of(10L, 11L));

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryTransferRepository, times(1)).deleteAllByEntryIds(idsCaptor.capture());
        ArgumentCaptor<Collection<Long>> entryIdsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryRepository).deleteByIds(entryIdsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(10L, 11L);
        assertThat(entryIdsCaptor.getValue()).containsExactly(10L, 11L);
    }

    @Test
    void shouldCreateWithExistingAccountAndCategory() {
        Entry input = newEntry("Descricao", new BigDecimal("15.00"));
        Account account = newAccount("Conta Corrente");
        Category category = newCategory("Categoria existente");

        when(accountService.findById(2L)).thenReturn(account);
        when(categoryService.findById(3L)).thenReturn(category);
        when(entryRepository.save(input)).thenReturn(input);

        Entry created = entryService.create(input, 2L, selection(3L, null, null, null));

        assertThat(created.getAccount().getTitle()).isEqualTo("Conta Corrente");
        assertThat(created.getCategory().getTitle()).isEqualTo("Categoria existente");
        assertThat(created.getEntrySource()).isEqualTo(EntrySource.MANUAL);
        assertThat(created.getRegistrationDate()).isNotNull();
        verify(accountService).validateEntrySettlementDateAgainstAccountDeactivation(2L, LocalDate.of(2026, 2, 11));
        verify(accountService).findById(2L);
        verify(accountService).adjustInitialBalanceForBackdatedEntry(2L, LocalDate.of(2026, 2, 11));
        verify(categoryService).findById(3L);
        verify(entryRepository).save(input);
    }

    @Test
    void shouldCreateMissingCategoryFromTitle() {
        Entry input = newEntry("Descricao", new BigDecimal("15.00"));
        Account account = newAccount("Conta Corrente");
        Category createdCategory = newCategory("Nova categoria");

        when(accountService.findById(2L)).thenReturn(account);
        when(categoryService.findOrCreateByTitle("Nova categoria")).thenReturn(createdCategory);
        when(entryRepository.save(input)).thenReturn(input);

        Entry created = entryService.create(input, 2L, selection(null, "Nova categoria", null, null));

        assertThat(created.getCategory().getTitle()).isEqualTo("Nova categoria");
        verify(accountService).validateEntrySettlementDateAgainstAccountDeactivation(2L, LocalDate.of(2026, 2, 11));
        verify(accountService).findById(2L);
        verify(categoryService).findOrCreateByTitle("Nova categoria");
        verify(entryRepository).save(input);
    }

    @Test
    void shouldFailCreateWhenSettlementDateIsAfterAccountDeactivationDate() {
        Entry input = newEntry("Descricao", new BigDecimal("15.00"));
        org.mockito.Mockito.doThrow(
            new EntrySettlementAfterAccountDeactivationException(LocalDate.of(2026, 2, 11), LocalDate.of(2026, 2, 10))
        )
            .when(accountService)
            .validateEntrySettlementDateAgainstAccountDeactivation(2L, LocalDate.of(2026, 2, 11));

        assertThatThrownBy(() -> entryService.create(input, 2L, selection(3L, null, null, null)))
            .isInstanceOf(EntrySettlementAfterAccountDeactivationException.class);

        verify(entryRepository, never()).save(input);
    }

    @Test
    void shouldListEntriesWithinSettlementDateRange() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        PagedResult<Entry> expected = new PagedResult<>(List.of(newEntry("Descricao", new BigDecimal("1.00"))), 0, 25, 1, 1);

        when(entryRepository.listMostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null, null, false, false, 0, 25))
            .thenReturn(expected);

        PagedResult<Entry> result = entryService.listMostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            null,
            null,
            null,
            false,
            false,
            0,
            25
        );

        assertThat(result).isEqualTo(expected);
        verify(entryRepository).listMostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null, null, false, false, 0, 25);
    }

    @Test
    void shouldListEntriesWithinSettlementDateRangeByAccountAndCategory() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        Long accountId = 10L;
        Long categoryId = 20L;
        PagedResult<Entry> expected = new PagedResult<>(List.of(newEntry("Descricao", new BigDecimal("1.00"))), 1, 50, 3, 2);

        when(entryRepository.listMostRecentBySettlementDateBetweenAndFilters(startDate, endDate, accountId, categoryId, "Mercado", false, false, 1, 50))
            .thenReturn(expected);

        PagedResult<Entry> result = entryService.listMostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            accountId,
            categoryId,
            "  Mercado  ",
            false,
            false,
            1,
            50
        );

        assertThat(result).isEqualTo(expected);
        verify(entryRepository).listMostRecentBySettlementDateBetweenAndFilters(startDate, endDate, accountId, categoryId, "Mercado", false, false, 1, 50);
    }

    @Test
    void shouldFailWhenRangeIsInvalid() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> entryService.listMostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            null,
            null,
            null,
            false,
            false,
            0,
            25
        ))
            .isInstanceOf(IllegalArgumentException.class);

        verify(entryRepository, never()).listMostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            null,
            null,
            null,
            false,
            false,
            0,
            25
        );
    }

    @Test
    void shouldMarkAcceptedSuggestionWhenUserKeepsSuggestedCategory() {
        Entry input = newEntry("Mercado", new BigDecimal("15.00"));
        Account account = newAccount("Conta Corrente");
        Category category = newCategory("Supermercado");

        when(accountService.findById(2L)).thenReturn(account);
        when(categoryService.findById(3L)).thenReturn(category);
        when(entryRepository.save(input)).thenReturn(input);

        Entry created = entryService.create(input, 2L, selection(3L, null, 3L, 0.91d));

        assertThat(created.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.ACCEPTED);
        assertThat(created.getSuggestedCategory()).isEqualTo(category);
        assertThat(created.getCategorySuggestionConfidence()).isEqualTo(0.91d);
    }

    @Test
    void shouldMarkRejectedSuggestionWhenUserChangesSuggestedCategory() {
        Entry input = newEntry("Mercado", new BigDecimal("15.00"));
        Account account = newAccount("Conta Corrente");
        Category selectedCategory = newCategory("Lazer");
        Category suggestedCategory = newCategory("Supermercado");

        when(accountService.findById(2L)).thenReturn(account);
        when(categoryService.findById(3L)).thenReturn(selectedCategory);
        when(categoryService.findById(4L)).thenReturn(suggestedCategory);
        when(entryRepository.save(input)).thenReturn(input);

        Entry created = entryService.create(input, 2L, selection(3L, null, 4L, 0.52d));

        assertThat(created.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.REJECTED);
        assertThat(created.getSuggestedCategory()).isEqualTo(suggestedCategory);
    }

    @Test
    void shouldConfirmPendingCategorySuggestion() {
        Entry entry = newEntry("Mercado", new BigDecimal("15.00"));
        Category category = newCategory("Supermercado");
        entry.setCategory(category);
        entry.setSuggestedCategory(category);
        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.PENDING);

        when(entryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(entry)).thenReturn(entry);

        Entry updated = entryService.confirmCategorySuggestion(1L);

        assertThat(updated.getCategorySuggestionStatus()).isEqualTo(EntryCategorySuggestionStatus.ACCEPTED);
        verify(entryRepository).save(entry);
    }

    @Test
    void shouldRejectRegularUpdateForTransferEntry() {
        Entry existing = newEntry("Transferência", new BigDecimal("-10.00"));
        existing.setEntryType(dev.ccosta.aisha.domain.entry.EntryType.TRANSFER);
        Entry updatedData = newEntry("Transferência", new BigDecimal("-10.00"));

        when(entryRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> entryService.update(1L, updatedData, 6L, selection(7L, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("transfer-specific flows");
    }

    private Entry newEntry(String description, BigDecimal amount) {
        Entry entry = new Entry();
        entry.setAccount(newAccount("Conta padrão"));
        entry.setDescription(description);
        entry.setCategory(newCategory("Geral"));
        entry.setMovementDate(LocalDate.of(2026, 2, 11));
        entry.setSettlementDate(LocalDate.of(2026, 2, 11));
        entry.setAmount(amount);
        return entry;
    }

    private Entry newTransferEntry(Long id) {
        Entry entry = mock(Entry.class);
        when(entry.getId()).thenReturn(id);
        return entry;
    }

    private Category newCategory(String title) {
        Category category = new Category();
        category.setTitle(title);
        return category;
    }

    private Account newAccount(String title) {
        Account account = new Account();
        account.setTitle(title);
        return account;
    }

    private EntryCategorySelection selection(Long categoryId, String newCategoryTitle, Long suggestedCategoryId, Double confidence) {
        return new EntryCategorySelection(categoryId, newCategoryTitle, suggestedCategoryId, confidence);
    }
}
