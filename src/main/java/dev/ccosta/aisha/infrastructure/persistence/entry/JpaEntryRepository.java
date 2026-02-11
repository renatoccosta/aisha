package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEntryRepository extends JpaRepository<Entry, Long> {

    @EntityGraph(attributePaths = {"category"})
    List<Entry> findTop100ByOrderBySettlementDateDescIdDesc();

    boolean existsByCategoryId(Long categoryId);
}
