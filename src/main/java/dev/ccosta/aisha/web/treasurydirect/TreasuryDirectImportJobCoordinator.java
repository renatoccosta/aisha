package dev.ccosta.aisha.web.treasurydirect;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.treasurydirect.TreasuryDirectImportService;
import dev.ccosta.aisha.application.treasurydirect.TreasuryDirectImportSummary;
import dev.ccosta.aisha.application.treasurydirect.UnsupportedTreasuryDirectFormatException;
import dev.ccosta.aisha.domain.account.Account;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Coordinates asynchronous Treasury Direct import jobs for the web import flow.
 */
@Component
public class TreasuryDirectImportJobCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TreasuryDirectImportJobCoordinator.class);

    private final Map<String, TreasuryDirectImportJobState> jobsById = new ConcurrentHashMap<>();
    private final AccountService accountService;
    private final TreasuryDirectImportService importService;
    private final TaskExecutor taskExecutor;

    public TreasuryDirectImportJobCoordinator(
        AccountService accountService,
        TreasuryDirectImportService importService,
        TaskExecutor taskExecutor
    ) {
        this.accountService = accountService;
        this.importService = importService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Starts a Treasury Direct import job after validating the selected account and uploaded file.
     *
     * @param file uploaded JSON file
     * @param accountId account used by the import routine
     * @return created job identifier
     */
    public String startJob(MultipartFile file, Long accountId) {
        validateAccount(accountId);
        validateImportFile(file);
        byte[] fileContent = readFileContent(file);
        String originalFilename = file.getOriginalFilename();
        String fileHash = sha256(fileContent);

        String jobId = UUID.randomUUID().toString();
        TreasuryDirectImportJobState state = new TreasuryDirectImportJobState(jobId);
        jobsById.put(jobId, state);
        taskExecutor.execute(() -> runImport(jobId, accountId, originalFilename, fileHash, fileContent));
        return jobId;
    }

    /**
     * Returns the latest import job snapshot.
     *
     * @param jobId job identifier
     * @return current snapshot, or null when the job is unknown
     */
    public TreasuryDirectImportJobSnapshot getSnapshot(String jobId) {
        TreasuryDirectImportJobState state = jobsById.get(jobId);
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

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
    }

    private byte[] readFileContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read file", ex);
        }
    }

    private String sha256(byte[] fileContent) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fileContent));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private void runImport(String jobId, Long accountId, String originalFilename, String fileHash, byte[] fileContent) {
        TreasuryDirectImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return;
        }

        try {
            state.setTotalSteps(1);
            log.info("Treasury Direct import started. jobId={}, filename={}, bytes={}", jobId, originalFilename, fileContent.length);
            TreasuryDirectImportSummary summary = importService.importFile(accountId, originalFilename, fileHash, fileContent);
            state.setProcessedSteps(1);
            state.markSuccess(summary);
        } catch (UnsupportedTreasuryDirectFormatException ex) {
            log.warn("Unsupported Treasury Direct format. jobId={}, filename={}", jobId, originalFilename);
            state.markFailure(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unknown error while importing Treasury Direct operations. jobId={}", jobId, ex);
            state.markFailure("Unknown Treasury Direct import error");
        }
    }

    private static final class TreasuryDirectImportJobState {

        private final String jobId;
        private volatile TreasuryDirectImportJobStatus status = TreasuryDirectImportJobStatus.PROCESSING;
        private volatile int totalSteps = 0;
        private volatile int processedSteps = 0;
        private volatile TreasuryDirectImportSummary summary;
        private volatile String failureMessage;

        private TreasuryDirectImportJobState(String jobId) {
            this.jobId = jobId;
        }

        private void setTotalSteps(int totalSteps) {
            this.totalSteps = totalSteps;
        }

        private void setProcessedSteps(int processedSteps) {
            this.processedSteps = processedSteps;
        }

        private void markSuccess(TreasuryDirectImportSummary summary) {
            this.summary = summary;
            this.status = TreasuryDirectImportJobStatus.SUCCESS;
        }

        private void markFailure(String failureMessage) {
            this.failureMessage = failureMessage;
            this.status = TreasuryDirectImportJobStatus.FAILED;
        }

        private TreasuryDirectImportJobSnapshot snapshot() {
            return new TreasuryDirectImportJobSnapshot(jobId, status, totalSteps, processedSteps, summary, failureMessage);
        }
    }
}
