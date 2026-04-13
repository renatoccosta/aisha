package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryNotFoundException;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCategorySelection;
import dev.ccosta.aisha.application.entry.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.EntryCsvImportOptions;
import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.application.entry.EntryTransferCounterpartRequest;
import dev.ccosta.aisha.application.entry.EntryTransferCreationRequest;
import dev.ccosta.aisha.application.entry.EntryTransferService;
import dev.ccosta.aisha.application.entry.EntryTransferView;
import dev.ccosta.aisha.application.entry.EntrySettlementAfterAccountDeactivationException;
import dev.ccosta.aisha.application.entry.statement.EntryStatementFormat;
import dev.ccosta.aisha.application.entry.statement.EntryStatementImportService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/entries")
public class EntryController {

    private static final Logger log = LoggerFactory.getLogger(EntryController.class);

    private static final long UNCATEGORIZED_FILTER_VALUE = -1L;
    private static final long NEW_CATEGORY_OPTION_VALUE = -2L;

    private final EntryService entryService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final EntryCategorySuggestionService entryCategorySuggestionService;
    private final EntryTransferService entryTransferService;
    private final EntryStatementImportService entryStatementImportService;
    private final EntryImportJobCoordinator entryImportJobCoordinator;

    public EntryController(
        EntryService entryService,
        AccountService accountService,
        CategoryService categoryService,
        EntryCategorySuggestionService entryCategorySuggestionService,
        EntryTransferService entryTransferService,
        EntryStatementImportService entryStatementImportService,
        EntryImportJobCoordinator entryImportJobCoordinator
    ) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.entryCategorySuggestionService = entryCategorySuggestionService;
        this.entryTransferService = entryTransferService;
        this.entryStatementImportService = entryStatementImportService;
        this.entryImportJobCoordinator = entryImportJobCoordinator;
    }

    @GetMapping
    public String list(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "pendingSuggestions", defaultValue = "false") boolean pendingSuggestions,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
        return "entries/list";
    }

    @GetMapping("/fragments/table")
    public String table(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "pendingSuggestions", defaultValue = "false") boolean pendingSuggestions,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        HttpServletResponse response,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
        setCanonicalEntriesPushUrl(request, response);
        return "entries/list :: listing";
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("mode", "idle");
        return "entries/import";
    }

    @GetMapping("/statement-import")
    public String statementImportPage(Model model) {
        fillStatementImportOptions(model);
        model.addAttribute("mode", "idle");
        return "entries/statement-import";
    }

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

    @PostMapping("/statement-import/jobs")
    public String startStatementImport(
        @RequestParam(name = "file", required = false) MultipartFile file,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "formatId", required = false) String formatId,
        Model model
    ) {
        fillStatementImportOptions(model);
        try {
            String jobId = entryImportJobCoordinator.startStatementJob(file, accountId, formatId);
            fillImportJobModel(model, entryImportJobCoordinator.getSnapshot(jobId));
        } catch (IllegalArgumentException ex) {
            fillImportErrorModel(model, ex.getMessage());
        }
        return "entries/statement-import :: result";
    }

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

    @GetMapping("/statement-import/jobs/{jobId}")
    public String statementImportStatus(@PathVariable String jobId, Model model) {
        fillStatementImportOptions(model);
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

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "returnTo", required = false) String returnTo, Model model) {
        EntryForm form = EntryForm.newWithCurrentDates();
        prepareFormForRendering(form);
        model.addAttribute("form", form);
        fillEntryFormAccountOptions(model, null);
        fillCategoryOptions(model);
        fillCategorySuggestionState(model, form);
        model.addAttribute("mode", "create");
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/form";
    }

    @GetMapping("/transfers/new")
    public String createTransferForm(@RequestParam(name = "returnTo", required = false) String returnTo, Model model) {
        model.addAttribute("form", TransferForm.newWithCurrentDates());
        fillTransferFormAccountOptions(model, null, null);
        model.addAttribute("mode", "create");
        model.addAttribute("transferFormAction", "/entries/transfers");
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-form";
    }

    @PostMapping("/transfers")
    public String createTransfer(
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateDistinctAccounts(form.getOriginAccountId(), form.getDestinationAccountId(), "destinationAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (bindingResult.hasErrors()) {
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("mode", "create");
            model.addAttribute("transferFormAction", "/entries/transfers");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        }

        try {
            entryTransferService.createTransfer(toTransferCreationRequest(form));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("originAccountId", "transferForm.originAccountId.notNull");
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("mode", "create");
            model.addAttribute("transferFormAction", "/entries/transfers");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("mode", "create");
            model.addAttribute("transferFormAction", "/entries/transfers");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("mode", "create");
            model.addAttribute("transferFormAction", "/entries/transfers");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        }
    }

    @GetMapping("/{id}/transfer/edit")
    public String editTransferForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        EntryTransferCreationRequest request = toTransferCreationRequest(entryTransferService.findTransferByAnyEntryId(id));
        TransferForm form = new TransferForm();
        form.setOriginAccountId(request.originAccountId());
        form.setDestinationAccountId(request.destinationAccountId());
        form.setMovementDate(request.movementDate());
        form.setSettlementDate(request.settlementDate());
        form.setDescription(request.description());
        form.setAmount(request.amount());
        form.setNotes(request.notes());
        model.addAttribute("form", form);
        fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
        model.addAttribute("entryId", id);
        model.addAttribute("mode", "edit");
        model.addAttribute("transferFormAction", "/entries/transfers/" + id);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-form";
    }

    @PostMapping("/transfers/{id}")
    public String updateTransfer(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateDistinctAccounts(form.getOriginAccountId(), form.getDestinationAccountId(), "destinationAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (bindingResult.hasErrors()) {
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            model.addAttribute("transferFormAction", "/entries/transfers/" + id);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        }

        try {
            entryTransferService.updateTransferByEntryId(id, toTransferCreationRequest(form));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("originAccountId", "transferForm.originAccountId.notNull");
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            model.addAttribute("transferFormAction", "/entries/transfers/" + id);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            model.addAttribute("transferFormAction", "/entries/transfers/" + id);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            model.addAttribute("transferFormAction", "/entries/transfers/" + id);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-form";
        }
    }

    @GetMapping("/{id}/transfer-counterpart")
    public String createTransferCounterpartForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry entry = entryService.findById(id);
        TransferForm form = new TransferForm();
        form.setMovementDate(entry.getMovementDate());
        form.setSettlementDate(entry.getSettlementDate());
        form.setDescription(entry.getDescription());
        form.setNotes(entry.getNotes());
        form.setAmount(entry.getAmount().abs());
        if (entry.getAmount().signum() < 0) {
            form.setOriginAccountId(entry.getAccount().getId());
        } else {
            form.setDestinationAccountId(entry.getAccount().getId());
        }

        model.addAttribute("form", form);
        fillSourceEntrySummary(model, entry);
        fillEntryFormAccountOptions(model, entry.getAccount().getId());
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-counterpart-form";
    }

    @PostMapping("/{id}/transfer-counterpart")
    public String createTransferCounterpart(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry sourceEntry = entryService.findById(id);
        Long fixedAccountId = sourceEntry.getAccount().getId();
        Long counterpartAccountId = sourceEntry.getAmount().signum() < 0 ? form.getDestinationAccountId() : form.getOriginAccountId();
        validateDistinctAccounts(fixedAccountId, counterpartAccountId, sourceEntry.getAmount().signum() < 0 ? "destinationAccountId" : "originAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (form.getAmount() != null && sourceEntry.getAmount() != null && form.getAmount().compareTo(sourceEntry.getAmount().abs()) != 0) {
            bindingResult.rejectValue("amount", "transferForm.amount.mustMatchSource");
        }
        if (bindingResult.hasErrors()) {
            fillSourceEntrySummary(model, sourceEntry);
            fillEntryFormAccountOptions(model, fixedAccountId);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-counterpart-form";
        }

        try {
            entryTransferService.createCounterpartFromEntry(id, new EntryTransferCounterpartRequest(
                counterpartAccountId,
                form.getMovementDate(),
                form.getSettlementDate(),
                form.getDescription(),
                form.getNotes()
            ));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            fillSourceEntrySummary(model, sourceEntry);
            fillEntryFormAccountOptions(model, fixedAccountId);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-counterpart-form";
        }
    }

    @GetMapping("/{id}/transfer-link")
    public String linkTransferForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry entry = entryService.findById(id);
        TransferLinkForm form = new TransferLinkForm();
        form.setSourceEntryId(id);
        model.addAttribute("form", form);
        fillSourceEntrySummary(model, entry);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-link-form";
    }

    @PostMapping("/{id}/transfer-link")
    public String linkTransfer(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") TransferLinkForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry sourceEntry = entryService.findById(id);
        if (form.getCounterpartEntryId() != null && form.getCounterpartEntryId().equals(id)) {
            bindingResult.rejectValue("counterpartEntryId", "transferLinkForm.counterpartEntryId.distinct");
        }
        if (bindingResult.hasErrors()) {
            fillSourceEntrySummary(model, sourceEntry);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-link-form";
        }

        try {
            entryTransferService.linkExistingEntries(id, form.getCounterpartEntryId());
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferLinkForm.invalid", new Object[] {ex.getMessage()}, null);
            fillSourceEntrySummary(model, sourceEntry);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-link-form";
        }
    }

    @PostMapping("/{id}/transfer-unlink")
    public String unlinkTransfer(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo
    ) {
        entryTransferService.unlinkTransferByEntryId(id);
        return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
    }

    @GetMapping("/fragments/category-suggestion")
    public String categorySuggestion(@ModelAttribute("form") EntryForm form, Model model) {
        fillCategoryOptions(model);
        applySuggestedCategory(form);
        prepareFormForRendering(form);
        fillCategorySuggestionState(model, form);
        return "entries/form :: categorySelectionSection";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") EntryForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateCategoryChoice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("mode", "create");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        }

        try {
            entryService.create(toDomain(form), form.getAccountId(), toCategorySelection(form));
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("mode", "create");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (CategoryNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "entryForm.categoryId.notNull");
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("mode", "create");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("mode", "create");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            validateCategoryChoice(form, bindingResult);
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("mode", "create");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        }
        return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
    }

    @GetMapping("/{id}/edit")
    public String editForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry entry = entryService.findById(id);
        if (entry.isTransfer()) {
            return editTransferForm(id, returnTo, model);
        }
        EntryForm form = fromDomain(entry);
        prepareFormForRendering(form);
        model.addAttribute("form", form);
        fillEntryFormAccountOptions(model, entry.getAccount().getId());
        fillCategoryOptions(model);
        fillCategorySuggestionState(model, form);
        fillTransferEntryState(model, entry);
        model.addAttribute("entryId", id);
        fillEntryRegistrationInfo(model, entry);
        model.addAttribute("mode", "edit");
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") EntryForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateCategoryChoice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
            Entry persistedEntry = entryService.findById(id);
            fillEntryRegistrationInfo(model, persistedEntry);
            fillTransferEntryState(model, persistedEntry);
            model.addAttribute("mode", "edit");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        }

        try {
            entryService.update(id, toDomain(form), form.getAccountId(), toCategorySelection(form));
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
            Entry persistedEntry = entryService.findById(id);
            fillEntryRegistrationInfo(model, persistedEntry);
            fillTransferEntryState(model, persistedEntry);
            model.addAttribute("mode", "edit");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (CategoryNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "entryForm.categoryId.notNull");
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
            Entry persistedEntry = entryService.findById(id);
            fillEntryRegistrationInfo(model, persistedEntry);
            fillTransferEntryState(model, persistedEntry);
            model.addAttribute("mode", "edit");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
            Entry persistedEntry = entryService.findById(id);
            fillEntryRegistrationInfo(model, persistedEntry);
            fillTransferEntryState(model, persistedEntry);
            model.addAttribute("mode", "edit");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            validateCategoryChoice(form, bindingResult);
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
            Entry persistedEntry = entryService.findById(id);
            fillEntryRegistrationInfo(model, persistedEntry);
            fillTransferEntryState(model, persistedEntry);
            model.addAttribute("mode", "edit");
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/form";
        }
        return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "pendingSuggestions", defaultValue = "false") boolean pendingSuggestions,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        entryService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            return "entries/list :: listing";
        }
        return "redirect:/entries";
    }

    @PostMapping("/{id}/confirm-category-suggestion")
    public String confirmCategorySuggestion(
        @PathVariable Long id,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "pendingSuggestions", defaultValue = "false") boolean pendingSuggestions,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        entryService.confirmCategorySuggestion(id);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            return "entries/list :: listing";
        }
        return "redirect:/entries";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "pendingSuggestions", defaultValue = "false") boolean pendingSuggestions,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        entryService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            return "entries/list :: listing";
        }
        return "redirect:/entries";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(EntryNotFoundException.class)
    public String handleNotFound(EntryNotFoundException ex, HttpServletRequest request) {
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
        Long categoryId,
        String description,
        boolean pendingSuggestions,
        Integer page,
        Integer size
    ) {
        DateFilterState effectiveDateFilter = resolveGlobalDateFilter(model, globalDateFilter);
        List<Account> accountOptions = accountService.listVisibleForEntryFilter(effectiveDateFilter.getStartDate());
        Long effectiveAccountId = accountId;
        Long requestedAccountId = accountId;
        if (requestedAccountId != null && accountOptions.stream().noneMatch(account -> account.getId().equals(requestedAccountId))) {
            effectiveAccountId = null;
        }

        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);

        PagedResult<Entry> pageResult = entryService.listMostRecentBySettlementDateBetweenAndFilters(
            effectiveDateFilter.getStartDate(),
            effectiveDateFilter.getEndDate(),
            effectiveAccountId,
            toEffectiveCategoryId(categoryId),
            normalizeDescriptionFilter(description),
            isWithoutCategoryFilter(categoryId),
            pendingSuggestions,
            requestedPage,
            pageSize
        );
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = entryService.listMostRecentBySettlementDateBetweenAndFilters(
                effectiveDateFilter.getStartDate(),
                effectiveDateFilter.getEndDate(),
                effectiveAccountId,
                toEffectiveCategoryId(categoryId),
                normalizeDescriptionFilter(description),
                isWithoutCategoryFilter(categoryId),
                pendingSuggestions,
                effectivePage,
                pageSize
            );
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        model.addAttribute("transferViewsByEntryId", buildTransferViewsByEntryId(pageResult.items()));
        model.addAttribute("entries", pageResult.items());
        model.addAttribute("selectedAccountId", effectiveAccountId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedDescription", normalizeDescriptionFilter(description));
        model.addAttribute("selectedPendingSuggestions", pendingSuggestions);
        model.addAttribute("accountOptions", accountOptions);
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);
        model.addAttribute(
            "returnTo",
            ReturnPathSupport.buildReturnPath(
                "/entries",
                "accountId",
                effectiveAccountId,
                "categoryId",
                categoryId,
                "description",
                normalizeDescriptionFilter(description),
                "pendingSuggestions",
                pendingSuggestions,
                "page",
                pagination.page(),
                "size",
                pagination.pageSize()
            )
        );
        fillCategoryOptions(model);
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
        log.warn("Global date filter was missing in entries listing flow. Falling back to default current-month range.");
        return fallback;
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private void setCanonicalEntriesPushUrl(HttpServletRequest request, HttpServletResponse response) {
        if (!isHtmx(request)) {
            return;
        }

        String queryString = request.getQueryString();
        String canonicalPath = "/entries";
        if (StringUtils.hasText(queryString)) {
            canonicalPath += "?" + queryString;
        }
        response.setHeader("HX-Push-Url", canonicalPath);
    }

    private boolean isWithoutCategoryFilter(Long categoryId) {
        return categoryId != null && categoryId == UNCATEGORIZED_FILTER_VALUE;
    }

    private Long toEffectiveCategoryId(Long categoryId) {
        if (isWithoutCategoryFilter(categoryId)) {
            return null;
        }
        return categoryId;
    }

    private String normalizeDescriptionFilter(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private Entry toDomain(EntryForm form) {
        Entry entry = new Entry();
        entry.setMovementDate(form.getMovementDate());
        entry.setSettlementDate(form.getSettlementDate());
        entry.setDescription(form.getDescription());
        entry.setNotes(form.getNotes());
        entry.setAmount(form.getAmount());
        return entry;
    }

    private EntryForm fromDomain(Entry entry) {
        EntryForm form = new EntryForm();
        form.setAccountId(entry.getAccount().getId());
        form.setMovementDate(entry.getMovementDate());
        form.setSettlementDate(entry.getSettlementDate());
        form.setDescription(entry.getDescription());
        form.setCategoryId(entry.getCategory() == null ? null : entry.getCategory().getId());
        form.setSuggestedCategoryId(entry.getSuggestedCategory() == null ? null : entry.getSuggestedCategory().getId());
        form.setSuggestedCategoryConfidence(entry.getCategorySuggestionConfidence());
        form.setNotes(entry.getNotes());
        form.setAmount(entry.getAmount());
        form.setTransferEntry(entry.isTransfer());
        return form;
    }


    private void fillEntryRegistrationInfo(Model model, Entry entry) {
        model.addAttribute("entrySource", entry.getEntrySource());
        model.addAttribute("entryRegistrationDate", entry.getRegistrationDate());
    }

    private void fillTransferEntryState(Model model, Entry entry) {
        if (entry == null || !entry.isTransfer()) {
            model.addAttribute("transferEntry", false);
            model.addAttribute("transferView", null);
            return;
        }
        model.addAttribute("transferEntry", true);
        model.addAttribute("transferView", entryTransferService.findTransferViewByEntryId(entry.getId()).orElse(null));
    }

    private void fillSourceEntrySummary(Model model, Entry entry) {
        model.addAttribute("sourceEntryId", entry.getId());
        model.addAttribute("sourceEntryAmount", entry.getAmount());
        model.addAttribute("sourceEntryAmountSign", entry.getAmount().signum());
        model.addAttribute("sourceEntryAccountTitle", accountService.findById(entry.getAccount().getId()).getTitle());
    }

    private void fillStatementImportOptions(Model model) {
        model.addAttribute("statementAccountOptions", accountService.listAllActiveOrdered());
        List<EntryStatementFormat> formats = entryStatementImportService.listAvailableFormats();
        model.addAttribute("statementFormats", formats);
    }

    private void fillEntryFormAccountOptions(Model model, Long selectedAccountId) {
        List<Account> accounts = accountService.listAvailableForEntryForm(selectedAccountId);
        model.addAttribute("accountOptions", accounts);
    }

    private void fillTransferFormAccountOptions(Model model, Long originAccountId, Long destinationAccountId) {
        List<Account> accounts = accountService.listAllOrdered()
            .stream()
            .filter(account ->
                account.getDeactivationDate() == null
                    || (originAccountId != null && originAccountId.equals(account.getId()))
                    || (destinationAccountId != null && destinationAccountId.equals(account.getId()))
            )
            .toList();
        model.addAttribute("accountOptions", accounts);
    }

    private void fillCategoryOptions(Model model) {
        List<CategoryOption> categoryOptions = categoryService.listHierarchyOptions();
        model.addAttribute("categoryOptions", categoryOptions);
    }

    private void fillCategorySuggestionState(Model model, EntryForm form) {
        if (form != null && form.isTransferEntry()) {
            model.addAttribute("suggestedCategory", null);
            model.addAttribute("pendingCategorySuggestion", false);
            model.addAttribute("showNewCategoryField", false);
            return;
        }
        if (form == null || form.getSuggestedCategoryId() == null) {
            model.addAttribute("suggestedCategory", null);
            model.addAttribute("pendingCategorySuggestion", false);
            model.addAttribute("showNewCategoryField", shouldShowNewCategoryField(form));
            return;
        }

        Category suggestedCategory = categoryService.findById(form.getSuggestedCategoryId());
        model.addAttribute("suggestedCategory", suggestedCategory);
        model.addAttribute(
            "pendingCategorySuggestion",
            effectiveCategoryId(form) != null && effectiveCategoryId(form).equals(form.getSuggestedCategoryId())
        );
        model.addAttribute("showNewCategoryField", shouldShowNewCategoryField(form));
    }

    private void validateCategoryChoice(EntryForm form, BindingResult bindingResult) {
        if (form != null && form.isTransferEntry()) {
            return;
        }
        boolean hasCategoryId = effectiveCategoryId(form) != null;
        boolean hasNewCategoryTitle = StringUtils.hasText(form.getNewCategoryTitle());
        if (hasCategoryId || hasNewCategoryTitle) {
            return;
        }

        if (isNewCategoryOptionSelected(form)) {
            bindingResult.addError(
                new FieldError(
                    "form",
                    "newCategoryTitle",
                    form.getNewCategoryTitle(),
                    false,
                    new String[] {"entryForm.newCategoryTitle.notBlank"},
                    null,
                    null
                )
            );
            return;
        }

        bindingResult.addError(
            new FieldError("form", "categoryId", form.getCategoryId(), false, new String[] {"entryForm.categoryId.notNull"}, null, null)
        );
    }

    private EntryCategorySelection toCategorySelection(EntryForm form) {
        return new EntryCategorySelection(
            effectiveCategoryId(form),
            form.getNewCategoryTitle(),
            form.getSuggestedCategoryId(),
            form.getSuggestedCategoryConfidence()
        );
    }

    private void applySuggestedCategory(EntryForm form) {
        if (!shouldSuggestCategory(form)) {
            if (effectiveCategoryId(form) == null && !StringUtils.hasText(form.getNewCategoryTitle())) {
                form.setSuggestedCategoryId(null);
                form.setSuggestedCategoryConfidence(null);
            }
            return;
        }

        entryCategorySuggestionService.suggest(
            new EntryCategorySuggestionRequest(form.getAccountId(), form.getDescription(), form.getAmount())
        ).ifPresentOrElse(
            suggestion -> applySuggestionToForm(form, suggestion),
            () -> {
                form.setSuggestedCategoryId(null);
                form.setSuggestedCategoryConfidence(null);
            }
        );
    }

    private boolean shouldSuggestCategory(EntryForm form) {
        if (form != null && form.isTransferEntry()) {
            return false;
        }
        return effectiveCategoryId(form) == null
            && !StringUtils.hasText(form.getNewCategoryTitle())
            && form.getAccountId() != null
            && form.getAmount() != null
            && StringUtils.hasText(form.getDescription());
    }

    private void applySuggestionToForm(EntryForm form, EntryCategorySuggestion suggestion) {
        form.setCategoryId(suggestion.category().getId());
        form.setSuggestedCategoryId(suggestion.category().getId());
        form.setSuggestedCategoryConfidence(suggestion.confidence());
    }

    private void prepareFormForRendering(EntryForm form) {
        if (form == null) {
            return;
        }
        if (form.getCategoryId() == null && StringUtils.hasText(form.getNewCategoryTitle())) {
            form.setCategoryId(NEW_CATEGORY_OPTION_VALUE);
        }
    }

    private boolean shouldShowNewCategoryField(EntryForm form) {
        return form != null
            && !form.isTransferEntry()
            && (isNewCategoryOptionSelected(form) || StringUtils.hasText(form.getNewCategoryTitle()));
    }

    private boolean isNewCategoryOptionSelected(EntryForm form) {
        return form != null && form.getCategoryId() != null && form.getCategoryId() == NEW_CATEGORY_OPTION_VALUE;
    }

    private Long effectiveCategoryId(EntryForm form) {
        if (form != null && form.isTransferEntry()) {
            return null;
        }
        if (isNewCategoryOptionSelected(form)) {
            return null;
        }
        return form == null ? null : form.getCategoryId();
    }

    private EntryTransferCreationRequest toTransferCreationRequest(TransferForm form) {
        return new EntryTransferCreationRequest(
            form.getOriginAccountId(),
            form.getDestinationAccountId(),
            form.getMovementDate(),
            form.getSettlementDate(),
            form.getDescription(),
            form.getAmount(),
            form.getNotes()
        );
    }

    private EntryTransferCreationRequest toTransferCreationRequest(dev.ccosta.aisha.domain.entry.EntryTransfer entryTransfer) {
        Entry originEntry = entryTransfer.getOriginEntry();
        Entry destinationEntry = entryTransfer.getDestinationEntry();
        return new EntryTransferCreationRequest(
            originEntry.getAccount().getId(),
            destinationEntry.getAccount().getId(),
            originEntry.getMovementDate(),
            originEntry.getSettlementDate(),
            originEntry.getDescription(),
            originEntry.getAmount().abs(),
            originEntry.getNotes()
        );
    }

    private void validateDistinctAccounts(Long firstAccountId, Long secondAccountId, String fieldName, BindingResult bindingResult) {
        if (firstAccountId != null && firstAccountId.equals(secondAccountId)) {
            bindingResult.rejectValue(fieldName, "transferForm.accounts.mustBeDifferent");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName, BindingResult bindingResult) {
        if (amount != null && amount.signum() <= 0) {
            bindingResult.rejectValue(fieldName, "transferForm.amount.positive");
        }
    }

    private Map<Long, EntryTransferView> buildTransferViewsByEntryId(List<Entry> entries) {
        Map<Long, EntryTransferView> transferViewsByEntryId = new LinkedHashMap<>();
        for (Entry entry : entries) {
            entryTransferService.findTransferViewByEntryId(entry.getId())
                .ifPresent(transferView -> transferViewsByEntryId.put(entry.getId(), transferView));
        }
        return transferViewsByEntryId;
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
