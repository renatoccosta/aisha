package dev.ccosta.aisha.application.dashboard;

import dev.ccosta.aisha.domain.account.AccountType;
import java.math.BigDecimal;

/**
 * Represents the current accumulated balance grouped by account type.
 *
 * @param accountType account type used for grouping
 * @param balance current balance for the type at the selected dashboard end date
 */
public record DashboardAccountTypeBalance(AccountType accountType, BigDecimal balance) {
}
