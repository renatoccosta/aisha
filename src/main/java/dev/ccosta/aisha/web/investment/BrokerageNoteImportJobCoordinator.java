package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Coordinates asynchronous brokerage note import jobs for the web import flow.
 */
@Component
public class BrokerageNoteImportJobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(BrokerageNoteImportJobCoordinator.class);

    private final Map<String, BrokerageNoteImportJobState> jobsById = new ConcurrentHashMap<>();
    private final AccountService accountService;
    private final TaskExecutor taskExecutor;

    public BrokerageNoteImportJobCoordinator(AccountService accountService, TaskExecutor taskExecutor) {
        this.accountService = accountService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Starts a brokerage note import job after validating the selected account and PDF file.
     *
     * @param file uploaded brokerage note PDF
     * @param accountId account used by the import routine
     * @return created job identifier
     */
    public String startJob(MultipartFile file, Long accountId) {
        validateAccount(accountId);
        validatePdfFile(file);
        byte[] fileContent = readFileContent(file);
        String originalFilename = file.getOriginalFilename();

        String jobId = UUID.randomUUID().toString();
        BrokerageNoteImportJobState state = new BrokerageNoteImportJobState(jobId);
        jobsById.put(jobId, state);
        taskExecutor.execute(() -> runPlaceholderImport(jobId, originalFilename, fileContent));
        return jobId;
    }

    /**
     * Returns the latest import job snapshot.
     *
     * @param jobId job identifier
     * @return current snapshot, or null when the job is unknown
     */
    public BrokerageNoteImportJobSnapshot getSnapshot(String jobId) {
        BrokerageNoteImportJobState state = jobsById.get(jobId);
        return state == null ? null : state.snapshot();
    }

    private void validateAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account must be informed");
        }
        Account account;
        try {
            account = accountService.findById(accountId);
        } catch (AccountNotFoundException ex) {
            throw new IllegalArgumentException("Selected account must exist", ex);
        }
        if (account.getDeactivationDate() != null) {
            throw new IllegalArgumentException("Selected account must be active");
        }
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are accepted");
        }
    }

    private byte[] readFileContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read file", ex);
        }
    }

    private void runPlaceholderImport(String jobId, String originalFilename, byte[] fileContent) {
        BrokerageNoteImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return;
        }

        Instant startedAt = Instant.now();
        try {
            state.setTotalSteps(1);
            log.info("Brokerage note import placeholder started. jobId={}, filename={}, bytes={}", jobId, originalFilename, fileContent.length);
            state.setProcessedSteps(1);
            state.markSuccess(new BrokerageNoteImportSummary(0, 0, 0, Duration.between(startedAt, Instant.now()).toMillis()));
        } catch (Exception ex) {
            log.error("Unknown error while importing brokerage notes. jobId={}", jobId, ex);
            state.markFailure("Unknown brokerage note import error");
        }
    }

    private static final class BrokerageNoteImportJobState {

        private final String jobId;
        private volatile BrokerageNoteImportJobStatus status = BrokerageNoteImportJobStatus.PROCESSING;
        private volatile int totalSteps = 0;
        private volatile int processedSteps = 0;
        private volatile BrokerageNoteImportSummary summary;
        private volatile String failureMessage;

        private BrokerageNoteImportJobState(String jobId) {
            this.jobId = jobId;
        }

        private void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
        }

        private void setProcessedSteps(int processedSteps) {
            this.processedSteps = processedSteps;
        }

        private void markSuccess(BrokerageNoteImportSummary summary) {
            this.summary = summary;
            this.status = BrokerageNoteImportJobStatus.SUCCESS;
        }

        private void markFailure(String failureMessage) {
            this.failureMessage = failureMessage;
            this.status = BrokerageNoteImportJobStatus.FAILED;
        }

        private BrokerageNoteImportJobSnapshot snapshot() {
            return new BrokerageNoteImportJobSnapshot(jobId, status, totalSteps, processedSteps, summary, failureMessage);
        }
    }
}
