package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.EntryImportSummary;

public record EntryImportJobSnapshot(
    String jobId,
    EntryImportJobStatus status,
    int totalRows,
    int processedRows,
    EntryImportSummary summary,
    Integer failedRow,
    EntryImportFailureCause failureCause,
    String failureMessage
) {
}
