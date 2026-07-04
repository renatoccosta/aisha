package dev.ccosta.aisha.web.brokeragenote;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.time.Clock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles brokerage note import pages, asynchronous job creation, and status fragments.
 */
@Controller
@RequestMapping("/investments/brokerage-note-import")
public class BrokerageNoteImportController {

    private final AccountService accountService;
    private final BrokerageNoteImportJobCoordinator jobCoordinator;

    public BrokerageNoteImportController(AccountService accountService, BrokerageNoteImportJobCoordinator jobCoordinator) {
        this.accountService = accountService;
        this.jobCoordinator = jobCoordinator;
    }

    /**
     * Renders the brokerage note import page in its idle state.
     *
     * @param globalDateFilter current global date filter
     * @param model view model
     * @return brokerage note import template
     */
    @GetMapping
    public String importPage(@ModelAttribute("globalDateFilter") DateFilterState globalDateFilter, Model model) {
        fillImportOptions(model, globalDateFilter);
        model.addAttribute("mode", "idle");
        return "investments/brokerage-note-import";
    }

    /**
     * Starts a brokerage note import job and returns the result fragment.
     *
     * @param file uploaded brokerage note file
     * @param accountId account used by the import routine
     * @param globalDateFilter current global date filter
     * @param model view model
     * @return brokerage note import result fragment
     */
    @PostMapping("/jobs")
    public String startImport(
        @RequestParam(name = "file", required = false) MultipartFile file,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        Model model
    ) {
        fillImportOptions(model, globalDateFilter);
        try {
            String jobId = jobCoordinator.startJob(file, accountId);
            fillImportJobModel(model, jobCoordinator.getSnapshot(jobId));
        } catch (IllegalArgumentException ex) {
            fillImportErrorModel(model, ex.getMessage());
        }
        return "investments/brokerage-note-import :: result";
    }

    /**
     * Returns the latest brokerage note import job status fragment.
     *
     * @param jobId job identifier
     * @param globalDateFilter current global date filter
     * @param model view model
     * @return brokerage note import result fragment
     */
    @GetMapping("/jobs/{jobId}")
    public String importStatus(
        @PathVariable String jobId,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        Model model
    ) {
        fillImportOptions(model, globalDateFilter);
        BrokerageNoteImportJobSnapshot snapshot = jobCoordinator.getSnapshot(jobId);
        if (snapshot == null) {
            fillImportErrorModel(model, "Unknown import job");
            return "investments/brokerage-note-import :: result";
        }

        fillImportJobModel(model, snapshot);
        return "investments/brokerage-note-import :: result";
    }

    private void fillImportOptions(Model model, DateFilterState globalDateFilter) {
        DateFilterState effectiveDateFilter = globalDateFilter != null
            ? globalDateFilter
            : DateFilterState.defaultState(Clock.systemDefaultZone());
        model.addAttribute("brokerageNoteAccountOptions", accountService.listVisibleForEntryFilter(effectiveDateFilter.getStartDate()));
    }

    private void fillImportErrorModel(Model model, String rawMessage) {
        model.addAttribute("mode", "failed");
        model.addAttribute("failureDetailMessage", rawMessage);
        model.addAttribute("failureCauseKey", toFailureMessageKey(rawMessage));
        model.addAttribute("progressPercent", 100);
    }

    private void fillImportJobModel(Model model, BrokerageNoteImportJobSnapshot snapshot) {
        if (snapshot == null) {
            fillImportErrorModel(model, "Unknown import job");
            return;
        }

        if (snapshot.status() == BrokerageNoteImportJobStatus.PROCESSING) {
            model.addAttribute("mode", "processing");
            model.addAttribute("jobId", snapshot.jobId());
            model.addAttribute("processedSteps", snapshot.processedSteps());
            model.addAttribute("totalSteps", snapshot.totalSteps());
            model.addAttribute("progressPercent", toProgressPercent(snapshot.processedSteps(), snapshot.totalSteps(), false));
            return;
        }

        if (snapshot.status() == BrokerageNoteImportJobStatus.SUCCESS) {
            model.addAttribute("mode", "success");
            model.addAttribute("summary", snapshot.summary());
            model.addAttribute("progressPercent", 100);
            return;
        }

        fillImportErrorModel(model, snapshot.failureMessage());
        model.addAttribute("progressPercent", toProgressPercent(snapshot.processedSteps(), snapshot.totalSteps(), true));
    }

    private int toProgressPercent(int processedSteps, int totalSteps, boolean forceCompleteOnEmpty) {
        if (totalSteps <= 0) {
            return forceCompleteOnEmpty ? 100 : 0;
        }
        return Math.min(100, (processedSteps * 100) / totalSteps);
    }

    private String toFailureMessageKey(String rawMessage) {
        if (rawMessage != null) {
            String normalizedMessage = rawMessage.toLowerCase();
            if (normalizedMessage.contains("must not be empty")) {
                return "investments.brokerageNoteImport.result.failure.emptyFile";
            }
            if (normalizedMessage.contains("unsupported brokerage note file format")) {
                return "investments.brokerageNoteImport.result.failure.unsupportedFormat";
            }
            if (normalizedMessage.contains("account must be informed")) {
                return "investments.brokerageNoteImport.result.failure.missingAccount";
            }
            if (normalizedMessage.contains("selected account must exist")) {
                return "investments.brokerageNoteImport.result.failure.invalidAccount";
            }
            if (normalizedMessage.contains("selected account must be active")) {
                return "investments.brokerageNoteImport.result.failure.inactiveAccount";
            }
        }
        return "investments.brokerageNoteImport.result.failure.unknown";
    }
}
