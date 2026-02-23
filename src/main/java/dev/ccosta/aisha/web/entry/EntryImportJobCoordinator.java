package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.EntryCsvImportService;
import dev.ccosta.aisha.application.entry.EntryCsvImportOptions;
import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.EntryImportProgressListener;
import dev.ccosta.aisha.application.entry.EntryImportSummary;
import dev.ccosta.aisha.application.entry.EntryImportValidationException;
import dev.ccosta.aisha.application.entry.statement.EntryStatementImportService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EntryImportJobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(EntryImportJobCoordinator.class);

    private final Map<String, EntryImportJobState> jobsById = new ConcurrentHashMap<>();
    private final EntryCsvImportService entryCsvImportService;
    private final EntryStatementImportService entryStatementImportService;
    private final TaskExecutor taskExecutor;

    public EntryImportJobCoordinator(
        EntryCsvImportService entryCsvImportService,
        EntryStatementImportService entryStatementImportService,
        TaskExecutor taskExecutor
    ) {
        this.entryCsvImportService = entryCsvImportService;
        this.entryStatementImportService = entryStatementImportService;
        this.taskExecutor = taskExecutor;
    }

    public String startCsvJob(MultipartFile file, EntryCsvImportOptions options) {
        validateCsvFile(file);
        byte[] fileContent = readFileContent(file);
        return startProcessingJob(listener -> entryCsvImportService.importCsv(fileContent, options, listener));
    }

    public String startStatementJob(MultipartFile file, Long accountId, String formatId) {
        validateNonEmptyFile(file);
        byte[] fileContent = readFileContent(file);
        return startProcessingJob(listener -> entryStatementImportService.importStatement(accountId, formatId, fileContent, listener));
    }

    public EntryImportJobSnapshot getSnapshot(String jobId) {
        EntryImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return null;
        }
        return state.snapshot();
    }

    private String startProcessingJob(ImportJobRunner importJobRunner) {
        String jobId = UUID.randomUUID().toString();
        EntryImportJobState state = new EntryImportJobState(jobId);
        jobsById.put(jobId, state);
        taskExecutor.execute(() -> runImport(jobId, importJobRunner));
        return jobId;
    }

    private byte[] readFileContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read file", ex);
        }
    }

    private void runImport(String jobId, ImportJobRunner importJobRunner) {
        EntryImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return;
        }

        try {
            EntryImportSummary summary = importJobRunner.run(new ProgressUpdater(state));
            state.markSuccess(summary);
        } catch (EntryImportValidationException ex) {
            state.markFailure(ex.getRowPosition(), ex.getColumnName(), ex.getCauseType(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unknown error while importing entries. jobId={}", jobId, ex);
            state.markFailure(null, null, EntryImportFailureCause.UNKNOWN_ERROR, "Unknown import error");
        }
    }

    private void validateCsvFile(MultipartFile file) {
        validateNonEmptyFile(file);
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are accepted");
        }
    }

    private void validateNonEmptyFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
    }

    private static final class ProgressUpdater implements EntryImportProgressListener {

        private final EntryImportJobState state;

        private ProgressUpdater(EntryImportJobState state) {
            this.state = state;
        }

        @Override
        public void onStart(int totalRows) {
            state.setTotalRows(totalRows);
        }

        @Override
        public void onRowProcessed(int processedRows) {
            state.setProcessedRows(processedRows);
        }
    }

    private static final class EntryImportJobState {

        private final String jobId;
        private volatile EntryImportJobStatus status = EntryImportJobStatus.PROCESSING;
        private volatile int totalRows = 0;
        private volatile int processedRows = 0;
        private volatile EntryImportSummary summary;
        private volatile Integer failedRow;
        private volatile String failedColumn;
        private volatile EntryImportFailureCause failureCause;
        private volatile String failureMessage;

        private EntryImportJobState(String jobId) {
            this.jobId = jobId;
        }

        private void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        private void setProcessedRows(int processedRows) {
            this.processedRows = processedRows;
        }

        private void markSuccess(EntryImportSummary summary) {
            this.summary = summary;
            this.status = EntryImportJobStatus.SUCCESS;
        }

        private void markFailure(Integer failedRow, String failedColumn, EntryImportFailureCause failureCause, String failureMessage) {
            this.failedRow = failedRow;
            this.failedColumn = failedColumn;
            this.failureCause = failureCause;
            this.failureMessage = failureMessage;
            this.status = EntryImportJobStatus.FAILED;
        }

        private EntryImportJobSnapshot snapshot() {
            return new EntryImportJobSnapshot(jobId, status, totalRows, processedRows, summary, failedRow, failedColumn, failureCause, failureMessage);
        }
    }

    @FunctionalInterface
    private interface ImportJobRunner {
        EntryImportSummary run(EntryImportProgressListener listener);
    }
}
