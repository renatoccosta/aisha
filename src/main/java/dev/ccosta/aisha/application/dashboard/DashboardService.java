package dev.ccosta.aisha.application.dashboard;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.account.AccountRepository;
import dev.ccosta.aisha.domain.account.AccountType;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final EntryRepository entryRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final InvestmentOperationRepository investmentOperationRepository;

    public DashboardService(
        EntryRepository entryRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        AssetRepository assetRepository,
        InvestmentOperationRepository investmentOperationRepository
    ) {
        this.entryRepository = entryRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.investmentOperationRepository = investmentOperationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary buildSummary(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        LocalDate previousStartDate = resolvePreviousStart(startDate, endDate);
        LocalDate previousEndDate = startDate.minusDays(1);
        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        List<Account> accounts = accountRepository.findAllOrdered();
        DashboardInvestmentOverview investmentOverview = buildInvestmentOverview(startDate, endDate);

        BigDecimal currentBalance = sumInitialBalancesUntil(accounts, endDate);
        BigDecimal previousBalance = sumInitialBalancesUntil(accounts, previousEndDate);
        BigDecimal currentExpenses = BigDecimal.ZERO;
        BigDecimal previousExpenses = BigDecimal.ZERO;
        BigDecimal currentRevenues = BigDecimal.ZERO;
        BigDecimal previousRevenues = BigDecimal.ZERO;

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            BigDecimal amount = entry.getAmount();
            if (mustIgnoreEntryByAccountStartingPoint(entry)) {
                continue;
            }

            currentBalance = currentBalance.add(amount);
            if (settlementDate.isBefore(startDate)) {
                previousBalance = previousBalance.add(amount);
            }

            if (isInsideRange(settlementDate, startDate, endDate)) {
                if (entry.isTransfer()) {
                    continue;
                }
                if (amount.signum() < 0) {
                    currentExpenses = currentExpenses.add(amount.abs());
                } else if (amount.signum() > 0) {
                    currentRevenues = currentRevenues.add(amount);
                }
                continue;
            }

            if (isInsideRange(settlementDate, previousStartDate, previousEndDate)) {
                if (entry.isTransfer()) {
                    continue;
                }
                if (amount.signum() < 0) {
                    previousExpenses = previousExpenses.add(amount.abs());
                } else if (amount.signum() > 0) {
                    previousRevenues = previousRevenues.add(amount);
                }
            }
        }

        return new DashboardSummary(
            metric(currentBalance, previousBalance),
            metric(currentExpenses, previousExpenses),
            metric(currentRevenues, previousRevenues),
            investmentOverview,
            buildAccountTypeBalances(accounts, entries, endDate),
            buildAccountBalances(accounts, entries, endDate)
        );
    }

    /**
     * Builds the investment overview section using historical cost and BRL-only safe aggregation.
     *
     * @param startDate filter start date
     * @param endDate filter end date
     * @return investment overview data
     */
    @Transactional(readOnly = true)
    public DashboardInvestmentOverview buildInvestmentOverview(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        LocalDate previousStartDate = resolvePreviousStart(startDate, endDate);
        LocalDate previousEndDate = startDate.minusDays(1);
        List<Asset> assets = assetRepository.findAllOrdered();
        List<InvestmentOperation> operations = investmentOperationRepository.findAllOrdered();

        InvestmentOverviewSnapshot currentSnapshot = buildInvestmentOverviewSnapshot(
            assets,
            operations,
            startDate,
            endDate,
            endDate
        );
        InvestmentOverviewSnapshot previousSnapshot = buildInvestmentOverviewSnapshot(
            assets,
            operations,
            previousStartDate,
            previousEndDate,
            previousEndDate
        );

        return new DashboardInvestmentOverview(
            metric(currentSnapshot.positionCost(), previousSnapshot.positionCost()),
            metric(currentSnapshot.periodNetFlow(), previousSnapshot.periodNetFlow()),
            metric(currentSnapshot.periodIncome(), previousSnapshot.periodIncome()),
            currentSnapshot.openAssetCount(),
            currentSnapshot.excludedAssetCount(),
            currentSnapshot.excludedOperationCount(),
            currentSnapshot.allocationsByAssetType()
        );
    }

    /**
     * Builds the evolution series for the investment period flow chart.
     *
     * @param startDate filter start date
     * @param endDate filter end date
     * @return investment cash flow evolution
     */
    @Transactional(readOnly = true)
    public DashboardInvestmentFlowEvolution buildInvestmentFlowEvolution(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        DashboardSeriesGranularity granularity = resolveGranularity(startDate, endDate);
        List<Asset> assets = assetRepository.findAllOrdered();
        List<InvestmentOperation> operations = investmentOperationRepository.findAllOrdered();
        Map<Long, List<InvestmentOperation>> operationsByAssetId = groupOperationsByAssetId(operations);
        Map<LocalDate, BigDecimal> inflowsByBucket = new HashMap<>();
        Map<LocalDate, BigDecimal> outflowsByBucket = new HashMap<>();
        LocalDate lastBucketWithRecords = null;

        for (Asset asset : assets) {
            List<InvestmentOperation> assetOperations = operationsByAssetId.getOrDefault(asset.getId(), List.of());
            if (!isBrlEligibleAsset(asset, assetOperations)) {
                continue;
            }

            for (InvestmentOperation operation : assetOperations) {
                LocalDate flowDate = resolveInvestmentFlowDate(operation);
                if (flowDate == null || !isInsideRange(flowDate, startDate, endDate)) {
                    continue;
                }

                BigDecimal signedAmount = signedCashAmount(operation);
                LocalDate bucketDate = normalizeBucketStart(flowDate, granularity);
                if (signedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    inflowsByBucket.merge(bucketDate, signedAmount, BigDecimal::add);
                    lastBucketWithRecords = maxDate(lastBucketWithRecords, bucketDate);
                } else if (signedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    outflowsByBucket.merge(bucketDate, signedAmount.abs(), BigDecimal::add);
                    lastBucketWithRecords = maxDate(lastBucketWithRecords, bucketDate);
                }
            }
        }

        LocalDate effectiveEndDate = resolveEffectiveEndDate(endDate, lastBucketWithRecords, granularity);
        List<DashboardInvestmentFlowPoint> points = new ArrayList<>();
        for (LocalDate bucketStart : buildBucketStarts(startDate, effectiveEndDate, granularity)) {
            BigDecimal inflows = inflowsByBucket.getOrDefault(bucketStart, BigDecimal.ZERO);
            BigDecimal outflows = outflowsByBucket.getOrDefault(bucketStart, BigDecimal.ZERO);
            points.add(new DashboardInvestmentFlowPoint(
                bucketStart,
                inflows,
                outflows,
                inflows.subtract(outflows)
            ));
        }

        return new DashboardInvestmentFlowEvolution(startDate, endDate, granularity, points);
    }

    @Transactional(readOnly = true)
    public DashboardBalanceEvolution buildBalanceEvolution(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        DashboardSeriesGranularity granularity = resolveGranularity(startDate, endDate);
        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        List<Account> accounts = accountRepository.findAllOrdered();
        Map<LocalDate, BigDecimal> periodAmountsByBucket = new HashMap<>();
        BigDecimal openingBalance = sumInitialBalancesUntil(accounts, startDate.minusDays(1));
        LocalDate lastBucketWithRecords = null;

        for (Account account : accounts) {
            if (account.getInitialBalance() == null || account.getInitialBalanceDate() == null) {
                continue;
            }
            if (account.getInitialBalanceDate().isBefore(startDate) || account.getInitialBalanceDate().isAfter(endDate)) {
                continue;
            }

            LocalDate bucketDate = normalizeBucketStart(account.getInitialBalanceDate(), granularity);
            periodAmountsByBucket.merge(bucketDate, account.getInitialBalance(), BigDecimal::add);
            lastBucketWithRecords = maxDate(lastBucketWithRecords, bucketDate);
        }

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            BigDecimal amount = entry.getAmount();
            if (mustIgnoreEntryByAccountStartingPoint(entry)) {
                continue;
            }

            if (settlementDate.isBefore(startDate)) {
                openingBalance = openingBalance.add(amount);
                continue;
            }
            if (settlementDate.isAfter(endDate)) {
                continue;
            }

            LocalDate bucketDate = normalizeBucketStart(settlementDate, granularity);
            periodAmountsByBucket.merge(bucketDate, amount, BigDecimal::add);
            lastBucketWithRecords = maxDate(lastBucketWithRecords, bucketDate);
        }

        List<DashboardBalancePoint> points = new ArrayList<>();
        BigDecimal accumulatedBalance = openingBalance;
        LocalDate effectiveEndDate = resolveEffectiveEndDate(endDate, lastBucketWithRecords, granularity);
        for (LocalDate bucketStart : buildBucketStarts(startDate, effectiveEndDate, granularity)) {
            BigDecimal periodAmount = periodAmountsByBucket.getOrDefault(bucketStart, BigDecimal.ZERO);
            accumulatedBalance = accumulatedBalance.add(periodAmount);
            points.add(new DashboardBalancePoint(bucketStart, periodAmount, accumulatedBalance));
        }

        return new DashboardBalanceEvolution(startDate, endDate, granularity, openingBalance, points);
    }

    @Transactional(readOnly = true)
    public DashboardRevenueExpenseEvolution buildRevenueExpenseEvolution(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        DashboardSeriesGranularity granularity = resolveGranularity(startDate, endDate);
        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        Map<LocalDate, BigDecimal> revenuesByBucket = new HashMap<>();
        Map<LocalDate, BigDecimal> expensesByBucket = new HashMap<>();
        LocalDate lastBucketWithRecords = null;

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            if (!isInsideRange(settlementDate, startDate, endDate)) {
                continue;
            }
            if (entry.isTransfer()) {
                continue;
            }

            BigDecimal amount = entry.getAmount();
            LocalDate bucketDate = normalizeBucketStart(settlementDate, granularity);
            if (amount.signum() < 0) {
                expensesByBucket.merge(bucketDate, amount.abs(), BigDecimal::add);
            } else if (amount.signum() > 0) {
                revenuesByBucket.merge(bucketDate, amount, BigDecimal::add);
            }
            lastBucketWithRecords = maxDate(lastBucketWithRecords, bucketDate);
        }

        List<DashboardRevenueExpensePoint> points = new ArrayList<>();
        LocalDate effectiveEndDate = resolveEffectiveEndDate(endDate, lastBucketWithRecords, granularity);
        for (LocalDate bucketStart : buildBucketStarts(startDate, effectiveEndDate, granularity)) {
            points.add(new DashboardRevenueExpensePoint(
                bucketStart,
                revenuesByBucket.getOrDefault(bucketStart, BigDecimal.ZERO),
                expensesByBucket.getOrDefault(bucketStart, BigDecimal.ZERO)
            ));
        }

        return new DashboardRevenueExpenseEvolution(startDate, endDate, granularity, points);
    }

    @Transactional(readOnly = true)
    public DashboardExpenseCategoryBreakdown buildExpenseCategoryBreakdown(
        LocalDate startDate,
        LocalDate endDate,
        Long parentCategoryId
    ) {
        return buildCategoryBreakdown(startDate, endDate, parentCategoryId, -1);
    }

    @Transactional(readOnly = true)
    public DashboardExpenseCategoryBreakdown buildRevenueCategoryBreakdown(
        LocalDate startDate,
        LocalDate endDate,
        Long parentCategoryId
    ) {
        return buildCategoryBreakdown(startDate, endDate, parentCategoryId, 1);
    }

    private DashboardExpenseCategoryBreakdown buildCategoryBreakdown(
        LocalDate startDate,
        LocalDate endDate,
        Long parentCategoryId,
        int expectedBalanceSign
    ) {
        validateRange(startDate, endDate);

        List<Category> categories = categoryRepository.findAllOrdered();
        Map<Long, Category> categoryById = new HashMap<>();
        Map<Long, List<Long>> childrenByParentId = new HashMap<>();
        for (Category category : categories) {
            categoryById.put(category.getId(), category);
            Long key = parentIdOf(category);
            childrenByParentId.computeIfAbsent(key, ignored -> new ArrayList<>()).add(category.getId());
        }

        if (parentCategoryId != null && !categoryById.containsKey(parentCategoryId)) {
            throw new IllegalArgumentException("Parent category was not found");
        }

        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        Map<Long, BigDecimal> balanceByCategoryId = new HashMap<>();

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            if (!isInsideRange(settlementDate, startDate, endDate)) {
                continue;
            }
            if (entry.isTransfer()) {
                continue;
            }
            if (entry.getCategory() == null) {
                continue;
            }

            Long categoryId = entry.getCategory().getId();
            balanceByCategoryId.merge(categoryId, entry.getAmount(), BigDecimal::add);
        }

        Map<Long, BigDecimal> subtreeExpenseByCategory = new HashMap<>();
        for (Long categoryId : categoryById.keySet()) {
            subtreeExpense(categoryId, childrenByParentId, balanceByCategoryId, subtreeExpenseByCategory);
        }

        List<Long> visibleCategoryIds = childrenByParentId.getOrDefault(parentCategoryId, List.of());
        List<DashboardExpenseCategoryItem> items = visibleCategoryIds
            .stream()
            .map(categoryId -> toItem(
                categoryId,
                categoryById,
                childrenByParentId,
                subtreeExpenseByCategory,
                expectedBalanceSign
            ))
            .filter(java.util.Objects::nonNull)
            .sorted(Comparator.comparing(DashboardExpenseCategoryItem::amount).reversed())
            .toList();

        return new DashboardExpenseCategoryBreakdown(
            startDate,
            endDate,
            parentCategoryId,
            currentParentName(parentCategoryId, categoryById),
            parentOfCurrent(parentCategoryId, categoryById),
            items
        );
    }

    @Transactional(readOnly = true)
    public DashboardCategoryTotalsEvolution buildCategoryTotalsEvolution(
        LocalDate startDate,
        LocalDate endDate,
        Long parentCategoryId
    ) {
        validateRange(startDate, endDate);

        DashboardSeriesGranularity granularity = resolveGranularity(startDate, endDate);
        List<Category> categories = categoryRepository.findAllOrdered();
        Map<Long, Category> categoryById = new HashMap<>();
        Map<Long, List<Long>> childrenByParentId = new HashMap<>();
        for (Category category : categories) {
            categoryById.put(category.getId(), category);
            Long key = parentIdOf(category);
            childrenByParentId.computeIfAbsent(key, ignored -> new ArrayList<>()).add(category.getId());
        }

        if (parentCategoryId != null && !categoryById.containsKey(parentCategoryId)) {
            throw new IllegalArgumentException("Parent category was not found");
        }

        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        Map<Long, Map<LocalDate, BigDecimal>> directAmountsByCategory = new HashMap<>();

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            if (!isInsideRange(settlementDate, startDate, endDate)) {
                continue;
            }
            if (entry.isTransfer()) {
                continue;
            }
            if (entry.getCategory() == null) {
                continue;
            }

            Long categoryId = entry.getCategory().getId();
            LocalDate bucketDate = normalizeBucketStart(settlementDate, granularity);
            directAmountsByCategory
                .computeIfAbsent(categoryId, ignored -> new HashMap<>())
                .merge(bucketDate, entry.getAmount(), BigDecimal::add);
        }

        Map<Long, Map<LocalDate, BigDecimal>> subtreeAmountsByCategory = new HashMap<>();
        for (Long categoryId : categoryById.keySet()) {
            subtreeAmounts(categoryId, childrenByParentId, directAmountsByCategory, subtreeAmountsByCategory);
        }

        List<Long> visibleCategoryIds = childrenByParentId.getOrDefault(parentCategoryId, List.of());
        LocalDate lastBucketWithRecords = null;
        List<CategorySeriesData> categorySeries = new ArrayList<>();

        for (Long categoryId : visibleCategoryIds) {
            Map<LocalDate, BigDecimal> valuesByBucket = subtreeAmountsByCategory.getOrDefault(categoryId, Map.of());
            if (valuesByBucket.isEmpty()) {
                continue;
            }

            Category category = categoryById.get(categoryId);
            categorySeries.add(new CategorySeriesData(
                categoryId,
                category == null ? "" : category.getTitle(),
                !childrenByParentId.getOrDefault(categoryId, List.of()).isEmpty(),
                valuesByBucket,
                sumAmounts(valuesByBucket)
            ));
            lastBucketWithRecords = maxDate(lastBucketWithRecords, latestBucket(valuesByBucket));
        }

        categorySeries.sort(Comparator.comparing(CategorySeriesData::total).reversed());

        LocalDate effectiveEndDate = resolveEffectiveEndDate(endDate, lastBucketWithRecords, granularity);
        List<LocalDate> buckets = buildBucketStarts(startDate, effectiveEndDate, granularity);
        List<DashboardCategoryTotalsSeries> series = categorySeries
            .stream()
            .map(data -> toCategoryTotalsSeries(data, buckets))
            .toList();

        return new DashboardCategoryTotalsEvolution(
            startDate,
            endDate,
            granularity,
            parentCategoryId,
            currentParentName(parentCategoryId, categoryById),
            parentOfCurrent(parentCategoryId, categoryById),
            buckets,
            series
        );
    }


    private List<DashboardAccountTypeBalance> buildAccountTypeBalances(List<Account> accounts, List<Entry> entries, LocalDate endDate) {
        Map<Long, BigDecimal> balanceByAccountId = new HashMap<>();
        Map<Long, AccountType> typeByAccountId = new HashMap<>();

        for (Account account : accounts) {
            if (account.getId() == null) {
                continue;
            }
            if (account.getInitialBalance() == null || account.getInitialBalanceDate() == null || account.getInitialBalanceDate().isAfter(endDate)) {
                balanceByAccountId.put(account.getId(), BigDecimal.ZERO);
            } else {
                balanceByAccountId.put(account.getId(), account.getInitialBalance());
            }
            typeByAccountId.put(account.getId(), account.getAccountType() == null ? AccountType.OTHER : account.getAccountType());
        }

        for (Entry entry : entries) {
            if (entry.getAccount() == null || entry.getAccount().getId() == null) {
                continue;
            }
            Long accountId = entry.getAccount().getId();
            if (!balanceByAccountId.containsKey(accountId)) {
                continue;
            }
            if (entry.getSettlementDate().isAfter(endDate) || mustIgnoreEntryByAccountStartingPoint(entry)) {
                continue;
            }
            balanceByAccountId.merge(accountId, entry.getAmount(), BigDecimal::add);
        }

        EnumMap<AccountType, BigDecimal> balanceByType = new EnumMap<>(AccountType.class);
        for (AccountType accountType : AccountType.values()) {
            balanceByType.put(accountType, BigDecimal.ZERO);
        }

        for (Map.Entry<Long, BigDecimal> entry : balanceByAccountId.entrySet()) {
            AccountType accountType = typeByAccountId.getOrDefault(entry.getKey(), AccountType.OTHER);
            balanceByType.merge(accountType, entry.getValue(), BigDecimal::add);
        }

        return java.util.Arrays.stream(AccountType.values())
            .map(accountType -> new DashboardAccountTypeBalance(accountType, balanceByType.getOrDefault(accountType, BigDecimal.ZERO)))
            .toList();
    }

    private List<DashboardAccountBalance> buildAccountBalances(List<Account> accounts, List<Entry> entries, LocalDate endDate) {
        Map<Long, BigDecimal> balanceByAccountId = new HashMap<>();

        for (Account account : accounts) {
            if (account.getId() == null) {
                continue;
            }

            if (account.getInitialBalance() == null || account.getInitialBalanceDate() == null || account.getInitialBalanceDate().isAfter(endDate)) {
                balanceByAccountId.put(account.getId(), BigDecimal.ZERO);
            } else {
                balanceByAccountId.put(account.getId(), account.getInitialBalance());
            }
        }

        for (Entry entry : entries) {
            if (entry.getAccount() == null || entry.getAccount().getId() == null) {
                continue;
            }
            Long accountId = entry.getAccount().getId();
            if (!balanceByAccountId.containsKey(accountId)) {
                continue;
            }
            if (entry.getSettlementDate().isAfter(endDate) || mustIgnoreEntryByAccountStartingPoint(entry)) {
                continue;
            }
            balanceByAccountId.merge(accountId, entry.getAmount(), BigDecimal::add);
        }

        return accounts.stream()
            .filter(account -> account.getId() != null)
            .map(account -> new DashboardAccountBalance(
                account.getId(),
                account.getTitle(),
                account.getAccountType() == null ? AccountType.OTHER : account.getAccountType(),
                balanceByAccountId.getOrDefault(account.getId(), BigDecimal.ZERO)
            ))
            .toList();
    }

    private InvestmentOverviewSnapshot buildInvestmentOverviewSnapshot(
        List<Asset> assets,
        List<InvestmentOperation> operations,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        LocalDate positionDate
    ) {
        Map<Long, List<InvestmentOperation>> operationsByAssetId = groupOperationsByAssetId(operations);
        EnumMap<AssetType, BigDecimal> positionCostByAssetType = new EnumMap<>(AssetType.class);
        BigDecimal positionCost = BigDecimal.ZERO;
        BigDecimal periodNetFlow = BigDecimal.ZERO;
        BigDecimal periodIncome = BigDecimal.ZERO;
        int openAssetCount = 0;
        int excludedAssetCount = 0;
        int excludedOperationCount = 0;

        for (AssetType type : AssetType.values()) {
            positionCostByAssetType.put(type, BigDecimal.ZERO);
        }

        for (Asset asset : assets) {
            if (asset.getId() == null) {
                continue;
            }

            List<InvestmentOperation> assetOperations = operationsByAssetId.getOrDefault(asset.getId(), List.of());
            if (!isBrlEligibleAsset(asset, assetOperations)) {
                excludedAssetCount++;
                excludedOperationCount += countOperationsInsideRange(assetOperations, periodStartDate, periodEndDate);
                continue;
            }

            InvestmentPositionSnapshot position = calculatePositionAt(asset, assetOperations, positionDate);
            if (position.quantity().compareTo(BigDecimal.ZERO) > 0 && position.totalCost().compareTo(BigDecimal.ZERO) > 0) {
                openAssetCount++;
                positionCost = positionCost.add(position.totalCost());
                positionCostByAssetType.merge(asset.getType(), position.totalCost(), BigDecimal::add);
            }

            for (InvestmentOperation operation : assetOperations) {
                LocalDate flowDate = resolveInvestmentFlowDate(operation);
                if (flowDate == null || !isInsideRange(flowDate, periodStartDate, periodEndDate)) {
                    continue;
                }
                BigDecimal signedAmount = signedCashAmount(operation);
                periodNetFlow = periodNetFlow.add(signedAmount);
                if (isIncomeOperation(operation.getOperationType()) && signedAmount.compareTo(BigDecimal.ZERO) > 0) {
                    periodIncome = periodIncome.add(signedAmount);
                }
            }
        }

        List<DashboardInvestmentAllocation> allocationsByAssetType = java.util.Arrays.stream(AssetType.values())
            .map(type -> new DashboardInvestmentAllocation(type.name(), type.name(), positionCostByAssetType.getOrDefault(type, BigDecimal.ZERO)))
            .filter(item -> item.amount().compareTo(BigDecimal.ZERO) > 0)
            .sorted(Comparator.comparing(DashboardInvestmentAllocation::amount).reversed())
            .toList();

        return new InvestmentOverviewSnapshot(
            positionCost,
            periodNetFlow,
            periodIncome,
            openAssetCount,
            excludedAssetCount,
            excludedOperationCount,
            allocationsByAssetType
        );
    }

    private DashboardMetric metric(BigDecimal currentValue, BigDecimal previousValue) {
        return new DashboardMetric(currentValue, previousValue, resolveVariationPercent(currentValue, previousValue));
    }

    private BigDecimal resolveVariationPercent(BigDecimal currentValue, BigDecimal previousValue) {
        if (previousValue.signum() == 0) {
            if (currentValue.signum() == 0) {
                return BigDecimal.ZERO;
            }
            return null;
        }

        return currentValue
            .subtract(previousValue)
            .multiply(BigDecimal.valueOf(100))
            .divide(previousValue.abs(), 2, RoundingMode.HALF_UP);
    }

    private DashboardSeriesGranularity resolveGranularity(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isBefore(startDate.plusMonths(2))) {
            return DashboardSeriesGranularity.MONTH;
        }
        return DashboardSeriesGranularity.DAY;
    }

    private List<LocalDate> buildBucketStarts(LocalDate startDate, LocalDate endDate, DashboardSeriesGranularity granularity) {
        if (endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }

        List<LocalDate> bucketStarts = new ArrayList<>();
        LocalDate cursor = normalizeBucketStart(startDate, granularity);

        while (!cursor.isAfter(endDate)) {
            bucketStarts.add(cursor);
            cursor = nextBucket(cursor, granularity);
        }

        return bucketStarts;
    }

    private LocalDate normalizeBucketStart(LocalDate date, DashboardSeriesGranularity granularity) {
        if (granularity == DashboardSeriesGranularity.MONTH) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    private LocalDate nextBucket(LocalDate date, DashboardSeriesGranularity granularity) {
        if (granularity == DashboardSeriesGranularity.MONTH) {
            return date.plusMonths(1);
        }
        return date.plusDays(1);
    }

    private LocalDate resolveEffectiveEndDate(LocalDate fallbackEndDate, LocalDate lastBucketWithRecords, DashboardSeriesGranularity granularity) {
        if (lastBucketWithRecords == null) {
            return startOfPreviousBucket(fallbackEndDate, granularity);
        }
        return lastBucketWithRecords;
    }

    private LocalDate startOfPreviousBucket(LocalDate date, DashboardSeriesGranularity granularity) {
        LocalDate normalized = normalizeBucketStart(date, granularity);
        if (granularity == DashboardSeriesGranularity.MONTH) {
            return normalized.minusMonths(1);
        }
        return normalized.minusDays(1);
    }

    private LocalDate maxDate(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private BigDecimal sumInitialBalancesUntil(List<Account> accounts, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            if (account.getInitialBalance() == null || account.getInitialBalanceDate() == null) {
                continue;
            }
            if (account.getInitialBalanceDate().isAfter(date)) {
                continue;
            }
            total = total.add(account.getInitialBalance());
        }
        return total;
    }

    private boolean mustIgnoreEntryByAccountStartingPoint(Entry entry) {
        if (entry.getAccount() == null || entry.getAccount().getInitialBalanceDate() == null) {
            return false;
        }
        return entry.getSettlementDate().isBefore(entry.getAccount().getInitialBalanceDate());
    }

    private Map<Long, List<InvestmentOperation>> groupOperationsByAssetId(List<InvestmentOperation> operations) {
        Map<Long, List<InvestmentOperation>> operationsByAssetId = new HashMap<>();
        for (InvestmentOperation operation : operations) {
            if (operation.getAsset() == null || operation.getAsset().getId() == null) {
                continue;
            }
            operationsByAssetId.computeIfAbsent(operation.getAsset().getId(), ignored -> new ArrayList<>()).add(operation);
        }
        return operationsByAssetId;
    }

    private InvestmentPositionSnapshot calculatePositionAt(Asset asset, List<InvestmentOperation> operations, LocalDate positionDate) {
        BigDecimal runningQuantity = defaultQuantity(asset.getOpeningPositionQuantity());
        BigDecimal runningTotalCost = defaultMoney(asset.getOpeningPositionTotalCost());

        if (asset.getOpeningPositionDate() != null && positionDate != null && asset.getOpeningPositionDate().isAfter(positionDate)) {
            runningQuantity = BigDecimal.ZERO;
            runningTotalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        for (InvestmentOperation operation : operations) {
            if (positionDate != null && operation.getTradeDate() != null && operation.getTradeDate().isAfter(positionDate)) {
                break;
            }
            PositionImpact impact = calculatePositionImpact(operation, runningQuantity, runningTotalCost);
            runningQuantity = impact.resultingQuantity();
            runningTotalCost = impact.resultingTotalCost();
        }

        return new InvestmentPositionSnapshot(runningQuantity, runningTotalCost);
    }

    private PositionImpact calculatePositionImpact(
        InvestmentOperation operation,
        BigDecimal runningQuantity,
        BigDecimal runningTotalCost
    ) {
        InvestmentOperationType type = operation.getOperationType();
        BigDecimal quantity = defaultQuantity(operation.getQuantity());
        BigDecimal resultingQuantity = runningQuantity;
        BigDecimal resultingTotalCost = runningTotalCost;

        if (isQuantityIncrease(type)) {
            BigDecimal costDelta = switch (type) {
                case BUY, SUBSCRIPTION, TRANSFER_IN -> acquisitionCost(operation);
                case BONUS, SPLIT -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            };
            resultingQuantity = runningQuantity.add(quantity);
            resultingTotalCost = sanitizeMoney(runningTotalCost.add(costDelta));
        } else if (isQuantityDecrease(type)) {
            BigDecimal quantityToRemove = quantity.max(BigDecimal.ZERO);
            BigDecimal removableQuantity = quantityToRemove.min(runningQuantity.max(BigDecimal.ZERO));
            BigDecimal allocatedCost = allocatedCost(removableQuantity, runningQuantity, runningTotalCost);
            resultingQuantity = sanitizeQuantity(runningQuantity.subtract(removableQuantity));
            resultingTotalCost = sanitizeMoney(runningTotalCost.subtract(allocatedCost));
            if (resultingQuantity.compareTo(BigDecimal.ZERO) == 0) {
                resultingTotalCost = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        } else if (type == InvestmentOperationType.AMORTIZATION) {
            resultingTotalCost = sanitizeMoney(runningTotalCost.subtract(unsignedMoneyAmount(operation)));
        }

        return new PositionImpact(resultingQuantity, resultingTotalCost);
    }

    private boolean isBrlEligibleAsset(Asset asset, List<InvestmentOperation> operations) {
        if (!isBrlCurrency(asset.getCurrency())) {
            return false;
        }
        if (asset.getOpeningPositionCurrency() != null && !asset.getOpeningPositionCurrency().isBlank()
            && !isBrlCurrency(asset.getOpeningPositionCurrency())) {
            return false;
        }
        for (InvestmentOperation operation : operations) {
            if (!isBrlCurrency(operation.getCurrency())) {
                return false;
            }
        }
        return true;
    }

    private boolean isBrlCurrency(String currency) {
        return currency != null && "BRL".equalsIgnoreCase(currency.trim());
    }

    private int countOperationsInsideRange(List<InvestmentOperation> operations, LocalDate startDate, LocalDate endDate) {
        int count = 0;
        for (InvestmentOperation operation : operations) {
            LocalDate flowDate = resolveInvestmentFlowDate(operation);
            if (flowDate != null && isInsideRange(flowDate, startDate, endDate)) {
                count++;
            }
        }
        return count;
    }

    private LocalDate resolveInvestmentFlowDate(InvestmentOperation operation) {
        if (operation.getSettlementDate() != null) {
            return operation.getSettlementDate();
        }
        return operation.getTradeDate();
    }

    private boolean isIncomeOperation(InvestmentOperationType operationType) {
        return switch (operationType) {
            case DIVIDEND, INTEREST, COUPON, AMORTIZATION, REDEMPTION -> true;
            default -> false;
        };
    }

    private boolean isQuantityIncrease(InvestmentOperationType type) {
        return switch (type) {
            case BUY, BONUS, SPLIT, SUBSCRIPTION, TRANSFER_IN -> true;
            default -> false;
        };
    }

    private boolean isQuantityDecrease(InvestmentOperationType type) {
        return switch (type) {
            case SELL, REDEMPTION, REVERSE_SPLIT, TRANSFER_OUT -> true;
            default -> false;
        };
    }

    private BigDecimal acquisitionCost(InvestmentOperation operation) {
        if (operation.getNetAmount() != null) {
            return operation.getNetAmount().setScale(2, RoundingMode.HALF_UP);
        }
        if (operation.getGrossAmount() != null) {
            return operation.getGrossAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .add(defaultMoney(operation.getFees()))
                .add(defaultMoney(operation.getTaxes()));
        }
        if (operation.getQuantity() != null && operation.getUnitPrice() != null) {
            return operation.getQuantity()
                .multiply(operation.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP)
                .add(defaultMoney(operation.getFees()))
                .add(defaultMoney(operation.getTaxes()));
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal allocatedCost(BigDecimal quantity, BigDecimal runningQuantity, BigDecimal runningTotalCost) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0
            || runningQuantity.compareTo(BigDecimal.ZERO) <= 0
            || runningTotalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (quantity.compareTo(runningQuantity) >= 0) {
            return sanitizeMoney(runningTotalCost);
        }
        BigDecimal averageCost = runningTotalCost.divide(runningQuantity, 8, RoundingMode.HALF_UP);
        return sanitizeMoney(averageCost.multiply(quantity));
    }

    private BigDecimal signedCashAmount(InvestmentOperation operation) {
        BigDecimal rawAmount = unsignedMoneyAmount(operation);
        if (rawAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return switch (operation.getOperationType()) {
            case BUY, SUBSCRIPTION, TAX, FEE -> rawAmount.negate();
            case SELL, DIVIDEND, INTEREST, AMORTIZATION, COUPON, REDEMPTION -> rawAmount;
            default -> operation.getNetAmount() != null
                ? operation.getNetAmount().setScale(2, RoundingMode.HALF_UP)
                : rawAmount;
        };
    }

    private BigDecimal unsignedMoneyAmount(InvestmentOperation operation) {
        if (operation.getNetAmount() != null) {
            return operation.getNetAmount().abs().setScale(2, RoundingMode.HALF_UP);
        }
        if (operation.getGrossAmount() != null) {
            return operation.getGrossAmount().abs().setScale(2, RoundingMode.HALF_UP);
        }
        if (operation.getQuantity() != null && operation.getUnitPrice() != null) {
            return operation.getQuantity()
                .multiply(operation.getUnitPrice())
                .abs()
                .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizeMoney(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sanitizeQuantity(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private DashboardCategoryTotalsSeries toCategoryTotalsSeries(CategorySeriesData data, List<LocalDate> buckets) {
        List<BigDecimal> values = new ArrayList<>(buckets.size());
        for (LocalDate bucket : buckets) {
            values.add(data.valuesByBucket().getOrDefault(bucket, BigDecimal.ZERO));
        }
        return new DashboardCategoryTotalsSeries(data.categoryId(), data.categoryName(), data.hasChildren(), values);
    }

    private Map<LocalDate, BigDecimal> subtreeAmounts(
        Long categoryId,
        Map<Long, List<Long>> childrenByParentId,
        Map<Long, Map<LocalDate, BigDecimal>> directAmountsByCategory,
        Map<Long, Map<LocalDate, BigDecimal>> memo
    ) {
        Map<LocalDate, BigDecimal> cached = memo.get(categoryId);
        if (cached != null) {
            return cached;
        }

        Map<LocalDate, BigDecimal> totalByBucket = new HashMap<>(directAmountsByCategory.getOrDefault(categoryId, Map.of()));
        for (Long childId : childrenByParentId.getOrDefault(categoryId, List.of())) {
            Map<LocalDate, BigDecimal> childValues = subtreeAmounts(childId, childrenByParentId, directAmountsByCategory, memo);
            for (Map.Entry<LocalDate, BigDecimal> childEntry : childValues.entrySet()) {
                totalByBucket.merge(childEntry.getKey(), childEntry.getValue(), BigDecimal::add);
            }
        }

        memo.put(categoryId, totalByBucket);
        return totalByBucket;
    }

    private BigDecimal sumAmounts(Map<LocalDate, BigDecimal> valuesByBucket) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : valuesByBucket.values()) {
            total = total.add(value.abs());
        }
        return total;
    }

    private LocalDate latestBucket(Map<LocalDate, BigDecimal> valuesByBucket) {
        LocalDate latest = null;
        for (LocalDate bucket : valuesByBucket.keySet()) {
            latest = maxDate(latest, bucket);
        }
        return latest;
    }

    private DashboardExpenseCategoryItem toItem(
        Long categoryId,
        Map<Long, Category> categoryById,
        Map<Long, List<Long>> childrenByParentId,
        Map<Long, BigDecimal> subtreeExpenseByCategory,
        int expectedBalanceSign
    ) {
        Category category = categoryById.get(categoryId);
        BigDecimal totalBalance = subtreeExpenseByCategory.getOrDefault(categoryId, BigDecimal.ZERO);
        if (totalBalance.signum() != expectedBalanceSign) {
            return null;
        }
        return new DashboardExpenseCategoryItem(
            categoryId,
            category == null ? "" : category.getTitle(),
            totalBalance.abs(),
            !childrenByParentId.getOrDefault(categoryId, List.of()).isEmpty()
        );
    }

    private BigDecimal subtreeExpense(
        Long categoryId,
        Map<Long, List<Long>> childrenByParentId,
        Map<Long, BigDecimal> expenseByCategoryId,
        Map<Long, BigDecimal> memo
    ) {
        BigDecimal cached = memo.get(categoryId);
        if (cached != null) {
            return cached;
        }

        BigDecimal total = expenseByCategoryId.getOrDefault(categoryId, BigDecimal.ZERO);
        for (Long childId : childrenByParentId.getOrDefault(categoryId, List.of())) {
            total = total.add(subtreeExpense(childId, childrenByParentId, expenseByCategoryId, memo));
        }

        memo.put(categoryId, total);
        return total;
    }

    private Long parentIdOf(Category category) {
        if (category.getParent() == null) {
            return null;
        }
        return category.getParent().getId();
    }

    private String currentParentName(Long parentCategoryId, Map<Long, Category> categoryById) {
        if (parentCategoryId == null) {
            return null;
        }
        Category category = categoryById.get(parentCategoryId);
        return category == null ? null : category.getTitle();
    }

    private Long parentOfCurrent(Long parentCategoryId, Map<Long, Category> categoryById) {
        if (parentCategoryId == null) {
            return null;
        }
        Category category = categoryById.get(parentCategoryId);
        if (category == null || category.getParent() == null) {
            return null;
        }
        return category.getParent().getId();
    }

    private record CategorySeriesData(
        Long categoryId,
        String categoryName,
        boolean hasChildren,
        Map<LocalDate, BigDecimal> valuesByBucket,
        BigDecimal total
    ) {
    }

    private record PositionImpact(BigDecimal resultingQuantity, BigDecimal resultingTotalCost) {
    }

    private record InvestmentPositionSnapshot(BigDecimal quantity, BigDecimal totalCost) {
    }

    private record InvestmentOverviewSnapshot(
        BigDecimal positionCost,
        BigDecimal periodNetFlow,
        BigDecimal periodIncome,
        int openAssetCount,
        int excludedAssetCount,
        int excludedOperationCount,
        List<DashboardInvestmentAllocation> allocationsByAssetType
    ) {
    }

    private LocalDate resolvePreviousStart(LocalDate startDate, LocalDate endDate) {
        long rangeLength = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return startDate.minusDays(rangeLength);
    }

    private boolean isInsideRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }
    }
}
