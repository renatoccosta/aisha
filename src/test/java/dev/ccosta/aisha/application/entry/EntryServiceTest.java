package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
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

        Entry updated = entryService.update(1L, updatedData, 6L, 7L, null);

        assertThat(updated.getAccount().getTitle()).isEqualTo("Conta nova");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
        assertThat(updated.getAmount()).isEqualByComparingTo("99.90");
        assertThat(updated.getCategory().getTitle()).isEqualTo("Alimentação");
        verify(entryRepository).save(existing);
    }

    @Test
    void shouldFailUpdateWhenEntryDoesNotExist() {
        Entry updatedData = newEntry("Descricao", new BigDecimal("99.90"));
        when(entryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entryService.update(999L, updatedData, 6L, 7L, null))
            .isInstanceOf(EntryNotFoundException.class)
            .hasMessageContaining("999");

        verify(entryRepository, never()).save(updatedData);
    }

    @Test
    void shouldIgnoreBulkDeleteWhenNoIds() {
        entryService.bulkDelete(List.of());

        verify(entryRepository, never()).deleteByIds(List.of());
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        entryService.bulkDelete(List.of(1L, 2L, 1L, 2L, 3L));

        ArgumentCaptor<Collection<Long>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(entryRepository).deleteByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void shouldCreateWithExistingAccountAndCategory() {
        Entry input = newEntry("Descricao", new BigDecimal("15.00"));
        Account account = newAccount("Conta Corrente");
        Category category = newCategory("Categoria existente");

        when(accountService.findById(2L)).thenReturn(account);
        when(categoryService.findById(3L)).thenReturn(category);
        when(entryRepository.save(input)).thenReturn(input);

        Entry created = entryService.create(input, 2L, 3L, null);

        assertThat(created.getAccount().getTitle()).isEqualTo("Conta Corrente");
        assertThat(created.getCategory().getTitle()).isEqualTo("Categoria existente");
        verify(accountService).findById(2L);
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

        Entry created = entryService.create(input, 2L, null, "Nova categoria");

        assertThat(created.getCategory().getTitle()).isEqualTo("Nova categoria");
        verify(accountService).findById(2L);
        verify(categoryService).findOrCreateByTitle("Nova categoria");
        verify(entryRepository).save(input);
    }

    @Test
    void shouldListEntriesWithinSettlementDateRange() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        List<Entry> expected = List.of(newEntry("Descricao", new BigDecimal("1.00")));

        when(entryRepository.listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null)).thenReturn(expected);

        List<Entry> result = entryService.listTop100MostRecentBySettlementDateBetween(startDate, endDate);

        assertThat(result).isEqualTo(expected);
        verify(entryRepository).listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null);
    }

    @Test
    void shouldListEntriesWithinSettlementDateRangeByAccountAndCategory() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);
        Long accountId = 10L;
        Long categoryId = 20L;
        List<Entry> expected = List.of(newEntry("Descricao", new BigDecimal("1.00")));

        when(entryRepository.listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, accountId, categoryId))
            .thenReturn(expected);

        List<Entry> result = entryService.listTop100MostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            accountId,
            categoryId
        );

        assertThat(result).isEqualTo(expected);
        verify(entryRepository).listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, accountId, categoryId);
    }

    @Test
    void shouldFailWhenRangeIsInvalid() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        assertThatThrownBy(() -> entryService.listTop100MostRecentBySettlementDateBetween(startDate, endDate))
            .isInstanceOf(IllegalArgumentException.class);

        verify(entryRepository, never()).listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null);
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
}
