package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryNotFoundException;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryCategorySelection;
import dev.ccosta.aisha.application.entry.EntryInUseException;
import dev.ccosta.aisha.application.entry.EntryRelationSummary;
import dev.ccosta.aisha.application.entry.EntryRelationSummaryService;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestion;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionRequest;
import dev.ccosta.aisha.application.entry.categorization.EntryCategorySuggestionService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.application.entry.EntrySettlementAfterAccountDeactivationException;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
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

@Controller
@RequestMapping("/entries")
public class EntryController {

    private static final Logger log = LoggerFactory.getLogger(EntryController.class);

    private static final long NEW_CATEGORY_OPTION_VALUE = -2L;

    private final EntryService entryService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final EntryCategorySuggestionService entryCategorySuggestionService;
    private final EntryTransferService entryTransferService;
    private final EntryRelationSummaryService entryRelationSummaryService;
    private final EntryListingModelAssembler listingModelAssembler;
    private final MessageSource messageSource;

    public EntryController(
        EntryService entryService,
        AccountService accountService,
        CategoryService categoryService,
        EntryCategorySuggestionService entryCategorySuggestionService,
        EntryTransferService entryTransferService,
        EntryRelationSummaryService entryRelationSummaryService,
        EntryListingModelAssembler listingModelAssembler,
        MessageSource messageSource
    ) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.entryCategorySuggestionService = entryCategorySuggestionService;
        this.entryTransferService = entryTransferService;
        this.entryRelationSummaryService = entryRelationSummaryService;
        this.listingModelAssembler = listingModelAssembler;
        this.messageSource = messageSource;
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
        listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
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
        listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
        setCanonicalEntriesPushUrl(request, response);
        return "entries/list :: listing";
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

    @GetMapping("/fragments/category-suggestion")
    public String categorySuggestion(@ModelAttribute("form") EntryForm form, Model model) {
        fillCategoryOptions(model);
        applySuggestedCategory(form);
        prepareFormForRendering(form);
        fillCategorySuggestionState(model, form);
        return "entries/form :: categorySelectionSection";
    }

