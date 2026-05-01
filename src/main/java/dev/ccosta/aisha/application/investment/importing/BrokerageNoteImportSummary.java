package dev.ccosta.aisha.application.investment.importing;

/**
 * Summarizes the result of a brokerage note import.
 *
 * @param importedNotes number of brokerage notes imported
 * @param importedOperations number of operations imported from brokerage notes
 * @param skippedDuplicateNotes number of notes skipped because they were already imported
 * @param durationMillis import duration in milliseconds
 */
public record BrokerageNoteImportSummary(
    int importedNotes,
    int importedOperations,
    int skippedDuplicateNotes,
    long durationMillis
) {
}
