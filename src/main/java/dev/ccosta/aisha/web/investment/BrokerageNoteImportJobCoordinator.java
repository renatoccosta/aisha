package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.importing.BrokerageNoteImportService;
import dev.ccosta.aisha.application.investment.importing.BrokerageNoteImportSummary;
import dev.ccosta.aisha.domain.account.Account;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    private final BrokerageNoteImportService importService;
    private final TaskExecutor taskExecutor;

    public BrokerageNoteImportJobCoordinator(
        AccountService accountService,
        BrokerageNoteImportService importService,
        TaskExecutor taskExecutor
    ) {
        this.accountService = accountService;
        this.importService = importService;
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
        String fileHash = sha256(fileContent);

        String jobId = UUID.randomUUID().toString();
        BrokerageNoteImportJobState state = new BrokerageNoteImportJobState(jobId);
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

    private String sha256(byte[] fileContent) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fileContent));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private void runImport(String jobId, Long accountId, String originalFilename, String fileHash, byte[] fileContent) {
        BrokerageNoteImportJobState state = jobsById.get(jobId);
        if (state == null) {
            return;
        }

        try {
            state.setTotalSteps(1);
            log.info("Brokerage note import started. jobId={}, filename={}, bytes={}", jobId, originalFilename, fileContent.length);
            BrokerageNoteImportSummary summary = importService.importPdf(accountId, originalFilename, fileHash, fileContent);
            state.setProcessedSteps(1);
            state.markSuccess(summary);
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
