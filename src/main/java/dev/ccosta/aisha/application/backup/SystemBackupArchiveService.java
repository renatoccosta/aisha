package dev.ccosta.aisha.application.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ccosta.aisha.application.ApplicationVersionProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds the final zip archive containing the database dump, backup manifest, and manual restore instructions.
 */
@Service
public class SystemBackupArchiveService {

    private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DatabaseDumpService databaseDumpService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock;
    private final Path backupDirectory;
    private final String applicationName;
    private final ApplicationVersionProvider applicationVersionProvider;

    public SystemBackupArchiveService(
        DatabaseDumpService databaseDumpService,
        Clock clock,
        @Value("${aisha.backup.directory:backups}") Path backupDirectory,
        @Value("${spring.application.name:aisha}") String applicationName,
        ApplicationVersionProvider applicationVersionProvider
    ) {
        this.databaseDumpService = databaseDumpService;
        this.clock = clock;
        this.backupDirectory = backupDirectory;
        this.applicationName = applicationName;
        this.applicationVersionProvider = applicationVersionProvider;
    }

    /**
     * Generates a timestamped backup zip file for the current database.
     *
     * @param requestedBy authenticated username that requested the backup
     * @param startedAt timestamp captured when the asynchronous job started
     * @return generated backup archive metadata
     */
    public SystemBackupResult createBackup(String requestedBy, Instant startedAt) {
        Path workDirectory = null;
        try {
            Files.createDirectories(backupDirectory);
            workDirectory = Files.createTempDirectory(backupDirectory, "work-");
            DatabaseBackupDump dump = databaseDumpService.dump(workDirectory);
            Instant completedAt = clock.instant();
            String backupFilename = "aisha-backup-" + FILENAME_TIMESTAMP.withZone(clock.getZone()).format(startedAt) + ".zip";
            Path backupFile = backupDirectory.resolve(backupFilename);
            SystemBackupManifest manifest = new SystemBackupManifest(
                startedAt.toString(),
                completedAt.toString(),
                requestedBy,
                applicationName,
                applicationVersionProvider.currentVersion(),
                dump.databaseProductName(),
                dump.databaseProductVersion(),
                sanitizeDatabaseUrl(dump.databaseUrl()),
                dump.strategy(),
                backupFilename,
                Map.of("dumpFilename", dump.dumpFilename())
            );

            writeArchive(backupFile, dump, manifest);
            return new SystemBackupResult(backupFile, backupFilename);
        } catch (IOException ex) {
            throw new SystemBackupException("Unable to create backup archive", ex);
        } finally {
            deleteWorkDirectory(workDirectory);
        }
    }

    private void writeArchive(Path backupFile, DatabaseBackupDump dump, SystemBackupManifest manifest) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(backupFile);
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addFile(zipOutputStream, dump.dumpFilename(), dump.dumpFile());
            addBytes(zipOutputStream, "manifest.json", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            addBytes(zipOutputStream, "README_RESTORE.txt", restoreInstructions(dump).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void addFile(ZipOutputStream zipOutputStream, String entryName, Path source) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(source, zipOutputStream);
        zipOutputStream.closeEntry();
    }

    private void addBytes(ZipOutputStream zipOutputStream, String entryName, byte[] content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
    }

    private String restoreInstructions(DatabaseBackupDump dump) {
        return """
            AI$HA system backup restore instructions

            This archive contains:
            - database.dump: database dump generated with %s
            - manifest.json: backup context and technical metadata
            - README_RESTORE.txt: this file

            Restore is currently manual.

            HSQLDB:
            1. Stop the application.
            2. Use the HSQLDB tools to run the database.dump SQL script into the target database.
            3. Start the application and verify the Flyway schema history and application screens.

            PostgreSQL:
            1. Stop the application.
            2. Create or clean the target database according to your recovery plan.
            3. Run: pg_restore --dbname=<target_database> --clean --if-exists database.dump
            4. Start the application and verify the Flyway schema history and application screens.

            Always restore into a safe test database first before replacing production data.
            """.formatted(dump.strategy());
    }

    private String sanitizeDatabaseUrl(String databaseUrl) {
        if (databaseUrl == null) {
            return null;
        }
        return databaseUrl.replaceAll("(?i)(password=)[^;&]+", "$1***");
    }

    private void deleteWorkDirectory(Path workDirectory) {
        if (workDirectory == null || !Files.exists(workDirectory)) {
            return;
        }
        try (var paths = Files.walk(workDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary files are best-effort cleanup only.
                }
            });
        } catch (IOException ignored) {
            // Temporary files are best-effort cleanup only.
        }
    }
}
