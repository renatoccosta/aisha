package dev.ccosta.aisha.infrastructure.persistence.category;

import dev.ccosta.aisha.domain.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"parent"})
    List<Category> findAllByOrderByTitleAscIdAsc();

    @EntityGraph(attributePaths = {"parent"})
    Page<Category> findAllByOrderByTitleAscIdAsc(Pageable pageable);

    Optional<Category> findByTitleIgnoreCase(String title);

    boolean existsByParentId(Long id);
}
