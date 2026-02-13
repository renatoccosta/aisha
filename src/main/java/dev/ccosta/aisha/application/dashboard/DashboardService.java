package dev.ccosta.aisha.application.dashboard;

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

    private static final int EXPENSE_CATEGORY_LIMIT = 5;

    private final EntryRepository entryRepository;

    public DashboardService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
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
        }

        List<DashboardBalancePoint> points = new ArrayList<>();
        BigDecimal accumulatedBalance = openingBalance;
        for (LocalDate bucketStart : buildBucketStarts(startDate, endDate, granularity)) {
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
        }

        List<DashboardRevenueExpensePoint> points = new ArrayList<>();
        for (LocalDate bucketStart : buildBucketStarts(startDate, endDate, granularity)) {
            points.add(new DashboardRevenueExpensePoint(
                bucketStart,
                revenuesByBucket.getOrDefault(bucketStart, BigDecimal.ZERO),
                expensesByBucket.getOrDefault(bucketStart, BigDecimal.ZERO)
            ));
        }

        return new DashboardRevenueExpenseEvolution(startDate, endDate, granularity, points);
    }

    @Transactional(readOnly = true)
    public DashboardExpenseCategoryBreakdown buildExpenseCategoryBreakdown(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);

        List<Entry> entries = entryRepository.listAllBySettlementDateLessThanEqual(endDate);
        Map<String, BigDecimal> expensesByCategory = new HashMap<>();

        for (Entry entry : entries) {
            LocalDate settlementDate = entry.getSettlementDate();
            if (!isInsideRange(settlementDate, startDate, endDate)) {
                continue;
            }

            BigDecimal amount = entry.getAmount();
            if (amount.signum() >= 0) {
                continue;
            }

            String categoryName = entry.getCategory().getTitle();
            expensesByCategory.merge(categoryName, amount.abs(), BigDecimal::add);
        }

        List<Map.Entry<String, BigDecimal>> sortedItems = expensesByCategory.entrySet()
            .stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
            .toList();

        List<DashboardExpenseCategoryItem> items = new ArrayList<>();
        BigDecimal othersTotal = BigDecimal.ZERO;

        for (int i = 0; i < sortedItems.size(); i++) {
            Map.Entry<String, BigDecimal> item = sortedItems.get(i);
            if (i < EXPENSE_CATEGORY_LIMIT) {
                items.add(new DashboardExpenseCategoryItem(item.getKey(), item.getValue(), false));
                continue;
            }

            othersTotal = othersTotal.add(item.getValue());
        }

        if (othersTotal.signum() > 0) {
            items.add(new DashboardExpenseCategoryItem(null, othersTotal, true));
        }

        return new DashboardExpenseCategoryBreakdown(startDate, endDate, items);
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
