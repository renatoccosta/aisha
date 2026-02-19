package dev.ccosta.aisha.application.entry;

public record EntryImportSummary(
    int importedCount,
    int skippedDuplicateCount,
    int createdAccountsCount,
    int createdCategoriesCount,
    long durationMillis
) {
}
