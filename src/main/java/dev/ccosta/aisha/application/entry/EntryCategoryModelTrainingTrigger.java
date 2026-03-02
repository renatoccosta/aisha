package dev.ccosta.aisha.application.entry;

/**
 * Enumerates the business events that may request a model training cycle.
 */
public enum EntryCategoryModelTrainingTrigger {
    INITIAL,
    CSV_IMPORT,
    MANUAL
}
