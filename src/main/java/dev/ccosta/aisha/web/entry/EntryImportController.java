package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.importing.EntryCsvImportOptions;
import dev.ccosta.aisha.application.entry.importing.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.importing.statement.EntryStatementFormat;
import dev.ccosta.aisha.application.entry.importing.statement.EntryStatementImportService;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles entry import pages, asynchronous job creation, and import job status fragments.
 */
@Controller
@RequestMapping("/entries")
public class EntryImportController {

    private static final Logger log = LoggerFactory.getLogger(EntryImportController.class);

    private final AccountService accountService;
    private final EntryStatementImportService entryStatementImportService;
    private final EntryImportJobCoordinator entryImportJobCoordinator;

    public EntryImportController(
        AccountService accountService,
        EntryStatementImportService entryStatementImportService,
        EntryImportJobCoordinator entryImportJobCoordinator
    ) {
        this.accountService = accountService;
        this.entryStatementImportService = entryStatementImportService;
        this.entryImportJobCoordinator = entryImportJobCoordinator;
    }

    /**
     * Renders the generic CSV import page in its idle state.
     *
     * @param model the view model to populate
     * @return the CSV import page template
     */
    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("mode", "idle");
        return "entries/import";
    }

    /**
     * Renders the statement import page with account and format options.
     *
     * @param globalDateFilter the current global date filter used to limit account options
     * @param model the view model to populate
     * @return the statement import page template
     */
    @GetMapping("/statement-import")
    public String statementImportPage(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        Model model
    ) {
        fillStatementImportOptions(model, globalDateFilter);
        model.addAttribute("mode", "idle");
        return "entries/statement-import";
    }

    /**
     * Starts an asynchronous CSV import job and returns the import result fragment.
     *
     * @return the CSV import result fragment
     */
    @PostMapping("/import/jobs")
    public String startImport(
        @RequestParam(name = "file", required = false) MultipartFile file,
        @RequestParam(name = "headerOption", defaultValue = "WITH_HEADER") String headerOption,
        @RequestParam(name = "separatorOption", defaultValue = "COMMA") String separatorOption,
        @RequestParam(name = "separatorOther", required = false) String separatorOther,
        @RequestParam(name = "dateFormatOption", defaultValue = "ISO") String dateFormatOption,
        @RequestParam(name = "dateFormatOther", required = false) String dateFormatOther,
        @RequestParam(name = "amountFormatOption", defaultValue = "PT_BR") String amountFormatOption,
        @RequestParam(name = "amountFormatOther", required = false) String amountFormatOther,
        Model model
    ) {
        try {
            log.info("Starting entries CSV import. filename={}", file != null ? file.getOriginalFilename() : null);
            EntryCsvImportOptions options = buildImportOptions(
                headerOption,
                separatorOption,
                separatorOther,
                dateFormatOption,
                dateFormatOther,
                amountFormatOption,
                amountFormatOther
            );
            String jobId = entryImportJobCoordinator.startCsvJob(file, options);
            fillImportJobModel(model, entryImportJobCoordinator.getSnapshot(jobId));
        } catch (IllegalArgumentException ex) {
            fillImportErrorModel(model, ex.getMessage());
        }
        return "entries/import :: result";
    }

    /**
     * Starts an asynchronous statement import job and returns the statement import result fragment.
     *
     * @return the statement import result fragment
     */
    @PostMapping("/statement-import/jobs")
    public String startStatementImport(
        @RequestParam(name = "file", required = false) MultipartFile file,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "formatId", required = false) String formatId,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        Model model
    ) {
        fillStatementImportOptions(model, globalDateFilter);
        try {
            String jobId = entryImportJobCoordinator.startStatementJob(file, accountId, formatId);
            fillImportJobModel(model, entryImportJobCoordinator.getSnapshot(jobId));
        } catch (IllegalArgumentException ex) {
            fillImportErrorModel(model, ex.getMessage());
        }
        return "entries/statement-import :: result";
    }

    /**
     * Returns the latest CSV import job status fragment.
     *
     * @return the CSV import result fragment
     */
    @GetMapping("/import/jobs/{jobId}")
    public String importStatus(@PathVariable String jobId, HttpServletRequest request, Model model) {
        EntryImportJobSnapshot snapshot = entryImportJobCoordinator.getSnapshot(jobId);
        if (snapshot == null) {
            String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY));
            log.warn("Import job snapshot not found. correlationId={}, jobId={}", correlationId, jobId);
            model.addAttribute("mode", "failed");
            model.addAttribute("failureCauseKey", "entries.import.result.failure.unknown");
            model.addAttribute("failedRow", null);
            return "entries/import :: result";
        }

        fillImportJobModel(model, snapshot);
        return "entries/import :: result";
    }

    /**
     * Returns the latest statement import job status fragment.
     *
     * @return the statement import result fragment
     */
    @GetMapping("/statement-import/jobs/{jobId}")
    public String statementImportStatus(
        @PathVariable String jobId,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        Model model
    ) {
        fillStatementImportOptions(model, globalDateFilter);
        EntryImportJobSnapshot snapshot = entryImportJobCoordinator.getSnapshot(jobId);
        if (snapshot == null) {
            model.addAttribute("mode", "failed");
            model.addAttribute("failureCauseKey", "entries.import.result.failure.unknown");
            model.addAttribute("failedRow", null);
            return "entries/statement-import :: result";
        }

        fillImportJobModel(model, snapshot);
        return "entries/statement-import :: result";
    }

    private void fillStatementImportOptions(Model model, DateFilterState globalDateFilter) {
        DateFilterState effectiveDateFilter = resolveGlobalDateFilter(model, globalDateFilter);
        model.addAttribute("statementAccountOptions", accountService.listVisibleForEntryFilter(effectiveDateFilter.getStartDate()));
        List<EntryStatementFormat> formats = entryStatementImportService.listAvailableFormats();
        model.addAttribute("statementFormats", formats);
    }

    private DateFilterState resolveGlobalDateFilter(Model model, DateFilterState globalDateFilter) {
        if (globalDateFilter != null) {
            return globalDateFilter;
        }

        Object modelAttribute = model.getAttribute("globalDateFilter");
        if (modelAttribute instanceof DateFilterState state) {
            return state;
        }

        DateFilterState fallback = DateFilterState.defaultState(java.time.Clock.systemDefaultZone());
        model.addAttribute("globalDateFilter", fallback);
        log.warn("Global date filter was missing in entries import flow. Falling back to default current-month range.");
        return fallback;
    }

    private void fillImportErrorModel(Model model, String rawMessage) {
        model.addAttribute("mode", "failed");
        model.addAttribute("failedRow", null);
        model.addAttribute("failedColumnKey", null);
        model.addAttribute("failureDetailMessage", rawMessage);
        model.addAttribute("failureCauseKey", toFailureMessageKey(rawMessage, EntryImportFailureCause.UNKNOWN_ERROR));
    }

    private void fillImportJobModel(Model model, EntryImportJobSnapshot snapshot) {
        if (snapshot == null) {
            model.addAttribute("mode", "failed");
            model.addAttribute("failureCauseKey", "entries.import.result.failure.unknown");
            model.addAttribute("failedRow", null);
            model.addAttribute("failedColumnKey", null);
            model.addAttribute("failureDetailMessage", "Unknown import job");
            return;
        }

        if (snapshot.status() == EntryImportJobStatus.PROCESSING) {
            model.addAttribute("mode", "processing");
            model.addAttribute("jobId", snapshot.jobId());
            model.addAttribute("processedRows", snapshot.processedRows());
            model.addAttribute("totalRows", snapshot.totalRows());
            model.addAttribute("progressPercent", toProgressPercent(snapshot.processedRows(), snapshot.totalRows(), false));
            return;
        }

        if (snapshot.status() == EntryImportJobStatus.SUCCESS) {
            model.addAttribute("mode", "success");
            model.addAttribute("summary", snapshot.summary());
            model.addAttribute("progressPercent", 100);
            return;
        }

        model.addAttribute("mode", "failed");
        model.addAttribute("failedRow", snapshot.failedRow());
        model.addAttribute("failedColumnKey", toColumnMessageKey(snapshot.failedColumn()));
        model.addAttribute("failureDetailMessage", snapshot.failureMessage());
        model.addAttribute("failureCauseKey", toFailureMessageKey(snapshot.failureMessage(), snapshot.failureCause()));
        model.addAttribute("progressPercent", toProgressPercent(snapshot.processedRows(), snapshot.totalRows(), true));
    }

    private int toProgressPercent(int processedRows, int totalRows, boolean forceCompleteOnEmpty) {
        if (totalRows <= 0) {
            return forceCompleteOnEmpty ? 100 : 0;
        }
        return Math.min(100, (processedRows * 100) / totalRows);
    }

    private String toFailureMessageKey(String rawMessage, EntryImportFailureCause causeType) {
        if (rawMessage != null) {
            String normalizedMessage = rawMessage.toLowerCase();
            if (normalizedMessage.contains("must not be empty")) {
                return "entries.import.result.failure.emptyFile";
            }
            if (normalizedMessage.contains("only csv files are accepted")) {
                return "entries.import.result.failure.invalidFileType";
            }
            if (normalizedMessage.contains("invalid separator")) {
                return "entries.import.result.failure.invalidSeparator";
            }
            if (normalizedMessage.contains("invalid date format pattern")) {
                return "entries.import.result.failure.invalidDateFormatPattern";
            }
            if (normalizedMessage.contains("invalid amount format pattern")) {
                return "entries.import.result.failure.invalidAmountFormatPattern";
            }
            if (normalizedMessage.contains("account must be informed")) {
                return "entries.import.result.failure.missingAccount";
            }
            if (normalizedMessage.contains("selected account must be active")) {
                return "entries.import.result.failure.inactiveAccount";
            }
            if (normalizedMessage.contains("statement format must be informed")) {
                return "entries.import.result.failure.missingStatementFormat";
            }
            if (normalizedMessage.contains("statement format is not supported")) {
                return "entries.import.result.failure.invalidStatementFormat";
            }
        }

        if (causeType == EntryImportFailureCause.MISSING_REQUIRED_FIELD) {
            return "entries.import.result.failure.missingData";
        }
        if (causeType == EntryImportFailureCause.INVALID_FORMAT) {
            return "entries.import.result.failure.invalidFormat";
        }
        return "entries.import.result.failure.unknown";
    }

    private String toColumnMessageKey(String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return null;
        }
        return switch (columnName) {
            case "account" -> "entries.import.column.account";
            case "movementDate" -> "entries.import.column.movementDate";
            case "settlementDate" -> "entries.import.column.settlementDate";
            case "description" -> "entries.import.column.description";
            case "category" -> "entries.import.column.category";
            case "amount" -> "entries.import.column.amount";
            case "notes" -> "entries.import.column.notes";
            case "externalId" -> "entries.import.column.externalId";
            default -> null;
        };
    }

    private EntryCsvImportOptions buildImportOptions(
        String headerOption,
        String separatorOption,
        String separatorOther,
        String dateFormatOption,
        String dateFormatOther,
        String amountFormatOption,
        String amountFormatOther
    ) {
        boolean hasHeader = resolveHasHeader(headerOption);
        char separator = resolveSeparator(separatorOption, separatorOther);
        String datePattern = resolveDatePattern(dateFormatOption, dateFormatOther);
        String amountPattern = resolveAmountPattern(amountFormatOption, amountFormatOther);
        return new EntryCsvImportOptions(separator, datePattern, amountPattern, hasHeader);
    }

    private boolean resolveHasHeader(String headerOption) {
        return !"WITHOUT_HEADER".equalsIgnoreCase(headerOption);
    }

    private char resolveSeparator(String separatorOption, String separatorOther) {
        if ("SEMICOLON".equalsIgnoreCase(separatorOption)) {
            return ';';
        }
        if ("PIPE".equalsIgnoreCase(separatorOption)) {
            return '|';
        }
        if ("TAB".equalsIgnoreCase(separatorOption)) {
            return '\t';
        }
        if ("OTHER".equalsIgnoreCase(separatorOption)) {
            return resolveCustomSeparator(separatorOther);
        }
        return ',';
    }

    private char resolveCustomSeparator(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new IllegalArgumentException("Invalid separator: blank");
        }

        String value = rawValue;
        if ("\\t".equals(value.trim())) {
            return '\t';
        }

        if (value.length() != 1) {
            throw new IllegalArgumentException("Invalid separator: expected one character");
        }

        return value.charAt(0);
    }

    private String resolveDatePattern(String dateFormatOption, String dateFormatOther) {
        if ("BR".equalsIgnoreCase(dateFormatOption)) {
            return "dd/MM/uuuu";
        }
        if ("US".equalsIgnoreCase(dateFormatOption)) {
            return "MM/dd/uuuu";
        }
        if ("DMY_DASH".equalsIgnoreCase(dateFormatOption)) {
            return "dd-MM-uuuu";
        }
        if ("OTHER".equalsIgnoreCase(dateFormatOption)) {
            if (!StringUtils.hasText(dateFormatOther)) {
                throw new IllegalArgumentException("Invalid date format pattern");
            }
            return dateFormatOther.trim();
        }
        return "uuuu-MM-dd";
    }

    private String resolveAmountPattern(String amountFormatOption, String amountFormatOther) {
        if ("US".equalsIgnoreCase(amountFormatOption)) {
            return "^-?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?$";
        }
        if ("OTHER".equalsIgnoreCase(amountFormatOption)) {
            if (!StringUtils.hasText(amountFormatOther)) {
                throw new IllegalArgumentException("Invalid amount format pattern");
            }
            return amountFormatOther.trim();
        }
        return "^-?(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d{1,2})?$";
    }
}
