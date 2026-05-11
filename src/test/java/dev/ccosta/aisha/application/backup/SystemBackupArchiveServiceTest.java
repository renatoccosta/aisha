package dev.ccosta.aisha.application.backup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ccosta.aisha.application.ApplicationVersionProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.info.BuildProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemBackupArchiveServiceTest {

    @TempDir
    private Path tempDirectory;

    @Mock
    private DatabaseDumpService databaseDumpService;

    @Test
    void shouldCreateBackupZipWithDatabaseDumpManifestAndRestoreInstructions() throws Exception {
        Path dumpFile = tempDirectory.resolve("database.dump");
        Files.writeString(dumpFile, "CREATE TABLE sample(id INTEGER);");
        when(databaseDumpService.dump(any())).thenReturn(new DatabaseBackupDump(
            dumpFile,
            "database.dump",
            "HSQL Database Engine",
            "2.7.4",
            "jdbc:hsqldb:mem:aisha",
            "HSQLDB SCRIPT"
        ));
        ObjectMapper objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T13:45:30Z"), ZoneId.of("UTC"));
        SystemBackupArchiveService service = new SystemBackupArchiveService(
            databaseDumpService,
            clock,
            tempDirectory.resolve("backups"),
            "aisha",
            new ApplicationVersionProvider(Optional.of(buildPropertiesWithVersion("1.2.3")), "desenvolvimento")
        );

        SystemBackupResult result = service.createBackup("admin", Instant.parse("2026-05-10T13:45:00Z"));

        assertThat(result.backupFilename()).isEqualTo("aisha-backup-20260510-104500.zip");
        assertThat(result.backupFile()).exists();
        try (ZipFile zipFile = new ZipFile(result.backupFile().toFile())) {
            assertThat(zipFile.getEntry("database.dump")).isNotNull();
            assertThat(zipFile.getEntry("manifest.json")).isNotNull();
            assertThat(zipFile.getEntry("README_RESTORE.txt")).isNotNull();

            JsonNode manifest = objectMapper.readTree(zipFile.getInputStream(zipFile.getEntry("manifest.json")));
            assertThat(manifest.get("backupStartedAt").asText()).isEqualTo("2026-05-10T13:45:00Z");
            assertThat(manifest.get("backupCompletedAt").asText()).isEqualTo("2026-05-10T13:45:30Z");
            assertThat(manifest.get("requestedBy").asText()).isEqualTo("admin");
            assertThat(manifest.get("applicationVersion").asText()).isEqualTo("1.2.3");
            assertThat(manifest.get("databaseProductName").asText()).isEqualTo("HSQL Database Engine");
            assertThat(manifest.get("dumpStrategy").asText()).isEqualTo("HSQLDB SCRIPT");
            assertThat(manifest.get("backupFilename").asText()).isEqualTo(result.backupFilename());
        }
    }

    private static BuildProperties buildPropertiesWithVersion(String version) {
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }
}
