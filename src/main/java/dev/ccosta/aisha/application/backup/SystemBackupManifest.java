package dev.ccosta.aisha.application.backup;

import java.util.Map;

/**
 * Describes the context and technical metadata of a generated system backup.
 */
public record SystemBackupManifest(
    String backupStartedAt,
    String backupCompletedAt,
    String requestedBy,
    String applicationName,
    String applicationVersion,
    String databaseProductName,
    String databaseProductVersion,
    String databaseUrl,
    String dumpStrategy,
    String backupFilename,
    Map<String, String> additionalMetadata
) {
}
