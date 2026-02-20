package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class EntryCsvImportServiceTest {

    private static final String AMOUNT_PATTERN_PT_BR = "^-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d{1,2})?$";
    private static final String AMOUNT_PATTERN_US = "^-?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?$";

    @Mock
    private EntryRepository entryRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private EntryCsvImportService entryCsvImportService;

    @Test
    void shouldImportSemicolonCsvAndSkipDuplicateFromSameFile() {
        when(accountService.listAllOrdered()).thenReturn(List.of());
        when(categoryService.listAllOrdered()).thenReturn(List.of());
        when(accountService.create(any(Account.class))).thenReturn(newAccount(1L, "Conta A"));
        when(categoryService.create(any(Category.class), eq(null))).thenReturn(newCategory(2L, "Mercado"));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta;Data de Movimentação;Data de Liquidação;Descrição;Categoria;Valor\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;1.234,56\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;1.234,56\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, true),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isEqualTo(1);
        assertThat(summary.createdAccountsCount()).isEqualTo(1);
        assertThat(summary.createdCategoriesCount()).isEqualTo(1);
        verify(entryRepository, times(1)).save(any(Entry.class));
        verify(entryRepository, times(1))
            .existsDuplicate(1L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 2), "Compra", 2L, new BigDecimal("1234.56"), null);
    }

    @Test
    void shouldSkipDuplicateAlreadyPersisted() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(true);

        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, false),
            null
        );

        assertThat(summary.importedCount()).isZero();
        assertThat(summary.skippedDuplicateCount()).isEqualTo(1);
        assertThat(summary.createdAccountsCount()).isZero();
        assertThat(summary.createdCategoriesCount()).isZero();
        verify(entryRepository, never()).save(any(Entry.class));
    }

    @Test
    void shouldTreatDifferentExternalIdsAsDifferentEntries() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                any(String.class)
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00;Obs 1;ext-1\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00;Obs 2;ext-2\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, false),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(2);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    @Test
    void shouldFailWhenRequiredFieldIsMissing() {
        String csv = ";2026-02-01;2026-02-02;Compra;Mercado;10,00\n";

        assertThatThrownBy(() -> entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, false),
            null
        ))
            .isInstanceOf(EntryImportValidationException.class)
            .satisfies(ex -> {
                EntryImportValidationException validationException = (EntryImportValidationException) ex;
                assertThat(validationException.getRowPosition()).isEqualTo(1);
                assertThat(validationException.getCauseType()).isEqualTo(EntryImportFailureCause.MISSING_REQUIRED_FIELD);
                assertThat(validationException.getColumnName()).isEqualTo("account");
            });
    }

    @Test
    void shouldParseCommaDelimitedCsvWhenAmountIsQuoted() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta,Data de Movimentação,Data de Liquidação,Descrição,Categoria,Valor\n"
            + "Conta A,2026-02-01,2026-02-02,Compra,Mercado,\"1.234,56\"\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(',', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, true),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    @Test
    void shouldMapOptionalNotesAndExternalIdColumns() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                any(String.class)
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta;Data de Movimentação;Data de Liquidação;Descrição;Categoria;Valor;Observação;Id Externo\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00;Compra semanal;ext-123\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, true),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isZero();
        verify(entryRepository).existsDuplicate(
            11L,
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 2),
            "Compra",
            21L,
            new BigDecimal("10.00"),
            "ext-123"
        );
        ArgumentCaptor<Entry> savedEntryCaptor = ArgumentCaptor.forClass(Entry.class);
        verify(entryRepository).save(savedEntryCaptor.capture());
        assertThat(savedEntryCaptor.getValue().getNotes()).isEqualTo("Compra semanal");
        assertThat(savedEntryCaptor.getValue().getExternalId()).isEqualTo("ext-123");
    }

    @Test
    void shouldParseCustomDatePattern() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta A;01/02/2026;02/02/2026;Compra;Mercado;10,00\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "dd/MM/uuuu", AMOUNT_PATTERN_PT_BR, false),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    @Test
    void shouldFailWhenDatePatternIsInvalid() {
        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00\n";

        assertThatThrownBy(() -> entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "dd/MM/'", AMOUNT_PATTERN_PT_BR, false),
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid date format pattern");
    }

    @Test
    void shouldParseUsAmountFormat() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;1,234.56\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_US, false),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    @Test
    void shouldParsePtBrAmountWithZeroOrOneDecimalPlaces() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;1.234\n"
            + "Conta A;2026-02-01;2026-02-02;Compra 2;Mercado;10,5\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, false),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(2);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    @Test
    void shouldFailWhenAmountPatternIsInvalid() {
        String csv = "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00\n";

        assertThatThrownBy(() -> entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", "[", false),
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid amount format pattern");
    }

    @Test
    void shouldAdjustInitialBalanceForExistingAccountAfterImportingBackdatedEntries() {
        Account existingAccount = newAccount(11L, "Conta A");
        existingAccount.setInitialBalance(new BigDecimal("100.00"));
        existingAccount.setInitialBalanceDate(LocalDate.of(2025, 12, 31));

        when(accountService.listAllOrdered()).thenReturn(List.of(existingAccount));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "Conta A;2025-12-15;2025-12-15;Compra 1;Mercado;10,00\n"
            + "Conta A;2025-12-05;2025-12-05;Compra 2;Mercado;25,00\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, false),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(2);
        ArgumentCaptor<Map<Long, LocalDate>> captor = ArgumentCaptor.forClass(Map.class);
        verify(accountService).adjustInitialBalanceForBackdatedEntries(captor.capture());
        assertThat(captor.getValue()).containsExactlyEntriesOf(Map.of(11L, LocalDate.of(2025, 12, 5)));
    }

    @Test
    void shouldReportPhysicalFileLineWhenHeaderExistsAndAmountIsInvalid() {
        String csv = "Conta;Data de Movimentação;Data de Liquidação;Descrição;Categoria;Valor\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;valor-invalido\n";

        assertThatThrownBy(() -> entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, true),
            null
        ))
            .isInstanceOf(EntryImportValidationException.class)
            .satisfies(ex -> {
                EntryImportValidationException validationException = (EntryImportValidationException) ex;
                assertThat(validationException.getRowPosition()).isEqualTo(2);
                assertThat(validationException.getColumnName()).isEqualTo("amount");
            });
    }

    @Test
    void shouldRespectHeaderOptionAndSkipFirstRow() {
        when(accountService.listAllOrdered()).thenReturn(List.of(newAccount(11L, "Conta A")));
        when(categoryService.listAllOrdered()).thenReturn(List.of(newCategory(21L, "Mercado")));
        when(
            entryRepository.existsDuplicate(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class),
                any(String.class),
                anyLong(),
                any(BigDecimal.class),
                isNull()
            )
        )
            .thenReturn(false);
        when(entryRepository.save(any(Entry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv = "IGNORAR;IGNORAR;IGNORAR;IGNORAR;IGNORAR;IGNORAR\n"
            + "Conta A;2026-02-01;2026-02-02;Compra;Mercado;10,00\n";

        EntryImportSummary summary = entryCsvImportService.importCsv(
            csv.getBytes(StandardCharsets.UTF_8),
            options(';', "uuuu-MM-dd", AMOUNT_PATTERN_PT_BR, true),
            null
        );

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.skippedDuplicateCount()).isZero();
    }

    private Account newAccount(Long id, String title) {
        Account account = new Account();
        account.setTitle(title);
        setId(account, id);
        return account;
    }

    private Category newCategory(Long id, String title) {
        Category category = new Category();
        category.setTitle(title);
        setId(category, id);
        return category;
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

    private EntryCsvImportOptions options(char delimiter, String datePattern, String amountPattern, boolean hasHeader) {
        return new EntryCsvImportOptions(delimiter, datePattern, amountPattern, hasHeader);
    }
}
