package dev.ccosta.aisha.application.dashboard;

import dev.ccosta.aisha.domain.account.AccountType;
import java.math.BigDecimal;

/**
 * Represents the current accumulated balance for a specific account at the selected dashboard end date.
 *
 * @param accountId account identifier
 * @param accountTitle account title shown to users
 * @param accountType account type used by dashboard drill-down interactions
 * @param balance current balance for the account
 */
public record DashboardAccountBalance(Long accountId, String accountTitle, AccountType accountType, BigDecimal balance) {
}
