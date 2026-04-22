package dev.ccosta.aisha.application.entry.categorization.model;

/**
 * Enumerates the business events that may request a model training cycle.
 */
public enum EntryCategoryModelTrainingTrigger {
    INITIAL,
    CSV_IMPORT,
    MANUAL
}
