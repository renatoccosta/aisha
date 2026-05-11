package dev.ccosta.aisha.application.backup;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Immutable view of the latest system backup execution state exposed to web controllers and write guards.
 */
public record SystemBackupSnapshot(
    SystemBackupStatus status,
    Instant startedAt,
    Instant completedAt,
    String requestedBy,
    Path backupFile,
    String backupFilename,
    String failureMessage
) {

    /**
     * Indicates whether a backup job is currently running.
     *
     * @return true when the current backup status is {@link SystemBackupStatus#RUNNING}
     */
    public boolean running() {
        return status == SystemBackupStatus.RUNNING;
    }

    /**
     * Indicates whether there is a downloadable backup artifact on disk.
     *
     * @return true when the snapshot references an existing backup file path
     */
    public boolean downloadable() {
        return backupFile != null && backupFilename != null;
    }
}
