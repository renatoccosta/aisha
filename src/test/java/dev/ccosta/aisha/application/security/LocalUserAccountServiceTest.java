package dev.ccosta.aisha.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.infrastructure.persistence.security.JpaLocalUserAccountRepository;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LocalUserAccountServiceTest {

    @Mock
    private JpaLocalUserAccountRepository localUserAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LocalUserAccountService localUserAccountService;

    @Test
    void shouldCreateLocalUserWithEncodedPassword() {
        when(localUserAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        LocalUserAccount saved = new LocalUserAccount("admin", "encoded-secret", true);
        when(localUserAccountRepository.save(any(LocalUserAccount.class))).thenReturn(saved);

        LocalUserAccount created = localUserAccountService.create("  admin  ", "secret", true);

        assertThat(created.getUsername()).isEqualTo("admin");
        assertThat(created.getPasswordHash()).isEqualTo("encoded-secret");
        assertThat(created.isEnabled()).isTrue();

        ArgumentCaptor<LocalUserAccount> accountCaptor = ArgumentCaptor.forClass(LocalUserAccount.class);
        verify(localUserAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getUsername()).isEqualTo("admin");
        assertThat(accountCaptor.getValue().getPasswordHash()).isEqualTo("encoded-secret");
    }

    @Test
    void shouldUpdateLocalUserWithoutChangingPasswordWhenBlank() {
        LocalUserAccount existing = new LocalUserAccount("olduser", "old-hash", true);

        when(localUserAccountRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(localUserAccountRepository.findByUsernameIgnoreCase("newuser")).thenReturn(Optional.empty());
        when(localUserAccountRepository.save(existing)).thenReturn(existing);

        LocalUserAccount updated = localUserAccountService.update(10L, "newuser", "", false);

        assertThat(updated.getUsername()).isEqualTo("newuser");
        assertThat(updated.getPasswordHash()).isEqualTo("old-hash");
        assertThat(updated.isEnabled()).isFalse();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldUpdateLocalUserWithNewEncodedPasswordWhenProvided() {
        LocalUserAccount existing = new LocalUserAccount("olduser", "old-hash", true);

        when(localUserAccountRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(localUserAccountRepository.findByUsernameIgnoreCase("olduser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");
        when(localUserAccountRepository.save(existing)).thenReturn(existing);

        LocalUserAccount updated = localUserAccountService.update(11L, "olduser", "new-secret", true);

        assertThat(updated.getPasswordHash()).isEqualTo("new-hash");
        verify(passwordEncoder).encode("new-secret");
    }

    @Test
    void shouldRejectDuplicateUsernameOnCreate() {
        LocalUserAccount existing = new LocalUserAccount("admin", "hash", true);
        when(localUserAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> localUserAccountService.create("admin", "secret", true))
            .isInstanceOf(LocalUserAccountUsernameAlreadyExistsException.class);

        verify(localUserAccountRepository, never()).save(any(LocalUserAccount.class));
    }

    @Test
    void shouldPreventDeletingAuthenticatedUser() {
        LocalUserAccount existing = new LocalUserAccount("admin", "hash", true);
        when(localUserAccountRepository.findById(20L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> localUserAccountService.deleteById(20L, "ADMIN"))
            .isInstanceOf(LocalUserAccountSelfDeletionException.class);

        verify(localUserAccountRepository, never()).deleteById(20L);
    }

    @Test
    void shouldRemoveDuplicateIdsInBulkDelete() {
        LocalUserAccount first = new LocalUserAccount("user1", "hash-1", true);
        LocalUserAccount second = new LocalUserAccount("user2", "hash-2", true);

        when(localUserAccountRepository.findById(1L)).thenReturn(Optional.of(first));
        when(localUserAccountRepository.findById(2L)).thenReturn(Optional.of(second));

        localUserAccountService.bulkDelete(List.of(1L, 2L, 1L), "admin");

        ArgumentCaptor<java.util.Collection<Long>> idsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(localUserAccountRepository).deleteByIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L);
    }
}
