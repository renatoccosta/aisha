package dev.ccosta.aisha.application.account;

import java.math.BigDecimal;
import java.util.List;

public record AccountBalanceRow(
    Long accountId,
    String accountTitle,
    String accountDescription,
    BigDecimal previousPeriodBalance,
    List<BigDecimal> periodBalances
) {
}
