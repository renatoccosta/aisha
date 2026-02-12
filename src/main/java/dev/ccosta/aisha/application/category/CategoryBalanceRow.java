package dev.ccosta.aisha.application.category;

import java.math.BigDecimal;
import java.util.List;

public record CategoryBalanceRow(
    Long categoryId,
    String categoryTitle,
    String categoryDescription,
    BigDecimal previousPeriodBalance,
    List<BigDecimal> periodBalances
) {
}
