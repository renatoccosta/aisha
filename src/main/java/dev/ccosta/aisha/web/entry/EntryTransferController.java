package dev.ccosta.aisha.web.entry;

import dev.ccosta.aisha.application.account.AccountNotFoundException;
import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.entry.EntryNotFoundException;
import dev.ccosta.aisha.application.entry.EntryService;
import dev.ccosta.aisha.application.entry.EntrySettlementAfterAccountDeactivationException;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferCounterpartRequest;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferCreationRequest;
import dev.ccosta.aisha.application.entry.transfer.EntryTransferService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.web.navigation.ReturnPathSupport;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles entry transfer creation, editing, linking, counterpart creation, and unlinking flows.
 */
@Controller
@RequestMapping("/entries")
public class EntryTransferController {

    private final EntryService entryService;
    private final AccountService accountService;
    private final EntryTransferService entryTransferService;
    private final EntryListingModelAssembler listingModelAssembler;
    private final MessageSource messageSource;

    public EntryTransferController(
        EntryService entryService,
        AccountService accountService,
        EntryTransferService entryTransferService,
        EntryListingModelAssembler listingModelAssembler,
        MessageSource messageSource
    ) {
        this.entryService = entryService;
        this.accountService = accountService;
        this.entryTransferService = entryTransferService;
        this.listingModelAssembler = listingModelAssembler;
        this.messageSource = messageSource;
    }

    /**
     * Renders the transfer creation form.
     *
     * @param returnTo optional safe return path
     * @param model the view model to populate
     * @return the transfer form template
     */
    @GetMapping("/transfers/new")
    public String createTransferForm(@RequestParam(name = "returnTo", required = false) String returnTo, Model model) {
        model.addAttribute("form", TransferForm.newWithCurrentDates());
        fillTransferFormAccountOptions(model, null, null);
        model.addAttribute("mode", "create");
        model.addAttribute("transferFormAction", "/entries/transfers");
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-form";
    }

    /**
     * Creates a transfer entry pair from the submitted form.
     *
     * @return a redirect to the safe return path, or the form template when validation fails
     */
    @PostMapping("/transfers")
    public String createTransfer(
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateDistinctAccounts(form.getOriginAccountId(), form.getDestinationAccountId(), "destinationAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (bindingResult.hasErrors()) {
            return renderTransferForm(model, form, "create", "/entries/transfers", null, returnTo);
        }

        try {
            entryTransferService.createTransfer(toTransferCreationRequest(form));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("originAccountId", "transferForm.originAccountId.notNull");
            return renderTransferForm(model, form, "create", "/entries/transfers", null, returnTo);
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            return renderTransferForm(model, form, "create", "/entries/transfers", null, returnTo);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            return renderTransferForm(model, form, "create", "/entries/transfers", null, returnTo);
        }
    }

