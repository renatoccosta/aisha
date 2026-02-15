package dev.ccosta.aisha.application.dashboard;

import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final EntryRepository entryRepository;
    private final CategoryRepository categoryRepository;

    public DashboardService(EntryRepository entryRepository, CategoryRepository categoryRepository) {
        this.entryRepository = entryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary buildSummary(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        LocalDate previousStartDate = resolvePreviousStart(startDate, endDate);
        LocalDate previousEndDate = startDate.minusDays(1);
        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);

        BigDecimal currentBalance = BigDecimal.ZERO;
        BigDecimal previousBalance = BigDecimal.ZERO;
        BigDecimal currentExpenses = BigDecimal.ZERO;
        BigDecimal previousExpenses = BigDecimal.ZERO;
        BigDecimal currentRevenues = BigDecimal.ZERO;
        BigDecimal previousRevenues = BigDecimal.ZERO;

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            BigDecimal amount = entry.getAmount();

            currentBalance = currentBalance.add(amount);
            if (settlementDate.isBefore(startDate)) {
                previousBalance = previousBalance.add(amount);
            }

            if (isInsideRange(settlementDate, startDate, endDate)) {
                if (amount.signum() < 0) {
                    currentExpenses = currentExpenses.add(amount.abs());
                } else if (amount.signum() > 0) {
                    currentRevenues = currentRevenues.add(amount);
                }
                continue;
            }

            if (isInsideRange(settlementDate, previousStartDate, previousEndDate)) {
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
            metric(currentRevenues, previousRevenues)
        );
    }

    @Transactional(readOnly = true)
    public DashboardBalanceEvolution buildBalanceEvolution(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        DashboardSeriesGranularity granularity = resolveGranularity(startDate, endDate);
        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        Map<LocalDate, BigDecimal> periodAmountsByBucket = new HashMap<>();
        BigDecimal openingBalance = BigDecimal.ZERO;
        LocalDate lastBucketWithRecords = null;

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            BigDecimal amount = entry.getAmount();

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
        Map<Long, BigDecimal> expenseByCategoryId = new HashMap<>();

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            if (!isInsideRange(settlementDate, startDate, endDate)) {
                continue;
            }

            BigDecimal amount = entry.getAmount();
            if (amount.signum() >= 0) {
                continue;
            }

            Long categoryId = entry.getCategory().getId();
            expenseByCategoryId.merge(categoryId, amount.abs(), BigDecimal::add);
        }

        Map<Long, BigDecimal> subtreeExpenseByCategory = new HashMap<>();
        for (Long categoryId : categoryById.keySet()) {
            subtreeExpense(categoryId, childrenByParentId, expenseByCategoryId, subtreeExpenseByCategory);
        }

        List<Long> visibleCategoryIds = childrenByParentId.getOrDefault(parentCategoryId, List.of());
        List<DashboardExpenseCategoryItem> items = visibleCategoryIds
            .stream()
            .map(categoryId -> toItem(categoryId, categoryById, childrenByParentId, subtreeExpenseByCategory))
            .filter(item -> item.amount().signum() > 0)
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
        Map<Long, BigDecimal> subtreeExpenseByCategory
    ) {
        Category category = categoryById.get(categoryId);
        return new DashboardExpenseCategoryItem(
            categoryId,
            category == null ? "" : category.getTitle(),
            subtreeExpenseByCategory.getOrDefault(categoryId, BigDecimal.ZERO),
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
