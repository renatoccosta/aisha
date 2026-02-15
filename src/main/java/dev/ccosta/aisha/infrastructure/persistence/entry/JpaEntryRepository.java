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

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findTop100BySettlementDateBetweenAndAccountIdOrderBySettlementDateDescIdDesc(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId
    );

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findTop100BySettlementDateBetweenAndCategoryIdOrderBySettlementDateDescIdDesc(
        LocalDate startDate,
        LocalDate endDate,
        Long categoryId
    );

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findTop100BySettlementDateBetweenAndAccountIdAndCategoryIdOrderBySettlementDateDescIdDesc(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId
    );

    @EntityGraph(attributePaths = {"account", "category"})
    List<Entry> findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(LocalDate endDate);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);
}
