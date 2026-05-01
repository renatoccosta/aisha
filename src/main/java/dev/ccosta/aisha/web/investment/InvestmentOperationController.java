package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.investment.AssetNotFoundException;
import dev.ccosta.aisha.application.investment.AssetService;
import dev.ccosta.aisha.application.investment.BrokerageNoteNotFoundException;
import dev.ccosta.aisha.application.investment.InvestmentOperationEntryLinkRequest;
import dev.ccosta.aisha.application.investment.InvestmentOperationNotFoundException;
import dev.ccosta.aisha.application.investment.InvestmentOperationService;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.investment.InvestmentOperationSourceType;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handles server-rendered CRUD screens for investment operations.
 */
@Controller
@RequestMapping("/investments/operations")
public class InvestmentOperationController {

    private static final Logger log = LoggerFactory.getLogger(InvestmentOperationController.class);

    private static final int FORM_OPTION_LIMIT = 100;
    private static final int ENTRY_CANDIDATE_DAYS_BEFORE = 30;
    private static final int ENTRY_CANDIDATE_DAYS_AFTER = 30;
    private static final Long NEW_ASSET_OPTION_ID = -1L;

    private final InvestmentOperationService operationService;
    private final AssetService assetService;
    private final AccountService accountService;
    private final EntryService entryService;

    public InvestmentOperationController(
        InvestmentOperationService operationService,
        AssetService assetService,
        AccountService accountService,
        EntryService entryService
    ) {
        this.operationService = operationService;
        this.assetService = assetService;
        this.accountService = accountService;
        this.entryService = entryService;
    }

