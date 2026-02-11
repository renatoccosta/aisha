package dev.ccosta.aisha.infrastructure.persistence.category;

import dev.ccosta.aisha.domain.category.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"parent"})
    List<Category> findAllByOrderByTitleAscIdAsc();

    Optional<Category> findByTitleIgnoreCase(String title);

    boolean existsByParentId(Long id);
}
