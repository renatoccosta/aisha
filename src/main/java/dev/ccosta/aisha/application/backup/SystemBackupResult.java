package dev.ccosta.aisha.application.backup;

import java.nio.file.Path;

/**
 * Result produced by a completed backup archive generation.
 */
public record SystemBackupResult(Path backupFile, String backupFilename) {
}
