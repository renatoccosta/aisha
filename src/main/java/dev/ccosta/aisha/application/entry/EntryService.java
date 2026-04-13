package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.entry.EntrySource;
import dev.ccosta.aisha.domain.entry.EntryTransfer;
import dev.ccosta.aisha.domain.entry.EntryTransferRepository;
import dev.ccosta.aisha.domain.entry.EntryType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final EntryTransferRepository entryTransferRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public EntryService(
        EntryRepository entryRepository,
        EntryTransferRepository entryTransferRepository,
        AccountService accountService,
        CategoryService categoryService
    ) {
        this.entryRepository = entryRepository;
        this.entryTransferRepository = entryTransferRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PagedResult<Entry> listMostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        String descriptionFilter,
        boolean onlyWithoutCategory,
        boolean onlyPendingCategorySuggestions,
        int page,
        int pageSize
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }
        String effectiveDescriptionFilter = StringUtils.hasText(descriptionFilter) ? descriptionFilter.trim() : null;
        return entryRepository.listMostRecentBySettlementDateBetweenAndFilters(
            startDate,
            endDate,
            accountId,
            categoryId,
            effectiveDescriptionFilter,
            onlyWithoutCategory,
            onlyPendingCategorySuggestions,
            page,
            pageSize
        );
    }

    @Transactional(readOnly = true)
    public Entry findById(Long id) {
        return entryRepository.findById(id)
            .orElseThrow(() -> new EntryNotFoundException(id));
    }

    @Transactional
    public Entry create(Entry entry, Long accountId, EntryCategorySelection categorySelection) {
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(accountId, entry.getSettlementDate());
        entry.setAccount(resolveAccount(accountId));
        Category category = resolveCategory(categorySelection);
        entry.setCategory(category);
        applyCategorySuggestionFeedback(entry, category, categorySelection);
        applyManualMetadataForCreation(entry);
        Entry createdEntry = entryRepository.save(entry);
        accountService.adjustInitialBalanceForBackdatedEntry(accountId, createdEntry.getSettlementDate());
        return createdEntry;
    }

    @Transactional
    public Entry update(Long id, Entry updatedData, Long accountId, EntryCategorySelection categorySelection) {
        accountService.validateEntrySettlementDateAgainstAccountDeactivation(accountId, updatedData.getSettlementDate());
        Entry existing = findById(id);
        if (existing.isTransfer()) {
            throw new IllegalArgumentException("Transfer entries must be updated through transfer-specific flows");
        }
        existing.setAccount(resolveAccount(accountId));
        existing.setMovementDate(updatedData.getMovementDate());
        existing.setSettlementDate(updatedData.getSettlementDate());
        existing.setDescription(updatedData.getDescription());
        Category category = resolveCategory(categorySelection);
        existing.setCategory(category);
        applyCategorySuggestionFeedback(existing, category, categorySelection);
        existing.setNotes(updatedData.getNotes());
        existing.setAmount(updatedData.getAmount());
        applyManualMetadataForLegacyUpdate(existing);
        return entryRepository.save(existing);
    }

    @Transactional
    public Entry confirmCategorySuggestion(Long id) {
        Entry entry = findById(id);
        if (entry.getCategory() == null || entry.getSuggestedCategory() == null) {
            throw new IllegalArgumentException("Entry does not have a suggested category to confirm");
        }

        entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.ACCEPTED);
        if (entry.getCategorySuggestionConfidence() == null) {
            entry.setCategorySuggestionConfidence(1.0d);
        }
        return entryRepository.save(entry);
    }

    @Transactional
    public void deleteById(Long id) {
        findById(id);
        entryRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        List<EntryTransfer> transfers = entryTransferRepository.findAllByEntryIds(uniqueIds);
        LinkedHashSet<Long> effectiveIds = new LinkedHashSet<>(uniqueIds);
        for (EntryTransfer transfer : transfers) {
            effectiveIds.add(transfer.getOriginEntry().getId());
            effectiveIds.add(transfer.getDestinationEntry().getId());
        }
        entryTransferRepository.deleteAllByEntryIds(effectiveIds);
        entryRepository.deleteByIds(effectiveIds);
    }

    private Category resolveCategory(EntryCategorySelection categorySelection) {
        Long categoryId = categorySelection == null ? null : categorySelection.categoryId();
        String newCategoryTitle = categorySelection == null ? null : categorySelection.newCategoryTitle();
        String normalizedTitle = newCategoryTitle == null ? "" : newCategoryTitle.trim();
        if (!normalizedTitle.isBlank()) {
            return categoryService.findOrCreateByTitle(normalizedTitle);
        }

        if (categoryId == null) {
            throw new IllegalArgumentException("Category must be informed");
        }

        return categoryService.findById(categoryId);
    }

    private Account resolveAccount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account must be informed");
        }

        return accountService.findById(accountId);
    }

    private void applyManualMetadataForCreation(Entry entry) {
        if (entry.getEntrySource() == null) {
            entry.setEntrySource(EntrySource.MANUAL);
        }
        if (entry.getRegistrationDate() == null) {
            entry.setRegistrationDate(LocalDate.now());
        }
        if (entry.getEntryType() == null) {
            entry.setEntryType(EntryType.REGULAR);
        }
    }

    private void applyManualMetadataForLegacyUpdate(Entry entry) {
        if (entry.getEntrySource() != null) {
            return;
        }
        entry.setEntrySource(EntrySource.MANUAL);
        entry.setRegistrationDate(LocalDate.now());
    }

    private void applyCategorySuggestionFeedback(Entry entry, Category category, EntryCategorySelection selection) {
        if (selection == null || selection.suggestedCategoryId() == null) {
            entry.setSuggestedCategory(null);
            entry.setCategorySuggestionConfidence(null);
            entry.setCategorySuggestionStatus(EntryCategorySuggestionStatus.NONE);
            return;
        }

        Category suggestedCategory = category.getId() != null && category.getId().equals(selection.suggestedCategoryId())
            ? category
            : categoryService.findById(selection.suggestedCategoryId());

        entry.setSuggestedCategory(suggestedCategory);
        entry.setCategorySuggestionConfidence(selection.suggestedCategoryConfidence());
        boolean acceptedSuggestion = (selection.categoryId() != null && selection.categoryId().equals(selection.suggestedCategoryId()))
            || (category.getId() != null && category.getId().equals(suggestedCategory.getId()));
        entry.setCategorySuggestionStatus(
            acceptedSuggestion
                ? EntryCategorySuggestionStatus.ACCEPTED
                : EntryCategorySuggestionStatus.REJECTED
        );
    }
}
