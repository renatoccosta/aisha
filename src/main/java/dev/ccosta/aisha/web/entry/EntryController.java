package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryOption;
import dev.ccosta.aisha.application.category.CategoryNotFoundException;
import dev.ccosta.aisha.application.category.CategoryService;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.util.StringUtils;
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

    private final EntryService entryService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public EntryController(EntryService entryService, AccountService accountService, CategoryService categoryService) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.categoryService = categoryService;
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
}