    /**
     * Renders the transfer edit form for either side of an existing transfer.
     *
     * @param id an entry id that belongs to the transfer
     * @param returnTo optional safe return path
     * @param model the view model to populate
     * @return the transfer form template
     */
    @GetMapping("/{id}/transfer/edit")
    public String editTransferForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        EntryTransferCreationRequest request = toTransferCreationRequest(entryTransferService.findTransferByAnyEntryId(id));
        TransferForm form = new TransferForm();
        form.setOriginAccountId(request.originAccountId());
        form.setDestinationAccountId(request.destinationAccountId());
        form.setMovementDate(request.movementDate());
        form.setSettlementDate(request.settlementDate());
        form.setDescription(request.description());
        form.setAmount(request.amount());
        form.setNotes(request.notes());
        model.addAttribute("form", form);
        fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
        model.addAttribute("entryId", id);
        model.addAttribute("mode", "edit");
        model.addAttribute("transferFormAction", "/entries/transfers/" + id);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-form";
    }

    /**
     * Updates both sides of an existing transfer.
     *
     * @return a redirect to the safe return path, or the form template when validation fails
     */
    @PostMapping("/transfers/{id}")
    public String updateTransfer(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        validateDistinctAccounts(form.getOriginAccountId(), form.getDestinationAccountId(), "destinationAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (bindingResult.hasErrors()) {
            return renderTransferForm(model, form, "edit", "/entries/transfers/" + id, id, returnTo);
        }

        try {
            entryTransferService.updateTransferByEntryId(id, toTransferCreationRequest(form));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (AccountNotFoundException ex) {
            bindingResult.rejectValue("originAccountId", "transferForm.originAccountId.notNull");
            return renderTransferForm(model, form, "edit", "/entries/transfers/" + id, id, returnTo);
        } catch (EntrySettlementAfterAccountDeactivationException ex) {
            bindingResult.rejectValue(
                "settlementDate",
                "entryForm.settlementDate.afterAccountDeactivation",
                new Object[] {ex.getAccountDeactivationDate()},
                null
            );
            return renderTransferForm(model, form, "edit", "/entries/transfers/" + id, id, returnTo);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            return renderTransferForm(model, form, "edit", "/entries/transfers/" + id, id, returnTo);
        }
    }

    /**
     * Renders the counterpart creation form for a regular entry.
     *
     * @param id the source regular entry id
     * @param returnTo optional safe return path
     * @param model the view model to populate
     * @return the transfer counterpart form template
     */
    @GetMapping("/{id}/transfer-counterpart")
    public String createTransferCounterpartForm(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry entry = entryService.findById(id);
        TransferForm form = new TransferForm();
        form.setMovementDate(entry.getMovementDate());
        form.setSettlementDate(entry.getSettlementDate());
        form.setDescription(entry.getDescription());
        form.setNotes(entry.getNotes());
        form.setAmount(entry.getAmount().abs());
        if (entry.getAmount().signum() < 0) {
            form.setOriginAccountId(entry.getAccount().getId());
        } else {
            form.setDestinationAccountId(entry.getAccount().getId());
        }

        model.addAttribute("form", form);
        fillSourceEntrySummary(model, entry);
        fillEntryFormAccountOptions(model, entry.getAccount().getId());
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-counterpart-form";
    }

    /**
     * Creates the missing counterpart entry and converts the source entry into a transfer.
     *
     * @return a redirect to the safe return path, or the counterpart form when validation fails
     */
    @PostMapping("/{id}/transfer-counterpart")
    public String createTransferCounterpart(
        @PathVariable Long id,
        @Valid @ModelAttribute("form") TransferForm form,
        BindingResult bindingResult,
        @RequestParam(name = "returnTo", required = false) String returnTo,
        Model model
    ) {
        Entry sourceEntry = entryService.findById(id);
        Long fixedAccountId = sourceEntry.getAccount().getId();
        Long counterpartAccountId = sourceEntry.getAmount().signum() < 0 ? form.getDestinationAccountId() : form.getOriginAccountId();
        validateDistinctAccounts(fixedAccountId, counterpartAccountId, sourceEntry.getAmount().signum() < 0 ? "destinationAccountId" : "originAccountId", bindingResult);
        validatePositiveAmount(form.getAmount(), "amount", bindingResult);
        if (form.getAmount() != null && sourceEntry.getAmount() != null && form.getAmount().compareTo(sourceEntry.getAmount().abs()) != 0) {
            bindingResult.rejectValue("amount", "transferForm.amount.mustMatchSource");
        }
        if (bindingResult.hasErrors()) {
            fillSourceEntrySummary(model, sourceEntry);
            fillEntryFormAccountOptions(model, fixedAccountId);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-counterpart-form";
        }

        try {
            entryTransferService.createCounterpartFromEntry(id, new EntryTransferCounterpartRequest(
                counterpartAccountId,
                form.getMovementDate(),
                form.getSettlementDate(),
                form.getDescription(),
                form.getNotes()
            ));
            return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("transferForm.invalid", new Object[] {ex.getMessage()}, null);
            fillSourceEntrySummary(model, sourceEntry);
            fillEntryFormAccountOptions(model, fixedAccountId);
            model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
            return "entries/transfer-counterpart-form";
        }
    }

    /**
     * Links one selected regular entry as the transfer counterpart for the source entry.
     *
     * @return the listing fragment for HTMX requests, otherwise a redirect to the entries page
     */
    @PostMapping("/{id}/transfer-link")
    public String linkTransferFromSelection(
        @PathVariable Long id,
        @RequestParam(name = "ids", required = false) List<Long> selectedIds,
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
        List<Long> counterpartIds = selectedIds == null
            ? List.of()
            : selectedIds.stream()
                .filter(selectedId -> selectedId != null && !selectedId.equals(id))
                .distinct()
                .toList();

        if (counterpartIds.size() != 1) {
            listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            addToast(model, "entries.list.toast.transfer.link.selection.single", "error");
            return isHtmx(request) ? "entries/list :: listing" : "redirect:/entries";
        }

        try {
            entryTransferService.linkExistingEntries(id, counterpartIds.get(0));
            listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            addToast(model, "entries.list.toast.transfer.link.success", "success");
            return isHtmx(request) ? "entries/list :: listing" : "redirect:/entries";
        } catch (EntryNotFoundException ex) {
            listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            addResolvedToast(
                model,
                messageSource.getMessage(
                    "entries.list.toast.transfer.link.incompatible",
                    new Object[] {translateTransferLinkSelectionError("selected entry was not found")},
                    org.springframework.context.i18n.LocaleContextHolder.getLocale()
                ),
                "error"
            );
            return isHtmx(request) ? "entries/list :: listing" : "redirect:/entries";
        } catch (IllegalArgumentException ex) {
            listingModelAssembler.fillListing(model, globalDateFilter, accountId, categoryId, description, pendingSuggestions, page, size);
            addResolvedToast(
                model,
                messageSource.getMessage(
                    "entries.list.toast.transfer.link.incompatible",
                    new Object[] {translateTransferLinkSelectionError(ex.getMessage())},
                    org.springframework.context.i18n.LocaleContextHolder.getLocale()
                ),
                "error"
            );
            return isHtmx(request) ? "entries/list :: listing" : "redirect:/entries";
        }
    }

    /**
     * Removes the transfer link for the entry and redirects back to the safe return path.
     *
     * @param id an entry id that belongs to the transfer
     * @param returnTo optional safe return path
     * @return a redirect to the safe return path
     */
    @PostMapping("/{id}/transfer-unlink")
    public String unlinkTransfer(
        @PathVariable Long id,
        @RequestParam(name = "returnTo", required = false) String returnTo
    ) {
        entryTransferService.unlinkTransferByEntryId(id);
        return ReturnPathSupport.resolveRedirect(returnTo, "/entries");
    }

    private String renderTransferForm(Model model, TransferForm form, String mode, String action, Long entryId, String returnTo) {
        fillTransferFormAccountOptions(model, form.getOriginAccountId(), form.getDestinationAccountId());
        if (entryId != null) {
            model.addAttribute("entryId", entryId);
        }
        model.addAttribute("mode", mode);
        model.addAttribute("transferFormAction", action);
        model.addAttribute("returnTo", ReturnPathSupport.resolveReturnPath(returnTo, "/entries"));
        return "entries/transfer-form";
    }

    private void fillSourceEntrySummary(Model model, Entry entry) {
        model.addAttribute("sourceEntryId", entry.getId());
        model.addAttribute("sourceEntryAmount", entry.getAmount());
        model.addAttribute("sourceEntryAmountSign", entry.getAmount().signum());
        model.addAttribute("sourceEntryAccountTitle", accountService.findById(entry.getAccount().getId()).getTitle());
    }

    private void fillEntryFormAccountOptions(Model model, Long selectedAccountId) {
        List<Account> accounts = accountService.listAvailableForEntryForm(selectedAccountId);
        model.addAttribute("accountOptions", accounts);
    }

    private void fillTransferFormAccountOptions(Model model, Long originAccountId, Long destinationAccountId) {
        List<Account> accounts = accountService.listAllOrdered()
            .stream()
            .filter(account ->
                account.getDeactivationDate() == null
                    || (originAccountId != null && originAccountId.equals(account.getId()))
                    || (destinationAccountId != null && destinationAccountId.equals(account.getId()))
            )
            .toList();
        model.addAttribute("accountOptions", accounts);
    }

    private String translateTransferLinkSelectionError(String message) {
        if (message == null) {
            return "motivo não identificado";
        }
        return switch (message) {
            case "selected entry was not found" -> "o lançamento selecionado não foi encontrado";
            case "Entry is already part of a transfer" -> "o lançamento selecionado já faz parte de uma transferência";
            case "A transfer requires two distinct entries" -> "o lançamento complementar deve ser diferente do lançamento base";
            case "Both entries must belong to an account" -> "ambos os lançamentos devem pertencer a uma conta";
            case "Transfer entries must belong to different accounts" -> "os lançamentos devem pertencer a contas diferentes";
            case "Transfer entries must have non-zero amounts" -> "os lançamentos devem ter valores diferentes de zero";
            case "Transfer entries must have opposite signs" -> "os lançamentos devem ter sinais opostos";
            case "Transfer entries must have the same absolute amount" -> "os lançamentos devem ter o mesmo valor absoluto";
            case "Transfer entries must have matching movement and settlement dates" ->
                "os lançamentos devem ter as mesmas datas de movimentação e liquidação";
            default -> message;
        };
    }

    private void addToast(Model model, String messageKey, String level) {
        model.addAttribute(
            "toastMessage",
            messageSource.getMessage(messageKey, null, org.springframework.context.i18n.LocaleContextHolder.getLocale())
        );
        model.addAttribute("toastLevel", level);
    }

    private void addResolvedToast(Model model, String message, String level) {
        model.addAttribute("toastMessage", message);
        model.addAttribute("toastLevel", level);
    }

    private boolean isHtmx(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("HX-Request"));
    }

    private EntryTransferCreationRequest toTransferCreationRequest(TransferForm form) {
        return new EntryTransferCreationRequest(
            form.getOriginAccountId(),
            form.getDestinationAccountId(),
            form.getMovementDate(),
            form.getSettlementDate(),
            form.getDescription(),
            form.getAmount(),
            form.getNotes()
        );
    }

    private EntryTransferCreationRequest toTransferCreationRequest(dev.ccosta.aisha.domain.entry.transfer.EntryTransfer entryTransfer) {
        Entry originEntry = entryTransfer.getOriginEntry();
        Entry destinationEntry = entryTransfer.getDestinationEntry();
        return new EntryTransferCreationRequest(
            originEntry.getAccount().getId(),
            destinationEntry.getAccount().getId(),
            originEntry.getMovementDate(),
            originEntry.getSettlementDate(),
            originEntry.getDescription(),
            originEntry.getAmount().abs(),
            originEntry.getNotes()
        );
    }

    private void validateDistinctAccounts(Long firstAccountId, Long secondAccountId, String fieldName, BindingResult bindingResult) {
        if (firstAccountId != null && firstAccountId.equals(secondAccountId)) {
            bindingResult.rejectValue(fieldName, "transferForm.accounts.mustBeDifferent");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName, BindingResult bindingResult) {
        if (amount != null && amount.signum() <= 0) {
            bindingResult.rejectValue(fieldName, "transferForm.amount.positive");
        }
    }
}
