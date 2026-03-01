package dev.ccosta.aisha.application.entry;

import java.math.BigDecimal;

public record EntryCategorySuggestionRequest(
    Long accountId,
    String description,
    BigDecimal amount
) {
}
