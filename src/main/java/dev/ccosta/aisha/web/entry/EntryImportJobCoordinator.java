package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.EntryCsvImportService;
import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.EntryImportProgressListener;
import dev.ccosta.aisha.application.entry.EntryImportSummary;
import dev.ccosta.aisha.application.entry.EntryImportValidationException;
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
    private final TaskExecutor taskExecutor;

    public EntryImportJobCoordinator(EntryCsvImportService entryCsvImportService, TaskExecutor taskExecutor) {
        this.entryCsvImportService = entryCsvImportService;
        this.taskExecutor = taskExecutor;
    }

    public String startJob(MultipartFile file) {
        validateFile(file);

        byte[] fileContent;
        try {
            fileContent = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read file", ex);
        }

        String jobId = UUID.randomUUID().toString();
        EntryImportJobState state = new EntryImportJobState(jobId);
        jobsById.put(jobId, state);

        taskExecutor.execute(() -> runImport(jobId, fileContent));
        return jobId;
    }

    public EntryImportJobSnapshot getSnapshot(String jobId) {
        EntryImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return null;
        }
        return state.snapshot();
    }

    private void runImport(String jobId, byte[] fileContent) {
        EntryImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return;
        }

        try {
            EntryImportSummary summary = entryCsvImportService.importCsv(fileContent, new ProgressUpdater(state));
            state.markSuccess(summary);
        } catch (EntryImportValidationException ex) {
            state.markFailure(ex.getRowPosition(), ex.getCauseType(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unknown error while importing entries CSV. jobId={}", jobId, ex);
            state.markFailure(null, EntryImportFailureCause.UNKNOWN_ERROR, "Unknown import error");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are accepted");
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

        private void markFailure(Integer failedRow, EntryImportFailureCause failureCause, String failureMessage) {
            this.failedRow = failedRow;
            this.failureCause = failureCause;
            this.failureMessage = failureMessage;
            this.status = EntryImportJobStatus.FAILED;
        }

        private EntryImportJobSnapshot snapshot() {
            return new EntryImportJobSnapshot(jobId, status, totalRows, processedRows, summary, failedRow, failureCause, failureMessage);
        }
    }
}
