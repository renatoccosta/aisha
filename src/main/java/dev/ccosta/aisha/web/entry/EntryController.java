package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryNotFoundException;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCsvImportOptions;
import dev.ccosta.aisha.application.entry.EntryImportFailureCause;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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

    private final EntryService entryService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final EntryImportJobCoordinator entryImportJobCoordinator;

    public EntryController(
        EntryService entryService,
        AccountService accountService,
        CategoryService categoryService,
        EntryImportJobCoordinator entryImportJobCoordinator
    ) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.entryImportJobCoordinator = entryImportJobCoordinator;
    }

    @GetMapping
    public String list(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, categoryId);
        return "entries/list";
    }

    @GetMapping("/fragments/table")
    public String table(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        Model model
    ) {
        fillListing(model, globalDateFilter, accountId, categoryId);
        return "entries/list :: table";
    }

    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("mode", "idle");
        return "entries/import";
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
            EntryCsvImportOptions options = buildImportOptions(
                headerOption,
                separatorOption,
                separatorOther,
                dateFormatOption,
                dateFormatOther,
                amountFormatOption,
                amountFormatOther
            );
            String jobId = entryImportJobCoordinator.startJob(file, options);
            fillImportJobModel(model, entryImportJobCoordinator.getSnapshot(jobId));
        } catch (IllegalArgumentException ex) {
            fillImportErrorModel(model, ex.getMessage());
        }
        return "entries/import :: result";
    }

    @GetMapping("/import/jobs/{jobId}")
    public String importStatus(@PathVariable String jobId, Model model) {
        EntryImportJobSnapshot snapshot = entryImportJobCoordinator.getSnapshot(jobId);
        if (snapshot == null) {
            model.addAttribute("mode", "failed");
            model.addAttribute("failureCauseKey", "entries.import.result.failure.unknown");
            model.addAttribute("failedRow", null);
            return "entries/import :: result";
        }

        fillImportJobModel(model, snapshot);
        return "entries/import :: result";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", EntryForm.newWithCurrentDates());
        fillAccountOptions(model);
        fillCategoryOptions(model);
        model.addAttribute("mode", "create");
        return "entries/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") EntryForm form, BindingResult bindingResult, Model model) {
        validateCategoryChoice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("mode", "create");
            return "entries/form";
        }

        try {
            entryService.create(toDomain(form), form.getAccountId(), form.getCategoryId(), form.getNewCategoryTitle());
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("mode", "create");
            return "entries/form";
        } catch (CategoryNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "entryForm.categoryId.notNull");
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("mode", "create");
            return "entries/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            validateCategoryChoice(form, bindingResult);
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("mode", "create");
            return "entries/form";
        }
        return "redirect:/entries";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Entry entry = entryService.findById(id);
        model.addAttribute("form", fromDomain(entry));
        fillAccountOptions(model);
        fillCategoryOptions(model);
        model.addAttribute("entryId", id);
        model.addAttribute("mode", "edit");
        return "entries/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") EntryForm form,
        BindingResult bindingResult,
        Model model
    ) {
        validateCategoryChoice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            return "entries/form";
        }

        try {
            entryService.update(id, toDomain(form), form.getAccountId(), form.getCategoryId(), form.getNewCategoryTitle());
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            return "entries/form";
        } catch (CategoryNotFoundException ex) {
            bindingResult.rejectValue("categoryId", "entryForm.categoryId.notNull");
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            return "entries/form";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("accountId", "entryForm.accountId.notNull");
            validateCategoryChoice(form, bindingResult);
            fillAccountOptions(model);
            fillCategoryOptions(model);
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            return "entries/form";
        }
        return "redirect:/entries";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        HttpServletRequest request,
        Model model
    ) {
        entryService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, accountId, categoryId);
            return "entries/list :: table";
        }
        return "redirect:/entries";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        HttpServletRequest request,
        Model model
    ) {
        entryService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, accountId, categoryId);
            return "entries/list :: table";
        }
        return "redirect:/entries";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(EntryNotFoundException.class)
    public String handleNotFound() {
        return "errors/404";
    }

    private void fillListing(Model model, DateFilterState globalDateFilter, Long accountId, Long categoryId) {
        model.addAttribute(
            "entries",
            entryService.listTop100MostRecentBySettlementDateBetweenAndFilters(
                globalDateFilter.getStartDate(),
                globalDateFilter.getEndDate(),
                accountId,
                categoryId
            )
        );
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedCategoryId", categoryId);
        fillAccountOptions(model);
        fillCategoryOptions(model);
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
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
        form.setCategoryId(entry.getCategory().getId());
        form.setNotes(entry.getNotes());
        form.setAmount(entry.getAmount());
        return form;
    }

    private void fillAccountOptions(Model model) {
        List<Account> accounts = accountService.listAllOrdered();
        model.addAttribute("accountOptions", accounts);
    }

    private void fillCategoryOptions(Model model) {
        List<CategoryOption> categoryOptions = categoryService.listHierarchyOptions();
        model.addAttribute("categoryOptions", categoryOptions);
    }

    private void validateCategoryChoice(EntryForm form, BindingResult bindingResult) {
        boolean hasCategoryId = form.getCategoryId() != null;
        boolean hasNewCategoryTitle = StringUtils.hasText(form.getNewCategoryTitle());
        if (hasCategoryId || hasNewCategoryTitle) {
            return;
        }

        bindingResult.addError(
            new FieldError("form", "categoryId", form.getCategoryId(), false, new String[] {"entryForm.categoryId.notNull"}, null, null)
        );
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
