package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.domain.entry.Entry;
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
@RequestMapping("/entries")
public class EntryController {

    private final EntryService entryService;

    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    @GetMapping
    public String list(Model model) {
        fillListing(model);
        return "entries/list";
    }

    @GetMapping("/fragments/table")
    public String table(Model model) {
        fillListing(model);
        return "entries/list :: table";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", EntryForm.newWithCurrentDates());
        model.addAttribute("mode", "create");
        return "entries/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") EntryForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "entries/form";
        }

        entryService.create(toDomain(form));
        return "redirect:/entries";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Entry entry = entryService.findById(id);
        model.addAttribute("form", fromDomain(entry));
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
        if (bindingResult.hasErrors()) {
            model.addAttribute("entryId", id);
            model.addAttribute("mode", "edit");
            return "entries/form";
        }

        entryService.update(id, toDomain(form));
        return "redirect:/entries";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpServletRequest request, Model model) {
        entryService.deleteById(id);
        if (isHtmx(request)) {
            fillListing(model);
            return "entries/list :: table";
        }
        return "redirect:/entries";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(
        @RequestParam(name = "ids", required = false) List<Long> ids,
        HttpServletRequest request,
        Model model
    ) {
        entryService.bulkDelete(ids);
        if (isHtmx(request)) {
            fillListing(model);
            return "entries/list :: table";
        }
        return "redirect:/entries";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(EntryNotFoundException.class)
    public String handleNotFound() {
        return "errors/404";
    }

    private void fillListing(Model model) {
        model.addAttribute("entries", entryService.listTop100MostRecentBySettlementDate());
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private Entry toDomain(EntryForm form) {
        Entry entry = new Entry();
        entry.setAccount(form.getAccount());
        entry.setMovementDate(form.getMovementDate());
        entry.setSettlementDate(form.getSettlementDate());
        entry.setDescription(form.getDescription());
        entry.setCategory(form.getCategory());
        entry.setNotes(form.getNotes());
        entry.setAmount(form.getAmount());
        return entry;
    }

    private EntryForm fromDomain(Entry entry) {
        EntryForm form = new EntryForm();
        form.setAccount(entry.getAccount());
        form.setMovementDate(entry.getMovementDate());
        form.setSettlementDate(entry.getSettlementDate());
        form.setDescription(entry.getDescription());
        form.setCategory(entry.getCategory());
        form.setNotes(entry.getNotes());
        form.setAmount(entry.getAmount());
        return form;
    }
}
