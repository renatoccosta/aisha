package dev.ccosta.aisha.application.dashboard;

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
class DashboardServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldBuildSummaryWithPreviousEquivalentPeriod() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 2, 10), "80.00"),
            newEntry(LocalDate.of(2026, 2, 12), "-30.00"),
            newEntry(LocalDate.of(2026, 3, 1), "100.00"),
            newEntry(LocalDate.of(2026, 3, 5), "-40.00"),
            newEntry(LocalDate.of(2026, 3, 20), "60.00")
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(summary.currentBalance().currentValue()).isEqualByComparingTo("170.00");
        assertThat(summary.currentBalance().previousValue()).isEqualByComparingTo("50.00");
        assertThat(summary.currentBalance().variationPercent()).isEqualByComparingTo("240.00");

        assertThat(summary.totalExpenses().currentValue()).isEqualByComparingTo("40.00");
        assertThat(summary.totalExpenses().previousValue()).isEqualByComparingTo("30.00");
        assertThat(summary.totalExpenses().variationPercent()).isEqualByComparingTo("33.33");

        assertThat(summary.totalRevenues().currentValue()).isEqualByComparingTo("160.00");
        assertThat(summary.totalRevenues().previousValue()).isEqualByComparingTo("80.00");
        assertThat(summary.totalRevenues().variationPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnNullVariationWhenPreviousValueIsZeroAndCurrentHasValue() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "90.00")
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(summary.totalRevenues().variationPercent()).isNull();
        assertThat(summary.totalExpenses().variationPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldBuildDailyEvolutionForRangeShorterThanTwoMonths() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 3))).thenReturn(List.of(
            newEntry(LocalDate.of(2025, 12, 31), "200.00"),
            newEntry(LocalDate.of(2026, 1, 1), "10.00"),
            newEntry(LocalDate.of(2026, 1, 2), "-3.00")
        ));

        DashboardBalanceEvolution evolution = dashboardService.buildBalanceEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 3)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.DAY);
        assertThat(evolution.openingBalance()).isEqualByComparingTo("200.00");
        assertThat(evolution.points()).hasSize(3);
        assertThat(evolution.points().get(0).periodAmount()).isEqualByComparingTo("10.00");
        assertThat(evolution.points().get(0).accumulatedBalance()).isEqualByComparingTo("210.00");
        assertThat(evolution.points().get(1).periodAmount()).isEqualByComparingTo("-3.00");
        assertThat(evolution.points().get(1).accumulatedBalance()).isEqualByComparingTo("207.00");
        assertThat(evolution.points().get(2).periodAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldBuildMonthlyEvolutionForRangeOfTwoMonthsOrMore() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 15))).thenReturn(List.of(
            newEntry(LocalDate.of(2025, 12, 10), "40.00"),
            newEntry(LocalDate.of(2026, 1, 20), "100.00"),
            newEntry(LocalDate.of(2026, 2, 10), "-25.00"),
            newEntry(LocalDate.of(2026, 3, 5), "10.00")
        ));

        DashboardBalanceEvolution evolution = dashboardService.buildBalanceEvolution(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 3, 15)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.points()).hasSize(3);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(0).periodAmount()).isEqualByComparingTo("100.00");
        assertThat(evolution.points().get(1).periodAmount()).isEqualByComparingTo("-25.00");
        assertThat(evolution.points().get(2).periodAmount()).isEqualByComparingTo("10.00");
    }

    private Entry newEntry(LocalDate settlementDate, String amount) {
        Entry entry = new Entry();
        entry.setAccount(newAccount());
        entry.setSettlementDate(settlementDate);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }

    private Account newAccount() {
        Account account = new Account();
        account.setTitle("Conta");
        return account;
    }
}
