package dev.ccosta.aisha.application.category;

import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EntryRepository entryRepository;

    public CategoryService(CategoryRepository categoryRepository, EntryRepository entryRepository) {
        this.categoryRepository = categoryRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> listAllOrdered() {
        return categoryRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<CategoryOption> listHierarchyOptions() {
        List<Category> categories = categoryRepository.findAllOrdered();
        return CategoryHierarchyBuilder.buildOptions(categories);
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    public Category create(Category category, Long parentId) {
        category.setParent(resolveParent(parentId, null));
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Category updatedData, Long parentId) {
        Category existing = findById(id);
        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
        existing.setParent(resolveParent(parentId, id));
        return categoryRepository.save(existing);
    }

    @Transactional
    public Category findOrCreateByTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Category title must not be blank");
        }

        return categoryRepository.findByTitleIgnoreCase(title)
            .orElseGet(() -> {
                Category category = new Category();
                category.setTitle(title);
                category.setDescription(null);
                category.setParent(null);
                return categoryRepository.save(category);
            });
    }

    @Transactional
    public void deleteById(Long id) {
        findById(id);
        ensureCategoryIsNotInUse(id);
        categoryRepository.deleteById(id);
    }

    @Transactional
    public void bulkDelete(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        for (Long id : uniqueIds) {
            findById(id);
            ensureCategoryIsNotInUse(id);
        }

        categoryRepository.deleteByIds(uniqueIds);
    }

    private Category resolveParent(Long parentId, Long currentId) {
        if (parentId == null) {
            return null;
        }

        Category parent = findById(parentId);
        ensureNoHierarchyCycle(currentId, parent);
        return parent;
    }

    private void ensureCategoryIsNotInUse(Long id) {
        if (categoryRepository.existsByParentId(id) || entryRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(id);
        }
    }

    private void ensureNoHierarchyCycle(Long currentId, Category parent) {
        if (currentId == null) {
            return;
        }

        Category cursor = parent;
        while (cursor != null) {
            if (currentId.equals(cursor.getId())) {
                throw new IllegalArgumentException("Category hierarchy cannot contain cycles");
            }
            cursor = cursor.getParent();
        }
    }
}
