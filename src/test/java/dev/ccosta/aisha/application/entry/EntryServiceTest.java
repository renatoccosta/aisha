package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private EntryService entryService;

    @Test
    void shouldUpdateExistingEntry() {
        Entry existing = newEntry("Conta antiga", "Descricao antiga", new BigDecimal("10.00"));
        Entry updatedData = newEntry("Conta nova", "Descricao nova", new BigDecimal("99.90"));

        when(entryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(entryRepository.save(existing)).thenReturn(existing);

        Entry updated = entryService.update(1L, updatedData);

        assertThat(updated.getAccount()).isEqualTo("Conta nova");
        assertThat(updated.getDescription()).isEqualTo("Descricao nova");
        assertThat(updated.getAmount()).isEqualByComparingTo("99.90");
        verify(entryRepository).save(existing);
    }

    @Test
    void shouldFailUpdateWhenEntryDoesNotExist() {
        Entry updatedData = newEntry("Conta", "Descricao", new BigDecimal("99.90"));
        when(entryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entryService.update(999L, updatedData))
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

    private Entry newEntry(String account, String description, BigDecimal amount) {
        Entry entry = new Entry();
        entry.setAccount(account);
        entry.setDescription(description);
        entry.setCategory("Geral");
        entry.setMovementDate(LocalDate.of(2026, 2, 11));
        entry.setSettlementDate(LocalDate.of(2026, 2, 11));
        entry.setAmount(amount);
        return entry;
    }
}
