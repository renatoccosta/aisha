package dev.ccosta.aisha.application.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates database-native dump artifacts for the database engine backing the application.
 */
@Service
public class DatabaseDumpService {

    private final DataSource dataSource;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String pgDumpCommand;
    private final Duration pgDumpTimeout;

    public DatabaseDumpService(
        DataSource dataSource,
        @Value("${spring.datasource.url}") String datasourceUrl,
        @Value("${spring.datasource.username:}") String datasourceUsername,
        @Value("${spring.datasource.password:}") String datasourcePassword,
        @Value("${aisha.backup.postgres.pg-dump-command:pg_dump}") String pgDumpCommand,
        @Value("${aisha.backup.postgres.pg-dump-timeout:PT5M}") Duration pgDumpTimeout
    ) {
        this.dataSource = dataSource;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.pgDumpCommand = pgDumpCommand;
        this.pgDumpTimeout = pgDumpTimeout;
    }

    /**
     * Dumps the current database into the provided work directory.
     *
     * @param workDirectory temporary directory used for dump files
     * @return metadata describing the generated dump artifact
     */
    public DatabaseBackupDump dump(Path workDirectory) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String productName = metadata.getDatabaseProductName();
            String normalizedProductName = productName.toLowerCase(Locale.ROOT);
            Path dumpFile = workDirectory.resolve("database.dump");

            if (normalizedProductName.contains("hsql")) {
                dumpHsqldb(connection, dumpFile);
                return new DatabaseBackupDump(
                    dumpFile,
                    "database.dump",
                    productName,
                    metadata.getDatabaseProductVersion(),
                    metadata.getURL(),
                    "HSQLDB SCRIPT"
                );
            }

            if (normalizedProductName.contains("postgres")) {
                dumpPostgres(dumpFile);
                return new DatabaseBackupDump(
                    dumpFile,
                    "database.dump",
                    productName,
                    metadata.getDatabaseProductVersion(),
                    metadata.getURL(),
                    "pg_dump custom format"
                );
            }

            throw new SystemBackupException("Unsupported database for backup: " + productName);
        } catch (SQLException ex) {
            throw new SystemBackupException("Unable to inspect database metadata for backup", ex);
        }
    }

    private void dumpHsqldb(Connection connection, Path dumpFile) {
        try (Statement statement = connection.createStatement()) {
            Files.deleteIfExists(dumpFile);
            statement.execute("SCRIPT '" + escapeHsqldbPath(dumpFile.toAbsolutePath().toString()) + "'");
        } catch (IOException | SQLException ex) {
            throw new SystemBackupException("Unable to dump HSQLDB database", ex);
        }
    }

    private String escapeHsqldbPath(String path) {
        return path.replace("'", "''");
    }

    private void dumpPostgres(Path dumpFile) {
        List<String> command = new ArrayList<>();
        command.add(pgDumpCommand);
        command.add("--format=custom");
        command.add("--file=" + dumpFile.toAbsolutePath());
        if (datasourceUsername != null && !datasourceUsername.isBlank()) {
            command.add("--username=" + datasourceUsername);
        }
        command.add("--dbname=" + toPostgresUri(datasourceUrl));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        if (datasourcePassword != null && !datasourcePassword.isBlank()) {
            processBuilder.environment().put("PGPASSWORD", datasourcePassword);
        }

        try {
            Process process = processBuilder.start();
            boolean completed = process.waitFor(pgDumpTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new SystemBackupException("pg_dump timed out");
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                throw new SystemBackupException("pg_dump failed with exit code " + process.exitValue() + ": " + output);
            }
        } catch (IOException ex) {
            throw new SystemBackupException("Unable to start pg_dump. Check if pg_dump is installed and configured.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SystemBackupException("Interrupted while waiting for pg_dump", ex);
        }
    }

    private String toPostgresUri(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "postgresql:" + jdbcUrl.substring("jdbc:postgresql:".length());
        }
        return jdbcUrl;
    }
}
