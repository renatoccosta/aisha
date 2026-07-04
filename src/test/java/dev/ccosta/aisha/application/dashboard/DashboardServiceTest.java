package dev.ccosta.aisha.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.account.AccountType;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.asset.AssetType;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private InvestmentOperationRepository investmentOperationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(assetRepository.findAllOrdered()).thenReturn(List.of());
        lenient().when(investmentOperationRepository.findAllOrdered()).thenReturn(List.of());
    }

    @Test
    void shouldBuildSummaryWithPreviousEquivalentPeriod() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of(
            newAccount("Conta A", "100.00", LocalDate.of(2026, 2, 1)),
            newAccount("Conta B", "50.00", LocalDate.of(2026, 3, 10))
        ));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 2, 10), "80.00"),
            newEntry(LocalDate.of(2026, 2, 12), "-30.00"),
            newEntry(LocalDate.of(2026, 3, 1), "100.00"),
            newEntry(LocalDate.of(2026, 3, 5), "-40.00"),
            newEntry(LocalDate.of(2026, 3, 20), "60.00")
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(summary.currentBalance().currentValue()).isEqualByComparingTo("320.00");
        assertThat(summary.currentBalance().previousValue()).isEqualByComparingTo("150.00");
        assertThat(summary.currentBalance().variationPercent()).isEqualByComparingTo("113.33");

        assertThat(summary.totalExpenses().currentValue()).isEqualByComparingTo("40.00");
        assertThat(summary.totalExpenses().previousValue()).isEqualByComparingTo("30.00");
        assertThat(summary.totalExpenses().variationPercent()).isEqualByComparingTo("33.33");

        assertThat(summary.totalRevenues().currentValue()).isEqualByComparingTo("160.00");
        assertThat(summary.totalRevenues().previousValue()).isEqualByComparingTo("80.00");
        assertThat(summary.totalRevenues().variationPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnNullVariationWhenPreviousValueIsZeroAndCurrentHasValue() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of());
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "90.00")
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(summary.totalRevenues().variationPercent()).isNull();
        assertThat(summary.totalExpenses().variationPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }


    @Test
    void shouldBuildAccountTypeBalancesInSummary() {
        Account checking = newAccount("Conta Corrente", "100.00", LocalDate.of(2026, 1, 1));
        setId(checking, 1L, Account.class);
        checking.setAccountType(AccountType.CHECKING);

        Account credit = newAccount("Cartão", "-50.00", LocalDate.of(2026, 1, 1));
        setId(credit, 2L, Account.class);
        credit.setAccountType(AccountType.CREDIT);

        when(accountRepository.findAllOrdered()).thenReturn(List.of(checking, credit));

        Entry checkingEntry = newEntry(LocalDate.of(2026, 1, 10), "25.00");
        checkingEntry.setAccount(checking);
        Entry creditEntry = newEntry(LocalDate.of(2026, 1, 10), "-10.00");
        creditEntry.setAccount(credit);

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(checkingEntry, creditEntry));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(summary.accountTypeBalances()).extracting(item -> item.accountType().name())
            .containsExactly("CHECKING", "CREDIT", "INVESTMENT", "FOOD", "OTHER");
        assertThat(summary.accountTypeBalances().get(0).balance()).isEqualByComparingTo("125.00");
        assertThat(summary.accountTypeBalances().get(1).balance()).isEqualByComparingTo("-60.00");
        assertThat(summary.accountTypeBalances().get(4).balance()).isEqualByComparingTo("0.00");
        assertThat(summary.accountBalances()).extracting(item -> item.accountTitle())
            .containsExactly("Conta Corrente", "Cartão");
        assertThat(summary.accountBalances().get(0).balance()).isEqualByComparingTo("125.00");
        assertThat(summary.accountBalances().get(1).balance()).isEqualByComparingTo("-60.00");
    }

    @Test
    void shouldBuildInvestmentOverviewUsingHistoricalCost() {
        Account investmentAccount = newAccount("Carteira XP", "0.00", LocalDate.of(2026, 1, 1));
        investmentAccount.setAccountType(AccountType.INVESTMENT);
        setId(investmentAccount, 90L, Account.class);

        Asset asset = newAsset(10L, investmentAccount, "PETR4", AssetType.STOCK, "BRL");
        asset.setOpeningPositionDate(LocalDate.of(2026, 1, 31));
        asset.setOpeningPositionQuantity(new BigDecimal("10.0000000000"));
        asset.setOpeningPositionTotalCost(new BigDecimal("100.00"));
        asset.setOpeningPositionCurrency("BRL");

        when(accountRepository.findAllOrdered()).thenReturn(List.of());
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 31))).thenReturn(List.of());
        when(assetRepository.findAllOrdered()).thenReturn(List.of(asset));
        when(investmentOperationRepository.findAllOrdered()).thenReturn(List.of(
            newOperation(asset, InvestmentOperationType.BUY, LocalDate.of(2026, 3, 5), "5.0000000000", "50.00"),
            newOperation(asset, InvestmentOperationType.DIVIDEND, LocalDate.of(2026, 3, 10), null, "10.00"),
            newOperation(asset, InvestmentOperationType.SELL, LocalDate.of(2026, 3, 15), "3.0000000000", "30.00")
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(summary.investmentOverview().positionCost().currentValue()).isEqualByComparingTo("120.00");
        assertThat(summary.investmentOverview().positionCost().previousValue()).isEqualByComparingTo("100.00");
        assertThat(summary.investmentOverview().periodNetFlow().currentValue()).isEqualByComparingTo("-10.00");
        assertThat(summary.investmentOverview().periodIncome().currentValue()).isEqualByComparingTo("10.00");
        assertThat(summary.investmentOverview().openAssetCount()).isEqualTo(1);
        assertThat(summary.investmentOverview().excludedAssetCount()).isEqualTo(0);
        assertThat(summary.investmentOverview().allocationsByAssetType()).extracting(DashboardInvestmentAllocation::key)
            .containsExactly("STOCK");
        assertThat(summary.investmentOverview().allocationsByAssetType().getFirst().amount()).isEqualByComparingTo("120.00");
    }

    @Test
    void shouldBuildInvestmentFlowEvolutionByMonth() {
        Account investmentAccount = newAccount("Carteira XP", "0.00", LocalDate.of(2026, 1, 1));
        investmentAccount.setAccountType(AccountType.INVESTMENT);
        setId(investmentAccount, 91L, Account.class);

        Asset asset = newAsset(11L, investmentAccount, "Tesouro Selic", AssetType.BOND_GOV, "BRL");
        when(assetRepository.findAllOrdered()).thenReturn(List.of(asset));
        when(investmentOperationRepository.findAllOrdered()).thenReturn(List.of(
            newOperation(asset, InvestmentOperationType.BUY, LocalDate.of(2026, 1, 10), "1.0000000000", "100.00"),
            newOperation(asset, InvestmentOperationType.DIVIDEND, LocalDate.of(2026, 3, 5), null, "12.00"),
            newOperation(asset, InvestmentOperationType.FEE, LocalDate.of(2026, 3, 8), null, "2.00")
        ));

        DashboardInvestmentFlowEvolution evolution = dashboardService.buildInvestmentFlowEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31)
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.points()).hasSize(3);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(0).inflows()).isEqualByComparingTo("0.00");
        assertThat(evolution.points().get(0).outflows()).isEqualByComparingTo("100.00");
        assertThat(evolution.points().get(0).netFlow()).isEqualByComparingTo("-100.00");
        assertThat(evolution.points().get(2).date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(evolution.points().get(2).inflows()).isEqualByComparingTo("12.00");
        assertThat(evolution.points().get(2).outflows()).isEqualByComparingTo("2.00");
        assertThat(evolution.points().get(2).netFlow()).isEqualByComparingTo("10.00");
    }

    @Test
    void shouldBuildDailyEvolutionForRangeShorterThanTwoMonths() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of());
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
    void shouldIncludeInitialBalanceInsidePeriodInBalanceEvolution() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of(
            newAccount("Conta com saldo inicial", "100.00", LocalDate.of(2026, 1, 2))
        ));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 3))).thenReturn(List.of());

        DashboardBalanceEvolution evolution = dashboardService.buildBalanceEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 3)
        );

        assertThat(evolution.openingBalance()).isEqualByComparingTo("0.00");
        assertThat(evolution.points()).hasSize(2);
        assertThat(evolution.points().get(0).date()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(evolution.points().get(0).periodAmount()).isEqualByComparingTo("0.00");
        assertThat(evolution.points().get(1).date()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(evolution.points().get(1).periodAmount()).isEqualByComparingTo("100.00");
        assertThat(evolution.points().get(1).accumulatedBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldBuildMonthlyEvolutionForRangeOfTwoMonthsOrMore() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of());
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
    void shouldIgnoreTransferEntriesInRevenueAndExpenseTotals() {
        Entry revenue = newEntry(LocalDate.of(2026, 1, 1), "15.00");
        Entry expense = newEntry(LocalDate.of(2026, 1, 1), "-4.00");
        Entry transferOut = newEntry(LocalDate.of(2026, 1, 1), "-10.00");
        transferOut.setEntryType(dev.ccosta.aisha.domain.entry.EntryType.TRANSFER);
        Entry transferIn = newEntry(LocalDate.of(2026, 1, 1), "10.00");
        transferIn.setEntryType(dev.ccosta.aisha.domain.entry.EntryType.TRANSFER);

        when(accountRepository.findAllOrdered()).thenReturn(List.of());
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            revenue,
            expense,
            transferOut,
            transferIn
        ));

        DashboardSummary summary = dashboardService.buildSummary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(summary.totalRevenues().currentValue()).isEqualByComparingTo("15.00");
        assertThat(summary.totalExpenses().currentValue()).isEqualByComparingTo("4.00");
    }

    @Test
    void shouldTrimTrailingMonthlyBucketsWithoutRecordsInBalanceEvolution() {
        when(accountRepository.findAllOrdered()).thenReturn(List.of());
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
    void shouldIgnoreUncategorizedEntriesInCategoryBreakdown() {
        Category rootHouse = newCategory(10L, "Casa", null);
        when(categoryRepository.findAllOrdered()).thenReturn(List.of(rootHouse));

        Entry uncategorized = new Entry();
        uncategorized.setAccount(newAccount());
        uncategorized.setSettlementDate(LocalDate.of(2026, 1, 9));
        uncategorized.setAmount(new BigDecimal("-999.00"));

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            uncategorized,
            newEntry(LocalDate.of(2026, 1, 10), "-12.00", rootHouse)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            null
        );

        assertThat(breakdown.items()).hasSize(1);
        assertThat(breakdown.items().getFirst().categoryName()).isEqualTo("Casa");
        assertThat(breakdown.items().getFirst().amount()).isEqualByComparingTo("12.00");
    }

    @Test
    void shouldUseNegativeCategoryBalanceForExpenseBreakdown() {
        Category rootSalary = newCategory(40L, "Salário", null);
        Category rootBills = newCategory(41L, "Contas", null);
        when(categoryRepository.findAllOrdered()).thenReturn(List.of(rootSalary, rootBills));

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "100.00", rootSalary),
            newEntry(LocalDate.of(2026, 1, 11), "-40.00", rootSalary),
            newEntry(LocalDate.of(2026, 1, 12), "-50.00", rootBills)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            null
        );

        assertThat(breakdown.items()).hasSize(1);
        assertThat(breakdown.items().getFirst().categoryName()).isEqualTo("Contas");
        assertThat(breakdown.items().getFirst().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldBuildRevenueCategoryBreakdownFromPositiveBalances() {
        Category rootSalary = newCategory(50L, "Salário", null);
        Category rootBills = newCategory(51L, "Contas", null);
        when(categoryRepository.findAllOrdered()).thenReturn(List.of(rootSalary, rootBills));

        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 1, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "200.00", rootSalary),
            newEntry(LocalDate.of(2026, 1, 11), "-20.00", rootSalary),
            newEntry(LocalDate.of(2026, 1, 12), "-50.00", rootBills)
        ));

        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildRevenueCategoryBreakdown(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            null
        );

        assertThat(breakdown.items()).hasSize(1);
        assertThat(breakdown.items().getFirst().categoryName()).isEqualTo("Salário");
        assertThat(breakdown.items().getFirst().amount()).isEqualByComparingTo("180.00");
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

    @Test
    void shouldBuildCategoryTotalsEvolutionForRootCategories() {
        Category rootFood = newCategory(30L, "Alimentação", null);
        Category rootHealth = newCategory(31L, "Saúde", null);
        Category subMarket = newCategory(32L, "Mercado", rootFood);
        Category subRestaurant = newCategory(33L, "Restaurante", rootFood);
        Category subPharmacy = newCategory(34L, "Farmácia", rootHealth);

        when(categoryRepository.findAllOrdered()).thenReturn(List.of(
            rootFood,
            rootHealth,
            subMarket,
            subRestaurant,
            subPharmacy
        ));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 3, 31))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 10), "-80.00", subMarket),
            newEntry(LocalDate.of(2026, 2, 12), "-20.00", subRestaurant),
            newEntry(LocalDate.of(2026, 3, 2), "-30.00", subPharmacy)
        ));

        DashboardCategoryTotalsEvolution evolution = dashboardService.buildCategoryTotalsEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            null
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.currentParentCategoryId()).isNull();
        assertThat(evolution.buckets()).containsExactly(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 3, 1)
        );
        assertThat(evolution.series()).hasSize(2);
        assertThat(evolution.series().get(0).categoryName()).isEqualTo("Alimentação");
        assertThat(evolution.series().get(0).values()).containsExactly(
            new BigDecimal("-80.00"),
            new BigDecimal("-20.00"),
            BigDecimal.ZERO
        );
        assertThat(evolution.series().get(0).hasChildren()).isTrue();
    }

    @Test
    void shouldDrillDownCategoryTotalsEvolutionAndTrimTrailingBuckets() {
        Category rootFood = newCategory(40L, "Alimentação", null);
        Category subMarket = newCategory(41L, "Mercado", rootFood);
        Category subRestaurant = newCategory(42L, "Restaurante", rootFood);

        when(categoryRepository.findAllOrdered()).thenReturn(List.of(rootFood, subMarket, subRestaurant));
        when(entryRepository.listAllBySettlementDateLessThanEqual(LocalDate.of(2026, 6, 30))).thenReturn(List.of(
            newEntry(LocalDate.of(2026, 1, 5), "-10.00", subMarket),
            newEntry(LocalDate.of(2026, 2, 7), "-15.00", subRestaurant)
        ));

        DashboardCategoryTotalsEvolution evolution = dashboardService.buildCategoryTotalsEvolution(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30),
            rootFood.getId()
        );

        assertThat(evolution.granularity()).isEqualTo(DashboardSeriesGranularity.MONTH);
        assertThat(evolution.currentParentCategoryId()).isEqualTo(rootFood.getId());
        assertThat(evolution.currentParentCategoryName()).isEqualTo("Alimentação");
        assertThat(evolution.drillUpParentCategoryId()).isNull();
        assertThat(evolution.buckets()).containsExactly(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 2, 1)
        );
        assertThat(evolution.series()).hasSize(2);
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

    private Account newAccount(String title, String initialBalance, LocalDate initialBalanceDate) {
        Account account = new Account();
        account.setTitle(title);
        account.setInitialBalance(new BigDecimal(initialBalance));
        account.setInitialBalanceDate(initialBalanceDate);
        return account;
    }

    private Category newCategory(Long id, String title, Category parent) {
        Category category = new Category();
        category.setTitle(title);
        category.setParent(parent);
        setId(category, id, Category.class);
        return category;
    }

    private Asset newAsset(Long id, Account account, String name, AssetType assetType, String currency) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(assetType);
        asset.setCurrency(currency);
        setId(asset, id, Asset.class);
        return asset;
    }

    private InvestmentOperation newOperation(
        Asset asset,
        InvestmentOperationType type,
        LocalDate tradeDate,
        String quantity,
        String netAmount
    ) {
        InvestmentOperation operation = new InvestmentOperation();
        operation.setAsset(asset);
        operation.setAccount(newAccount());
        operation.setOperationType(type);
        operation.setTradeDate(tradeDate);
        operation.setSettlementDate(tradeDate);
        operation.setCurrency("BRL");
        if (quantity != null) {
            operation.setQuantity(new BigDecimal(quantity));
        }
        if (netAmount != null) {
            operation.setNetAmount(new BigDecimal(netAmount));
        }
        return operation;
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
