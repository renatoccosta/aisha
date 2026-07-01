package dev.ccosta.aisha.application.investment.importing;

/**
 * Summarizes the result of a Treasury Direct operations import.
 *
 * @param importedOperations number of investment operations imported
 * @param importedEntries number of financial entries created and linked
 * @param skippedDuplicateOperations number of operations skipped because their external id already exists
 * @param durationMillis import duration in milliseconds
 */
public record TreasuryDirectImportSummary(
    int importedOperations,
    int importedEntries,
    int skippedDuplicateOperations,
    long durationMillis
) {
}
