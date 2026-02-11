package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.category.CategoryService;
import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntryService {

    private final EntryRepository entryRepository;
    private final CategoryService categoryService;

    public EntryService(EntryRepository entryRepository, CategoryService categoryService) {
        this.entryRepository = entryRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public List<Entry> listTop100MostRecentBySettlementDate() {
        return entryRepository.listTop100MostRecentBySettlementDate();
    }

    @Transactional(readOnly = true)
    public Entry findById(Long id) {
        return entryRepository.findById(id)
            .orElseThrow(() -> new EntryNotFoundException(id));
    }

    @Transactional
    public Entry create(Entry entry, Long categoryId, String newCategoryTitle) {
        entry.setCategory(resolveCategory(categoryId, newCategoryTitle));
        return entryRepository.save(entry);
    }

    @Transactional
    public Entry update(Long id, Entry updatedData, Long categoryId, String newCategoryTitle) {
        Entry existing = findById(id);
        existing.setAccount(updatedData.getAccount());
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
}
