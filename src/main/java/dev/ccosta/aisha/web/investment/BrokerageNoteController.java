package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.BrokerageNoteNotFoundException;
import dev.ccosta.aisha.application.investment.BrokerageNoteService;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handles server-rendered brokerage note listing and details screens.
 */
@Controller
@RequestMapping("/investments/brokerage-notes")
public class BrokerageNoteController {

    private static final Logger log = LoggerFactory.getLogger(BrokerageNoteController.class);

    private final BrokerageNoteService brokerageNoteService;
    private final AccountService accountService;

    public BrokerageNoteController(BrokerageNoteService brokerageNoteService, AccountService accountService) {
        this.brokerageNoteService = brokerageNoteService;
        this.accountService = accountService;
    }

    /**
     * Lists brokerage notes using server-side pagination.
     *
     * @param globalDateFilter settlement date filter from the session
     * @param accountId optional account filter
     * @param tradeStartDate optional inclusive trade start date
     * @param tradeEndDate optional inclusive trade end date
     * @param noteNumber optional broker note number prefix
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return brokerage note list template
     */
    @GetMapping
    public String list(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "tradeStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeStartDate,
        @RequestParam(name = "tradeEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeEndDate,
        @RequestParam(name = "noteNumber", required = false) String noteNumber,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, tradeStartDate, tradeEndDate, noteNumber, page, size);
        return "investments/brokerage-notes/list";
    }

    /**
     * Returns only the brokerage note table fragment for HTMX refreshes.
     *
     * @param globalDateFilter settlement date filter from the session
     * @param accountId optional account filter
     * @param tradeStartDate optional inclusive trade start date
     * @param tradeEndDate optional inclusive trade end date
     * @param noteNumber optional broker note number prefix
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return brokerage note table fragment
     */
    @GetMapping("/fragments/table")
    public String table(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "tradeStartDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeStartDate,
        @RequestParam(name = "tradeEndDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeEndDate,
        @RequestParam(name = "noteNumber", required = false) String noteNumber,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, tradeStartDate, tradeEndDate, noteNumber, page, size);
        return "investments/brokerage-notes/list :: table";
    }

    /**
     * Displays all non-sensitive fields for one brokerage note.
     *
     * @param id brokerage note identifier
     * @param returnTo optional safe return path
     * @param model view model
     * @return brokerage note details template
     */
    @GetMapping("/{id}")
    public String details(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        BrokerageNote brokerageNote = brokerageNoteService.findById(id);
        model.addAttribute("brokerageNote", brokerageNote);
        model.addAttribute("netEntryId", brokerageNote.getNetEntry().getId());
        model.addAttribute("brokerageNoteDetailsReturnPath", "/investments/brokerage-notes/" + brokerageNote.getId());
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/investments/brokerage-notes"));
        return "investments/brokerage-notes/details";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BrokerageNoteNotFoundException.class)
    public String handleNotFound(BrokerageNoteNotFoundException ex, HttpServletRequest request) {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY));
        log.warn(
            "Resource not found returned to user. correlationId={}, type={}, method={}, path={}, message={}",
            correlationId,
            ex.getClass().getSimpleName(),
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );
        return "errors/404";
    }

    private void fillListing(
        Model model,
        DateFilterState globalDateFilter,
        Long accountId,
        LocalDate tradeStartDate,
        LocalDate tradeEndDate,
        String noteNumber,
        Integer page,
        Integer size
    ) {
        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);
        String selectedNoteNumber = normalizeFilter(noteNumber);
        PagedResult<BrokerageNote> pageResult = brokerageNoteService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            accountId,
            tradeStartDate,
            tradeEndDate,
            selectedNoteNumber,
            requestedPage,
            pageSize
        );
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = brokerageNoteService.listPageOrdered(
                globalDateFilter.getStartDate(),
                globalDateFilter.getEndDate(),
                accountId,
                tradeStartDate,
                tradeEndDate,
                selectedNoteNumber,
                effectivePage,
                pageSize
            );
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        model.addAttribute("brokerageNotes", pageResult.items());
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);
        model.addAttribute("accountOptions", accountService.listVisibleForEntryFilter(globalDateFilter.getStartDate()));
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedTradeStartDate", tradeStartDate);
        model.addAttribute("selectedTradeEndDate", tradeEndDate);
        model.addAttribute("selectedNoteNumber", selectedNoteNumber);
        model.addAttribute(
            "returnTo",
            ReturnPathSupport.buildReturnPath(
                "/investments/brokerage-notes",
                "accountId", accountId,
                "tradeStartDate", tradeStartDate,
                "tradeEndDate", tradeEndDate,
                "noteNumber", selectedNoteNumber,
                "page", pagination.page(),
                "size", pagination.pageSize()
            )
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
