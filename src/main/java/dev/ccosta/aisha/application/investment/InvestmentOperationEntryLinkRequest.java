package dev.ccosta.aisha.application.investment;

import java.math.BigDecimal;

public record InvestmentOperationEntryLinkRequest(Long entryId, BigDecimal allocatedAmount) {
}
