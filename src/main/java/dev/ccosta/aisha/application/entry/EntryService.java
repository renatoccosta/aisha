package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.account.AccountService;
import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public EntryService(EntryRepository entryRepository, AccountService accountService, CategoryService categoryService) {
        this.entryRepository = entryRepository;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<Entry> listTop100MostRecentBySettlementDate() {
        return entryRepository.listTop100MostRecentBySettlementDate();
    }

    @Transactional(readOnly = true)
    public List<Entry> listTop100MostRecentBySettlementDateBetween(LocalDate startDate, LocalDate endDate) {
        return listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, null, null);
    }

    @Transactional(readOnly = true)
    public List<Entry> listTop100MostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }
        return entryRepository.listTop100MostRecentBySettlementDateBetweenAndFilters(startDate, endDate, accountId, categoryId);
    }

    @Transactional(readOnly = true)
    public Entry findById(Long id) {
        return entryRepository.findById(id)
            .orElseThrow(() -> new EntryNotFoundException(id));
    }

    @Transactional
    public Entry create(Entry entry, Long accountId, Long categoryId, String newCategoryTitle) {
        entry.setAccount(resolveAccount(accountId));
        entry.setCategory(resolveCategory(categoryId, newCategoryTitle));
        return entryRepository.save(entry);
    }

    @Transactional
    public Entry update(Long id, Entry updatedData, Long accountId, Long categoryId, String newCategoryTitle) {
        Entry existing = findById(id);
        existing.setAccount(resolveAccount(accountId));
        existing.setMovementDate(updatedData.getMovementDate());
        existing.setSettlementDate(updatedData.getSettlementDate());
        existing.setDescription(updatedData.getDescription());
        existing.setCategory(resolveCategory(categoryId, newCategoryTitle));
        existing.setNotes(updatedData.getNotes());
        existing.setAmount(updatedData.getAmount());
        return entryRepository.save(existing);
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
        entryRepository.deleteByIds(uniqueIds);
    }

    private Category resolveCategory(Long categoryId, String newCategoryTitle) {
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
}
