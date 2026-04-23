package dev.ccosta.aisha.web.investment;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.investment.AssetInUseException;
import dev.ccosta.aisha.application.investment.AssetNotFoundException;
import dev.ccosta.aisha.application.investment.AssetService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetIndexerType;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.logging.CorrelationIdFilter;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
 * Handles server-rendered CRUD screens for investment assets.
 */
@Controller
@RequestMapping("/investments/assets")
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);

    private final AssetService assetService;
    private final AccountService accountService;

    public AssetController(AssetService assetService, AccountService accountService) {
        this.assetService = assetService;
        this.accountService = accountService;
    }

    /**
     * Lists investment assets using server-side pagination.
     *
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return asset list template
     */
    @GetMapping
    public String list(
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "type", required = false) AssetType type,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, accountId, type, description, page, size);
        return "investments/assets/list";
    }

    /**
     * Returns only the asset table fragment for HTMX refreshes.
     *
     * @param page requested zero-based page
     * @param size requested page size
     * @param model view model
     * @return asset table fragment
     */
    @GetMapping("/fragments/table")
    public String table(
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "type", required = false) AssetType type,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, accountId, type, description, page, size);
        return "investments/assets/list :: table";
    }

    /**
     * Opens the creation form.
     *
     * @param returnTo optional safe return path
     * @param model view model
     * @return asset form template
     */
    @GetMapping("/new")
    public String createForm(@RequestParam(name = "returnTo", required = false) String returnTo, Model model) {
        AssetForm form = new AssetForm();
        fillFormModel(model, form, "create", null, returnTo);
        return "investments/assets/form";
    }

    /**
     * Creates an investment asset from submitted form data.
     *
     * @param form submitted form
     * @param bindingResult validation result
     * @param returnTo optional safe return path
     * @param model view model
     * @return redirect or form template with errors
     */
    @PostMapping
    public String create(
        @Valid @ModelAttribute("form") AssetForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            fillFormModel(model, form, "create", null, returnTo);
            return "investments/assets/form";
        }

        assetService.create(toDomain(form), form.getAccountId());
        return ReturnPathSupport.resolveRedirect(returnTo, "/investments/assets");
    }

    /**
     * Opens the edit form for an existing asset.
     *
     * @param id asset identifier
     * @param returnTo optional safe return path
     * @param model view model
     * @return asset form template
     */
    @GetMapping("/{id}/edit")
    public String editForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Asset asset = assetService.findById(id);
        fillFormModel(model, fromDomain(asset), "edit", id, returnTo);
        return "investments/assets/form";
    }

    /**
     * Updates an existing investment asset.
     *
     * @param id asset identifier
     * @param form submitted form
     * @param bindingResult validation result
     * @param returnTo optional safe return path
     * @param model view model
     * @return redirect or form template with errors
     */
    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") AssetForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            fillFormModel(model, form, "edit", id, returnTo);
            return "investments/assets/form";
        }

        assetService.update(id, toDomain(form), form.getAccountId());
        return ReturnPathSupport.resolveRedirect(returnTo, "/investments/assets");
    }

    /**
     * Deletes one asset and refreshes the listing when called through HTMX.
     *
     * @param id asset identifier
     * @param page current page
     * @param size current page size
     * @param request HTTP request
     * @param model view model
     * @return redirect or table fragment
     */
    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "type", required = false) AssetType type,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        assetService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, accountId, type, description, page, size);
            return "investments/assets/list :: table";
        }
        return "redirect:/investments/assets";
    }

    /**
     * Deletes selected assets and refreshes the listing when called through HTMX.
     *
     * @param ids selected asset identifiers
     * @param page current page
     * @param size current page size
     * @param request HTTP request
     * @param model view model
     * @return redirect or table fragment
     */
    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @RequestParam(name = "accountId", required = false) Long accountId,
        @RequestParam(name = "type", required = false) AssetType type,
        @RequestParam(name = "description", required = false) String description,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        assetService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, accountId, type, description, page, size);
            return "investments/assets/list :: table";
        }
        return "redirect:/investments/assets";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AssetInUseException.class)
    public String handleInUse(AssetInUseException ex, HttpServletRequest request, Model model) {
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
        fillListing(model, null, null, null, null, null);
        model.addAttribute("hasError", true);
        if (isHtmx(request)) {
            return "investments/assets/list :: table";
        }
        return "investments/assets/list";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AssetNotFoundException.class)
    public String handleNotFound(AssetNotFoundException ex, HttpServletRequest request) {
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

    private void fillListing(Model model, Long accountId, AssetType type, String description, Integer page, Integer size) {
        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);
        String selectedDescription = normalizeFilter(description);
        PagedResult<Asset> pageResult = assetService.listPageOrdered(accountId, type, selectedDescription, requestedPage, pageSize);
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = assetService.listPageOrdered(accountId, type, selectedDescription, effectivePage, pageSize);
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        model.addAttribute("assets", pageResult.items());
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);
        model.addAttribute("accountOptions", accountService.listAvailableForEntryForm(accountId));
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedAssetType", type);
        model.addAttribute("selectedDescription", selectedDescription);
        model.addAttribute(
            "returnTo",
            ReturnPathSupport.buildReturnPath(
                "/investments/assets",
                "accountId", accountId,
                "type", type,
                "description", selectedDescription,
                "page", pagination.page(),
                "size", pagination.pageSize()
            )
        );
    }

    private void fillFormModel(Model model, AssetForm form, String mode, Long assetId, String returnTo) {
        model.addAttribute("form", form);
        model.addAttribute("mode", mode);
        model.addAttribute("assetId", assetId);
        model.addAttribute("assetTypes", AssetType.values());
        model.addAttribute("indexerTypes", AssetIndexerType.values());
        model.addAttribute("accounts", accountService.listAvailableForEntryForm(form.getAccountId()));
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/investments/assets"));
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private Asset toDomain(AssetForm form) {
        Asset asset = new Asset();
        asset.setType(form.getType());
        asset.setName(form.getName());
        asset.setTicker(form.getTicker());
        asset.setIsin(form.getIsin());
        asset.setIssuer(form.getIssuer());
        asset.setCurrency(form.getCurrency());
        asset.setMaturityDate(form.getMaturityDate());
        asset.setIndexerType(form.getIndexerType());
        asset.setIndexerSpread(form.getIndexerSpread());
        asset.setOpeningPositionDate(form.getOpeningPositionDate());
        asset.setOpeningPositionQuantity(form.getOpeningPositionQuantity());
        asset.setOpeningPositionTotalCost(form.getOpeningPositionTotalCost());
        asset.setOpeningPositionCurrency(form.getOpeningPositionCurrency());
        return asset;
    }

    private AssetForm fromDomain(Asset asset) {
        AssetForm form = new AssetForm();
        form.setAccountId(asset.getAccount().getId());
        form.setType(asset.getType());
        form.setName(asset.getName());
        form.setTicker(asset.getTicker());
        form.setIsin(asset.getIsin());
        form.setIssuer(asset.getIssuer());
        form.setCurrency(asset.getCurrency());
        form.setMaturityDate(asset.getMaturityDate());
        form.setIndexerType(asset.getIndexerType());
        form.setIndexerSpread(asset.getIndexerSpread());
        form.setOpeningPositionDate(asset.getOpeningPositionDate());
        form.setOpeningPositionQuantity(asset.getOpeningPositionQuantity());
        form.setOpeningPositionTotalCost(asset.getOpeningPositionTotalCost());
        form.setOpeningPositionCurrency(asset.getOpeningPositionCurrency());
        return form;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
