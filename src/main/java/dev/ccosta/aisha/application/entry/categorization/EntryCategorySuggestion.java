package dev.ccosta.aisha.application.entry.categorization;

import dev.ccosta.aisha.domain.category.Category;

public record EntryCategorySuggestion(
    Category category,
    double confidence,
    String modelName
) {
}
