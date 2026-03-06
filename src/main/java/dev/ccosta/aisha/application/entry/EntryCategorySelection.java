package dev.ccosta.aisha.application.entry;

public record EntryCategorySelection(
    Long categoryId,
    String newCategoryTitle,
    Long suggestedCategoryId,
    Double suggestedCategoryConfidence
) {
}
