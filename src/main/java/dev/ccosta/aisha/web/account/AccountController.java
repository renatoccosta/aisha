package dev.ccosta.aisha.web.account;

import dev.ccosta.aisha.application.account.AccountInUseException;
import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.domain.account.Account;
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

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public String list(Model model) {
        fillListing(model);
        return "accounts/list";
    }

    @GetMapping("/fragments/table")
    public String table(Model model) {
        fillListing(model);
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

        accountService.create(toDomain(form));
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

        accountService.update(id, toDomain(form));
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpServletRequest request, Model model) {
        accountService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model);
            return "accounts/list :: table";
        }
        return "redirect:/accounts";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        HttpServletRequest request,
        Model model
    ) {
        accountService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model);
            return "accounts/list :: table";
        }
        return "redirect:/accounts";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(AccountInUseException.class)
    public String handleInUse(HttpServletRequest request, Model model) {
        fillListing(model);
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

    private void fillListing(Model model) {
        model.addAttribute("accounts", accountService.listAllOrdered());
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private Account toDomain(AccountForm form) {
        Account account = new Account();
        account.setTitle(form.getTitle());
        account.setDescription(form.getDescription());
        return account;
    }

    private AccountForm fromDomain(Account account) {
        AccountForm form = new AccountForm();
        form.setTitle(account.getTitle());
        form.setDescription(account.getDescription());
        return form;
    }
}
