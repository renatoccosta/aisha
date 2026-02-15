package dev.ccosta.aisha.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
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

    @Mock
    private CategoryRepository categoryRepository;

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
        assertThat(evolution.points()).hasSize(2);
        assertThat(evolution.points().get(0).periodAmount()).isEqualByComparingTo("10.00");
        assertThat(evolution.points().get(0).accumulatedBalance()).isEqualByComparingTo("210.00");
        assertThat(evolution.points().get(1).periodAmount()).isEqualByComparingTo("-3.00");
        assertThat(evolution.points().get(1).accumulatedBalance()).isEqualByComparingTo("207.00");
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

    @Test
    void shouldBuildDailyRevenueExpenseEvolutionForRangeShorterThanTwoMonths() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 3))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 1), "15.00"),
            newEntry(LocalDate.of(2026, 1, 1), "-4.00"),
            newEntry(LocalDate.of(2026, 1, 2), "-3.50")
        ));

        DashboardRevenueExpenseEvolution evolution = dashboardService.buildRevenueExpenseEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 3)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.DAY);
        assertThat(evolution.points()).hasSize(2);
        assertThat(evolution.points().get(0).revenues()).isEqualByComparingTo("15.00");
        assertThat(evolution.points().get(0).expenses()).isEqualByComparingTo("4.00");
        assertThat(evolution.points().get(1).revenues()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(evolution.points().get(1).expenses()).isEqualByComparingTo("3.50");
    }

    @Test
    void shouldTrimTrailingMonthlyBucketsWithoutRecordsInBalanceEvolution() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 6, 30))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 20), "50.00"),
            newEntry(LocalDate.of(2026, 2, 10), "-10.00")
        ));

        DashboardBalanceEvolution evolution = dashboardService.buildBalanceEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.points()).hasSize(2);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(1).date()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void shouldTrimTrailingMonthlyBucketsWithoutRecordsInRevenueExpenseEvolution() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 5, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 15), "100.00"),
            newEntry(LocalDate.of(2026, 3, 10), "-20.00")
        ));

        DashboardRevenueExpenseEvolution evolution = dashboardService.buildRevenueExpenseEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 5, 31)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.points()).hasSize(3);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(1).date()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(evolution.points().get(2).date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void shouldBuildMonthlyRevenueExpenseEvolutionForRangeOfTwoMonthsOrMore() {
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 20))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 20), "100.00"),
            newEntry(LocalDate.of(2026, 2, 5), "-25.00"),
            newEntry(LocalDate.of(2026, 2, 20), "40.00"),
            newEntry(LocalDate.of(2026, 3, 2), "-10.00")
        ));

        DashboardRevenueExpenseEvolution evolution = dashboardService.buildRevenueExpenseEvolution(
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 3, 20)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.points()).hasSize(3);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(0).revenues()).isEqualByComparingTo("100.00");
        assertThat(evolution.points().get(0).expenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(evolution.points().get(1).revenues()).isEqualByComparingTo("40.00");
        assertThat(evolution.points().get(1).expenses()).isEqualByComparingTo("25.00");
        assertThat(evolution.points().get(2).revenues()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(evolution.points().get(2).expenses()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldBuildExpenseCategoryBreakdownForRootCategories() {
        Category rootHousing = newCategory(1L, "Moradia", null);
        Category rootFood = newCategory(2L, "Alimentação", null);
        Category rootTransport = newCategory(3L, "Transporte", null);
        Category rootHealth = newCategory(4L, "Saúde", null);
        Category rootEducation = newCategory(5L, "Educação", null);
        Category rootLeisure = newCategory(6L, "Lazer", null);
        Category rootServices = newCategory(7L, "Serviços", null);
        Category subMarket = newCategory(8L, "Mercado", rootFood);
        Category subRestaurant = newCategory(9L, "Restaurante", rootFood);

        when(categoryRepository.findAllOrdered()).thenReturn(List.of(
            rootHousing,
            rootFood,
            rootTransport,
            rootHealth,
            rootEducation,
            rootLeisure,
            rootServices,
            subMarket,
            subRestaurant
        ));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 2, 28))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 2, 1), "-100.00", rootHousing),
            newEntry(LocalDate.of(2026, 2, 2), "-80.00", subMarket),
            newEntry(LocalDate.of(2026, 2, 3), "-20.00", subRestaurant),
            newEntry(LocalDate.of(2026, 2, 4), "-60.00", rootTransport),
            newEntry(LocalDate.of(2026, 2, 5), "-40.00", rootHealth),
            newEntry(LocalDate.of(2026, 2, 6), "-30.00", rootEducation),
            newEntry(LocalDate.of(2026, 2, 7), "-20.00", rootLeisure),
            newEntry(LocalDate.of(2026, 2, 8), "-10.00", rootServices)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28),
            null
        );

        assertThat(breakdown.currentParentCategoryId()).isNull();
        assertThat(breakdown.drillUpParentCategoryId()).isNull();
        assertThat(breakdown.items()).hasSize(7);
        assertThat(breakdown.items().get(0).categoryName()).isEqualTo("Moradia");
        assertThat(breakdown.items().get(0).amount()).isEqualByComparingTo("100.00");
        assertThat(breakdown.items().get(1).categoryName()).isEqualTo("Alimentação");
        assertThat(breakdown.items().get(1).amount()).isEqualByComparingTo("100.00");
        assertThat(breakdown.items().get(1).hasChildren()).isTrue();
    }

    @Test
    void shouldReturnOnlyExistingCategoriesWhenLessThanFive() {
        Category rootHouse = newCategory(10L, "Casa", null);
        Category rootTransport = newCategory(11L, "Transporte", null);
        when(categoryRepository.findAllOrdered()).thenReturn(List.of(rootHouse, rootTransport));

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "-12.00", rootHouse),
            newEntry(LocalDate.of(2026, 1, 11), "-8.00", rootTransport)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            null
        );

        assertThat(breakdown.items()).hasSize(2);
        assertThat(breakdown.items().get(0).categoryName()).isEqualTo("Casa");
        assertThat(breakdown.items().get(0).hasChildren()).isFalse();
        assertThat(breakdown.items().get(1).categoryName()).isEqualTo("Transporte");
    }

    @Test
    void shouldDrillDownIntoSelectedRootCategory() {
        Category rootFood = newCategory(20L, "Alimentação", null);
        Category rootHealth = newCategory(21L, "Saúde", null);
        Category subMarket = newCategory(22L, "Mercado", rootFood);
        Category subRestaurant = newCategory(23L, "Restaurante", rootFood);
        Category subPharmacy = newCategory(24L, "Farmácia", rootHealth);

        when(categoryRepository.findAllOrdered()).thenReturn(List.of(
            rootFood,
            rootHealth,
            subMarket,
            subRestaurant,
            subPharmacy
        ));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 3, 1), "-50.00", subMarket),
            newEntry(LocalDate.of(2026, 3, 2), "-30.00", subRestaurant),
            newEntry(LocalDate.of(2026, 3, 3), "-20.00", subPharmacy)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            rootFood.getId()
        );

        assertThat(breakdown.currentParentCategoryId()).isEqualTo(rootFood.getId());
        assertThat(breakdown.currentParentCategoryName()).isEqualTo("Alimentação");
        assertThat(breakdown.drillUpParentCategoryId()).isNull();
        assertThat(breakdown.items()).hasSize(2);
        assertThat(breakdown.items().get(0).categoryName()).isEqualTo("Mercado");
        assertThat(breakdown.items().get(0).amount()).isEqualByComparingTo("50.00");
        assertThat(breakdown.items().get(1).categoryName()).isEqualTo("Restaurante");
    }

    private Entry newEntry(LocalDate settlementDate, String amount) {
        return newEntry(settlementDate, amount, "Geral");
    }

    private Entry newEntry(LocalDate settlementDate, String amount, String categoryName) {
        return newEntry(settlementDate, amount, newCategory(999L, categoryName, null));
    }

    private Entry newEntry(LocalDate settlementDate, String amount, Category category) {
        Entry entry = new Entry();
        entry.setAccount(newAccount());
        entry.setCategory(category);
        entry.setSettlementDate(settlementDate);
        entry.setAmount(new BigDecimal(amount));
        return entry;
    }

    private Account newAccount() {
        Account account = new Account();
        account.setTitle("Conta");
        return account;
    }

    private Category newCategory(Long id, String title, Category parent) {
        Category category = new Category();
        category.setTitle(title);
        category.setParent(parent);
        setId(category, id, Category.class);
        return category;
    }

    private <T> void setId(T target, Long id, Class<T> type) {
        try {
            var idField = type.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
