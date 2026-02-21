package dev.ccosta.aisha.domain.category;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import dev.ccosta.aisha.domain.shared.PagedResult;

public interface CategoryRepository {

    List<Category> findAllOrdered();

    PagedResult<Category> findPageOrdered(int page, int pageSize);

    Optional<Category> findById(Long id);

    Optional<Category> findByTitleIgnoreCase(String title);

    Category save(Category category);

    boolean existsByParentId(Long id);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
