package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByAccountIdAndMovementDateAndSettlementDateAndDescriptionAndCategoryIdAndAmountAndExternalId(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    );

    boolean existsByCategoryId(Long categoryId);

    boolean existsByAccountId(Long accountId);

    @Query(
        "select coalesce(sum(e.amount), 0) "
            + "from Entry e "
            + "where e.account.id = :accountId "
            + "and e.settlementDate between :startDate and :endDate"
    )
    BigDecimal sumAmountByAccountIdAndSettlementDateBetween(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("select max(e.settlementDate) from Entry e where e.account.id = :accountId")
    LocalDate findLatestSettlementDateByAccountId(@Param("accountId") Long accountId);
}
