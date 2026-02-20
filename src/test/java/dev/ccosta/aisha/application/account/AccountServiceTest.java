package dev.ccosta.aisha.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccount() {
        Account input = newAccount("Conta Corrente", "100.00", LocalDate.of(2026, 1, 1));

        when(accountRepository.save(input)).thenReturn(input);

        Account created = accountService.create(input);

        assertThat(created.getTitle()).isEqualTo("Conta Corrente");
        assertThat(created.getInitialBalance()).isEqualByComparingTo("100.00");
        assertThat(created.getInitialBalanceDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        verify(accountRepository).save(input);
    }

    @Test
    void shouldUpdateAccount() {
        Account existing = newAccount("Conta antiga", "10.00", LocalDate.of(2026, 1, 1));
        Account updatedData = newAccount("Conta nova", "250.75", LocalDate.of(2026, 2, 5));
        updatedData.setDescription("Descricao nova");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(existing)).thenReturn(existing);

        Account updated = accountService.update(10L, updatedData);

        assertThat(updated.getTitle()).isEqualTo("Conta nova");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
        assertThat(updated.getInitialBalance()).isEqualByComparingTo("250.75");
        assertThat(updated.getInitialBalanceDate()).isEqualTo(LocalDate.of(2026, 2, 5));
    }

    @Test
    void shouldPreventDeleteWhenAccountHasEntries() {
        Account existing = newAccount("Conta em uso", "0.00", LocalDate.of(2026, 1, 1));

        when(accountRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(entryRepository.existsByAccountId(12L)).thenReturn(true);

        assertThatThrownBy(() -> accountService.deleteById(12L))
            .isInstanceOf(AccountInUseException.class)
            .hasMessageContaining("12");

        verify(accountRepository, never()).deleteById(12L);
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        Account existing = newAccount("Conta", "0.00", LocalDate.of(2026, 1, 1));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(entryRepository.existsByAccountId(1L)).thenReturn(false);
        when(entryRepository.existsByAccountId(2L)).thenReturn(false);
        when(entryRepository.existsByAccountId(3L)).thenReturn(false);

        accountService.bulkDelete(List.of(1L, 2L, 1L, 3L));

        ArgumentCaptor<java.util.Collection<Long>> idsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(accountRepository).deleteByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L, 3L);
    }

    @Test
    void shouldAdjustInitialBalanceWhenBackdatedEntryIsOnOrBeforeInitialBalanceDate() {
        Account existing = newAccount("Conta Corrente", "100.00", LocalDate.of(2025, 12, 31));

        when(accountRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(
            entryRepository.sumAmountByAccountIdAndSettlementDateBetween(
                10L,
                LocalDate.of(2025, 12, 5),
                LocalDate.of(2025, 12, 31)
            )
        ).thenReturn(new BigDecimal("25.00"));

        accountService.adjustInitialBalanceForBackdatedEntry(10L, LocalDate.of(2025, 12, 5));

        assertThat(existing.getInitialBalanceDate()).isEqualTo(LocalDate.of(2025, 12, 4));
        assertThat(existing.getInitialBalance()).isEqualByComparingTo("75.00");
        verify(accountRepository).save(existing);
    }

    @Test
    void shouldNotAdjustInitialBalanceWhenEntryIsAfterInitialBalanceDate() {
        Account existing = newAccount("Conta Corrente", "100.00", LocalDate.of(2025, 12, 31));

        when(accountRepository.findById(10L)).thenReturn(Optional.of(existing));

        accountService.adjustInitialBalanceForBackdatedEntry(10L, LocalDate.of(2026, 1, 1));

        verify(entryRepository, never()).sumAmountByAccountIdAndSettlementDateBetween(
            10L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2025, 12, 31)
        );
        verify(accountRepository, never()).save(existing);
    }

    @Test
    void shouldAdjustMultipleAffectedAccountsFromImport() {
        Account accountA = newAccount("Conta A", "100.00", LocalDate.of(2025, 12, 31));
        Account accountB = newAccount("Conta B", "200.00", LocalDate.of(2026, 1, 10));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(accountA));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(accountB));
        when(
            entryRepository.sumAmountByAccountIdAndSettlementDateBetween(
                1L,
                LocalDate.of(2025, 12, 5),
                LocalDate.of(2025, 12, 31)
            )
        ).thenReturn(new BigDecimal("25.00"));
        when(
            entryRepository.sumAmountByAccountIdAndSettlementDateBetween(
                2L,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10)
            )
        ).thenReturn(new BigDecimal("30.00"));

        accountService.adjustInitialBalanceForBackdatedEntries(
            Map.of(
                1L, LocalDate.of(2025, 12, 5),
                2L, LocalDate.of(2026, 1, 10)
            )
        );

        assertThat(accountA.getInitialBalanceDate()).isEqualTo(LocalDate.of(2025, 12, 4));
        assertThat(accountA.getInitialBalance()).isEqualByComparingTo("75.00");
        assertThat(accountB.getInitialBalanceDate()).isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(accountB.getInitialBalance()).isEqualByComparingTo("170.00");
        verify(accountRepository).save(accountA);
        verify(accountRepository).save(accountB);
    }

    private Account newAccount(String title, String initialBalance, LocalDate initialBalanceDate) {
        Account account = new Account();
        account.setTitle(title);
        account.setInitialBalance(new BigDecimal(initialBalance));
        account.setInitialBalanceDate(initialBalanceDate);
        return account;
    }
}
