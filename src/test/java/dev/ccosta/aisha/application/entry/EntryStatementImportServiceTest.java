package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.statement.EntryStatementFormat;
import dev.ccosta.aisha.application.entry.statement.EntryStatementImportRecord;
import dev.ccosta.aisha.application.entry.statement.EntryStatementImportService;
import dev.ccosta.aisha.application.entry.statement.EntryStatementParser;
import dev.ccosta.aisha.application.entry.statement.EntryStatementParserRegistry;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntryStatementImportServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private EntryStatementParserRegistry parserRegistry;

    @Mock
    private EntryStatementParser parser;

    @Mock
    private EntryCategorySuggestionService entryCategorySuggestionService;

    @InjectMocks
    private EntryStatementImportService entryStatementImportService;

    @Test
    void shouldImportStatementAndSkipDuplicatesFromSameFile() {
        Account account = newAccount(1L, "Conta Corrente");
        when(accountService.findById(1L)).thenReturn(account);
        when(parserRegistry.resolve("basic-csv")).thenReturn(parser);
        when(parser.parse(any(byte[].class))).thenReturn(List.of(
            new EntryStatementImportRecord(2, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2), "Compra", new BigDecimal("10.00"), null, "ext-1"),
            new EntryStatementImportRecord(3, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2), "Compra", new BigDecimal("10.00"), null, "ext-1")
        ));
        when(entryRepository.existsDuplicate(anyLong(), any(LocalDate.class), any(LocalDate.class), any(String.class), eq(null), any(BigDecimal.class), any(String.class)))
            .thenReturn(false);
        when(entryRepository.existsDuplicateIgnoringCategory(anyLong(), any(LocalDate.class), any(LocalDate.class), any(String.class), any(BigDecimal.class), any(String.class)))
            .thenReturn(false);
        when(entryCategorySuggestionService.suggest(any())).thenReturn(java.util.Optional.empty());

        EntryImportSummary summary = entryStatementImportService.importStatement(1L, "basic-csv", new byte[] {1, 2, 3}, null);

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isEqualTo(1);
        assertThat(summary.createdAccountsCount()).isZero();
        assertThat(summary.createdCategoriesCount()).isZero();

        ArgumentCaptor<Entry> savedEntryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository).save(savedEntryCaptor.capture());
        assertThat(savedEntryCaptor.getValue().getAccount().getId()).isEqualTo(1L);
        assertThat(savedEntryCaptor.getValue().getCategory()).isNull();
    }

    @Test
    void shouldAdjustInitialBalanceForBackdatedImportedRecords() {
        Account account = newAccount(1L, "Conta Corrente");
        account.setInitialBalance(new BigDecimal("100.00"));
        account.setInitialBalanceDate(LocalDate.of(2026, 1, 31));

        when(accountService.findById(1L)).thenReturn(account);
        when(parserRegistry.resolve("basic-csv")).thenReturn(parser);
        when(parser.parse(any(byte[].class))).thenReturn(List.of(
            new EntryStatementImportRecord(1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 15), "Compra 1", new BigDecimal("10.00"), null, null),
            new EntryStatementImportRecord(2, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5), "Compra 2", new BigDecimal("20.00"), null, null)
        ));
        when(entryRepository.existsDuplicate(anyLong(), any(LocalDate.class), any(LocalDate.class), any(String.class), eq(null), any(BigDecimal.class), eq(null)))
            .thenReturn(false);
        when(entryRepository.existsDuplicateIgnoringCategory(anyLong(), any(LocalDate.class), any(LocalDate.class), any(String.class), any(BigDecimal.class), eq(null)))
            .thenReturn(false);
        when(entryCategorySuggestionService.suggest(any())).thenReturn(java.util.Optional.empty());

        EntryImportSummary summary = entryStatementImportService.importStatement(1L, "basic-csv", new byte[] {1}, null);

        assertThat(summary.importedCount()).isEqualTo(2);
        ArgumentCaptor<Map<Long, LocalDate>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(accountService).adjustInitialBalanceForBackdatedEntries(mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsExactlyEntriesOf(Map.of(1L, LocalDate.of(2026, 1, 5)));
    }

    @Test
    void shouldRejectInactiveAccount() {
        Account account = newAccount(1L, "Conta Inativa");
        account.setDeactivationDate(LocalDate.of(2026, 1, 1));
        when(accountService.findById(1L)).thenReturn(account);

        assertThatThrownBy(() -> entryStatementImportService.importStatement(1L, "basic-csv", new byte[] {1}, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("active");

        verify(parserRegistry, never()).resolve(any(String.class));
    }

    @Test
    void shouldExposeAvailableFormatsFromRegistry() {
        when(parserRegistry.listFormats()).thenReturn(List.of(new EntryStatementFormat("basic-csv", "label.key", "help.key")));

        List<EntryStatementFormat> result = entryStatementImportService.listAvailableFormats();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("basic-csv");
    }

    private Account newAccount(Long id, String title) {
        Account account = new Account();
        account.setTitle(title);
        setId(account, id);
        return account;
    }

    private void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
