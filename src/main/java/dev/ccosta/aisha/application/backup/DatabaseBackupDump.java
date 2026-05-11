package dev.ccosta.aisha.application.backup;

import java.nio.file.Path;

/**
 * Database dump artifact and descriptive metadata created as part of a system backup.
 */
public record DatabaseBackupDump(
    Path dumpFile,
    String dumpFilename,
    String databaseProductName,
    String databaseProductVersion,
    String databaseUrl,
    String strategy
) {
}
