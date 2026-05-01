package dev.ccosta.aisha.application.entry.categorization.model;

import java.time.LocalDateTime;

/**
 * Provides the current state of the persisted entry category suggestion model for UI and operations.
 */
public record EntryCategoryModelStatusView(
    boolean modelAvailable,
    boolean trainingInProgress,
    boolean retrainQueued,
    Long version,
    String status,
    String modelName,
    String pipelineVersion,
    Integer trainingExampleCount,
    Integer labelCount,
    Integer vocabularySize,
    String trigger,
    LocalDateTime completedAt,
    String failureMessage
) {
}
