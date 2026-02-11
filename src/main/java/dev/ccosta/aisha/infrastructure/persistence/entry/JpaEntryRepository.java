package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEntryRepository extends JpaRepository<Entry, Long> {

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findTop100ByOrderBySettlementDateDescIdDesc();

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findTop100BySettlementDateBetweenOrderBySettlementDateDescIdDesc(LocalDate startDate, LocalDate endDate);

    @EntityGraph(attributePaths = {"account"})
    List<Entry> findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(LocalDate endDate);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);
}
