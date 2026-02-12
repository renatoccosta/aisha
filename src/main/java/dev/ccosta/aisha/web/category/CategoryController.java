package dev.ccosta.aisha.web.category;

import dev.ccosta.aisha.application.category.CategoryInUseException;
import dev.ccosta.aisha.application.category.CategoryNotFoundException;
import dev.ccosta.aisha.application.category.CategoryBalanceReportService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
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
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryBalanceReportService categoryBalanceReportService;

    public CategoryController(CategoryService categoryService, CategoryBalanceReportService categoryBalanceReportService) {
        this.categoryService = categoryService;
        this.categoryBalanceReportService = categoryBalanceReportService;
    }

    @GetMapping
    public String list(@ModelAttribute("globalDateFilter") DateFilterState globalDateFilter, Model model) {
        fillListing(model, globalDateFilter);
        return "categories/list";
    }

    @GetMapping("/fragments/table")
    public String table(@ModelAttribute("globalDateFilter") DateFilterState globalDateFilter, Model model) {
        fillListing(model, globalDateFilter);
        return "categories/list :: table";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CategoryForm());
        fillParentOptions(model, null);
        model.addAttribute("mode", "create");
        return "categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            fillParentOptions(model, null);
            model.addAttribute("mode", "create");
            return "categories/form";
        }

        categoryService.create(toDomain(form), form.getParentId());
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id);
        model.addAttribute("form", fromDomain(category));
        model.addAttribute("categoryId", id);
        fillParentOptions(model, id);
        model.addAttribute("mode", "edit");
        return "categories/form";
    }

    @PostMapping("/{id}")
    public String update(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") CategoryForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            fillParentOptions(model, id);
            model.addAttribute("mode", "edit");
            return "categories/form";
        }

        categoryService.update(id, toDomain(form), form.getParentId());
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpServletRequest request, Model model) {
        categoryService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model, null);
            return "categories/list :: table";
        }
        return "redirect:/categories";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        HttpServletRequest request,
        Model model
    ) {
        categoryService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model, null);
            return "categories/list :: table";
        }
        return "redirect:/categories";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler({CategoryInUseException.class, IllegalArgumentException.class})
    public String handleInUse(HttpServletRequest request, Model model) {
        fillListing(model, null);
        model.addAttribute("hasError", true);
        if (isHtmx(request)) {
            return "categories/list :: table";
        }
        return "categories/list";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(CategoryNotFoundException.class)
    public String handleNotFound() {
        return "errors/404";
    }

    private void fillListing(Model model, DateFilterState globalDateFilter) {
        List<Category> categories = categoryService.listAllOrdered();
        model.addAttribute("categories", categories);

        DateFilterState effectiveFilter = globalDateFilter != null
            ? globalDateFilter
            : (DateFilterState) model.getAttribute("globalDateFilter");
        if (effectiveFilter == null) {
            return;
        }

        model.addAttribute(
            "categoryBalanceReport",
            categoryBalanceReportService.buildReport(
                categories,
                effectiveFilter.getStartDate(),
                effectiveFilter.getEndDate()
            )
        );
    }

    private void fillParentOptions(Model model, Long currentCategoryId) {
        model.addAttribute("parentOptions", categoryService.listHierarchyOptions());
        model.addAttribute("currentCategoryId", currentCategoryId);
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private Category toDomain(CategoryForm form) {
        Category category = new Category();
        category.setTitle(form.getTitle());
        category.setDescription(form.getDescription());
        return category;
    }

    private CategoryForm fromDomain(Category category) {
        CategoryForm form = new CategoryForm();
        form.setTitle(category.getTitle());
        form.setDescription(category.getDescription());
        if (category.getParent() != null) {
            form.setParentId(category.getParent().getId());
        }
        return form;
    }
}
