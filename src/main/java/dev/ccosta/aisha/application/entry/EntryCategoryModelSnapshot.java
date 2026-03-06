package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.ai.classification.TextClassificationModel;
import dev.ccosta.aisha.domain.entry.EntryCategorySuggestionModelArtifact;

/**
 * Couples a persisted model artifact with the in-memory model instance reconstructed from it.
 */
public record EntryCategoryModelSnapshot(
    EntryCategorySuggestionModelArtifact artifact,
    TextClassificationModel<Long> model
) {
}
