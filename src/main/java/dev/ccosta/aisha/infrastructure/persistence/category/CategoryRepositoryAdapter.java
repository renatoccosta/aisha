package dev.ccosta.aisha.infrastructure.persistence.category;

import dev.ccosta.aisha.domain.category.Category;
import dev.ccosta.aisha.domain.category.CategoryRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final JpaCategoryRepository jpaCategoryRepository;

    public CategoryRepositoryAdapter(JpaCategoryRepository jpaCategoryRepository) {
        this.jpaCategoryRepository = jpaCategoryRepository;
    }

    @Override
    public List<Category> findAllOrdered() {
        return jpaCategoryRepository.findAllByOrderByTitleAscIdAsc();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jpaCategoryRepository.findById(id);
    }

    @Override
    public Optional<Category> findByTitleIgnoreCase(String title) {
        return jpaCategoryRepository.findByTitleIgnoreCase(title);
    }

    @Override
    public Category save(Category category) {
        return jpaCategoryRepository.save(category);
    }

    @Override
    public boolean existsByParentId(Long id) {
        return jpaCategoryRepository.existsByParentId(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaCategoryRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaCategoryRepository.deleteAllByIdInBatch(ids);
    }
}
