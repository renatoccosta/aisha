package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.investment.importing.TreasuryDirectImportSummary;

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
