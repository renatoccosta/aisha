package dev.ccosta.aisha.application.account;

import java.time.LocalDate;
import java.util.List;

public record AccountBalanceReport(
    LocalDate startDate,
    LocalDate endDate,
    AccountBalanceGranularity granularity,
    List<AccountBalanceBucket> buckets,
    List<AccountBalanceRow> rows
) {
}
