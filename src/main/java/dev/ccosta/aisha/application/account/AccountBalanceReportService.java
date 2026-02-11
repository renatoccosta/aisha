package dev.ccosta.aisha.application.account;

import dev.ccosta.aisha.domain.account.Account;
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
public class AccountBalanceReportService {

    private final EntryRepository entryRepository;

    public AccountBalanceReportService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public AccountBalanceReport buildReport(List<Account> accounts, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }

        AccountBalanceGranularity granularity = resolveGranularity(startDate, endDate);
        List<AccountBalanceBucket> buckets = buildBuckets(startDate, endDate, granularity);

        Map<Long, BigDecimal> previousBalancesByAccount = new HashMap<>();
        Map<Long, Map<LocalDate, BigDecimal>> periodBalancesByAccount = new HashMap<>();
        for (Entry entry : entryRepository.listAllBySettlementDateLessThanEqual(endDate)) {
            Long accountId = entry.getAccount().getId();
            BigDecimal amount = entry.getAmount();
            LocalDate settlementDate = entry.getSettlementDate();

            if (settlementDate.isBefore(startDate)) {
                previousBalancesByAccount.merge(accountId, amount, BigDecimal::add);
                continue;
            }

            LocalDate bucketStart = normalizeBucketStart(settlementDate, granularity);
            periodBalancesByAccount
                .computeIfAbsent(accountId, ignored -> new HashMap<>())
                .merge(bucketStart, amount, BigDecimal::add);
        }

        List<AccountBalanceRow> rows = new ArrayList<>();
        for (Account account : accounts) {
            Map<LocalDate, BigDecimal> accountBuckets = periodBalancesByAccount.getOrDefault(account.getId(), Map.of());
            List<BigDecimal> periodBalances = new ArrayList<>(buckets.size());
            for (AccountBalanceBucket bucket : buckets) {
                periodBalances.add(accountBuckets.getOrDefault(bucket.startDate(), BigDecimal.ZERO));
            }

            rows.add(new AccountBalanceRow(
                account.getId(),
                account.getTitle(),
                account.getDescription(),
                previousBalancesByAccount.getOrDefault(account.getId(), BigDecimal.ZERO),
                periodBalances
            ));
        }

        return new AccountBalanceReport(startDate, endDate, granularity, buckets, rows);
    }

    private AccountBalanceGranularity resolveGranularity(LocalDate startDate, LocalDate endDate) {
        if (endDate.isAfter(startDate.plusYears(1))) {
            return AccountBalanceGranularity.YEAR;
        }
        if (endDate.isAfter(startDate.plusMonths(1))) {
            return AccountBalanceGranularity.MONTH;
        }
        return AccountBalanceGranularity.DAY;
    }

    private List<AccountBalanceBucket> buildBuckets(
        LocalDate startDate,
        LocalDate endDate,
        AccountBalanceGranularity granularity
    ) {
        List<AccountBalanceBucket> buckets = new ArrayList<>();

        LocalDate cursor = normalizeBucketStart(startDate, granularity);
        while (!cursor.isAfter(endDate)) {
            LocalDate bucketStart = cursor;
            LocalDate bucketEnd = min(lastDateOfBucket(cursor, granularity), endDate);
            buckets.add(new AccountBalanceBucket(bucketStart, bucketEnd));
            cursor = nextBucketStart(cursor, granularity);
        }

        return buckets;
    }

    private LocalDate normalizeBucketStart(LocalDate date, AccountBalanceGranularity granularity) {
        if (granularity == AccountBalanceGranularity.YEAR) {
            return date.withDayOfYear(1);
        }
        if (granularity == AccountBalanceGranularity.MONTH) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    private LocalDate lastDateOfBucket(LocalDate date, AccountBalanceGranularity granularity) {
        if (granularity == AccountBalanceGranularity.YEAR) {
            return date.withDayOfYear(date.lengthOfYear());
        }
        if (granularity == AccountBalanceGranularity.MONTH) {
            return date.withDayOfMonth(date.lengthOfMonth());
        }
        return date;
    }

    private LocalDate nextBucketStart(LocalDate date, AccountBalanceGranularity granularity) {
        if (granularity == AccountBalanceGranularity.YEAR) {
            return date.plusYears(1);
        }
        if (granularity == AccountBalanceGranularity.MONTH) {
            return date.plusMonths(1);
        }
        return date.plusDays(1);
    }

    private LocalDate min(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

}
