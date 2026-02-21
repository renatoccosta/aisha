package dev.ccosta.aisha.web.security;

import dev.ccosta.aisha.application.security.LocalUserAccountNotFoundException;
import dev.ccosta.aisha.application.security.LocalUserAccountSelfDeletionException;
import dev.ccosta.aisha.application.security.LocalUserAccountService;
import dev.ccosta.aisha.application.security.LocalUserAccountUsernameAlreadyExistsException;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.persistence.security.LocalUserAccount;
import dev.ccosta.aisha.web.pagination.PaginationSupport;
import dev.ccosta.aisha.web.pagination.PaginationView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
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
import org.springframework.util.StringUtils;

@Controller
@RequestMapping("/local-users")
public class LocalUserAccountController {

    private final LocalUserAccountService localUserAccountService;

    public LocalUserAccountController(LocalUserAccountService localUserAccountService) {
        this.localUserAccountService = localUserAccountService;
    }

    @GetMapping
    public String list(
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, page, size);
        return "local-users/list";
    }

    @GetMapping("/fragments/table")
    public String table(
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Model model
    ) {
        fillListing(model, page, size);
        return "local-users/list :: table";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new LocalUserAccountForm());
        model.addAttribute("mode", "create");
        return "local-users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") LocalUserAccountForm form, BindingResult bindingResult, Model model) {
        validateCreateForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "local-users/form";
        }

        localUserAccountService.create(form.getUsername(), form.getPassword(), form.isEnabled());
        return "redirect:/local-users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        LocalUserAccount account = localUserAccountService.findById(id);
        model.addAttribute("form", fromDomain(account));
        model.addAttribute("localUserId", id);
        model.addAttribute("mode", "edit");
        return "local-users/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") LocalUserAccountForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("localUserId", id);
            model.addAttribute("mode", "edit");
            return "local-users/form";
        }

        localUserAccountService.update(id, form.getUsername(), form.getPassword(), form.isEnabled());
        return "redirect:/local-users";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @PathVariable Long id,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Principal principal,
        HttpServletRequest request,
        Model model
    ) {
        localUserAccountService.deleteById(id, principal != null ? principal.getName() : null);
        if (isHtmx(request)) {
            fillListing(model, page, size);
            return "local-users/list :: table";
        }
        return "redirect:/local-users";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size,
        Principal principal,
        HttpServletRequest request,
        Model model
    ) {
        localUserAccountService.bulkDelete(ids, principal != null ? principal.getName() : null);
        if (isHtmx(request)) {
            fillListing(model, page, size);
            return "local-users/list :: table";
        }
        return "redirect:/local-users";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler({
        LocalUserAccountUsernameAlreadyExistsException.class,
        LocalUserAccountSelfDeletionException.class,
        IllegalArgumentException.class
    })
    public String handleBadRequest(RuntimeException ex, HttpServletRequest request, Model model) {
        fillListing(model, null, null);
        model.addAttribute("hasError", true);

        if (ex instanceof LocalUserAccountUsernameAlreadyExistsException) {
            model.addAttribute("errorKey", "localUsers.error.usernameAlreadyExists");
        } else if (ex instanceof LocalUserAccountSelfDeletionException) {
            model.addAttribute("errorKey", "localUsers.error.selfDeletion");
        } else {
            model.addAttribute("errorKey", "localUsers.error.invalidOperation");
        }

        if (isHtmx(request)) {
            return "local-users/list :: table";
        }
        return "local-users/list";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(LocalUserAccountNotFoundException.class)
    public String handleNotFound() {
        return "errors/404";
    }

    private void fillListing(Model model, Integer page, Integer size) {
        int requestedPage = PaginationSupport.sanitizePage(page);
        int pageSize = PaginationSupport.sanitizePageSize(size);
        PagedResult<LocalUserAccount> pageResult = localUserAccountService.listPageOrdered(requestedPage, pageSize);
        int effectivePage = PaginationSupport.clampPageIndex(requestedPage, pageResult.totalPages());
        if (effectivePage != requestedPage) {
            pageResult = localUserAccountService.listPageOrdered(effectivePage, pageSize);
        }

        PaginationView pagination = PaginationSupport.toView(pageResult);
        model.addAttribute("localUsers", pageResult.items());
        model.addAttribute("pagination", pagination);
        model.addAttribute("allowedPageSizes", PaginationSupport.ALLOWED_PAGE_SIZES);
    }

    private LocalUserAccountForm fromDomain(LocalUserAccount account) {
        LocalUserAccountForm form = new LocalUserAccountForm();
        form.setUsername(account.getUsername());
        form.setEnabled(account.isEnabled());
        return form;
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private void validateCreateForm(LocalUserAccountForm form, BindingResult bindingResult) {
        if (!StringUtils.hasText(form.getPassword())) {
            bindingResult.rejectValue("password", "localUserForm.password.notBlank");
        }
    }
}
