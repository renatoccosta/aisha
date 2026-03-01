package dev.ccosta.aisha.domain.entry;

import java.math.BigDecimal;

public record EntryCategoryTrainingExample(
    Long accountId,
    String description,
    BigDecimal amount,
    Long categoryId
) {
}