    @GetMapping("/{id}")
    public String details(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry entry = entryService.findById(id);
        EntryRelationSummary relationSummary = entryRelationSummaryService.summarize(entry);
        model.addAttribute("entry", entry);
        fillEntryDetailsState(model, entry, relationSummary);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/details";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") EntryForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        form.setTransferEntry(false);
        form.setEquityEntry(false);
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
            return "redirect:" + ReturnPathSupport.buildReturnPath(
                "/entries/" + id + "/transfer/edit",
                "returnTo",
                ReturnPathSupport.resolveReturnPath(returnTo, "/entries")
            );
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
        Entry persistedEntry = entryService.findById(id);
        form.setTransferEntry(persistedEntry.isTransfer());
        form.setEquityEntry(persistedEntry.isEquityEffect());
        validateCategoryChoice(form, bindingResult);
        if (bindingResult.hasErrors()) {
            prepareFormForRendering(form);
            fillEntryFormAccountOptions(model, form.getAccountId());
            fillCategoryOptions(model);
            fillCategorySuggestionState(model, form);
            model.addAttribute("entryId", id);
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
        try {
            entryService.deleteById(id);
        } catch (EntryInUseException ex) {
            if (isHtmx(request)) {
                return handleHtmxInUse(ex, request, model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            }
            throw ex;
        }
        if (isHtmx(request)) {
            listingModelAssembler.fillListingAfterMutation(
                model,
                globalDateFilter,
                accountId,
                categoryId,
                description,
                pendingSuggestions,
                page,
                size
            );
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
            listingModelAssembler.fillListingAfterMutation(
                model,
                globalDateFilter,
                accountId,
                categoryId,
                description,
                pendingSuggestions,
                page,
                size
            );
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
        try {
            entryService.bulkDelete(ids);
        } catch (EntryInUseException ex) {
            if (isHtmx(request)) {
                return handleHtmxInUse(ex, request, model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            }
            throw ex;
        }
        if (isHtmx(request)) {
            listingModelAssembler.fillListingAfterMutation(
                model,
                globalDateFilter,
                accountId,
                categoryId,
                description,
                pendingSuggestions,
                page,
                size
            );
            return "entries/list :: listing";
        }
        return "redirect:/entries";
    }

    @PostMapping("/bulk-confirm-category-suggestions")
    public String bulkConfirmCategorySuggestions(
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
        entryService.bulkConfirmCategorySuggestions(ids);
        if (isHtmx(request)) {
            listingModelAssembler.fillListingAfterMutation(
                model,
                globalDateFilter,
                accountId,
                categoryId,
                description,
                pendingSuggestions,
                page,
                size
            );
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(EntryInUseException.class)
    public String handleInUse(
        EntryInUseException ex,
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        HttpServletRequest request,
        Model model
    ) {
        logBusinessError(ex, request);
        listingModelAssembler.fillListing(model, globalDateFilter, null, null, null, false, null, null);
        addToast(model, "entries.list.toast.delete.inUse", "error");
        if (isHtmx(request)) {
            return "entries/list :: listing";
        }
        return "entries/list";
    }

    private String handleHtmxInUse(
        EntryInUseException ex,
        HttpServletRequest request,
        Model model,
        DateFilterState globalDateFilter,
        Long accountId,
        Long categoryId,
        String description,
        boolean pendingSuggestions,
        Integer page,
        Integer size
    ) {
        logBusinessError(ex, request);
        listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
        addToast(model, "entries.list.toast.delete.inUse", "error");
        return "entries/list :: listing";
    }

    private void logBusinessError(RuntimeException ex, HttpServletRequest request) {
        String correlationId = String.valueOf(request.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY));
        log.warn(
            "Business error returned to user. correlationId={}, type={}, method={}, path={}, message={}",
            correlationId,
            ex.getClass().getSimpleName(),
            request.getMethod(),
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );
    }

    private void addToast(Model model, String messageKey, String level) {
        model.addAttribute(
            "toastMessage",
            messageSource.getMessage(messageKey, null, org.springframework.context.i18n.LocaleContextHolder.getLocale())
        );
        model.addAttribute("toastLevel", level);
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
        form.setEquityEntry(entry.isEquityEffect());
        return form;
    }


    private void fillEntryRegistrationInfo(Model model, Entry entry) {
        model.addAttribute("entrySource", entry.getEntrySource());
        model.addAttribute("entryRegistrationDate", entry.getRegistrationDate());
    }

    private void fillEntryDetailsState(Model model, Entry entry, EntryRelationSummary relationSummary) {
        Locale locale = LocaleContextHolder.getLocale();
        String detailReturnPath = "/entries/" + entry.getId();

        model.addAttribute("entryDetailsReturnPath", detailReturnPath);
        model.addAttribute(
            "entryDetailsHeading",
            messageSource.getMessage("entries.details.heading", new Object[] {entry.getId()}, locale)
        );
        EntryActionState actionState = EntryActionState.from(entry, relationSummary);
        model.addAttribute("entryActionState", actionState);
        model.addAttribute("entryTransfer", entry.isTransfer());
        model.addAttribute("entryRegular", !entry.isTransfer());
        model.addAttribute("entryRelationSummary", relationSummary);
        model.addAttribute("transferView", relationSummary.transferView());
        model.addAttribute("transferEntry", relationSummary.hasTransfer());
        model.addAttribute("entryAccountTitle", entry.getAccount().getTitle());
        model.addAttribute("entryCategoryTitle", displayText(entry.getCategory() == null ? null : entry.getCategory().getTitle()));
        model.addAttribute(
            "entrySuggestedCategoryTitle",
            displayText(entry.getSuggestedCategory() == null ? null : entry.getSuggestedCategory().getTitle())
        );
        model.addAttribute("entryTypeLabel", enumMessage("entry.type.", entry.getEntryType().name(), locale));
        model.addAttribute("entryEffectLabel", enumMessage("entry.effect.", entry.getEntryEffect().name(), locale));
        model.addAttribute("entrySourceLabel", entry.getEntrySource() == null ? "-" : enumMessage("entry.source.", entry.getEntrySource().name(), locale));
        model.addAttribute(
            "entryRegistrationDateLabel",
            entry.getRegistrationDate() == null ? "-" : entry.getRegistrationDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        );
        model.addAttribute(
            "entryCategorySuggestionStatusLabel",
            enumMessage("entry.categorySuggestionStatus.", entry.getCategorySuggestionStatus().name(), locale)
        );
        model.addAttribute("entryCategorySuggestionConfidenceLabel", confidenceLabel(entry.getCategorySuggestionConfidence(), locale));
        model.addAttribute("entryExternalIdLabel", displayText(entry.getExternalId()));
        model.addAttribute("entryNotesLabel", displayText(entry.getNotes()));

        model.addAttribute(
            "transferCounterpartEntryId",
            relationSummary.transferView() == null ? null : relationSummary.transferView().counterpartEntryId()
        );
        model.addAttribute("linkedInvestmentOperationId", relationSummary.investmentOperationId());
        model.addAttribute("linkedBrokerageNoteId", relationSummary.brokerageNoteId());
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

    private String displayText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String enumMessage(String prefix, String enumName, Locale locale) {
        return messageSource.getMessage(prefix + enumName.toLowerCase(Locale.ROOT), null, locale);
    }

    private String confidenceLabel(Double confidence, Locale locale) {
        if (confidence == null) {
            return "-";
        }
        NumberFormat percentFormat = NumberFormat.getPercentInstance(locale);
        percentFormat.setMaximumFractionDigits(0);
        percentFormat.setMinimumFractionDigits(0);
        return percentFormat.format(confidence);
    }

    private void fillEntryFormAccountOptions(Model model, Long selectedAccountId) {
        List<Account> accounts = accountService.listAvailableForEntryForm(selectedAccountId);
        model.addAttribute("accountOptions", accounts);
    }

    private void fillCategoryOptions(Model model) {
        List<CategoryOption> categoryOptions = categoryService.listHierarchyOptions();
        model.addAttribute("categoryOptions", categoryOptions);
    }

    private void fillCategorySuggestionState(Model model, EntryForm form) {
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
        if (isNewCategoryOptionSelected(form)) {
            return null;
        }
        return form == null ? null : form.getCategoryId();
    }

}
