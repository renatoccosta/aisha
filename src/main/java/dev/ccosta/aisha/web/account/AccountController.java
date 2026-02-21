package dev.ccosta.aisha.web.account;

import dev.ccosta.aisha.application.account.AccountInUseException;
import dev.ccosta.aisha.application.account.AccountInvalidDeactivationDateException;
import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountBalanceReportService;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountBalanceReportService accountBalanceReportService;

    public AccountController(AccountService accountService, AccountBalanceReportService accountBalanceReportService) {
        this.accountService = accountService;
        this.accountBalanceReportService = accountBalanceReportService;
    }

    @GetMapping
    public String list(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, page, size);
        return "accounts/list";
    }

    @GetMapping("/fragments/table")
    public String table(
        @ModelAttribute("globalDateFilter") DateFilterState globalDateFilter,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, globalDateFilter, page, size);
        return "accounts/list :: table";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new AccountForm());
        model.addAttribute("mode", "create");
        return "accounts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") AccountForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "accounts/form";
        }

        try {
            accountService.create(toDomain(form));
        } catch (AccountInvalidDeactivationDateException ex) {
            bindingResult.rejectValue(
                "deactivationDate",
                "accountForm.deactivationDate.invalid",
                new Object[] {ex.getLatestSettlementDate()},
                null
            );
            model.addAttribute("mode", "create");
            return "accounts/form";
        }
        return "redirect:/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Account account = accountService.findById(id);
        model.addAttribute("form", fromDomain(account));
        model.addAttribute("accountId", id);
        model.addAttribute("mode", "edit");
        return "accounts/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") AccountForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("accountId", id);
            model.addAttribute("mode", "edit");
            return "accounts/form";
        }

        try {
            accountService.update(id, toDomain(form));
        } catch (AccountInvalidDeactivationDateException ex) {
            bindingResult.rejectValue(
                "deactivationDate",
                "accountForm.deactivationDate.invalid",
                new Object[] {ex.getLatestSettlementDate()},
                null
            );
            model.addAttribute("accountId", id);
            model.addAttribute("mode", "edit");
            return "accounts/form";
        }
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        accountService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, null, page, size);
            return "accounts/list :: table";
        }
        return "redirect:/accounts";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        HttpServletRequest request,
        Model model
    ) {
        accountService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, null, page, size);
            return "accounts/list :: table";
        }
        return "redirect:/accounts";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(AccountInUseException.class)
    public String handleInUse(HttpServletRequest request, Model model) {
        fillListing(model, null, null, null);
        model.addAttribute("hasError", true);
        if (isHtmx(request)) {
            return "accounts/list :: table";
        }
        return "accounts/list";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(AccountNotFoundException.class)
    public String handleNotFound() {
        return "errors/404";
    }

    private void fillListing(Model model, DateFilterState globalDateFilter, Integer page, Integer size) {
        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);
        PagedResult<Account> pageResult = accountService.listPageOrdered(requestedPage, pageSize);
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = accountService.listPageOrdered(effectivePage, pageSize);
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        List<Account> accounts = pageResult.items();
        model.addAttribute("accounts", accounts);
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);

        DateFilterState effectiveFilter = globalDateFilter != null
            ? globalDateFilter
            : (DateFilterState) model.getAttribute("globalDateFilter");
        if (effectiveFilter == null) {
            return;
        }

        model.addAttribute(
            "accountBalanceReport",
            accountBalanceReportService.buildReport(
                accounts,
                effectiveFilter.getStartDate(),
                effectiveFilter.getEndDate()
            )
        );
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private Account toDomain(AccountForm form) {
        Account account = new Account();
        account.setTitle(form.getTitle());
        account.setDescription(form.getDescription());
        account.setInitialBalance(form.getInitialBalance());
        account.setInitialBalanceDate(form.getInitialBalanceDate());
        account.setDeactivationDate(form.getDeactivationDate());
        return account;
    }

    private AccountForm fromDomain(Account account) {
        AccountForm form = new AccountForm();
        form.setTitle(account.getTitle());
        form.setDescription(account.getDescription());
        form.setInitialBalance(account.getInitialBalance());
        form.setInitialBalanceDate(account.getInitialBalanceDate());
        form.setDeactivationDate(account.getDeactivationDate());
        return form;
    }
}
