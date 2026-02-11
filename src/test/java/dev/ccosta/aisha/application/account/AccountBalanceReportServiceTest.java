package dev.ccosta.aisha.application.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountBalanceReportServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private AccountBalanceReportService accountBalanceReportService;

    @Test
    void shouldBuildMonthlyBalancesForOneYearRange() {
        Account checking = newAccount(1L, "Conta Corrente");
        Account cash = newAccount(2L, "Carteira");

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 12, 31))).thenReturn(List.of(
            newEntry(1L, LocalDate.of(2025, 12, 10), "100.00"),
            newEntry(1L, LocalDate.of(2026, 1, 10), "500.00"),
            newEntry(1L, LocalDate.of(2026, 2, 5), "-120.50"),
            newEntry(2L, LocalDate.of(2026, 2, 8), "40.00")
        ));

        AccountBalanceReport report = accountBalanceReportService.buildReport(
            List.of(checking, cash),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31)
        );

        assertThat(report.granularity()).isEqualTo(AccountBalanceGranularity.MONTH);
        assertThat(report.buckets()).hasSize(12);
        assertThat(report.buckets().get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(report.buckets().get(11).startDate()).isEqualTo(LocalDate.of(2026, 12, 1));

        AccountBalanceRow checkingRow = report.rows().get(0);
        assertThat(checkingRow.previousPeriodBalance()).isEqualByComparingTo("100.00");
        assertThat(checkingRow.periodBalances().get(0)).isEqualByComparingTo("500.00");
        assertThat(checkingRow.periodBalances().get(1)).isEqualByComparingTo("-120.50");
        assertThat(checkingRow.periodBalances().get(2)).isEqualByComparingTo(BigDecimal.ZERO);

        AccountBalanceRow cashRow = report.rows().get(1);
        assertThat(cashRow.previousPeriodBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cashRow.periodBalances().get(1)).isEqualByComparingTo("40.00");
        assertThat(cashRow.periodBalances().get(10)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldBuildDailyBalancesForShortRange() {
        Account checking = newAccount(1L, "Conta Corrente");

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 2, 13))).thenReturn(List.of(
            newEntry(1L, LocalDate.of(2026, 2, 9), "200.00"),
            newEntry(1L, LocalDate.of(2026, 2, 10), "-50.00"),
            newEntry(1L, LocalDate.of(2026, 2, 12), "25.00")
        ));

        AccountBalanceReport report = accountBalanceReportService.buildReport(
            List.of(checking),
            LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 13)
        );

        assertThat(report.granularity()).isEqualTo(AccountBalanceGranularity.DAY);
        assertThat(report.buckets()).hasSize(4);

        AccountBalanceRow row = report.rows().getFirst();
        assertThat(row.previousPeriodBalance()).isEqualByComparingTo("200.00");
        assertThat(row.periodBalances()).containsExactly(
            new BigDecimal("-50.00"),
            BigDecimal.ZERO,
            new BigDecimal("25.00"),
            BigDecimal.ZERO
        );
    }

    private Account newAccount(Long id, String title) {
        Account account = new Account();
        account.setTitle(title);

        try {
            var idField = Account.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        return account;
    }

    private Entry newEntry(Long accountId, LocalDate settlementDate, String amount) {
        Entry entry = new Entry();
        entry.setAccount(newAccount(accountId, "Conta"));
        entry.setSettlementDate(settlementDate);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }
}
