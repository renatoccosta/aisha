package dev.ccosta.aisha.application.backup;

import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Coordinates asynchronous system backup executions and exposes their current state.
 */
@Component
public class SystemBackupCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SystemBackupCoordinator.class);

    private final SystemBackupArchiveService archiveService;
    private final TaskExecutor taskExecutor;
    private final Clock clock;
    private final Object monitor = new Object();
    private SystemBackupSnapshot snapshot = new SystemBackupSnapshot(SystemBackupStatus.IDLE, null, null, null, null, null, null);

    public SystemBackupCoordinator(SystemBackupArchiveService archiveService, TaskExecutor taskExecutor, Clock clock) {
        this.archiveService = archiveService;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    /**
     * Starts a backup job when none is currently running.
     *
     * @param requestedBy authenticated username requesting the backup
     * @return snapshot immediately after scheduling, or the current running snapshot when another job is active
     */
    public SystemBackupSnapshot startBackup(String requestedBy) {
        synchronized (monitor) {
            if (snapshot.running()) {
                return snapshot;
            }

            Instant startedAt = clock.instant();
            snapshot = new SystemBackupSnapshot(
                SystemBackupStatus.RUNNING,
                startedAt,
                null,
                requestedBy,
                snapshot.backupFile(),
                snapshot.backupFilename(),
                null
            );
            taskExecutor.execute(() -> runBackup(requestedBy, startedAt));
            return snapshot;
        }
    }

    /**
     * Returns the latest known backup execution state.
     *
     * @return current immutable backup snapshot
     */
    public SystemBackupSnapshot currentSnapshot() {
        synchronized (monitor) {
            if (snapshot.backupFile() != null && !Files.exists(snapshot.backupFile())) {
                snapshot = new SystemBackupSnapshot(
                    snapshot.status(),
                    snapshot.startedAt(),
                    snapshot.completedAt(),
                    snapshot.requestedBy(),
                    null,
                    null,
                    snapshot.failureMessage()
                );
            }
            return snapshot;
        }
    }

    /**
     * Indicates whether data-changing requests should be blocked.
     *
     * @return true when a backup is actively running
     */
    public boolean backupRunning() {
        return currentSnapshot().running();
    }

    private void runBackup(String requestedBy, Instant startedAt) {
        try {
            log.info("Starting system backup. requestedBy={}", requestedBy);
            SystemBackupResult result = archiveService.createBackup(requestedBy, startedAt);
            synchronized (monitor) {
                snapshot = new SystemBackupSnapshot(
                    SystemBackupStatus.SUCCESS,
                    startedAt,
                    clock.instant(),
                    requestedBy,
                    result.backupFile(),
                    result.backupFilename(),
                    null
                );
            }
            log.info("System backup completed. requestedBy={}, backupFilename={}", requestedBy, result.backupFilename());
        } catch (Exception ex) {
            synchronized (monitor) {
                snapshot = new SystemBackupSnapshot(
                    SystemBackupStatus.FAILED,
                    startedAt,
                    clock.instant(),
                    requestedBy,
                    snapshot.backupFile(),
                    snapshot.backupFilename(),
                    ex.getMessage()
                );
            }
            log.error("System backup failed. requestedBy={}", requestedBy, ex);
        }
    }
}
