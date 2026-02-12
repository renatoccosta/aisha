package dev.ccosta.aisha.application.category;

import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryBalanceReportService {

    private final EntryRepository entryRepository;

    public CategoryBalanceReportService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public CategoryBalanceReport buildReport(List<Category> categories, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }

        CategoryBalanceGranularity granularity = resolveGranularity(startDate, endDate);
        List<CategoryBalanceBucket> buckets = buildBuckets(startDate, endDate, granularity);

        Map<Long, BigDecimal> previousBalancesByCategory = new HashMap<>();
        Map<Long, Map<LocalDate, BigDecimal>> periodBalancesByCategory = new HashMap<>();
        for (Entry entry : entryRepository.listAllBySettlementDateLessThanEqual(endDate)) {
            Long categoryId = entry.getCategory().getId();
            BigDecimal amount = entry.getAmount();
            LocalDate settlementDate = entry.getSettlementDate();

            if (settlementDate.isBefore(startDate)) {
                previousBalancesByCategory.merge(categoryId, amount, BigDecimal::add);
                continue;
            }

            LocalDate bucketStart = normalizeBucketStart(settlementDate, granularity);
            periodBalancesByCategory
                .computeIfAbsent(categoryId, ignored -> new HashMap<>())
                .merge(bucketStart, amount, BigDecimal::add);
        }

        List<CategoryBalanceRow> rows = new ArrayList<>();
        for (Category category : categories) {
            Map<LocalDate, BigDecimal> categoryBuckets = periodBalancesByCategory.getOrDefault(category.getId(), Map.of());
            List<BigDecimal> periodBalances = new ArrayList<>(buckets.size());
            for (CategoryBalanceBucket bucket : buckets) {
                periodBalances.add(categoryBuckets.getOrDefault(bucket.startDate(), BigDecimal.ZERO));
            }

            rows.add(new CategoryBalanceRow(
                category.getId(),
                category.getTitle(),
                category.getDescription(),
                previousBalancesByCategory.getOrDefault(category.getId(), BigDecimal.ZERO),
                periodBalances
            ));
        }

        return new CategoryBalanceReport(startDate, endDate, granularity, buckets, rows);
    }

    private CategoryBalanceGranularity resolveGranularity(LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(startDate.plusYears(1))) {
            return CategoryBalanceGranularity.YEAR;
        }
        if (endDate.isAfter(startDate.plusMonths(1))) {
            return CategoryBalanceGranularity.MONTH;
        }
        return CategoryBalanceGranularity.DAY;
    }

    private List<CategoryBalanceBucket> buildBuckets(
        LocalDate startDate,
        LocalDate endDate,
        CategoryBalanceGranularity granularity
    ) {
        List<CategoryBalanceBucket> buckets = new ArrayList<>();

        LocalDate cursor = normalizeBucketStart(startDate, granularity);
        while (!cursor.isAfter(endDate)) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd = min(lastDateOfBucket(cursor, granularity), endDate);
            buckets.add(new CategoryBalanceBucket(bucketStart, bucketEnd));
            cursor = nextBucketStart(cursor, granularity);
        }

        return buckets;
    }

    private LocalDate normalizeBucketStart(LocalDate date, CategoryBalanceGranularity granularity) {
        if (granularity == CategoryBalanceGranularity.YEAR) {
            return date.withDayOfYear(1);
        }
        if (granularity == CategoryBalanceGranularity.MONTH) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    private LocalDate lastDateOfBucket(LocalDate date, CategoryBalanceGranularity granularity) {
        if (granularity == CategoryBalanceGranularity.YEAR) {
            return date.withDayOfYear(date.lengthOfYear());
        }
        if (granularity == CategoryBalanceGranularity.MONTH) {
            return date.withDayOfMonth(date.lengthOfMonth());
        }
        return date;
    }

    private LocalDate nextBucketStart(LocalDate date, CategoryBalanceGranularity granularity) {
        if (granularity == CategoryBalanceGranularity.YEAR) {
            return date.plusYears(1);
        }
        if (granularity == CategoryBalanceGranularity.MONTH) {
            return date.plusMonths(1);
        }
        return date.plusDays(1);
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }
}
