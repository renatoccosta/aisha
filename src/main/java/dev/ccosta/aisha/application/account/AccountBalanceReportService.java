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

        Map<Long, Account> accountById = new HashMap<>();
        for (Account account : accounts) {
            accountById.put(account.getId(), account);
        }

        Map<Long, BigDecimal> previousBalancesByAccount = new HashMap<>();
        Map<Long, Map<LocalDate, BigDecimal>> periodBalancesByAccount = new HashMap<>();

        for (Account account : accounts) {
            applyInitialBalance(account, startDate, endDate, granularity, previousBalancesByAccount, periodBalancesByAccount);
        }

        for (Entry entry : entryRepository.listAllBySettlementDateLessThanEqual(endDate)) {
            Long accountId = entry.getAccount().getId();
            Account account = accountById.get(accountId);
            if (account == null) {
                continue;
            }

            BigDecimal amount = entry.getAmount();
            LocalDate settlementDate = entry.getSettlementDate();
            if (mustIgnoreEntry(settlementDate, account)) {
                continue;
            }

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
            BigDecimal previousPeriodBalance = previousBalancesByAccount.getOrDefault(account.getId(), BigDecimal.ZERO);
            List<BigDecimal> periodBalances = new ArrayList<>(buckets.size());
            BigDecimal runningBalance = previousPeriodBalance;
            for (AccountBalanceBucket bucket : buckets) {
                runningBalance = runningBalance.add(accountBuckets.getOrDefault(bucket.startDate(), BigDecimal.ZERO));
                periodBalances.add(runningBalance);
            }

            rows.add(new AccountBalanceRow(
                account.getId(),
                account.getTitle(),
                account.getDescription(),
                previousPeriodBalance,
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

    private void applyInitialBalance(
        Account account,
        LocalDate startDate,
        LocalDate endDate,
        AccountBalanceGranularity granularity,
        Map<Long, BigDecimal> previousBalancesByAccount,
        Map<Long, Map<LocalDate, BigDecimal>> periodBalancesByAccount
    ) {
        if (account.getInitialBalanceDate() == null || account.getInitialBalance() == null) {
            return;
        }
        if (account.getInitialBalanceDate().isAfter(endDate)) {
            return;
        }
        if (account.getInitialBalanceDate().isBefore(startDate)) {
            previousBalancesByAccount.merge(account.getId(), account.getInitialBalance(), BigDecimal::add);
            return;
        }

        LocalDate bucketStart = normalizeBucketStart(account.getInitialBalanceDate(), granularity);
        periodBalancesByAccount
            .computeIfAbsent(account.getId(), ignored -> new HashMap<>())
            .merge(bucketStart, account.getInitialBalance(), BigDecimal::add);
    }

    private boolean mustIgnoreEntry(LocalDate settlementDate, Account account) {
        LocalDate initialBalanceDate = account.getInitialBalanceDate();
        return initialBalanceDate != null && settlementDate.isBefore(initialBalanceDate);
    }

}
