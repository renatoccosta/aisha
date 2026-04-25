package dev.ccosta.aisha.web.investment;

/**
 * Immutable view of a brokerage note import job state used by polling fragments.
 *
 * @param jobId unique job identifier
 * @param status current job status
 * @param totalSteps total processing steps
 * @param processedSteps processed steps
 * @param summary success summary, when available
 * @param failureMessage failure detail, when available
 */
public record BrokerageNoteImportJobSnapshot(
    String jobId,
    BrokerageNoteImportJobStatus status,
    int totalSteps,
    int processedSteps,
    BrokerageNoteImportSummary summary,
    String failureMessage
) {
}