    /**
     * Lists investment operations using server-side pagination.
     *
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return operation list template
     */
    @GetMapping
    public String list(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "asset", required = false) String asset,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "operationType", required = false) InvestmentOperationType operationType,
        @RequestParam(name = "brokerageNoteId", required = false) Long brokerageNoteId,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, asset, accountId, operationType, brokerageNoteId, page, size);
        return "investments/operations/list";
    }

    /**
     * Returns only the operation table fragment for HTMX refreshes.
     *
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return operation table fragment
     */
    @GetMapping("/fragments/table")
    public String table(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "asset", required = false) String asset,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "operationType", required = false) InvestmentOperationType operationType,
        @RequestParam(name = "brokerageNoteId", required = false) Long brokerageNoteId,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, asset, accountId, operationType, brokerageNoteId, page, size);
        return "investments/operations/list :: table";
    }

    /**
     * Displays all fields for one investment operation.
     *
     * @param id operation identifier
     * @param returnTo optional safe return path
     * @param model view model
     * @return operation details template
     */
    @GetMapping("/{id}")
    public String details(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        InvestmentOperation operation = operationService.findById(id);
        model.addAttribute("operation", operation);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/investments/operations"));
        return "investments/operations/details";
    }

    /**
     * Opens the creation form.
     *
     * @param returnTo optional safe return path
     * @param model view model
     * @return operation form template
     */
    @GetMapping("/new")
    public String createForm(@RequestParam(name = "returnTo", required = false) String returnTo, Model model) {
        InvestmentOperationForm form = new InvestmentOperationForm();
        fillFormModel(model, form, "create", null, returnTo, List.of());
        return "investments/operations/form";
    }

    /**
     * Refreshes financial entry candidates after asset or date changes in the operation form.
     *
     * @param form current operation form values
     * @param model view model
     * @return entry candidate form fragment
     */
    @GetMapping("/fragments/entry-candidates")
    public String entryCandidatesFragment(@ModelAttribute("form") InvestmentOperationForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("entryCandidates", entryCandidates(form));
        return "investments/operations/form :: entryLinkSelection";
    }

    /**
     * Creates an investment operation and optional financial entry links.
     *
     * @param form submitted form
     * @param bindingResult validation result
     * @param returnTo optional safe return path
     * @param model view model
     * @return redirect or form template with errors
     */
    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") InvestmentOperationForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateAssetSelection(form, bindingResult);
        if (bindingResult.hasErrors()) {
            fillFormModel(model, form, "create", null, returnTo, entryCandidates(form));
            return "investments/operations/form";
        }

        try {
            Long assetId = resolveAssetId(form);
            operationService.create(toDomain(form), assetId, form.getAccountId(), toLinkRequests(form.getLinkedEntryIds()));
        } catch (AssetNotFoundException ex) {
            bindingResult.rejectValue("assetId", "investmentOperationForm.assetId.notNull");
            fillFormModel(model, form, "create", null, returnTo, entryCandidates(form));
            return "investments/operations/form";
        } catch (EntryNotFoundException ex) {
            bindingResult.rejectValue("linkedEntryIds", "investmentOperationForm.linkedEntryIds.invalid");
            fillFormModel(model, form, "create", null, returnTo, entryCandidates(form));
            return "investments/operations/form";
        }
        return ReturnPathSupport.resolveRedirect(returnTo, "/investments/operations");
    }

    /**
     * Opens the edit form for an existing operation.
     *
     * @param id operation identifier
     * @param returnTo optional safe return path
     * @param model view model
     * @return operation form template
     */
    @GetMapping("/{id}/edit")
    public String editForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        InvestmentOperation operation = operationService.findById(id);
        InvestmentOperationForm form = fromDomain(operation);
        List<Entry> selectedEntries = operationService.listLinksByOperationId(id)
            .stream()
            .map(InvestmentOperationEntryLink::getEntry)
            .toList();
        fillFormModel(model, form, "edit", id, returnTo, mergeEntryCandidates(form, selectedEntries));
        return "investments/operations/form";
    }

    /**
     * Updates an investment operation and replaces its financial entry links.
     *
     * @param id operation identifier
     * @param form submitted form
     * @param bindingResult validation result
     * @param returnTo optional safe return path
     * @param model view model
     * @return redirect or form template with errors
     */
    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") InvestmentOperationForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateAssetSelection(form, bindingResult);
        if (bindingResult.hasErrors()) {
            fillFormModel(model, form, "edit", id, returnTo, entryCandidates(form));
            return "investments/operations/form";
        }

        try {
            Long assetId = resolveAssetId(form);
            operationService.update(
                id,
                toDomain(form),
                assetId,
                form.getAccountId(),
                form.getBrokerageNoteId(),
                toLinkRequests(form.getLinkedEntryIds())
            );
        } catch (AssetNotFoundException ex) {
            bindingResult.rejectValue("assetId", "investmentOperationForm.assetId.notNull");
            fillFormModel(model, form, "edit", id, returnTo, entryCandidates(form));
            return "investments/operations/form";
        } catch (EntryNotFoundException ex) {
            bindingResult.rejectValue("linkedEntryIds", "investmentOperationForm.linkedEntryIds.invalid");
            fillFormModel(model, form, "edit", id, returnTo, entryCandidates(form));
            return "investments/operations/form";
        }
        return ReturnPathSupport.resolveRedirect(returnTo, "/investments/operations");
    }

    /**
     * Deletes one operation and refreshes the listing when called through HTMX.
     *
     * @param id operation identifier
     * @param page current page
     * @param size current page size
     * @param request HTTP request
     * @param model view model
     * @return redirect or table fragment
     */
    @PostMapping("/{id}/delete")
    public String delete(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @PathVariable Long id,
        @RequestParam(name = "asset", required = false) String asset,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "operationType", required = false) InvestmentOperationType operationType,
        @RequestParam(name = "brokerageNoteId", required = false) Long brokerageNoteId,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        operationService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, asset, accountId, operationType, brokerageNoteId, page, size);
            return "investments/operations/list :: table";
        }
        return "redirect:/investments/operations";
    }

    /**
     * Deletes selected operations and refreshes the listing when called through HTMX.
     *
     * @param ids selected operation identifiers
     * @param page current page
     * @param size current page size
     * @param request HTTP request
     * @param model view model
     * @return redirect or table fragment
     */
    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @RequestParam(name = "asset", required = false) String asset,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "operationType", required = false) InvestmentOperationType operationType,
        @RequestParam(name = "brokerageNoteId", required = false) Long brokerageNoteId,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        operationService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, globalDateFilter, asset, accountId, operationType, brokerageNoteId, page, size);
            return "investments/operations/list :: table";
        }
        return "redirect:/investments/operations";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({InvestmentOperationNotFoundException.class, AssetNotFoundException.class, BrokerageNoteNotFoundException.class})
    public String handleNotFound(RuntimeException ex, HttpServletRequest request) {
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
        String asset,
        Long accountId,
        InvestmentOperationType operationType,
        Long brokerageNoteId,
        Integer page,
        Integer size
    ) {
        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);
        String selectedAsset = normalizeFilter(asset);
        PagedResult<InvestmentOperation> pageResult = operationService.listPageOrdered(
            globalDateFilter.getStartDate(),
            globalDateFilter.getEndDate(),
            selectedAsset,
            accountId,
            operationType,
            brokerageNoteId,
            requestedPage,
            pageSize
        );
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = operationService.listPageOrdered(
                globalDateFilter.getStartDate(),
                globalDateFilter.getEndDate(),
                selectedAsset,
                accountId,
                operationType,
                brokerageNoteId,
                effectivePage,
                pageSize
            );
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        model.addAttribute("operations", pageResult.items());
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);
        model.addAttribute("accountOptions", accountService.listAvailableForEntryForm(accountId));
        model.addAttribute("operationTypes", InvestmentOperationType.values());
        model.addAttribute("selectedAsset", selectedAsset);
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedOperationType", operationType);
        model.addAttribute("selectedBrokerageNoteId", brokerageNoteId);
        model.addAttribute(
            "returnTo",
            ReturnPathSupport.buildReturnPath(
                "/investments/operations",
                "asset", selectedAsset,
                "accountId", accountId,
                "operationType", operationType,
                "brokerageNoteId", brokerageNoteId,
                "page", pagination.page(),
                "size", pagination.pageSize()
            )
        );
    }

    private void fillFormModel(
        Model model,
        InvestmentOperationForm form,
        String mode,
        Long operationId,
        String returnTo,
        List<Entry> entryCandidates
    ) {
        model.addAttribute("form", form);
        model.addAttribute("mode", mode);
        model.addAttribute("operationId", operationId);
        model.addAttribute("assets", assetService.listPageOrdered(0, FORM_OPTION_LIMIT).items());
        model.addAttribute("newAssetOptionId", NEW_ASSET_OPTION_ID);
        model.addAttribute("accounts", accountService.listAvailableForEntryForm(form.getAccountId()));
        model.addAttribute("operationTypes", InvestmentOperationType.values());
        model.addAttribute("sourceTypes", sourceTypesFor(form));
        model.addAttribute("brokerageNote", findBrokerageNoteForForm(form));
        model.addAttribute("entryCandidates", entryCandidates);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/investments/operations"));
    }

    private List<Entry> entryCandidates(InvestmentOperationForm form) {
        if (form.getAccountId() == null) {
            return List.of();
        }

        LocalDate referenceDate = form.getSettlementDate() != null ? form.getSettlementDate() : form.getTradeDate();
        if (referenceDate == null) {
            return List.of();
        }

        return entryService.listMostRecentBySettlementDateBetweenAndFilters(
            referenceDate.minusDays(ENTRY_CANDIDATE_DAYS_BEFORE),
            referenceDate.plusDays(ENTRY_CANDIDATE_DAYS_AFTER),
            form.getAccountId(),
            null,
            null,
            false,
            false,
            0,
            FORM_OPTION_LIMIT
        ).items();
    }

    private List<Entry> mergeEntryCandidates(InvestmentOperationForm form, List<Entry> selectedEntries) {
        LinkedHashSet<Long> seenIds = new LinkedHashSet<>();
        java.util.ArrayList<Entry> merged = new java.util.ArrayList<>();
        for (Entry entry : selectedEntries) {
            if (entry.getId() != null && seenIds.add(entry.getId())) {
                merged.add(entry);
            }
        }
        for (Entry entry : entryCandidates(form)) {
            if (entry.getId() != null && seenIds.add(entry.getId())) {
                merged.add(entry);
            }
        }
        return merged;
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private InvestmentOperation toDomain(InvestmentOperationForm form) {
        InvestmentOperation operation = new InvestmentOperation();
        operation.setOperationType(form.getOperationType());
        operation.setTradeDate(form.getTradeDate());
        operation.setSettlementDate(form.getSettlementDate());
        operation.setQuantity(form.getQuantity());
        operation.setUnitPrice(form.getUnitPrice());
        operation.setGrossAmount(form.getGrossAmount());
        operation.setNetAmount(form.getNetAmount());
        operation.setFees(form.getFees());
        operation.setTaxes(form.getTaxes());
        operation.setCurrency(form.getCurrency());
        operation.setNotes(form.getNotes());
        operation.setSourceType(form.getSourceType());
        return operation;
    }

    private void validateAssetSelection(InvestmentOperationForm form, BindingResult bindingResult) {
        if (!isNewAssetSelected(form)) {
            return;
        }
        if (form.getNewAssetName() == null || form.getNewAssetName().isBlank()) {
            bindingResult.rejectValue("newAssetName", "investmentOperationForm.newAssetName.notBlank");
        }
    }

    private Long resolveAssetId(InvestmentOperationForm form) {
        if (!isNewAssetSelected(form)) {
            return form.getAssetId();
        }

        Asset asset = new Asset();
        asset.setName(form.getNewAssetName().trim());
        asset.setType(AssetType.OTHER);
        return assetService.create(asset).getId();
    }

    private boolean isNewAssetSelected(InvestmentOperationForm form) {
        return NEW_ASSET_OPTION_ID.equals(form.getAssetId());
    }

    private InvestmentOperationForm fromDomain(InvestmentOperation operation) {
        InvestmentOperationForm form = new InvestmentOperationForm();
        form.setAssetId(operation.getAsset().getId());
        form.setAccountId(operation.getAccount().getId());
        form.setOperationType(operation.getOperationType());
        form.setTradeDate(operation.getTradeDate());
        form.setSettlementDate(operation.getSettlementDate());
        form.setQuantity(operation.getQuantity());
        form.setUnitPrice(operation.getUnitPrice());
        form.setGrossAmount(operation.getGrossAmount());
        form.setNetAmount(operation.getNetAmount());
        form.setFees(operation.getFees());
        form.setTaxes(operation.getTaxes());
        form.setCurrency(operation.getCurrency());
        form.setNotes(operation.getNotes());
        form.setSourceType(operation.getSourceType());
        if (operation.getBrokerageNote() != null) {
            form.setBrokerageNoteId(operation.getBrokerageNote().getId());
        }
        form.setLinkedEntryIds(operationService.listLinksByOperationId(operation.getId()).stream()
            .map(InvestmentOperationEntryLink::getEntry)
            .map(Entry::getId)
            .toList());
        return form;
    }

    private List<InvestmentOperationSourceType> sourceTypesFor(InvestmentOperationForm form) {
        if (form.getSourceType() == InvestmentOperationSourceType.BROKER_NOTE) {
            return List.of(InvestmentOperationSourceType.BROKER_NOTE);
        }
        return List.of(InvestmentOperationSourceType.MANUAL);
    }

    private BrokerageNote findBrokerageNoteForForm(InvestmentOperationForm form) {
        Long brokerageNoteId = form.getBrokerageNoteId();
        if (brokerageNoteId == null) {
            return null;
        }
        return operationService.findBrokerageNoteById(brokerageNoteId);
    }

    private Collection<InvestmentOperationEntryLinkRequest> toLinkRequests(Collection<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return List.of();
        }
        return entryIds.stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .map(entryId -> new InvestmentOperationEntryLinkRequest(entryId, null))
            .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
