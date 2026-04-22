package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;

/**
 * Assembles listing model attributes for the entries table and HTMX listing fragments.
 */
@Component
class EntryListingModelAssembler {

    private static final Logger log = LoggerFactory.getLogger(EntryListingModelAssembler.class);

    private static final long UNCATEGORIZED_FILTER_VALUE = -1L;

    private final EntryService entryService;
    private final AccountService accountService;
    private final CategoryService categoryService;
    private final EntryTransferService entryTransferService;

    EntryListingModelAssembler(
        EntryService entryService,
        AccountService accountService,
        CategoryService categoryService,
        EntryTransferService entryTransferService
    ) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.categoryService = categoryService;
        this.entryTransferService = entryTransferService;
    }

    /**
     * Populates all attributes required by the full entries list and its table fragment.
     *
     * @param model the view model to populate
     * @param globalDateFilter current global date filter, or null to resolve a fallback
     * @param accountId selected account filter
     * @param categoryId selected category filter, including the special uncategorized value
     * @param description selected description filter
     * @param pendingSuggestions whether only pending category suggestions should be listed
     * @param page requested page index
     * @param size requested page size
     */
    void fillListing(
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

    private void fillCategoryOptions(Model model) {
        List<CategoryOption> categoryOptions = categoryService.listHierarchyOptions();
        model.addAttribute("categoryOptions", categoryOptions);
    }

    private Map<Long, EntryTransferView> buildTransferViewsByEntryId(List<Entry> entries) {
        Map<Long, EntryTransferView> transferViewsByEntryId = new LinkedHashMap<>();
        for (Entry entry : entries) {
            entryTransferService.findTransferViewByEntryId(entry.getId())
                .ifPresent(transferView -> transferViewsByEntryId.put(entry.getId(), transferView));
        }
        return transferViewsByEntryId;
    }
}
