package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.importing.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.importing.EntryImportSummary;

public record EntryImportJobSnapshot(
    String jobId,
    EntryImportJobStatus status,
    int totalRows,
    int processedRows,
    EntryImportSummary summary,
    Integer failedRow,
    String failedColumn,
    EntryImportFailureCause failureCause,
    String failureMessage
) {
}
