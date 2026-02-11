package dev.ccosta.aisha.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
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
        Account input = newAccount("Conta Corrente");

        when(accountRepository.save(input)).thenReturn(input);

        Account created = accountService.create(input);

        assertThat(created.getTitle()).isEqualTo("Conta Corrente");
        verify(accountRepository).save(input);
    }

    @Test
    void shouldUpdateAccount() {
        Account existing = newAccount("Conta antiga");
        Account updatedData = newAccount("Conta nova");
        updatedData.setDescription("Descricao nova");

        when(accountRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(existing)).thenReturn(existing);

        Account updated = accountService.update(10L, updatedData);

        assertThat(updated.getTitle()).isEqualTo("Conta nova");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
    }

    @Test
    void shouldPreventDeleteWhenAccountHasEntries() {
        Account existing = newAccount("Conta em uso");

        when(accountRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(entryRepository.existsByAccountId(12L)).thenReturn(true);

        assertThatThrownBy(() -> accountService.deleteById(12L))
            .isInstanceOf(AccountInUseException.class)
            .hasMessageContaining("12");

        verify(accountRepository, never()).deleteById(12L);
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        Account existing = newAccount("Conta");

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

    private Account newAccount(String title) {
        Account account = new Account();
        account.setTitle(title);
        return account;
    }
}
