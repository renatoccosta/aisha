package dev.ccosta.aisha.application.entry.importing;

public record EntryImportSummary(
    int importedCount,
    int skippedDuplicateCount,
    int createdAccountsCount,
    int createdCategoriesCount,
    long durationMillis
) {
}
