package dev.ccosta.aisha.application.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.category.Category;
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
class CategoryBalanceReportServiceTest {

    @Mock
    private EntryRepository entryRepository;

    @InjectMocks
    private CategoryBalanceReportService categoryBalanceReportService;

    @Test
    void shouldBuildMonthlyBalancesForOneYearRange() {
        Category housing = newCategory(1L, "Moradia");
        Category leisure = newCategory(2L, "Lazer");

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 12, 31))).thenReturn(List.of(
            newEntry(1L, LocalDate.of(2025, 12, 10), "-100.00"),
            newEntry(1L, LocalDate.of(2026, 1, 10), "-500.00"),
            newEntry(1L, LocalDate.of(2026, 2, 5), "-120.50"),
            newEntry(2L, LocalDate.of(2026, 2, 8), "-40.00")
        ));

        CategoryBalanceReport report = categoryBalanceReportService.buildReport(
            List.of(housing, leisure),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31)
        );

        assertThat(report.granularity()).isEqualTo(CategoryBalanceGranularity.MONTH);
        assertThat(report.buckets()).hasSize(12);
        assertThat(report.buckets().get(0).startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(report.buckets().get(11).startDate()).isEqualTo(LocalDate.of(2026, 12, 1));

        CategoryBalanceRow housingRow = report.rows().get(0);
        assertThat(housingRow.previousPeriodBalance()).isEqualByComparingTo("-100.00");
        assertThat(housingRow.periodBalances().get(0)).isEqualByComparingTo("-500.00");
        assertThat(housingRow.periodBalances().get(1)).isEqualByComparingTo("-120.50");
        assertThat(housingRow.periodBalances().get(2)).isEqualByComparingTo(BigDecimal.ZERO);

        CategoryBalanceRow leisureRow = report.rows().get(1);
        assertThat(leisureRow.previousPeriodBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(leisureRow.periodBalances().get(1)).isEqualByComparingTo("-40.00");
        assertThat(leisureRow.periodBalances().get(10)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldBuildDailyBalancesForShortRange() {
        Category housing = newCategory(1L, "Moradia");

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 2, 13))).thenReturn(List.of(
            newEntry(1L, LocalDate.of(2026, 2, 9), "-200.00"),
            newEntry(1L, LocalDate.of(2026, 2, 10), "-50.00"),
            newEntry(1L, LocalDate.of(2026, 2, 12), "25.00")
        ));

        CategoryBalanceReport report = categoryBalanceReportService.buildReport(
            List.of(housing),
            LocalDate.of(2026, 2, 10),
            LocalDate.of(2026, 2, 13)
        );

        assertThat(report.granularity()).isEqualTo(CategoryBalanceGranularity.DAY);
        assertThat(report.buckets()).hasSize(4);

        CategoryBalanceRow row = report.rows().getFirst();
        assertThat(row.previousPeriodBalance()).isEqualByComparingTo("-200.00");
        assertThat(row.periodBalances()).containsExactly(
            new BigDecimal("-50.00"),
            BigDecimal.ZERO,
            new BigDecimal("25.00"),
            BigDecimal.ZERO
        );
    }

    private Category newCategory(Long id, String title) {
        Category category = new Category();
        category.setTitle(title);

        try {
            var idField = Category.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        return category;
    }

    private Entry newEntry(Long categoryId, LocalDate settlementDate, String amount) {
        Entry entry = new Entry();
        entry.setCategory(newCategory(categoryId, "Categoria"));
        entry.setSettlementDate(settlementDate);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }
}
