package dev.ccosta.aisha.web.treasurydirect;

import dev.ccosta.aisha.application.treasurydirect.TreasuryDirectImportSummary;

/**
 * Immutable view of a Treasury Direct import job state used by polling fragments.
 *
 * @param jobId unique job identifier
 * @param status current job status
 * @param totalSteps total processing steps
 * @param processedSteps processed steps
 * @param summary success summary, when available
 * @param failureMessage failure detail, when available
 */
public record TreasuryDirectImportJobSnapshot(
    String jobId,
    TreasuryDirectImportJobStatus status,
    int totalSteps,
    int processedSteps,
    TreasuryDirectImportSummary summary,
    String failureMessage
) {
}
