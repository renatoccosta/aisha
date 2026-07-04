package dev.ccosta.aisha.application.operation;

import java.math.BigDecimal;

public record InvestmentOperationEntryLinkRequest(Long entryId, BigDecimal allocatedAmount) {
}
