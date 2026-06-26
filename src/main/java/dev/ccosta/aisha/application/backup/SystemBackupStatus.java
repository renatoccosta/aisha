package dev.ccosta.aisha.application.backup;

/**
 * Represents the lifecycle state of the latest system backup execution.
 */
public enum SystemBackupStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED
}
