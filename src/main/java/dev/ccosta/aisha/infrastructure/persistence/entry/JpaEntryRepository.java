package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.EntryCategoryTrainingExample;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEntryRepository extends JpaRepository<Entry, Long> {

    @EntityGraph(attributePaths = {"account", "category", "suggestedCategory"})
    @Query(
        """
        select e
        from Entry e
        where e.settlementDate between :startDate and :endDate
          and (:accountId is null or e.account.id = :accountId)
          and (
                (:onlyWithoutCategory = true and e.category is null)
                or (:onlyWithoutCategory = false and (:categoryId is null or e.category.id = :categoryId))
          )
          and (
                :descriptionFilter is null
                or upper(function('translate', e.description, :accentedCharacters, :plainCharacters))
                    like concat(
                        '%',
                        upper(:descriptionFilter),
                        '%'
                    ) escape '\\'
          )
          and (:onlyPendingCategorySuggestions = false or e.categorySuggestionStatus = :pendingStatus)
        order by e.settlementDate desc, e.id desc
        """
    )
    Page<Entry> searchBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        @Param("accountId") Long accountId,
        @Param("categoryId") Long categoryId,
        @Param("descriptionFilter") String descriptionFilter,
        @Param("onlyWithoutCategory") boolean onlyWithoutCategory,
        @Param("onlyPendingCategorySuggestions") boolean onlyPendingCategorySuggestions,
        @Param("pendingStatus") EntryCategorySuggestionStatus pendingStatus,
        @Param("accentedCharacters") String accentedCharacters,
        @Param("plainCharacters") String plainCharacters,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"account", "category", "suggestedCategory"})
    List<Entry> findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(LocalDate endDate);

    @Query(
        """
        select new dev.ccosta.aisha.domain.entry.EntryCategoryTrainingExample(
            e.account.id,
            e.description,
            e.amount,
            e.category.id
        )
        from Entry e
        where e.category is not null
          and e.categorySuggestionStatus <> dev.ccosta.aisha.domain.entry.EntryCategorySuggestionStatus.PENDING
        order by e.id asc
        """
    )
    List<EntryCategoryTrainingExample> findCategoryTrainingExamples();

    boolean existsByAccountIdAndMovementDateAndSettlementDateAndDescriptionAndCategoryIdAndAmountAndExternalId(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    );

    @Query(
        """
        select (count(e) > 0)
        from Entry e
        where e.account.id = :accountId
          and e.movementDate = :movementDate
          and e.settlementDate = :settlementDate
          and e.description = :description
          and e.amount = :amount
          and (
                (:externalId is null and e.externalId is null)
                or e.externalId = :externalId
          )
        """
    )
    boolean existsDuplicateIgnoringCategory(
        @Param("accountId") Long accountId,
        @Param("movementDate") LocalDate movementDate,
        @Param("settlementDate") LocalDate settlementDate,
        @Param("description") String description,
        @Param("amount") BigDecimal amount,
        @Param("externalId") String externalId
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
