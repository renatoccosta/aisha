package dev.ccosta.aisha.application.entry.categorization;

import java.math.BigDecimal;

public record EntryCategorySuggestionRequest(
    Long accountId,
    String description,
    BigDecimal amount
) {
}
