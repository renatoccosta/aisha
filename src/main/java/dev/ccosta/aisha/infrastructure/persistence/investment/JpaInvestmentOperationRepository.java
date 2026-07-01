package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInvestmentOperationRepository extends JpaRepository<InvestmentOperation, Long> {

    @EntityGraph(attributePaths = {"asset", "account"})
    java.util.List<InvestmentOperation> findAllByOrderByTradeDateAscIdAsc();

    @EntityGraph(attributePaths = {"asset", "account"})
    Page<InvestmentOperation> findAllByOrderByTradeDateDescIdDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"asset", "account", "brokerageNote"})
    Optional<InvestmentOperation> findById(Long id);

    @EntityGraph(attributePaths = {"asset", "account"})
    java.util.List<InvestmentOperation> findAllByAssetIdOrderByTradeDateAscIdAsc(Long assetId);

    @EntityGraph(attributePaths = {"asset", "account", "brokerageNote"})
    @Query(
        """
        select o
        from InvestmentOperation o
        where (:brokerageNoteId is not null or o.settlementDate between :startDate and :endDate)
          and (:accountId is null or o.account.id = :accountId)
          and (:operationType is null or o.operationType = :operationType)
          and (:brokerageNoteId is null or o.brokerageNote.id = :brokerageNoteId)
        order by o.tradeDate desc, o.id desc
        """
    )
    Page<InvestmentOperation> searchByFiltersWithoutAsset(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("accountId") Long accountId,
        @Param("operationType") InvestmentOperationType operationType,
        @Param("brokerageNoteId") Long brokerageNoteId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"asset", "account", "brokerageNote"})
    @Query(
        """
        select o
        from InvestmentOperation o
        where (:brokerageNoteId is not null or o.settlementDate between :startDate and :endDate)
          and (:accountId is null or o.account.id = :accountId)
          and (:operationType is null or o.operationType = :operationType)
          and (:brokerageNoteId is null or o.brokerageNote.id = :brokerageNoteId)
          and (
                :assetFilter is null
                or upper(
                    function(
                        'translate',
                        concat(coalesce(o.asset.name, ''), ' ', coalesce(o.asset.ticker, '')),
                        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
                        'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'
                    )
                )
                    like concat('%', :assetFilter, '%') escape '\\'
          )
        order by o.tradeDate desc, o.id desc
        """
    )
    Page<InvestmentOperation> searchByFilters(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("assetFilter") String assetFilter,
        @Param("accountId") Long accountId,
        @Param("operationType") InvestmentOperationType operationType,
        @Param("brokerageNoteId") Long brokerageNoteId,
        Pageable pageable
    );

    boolean existsByAssetId(Long assetId);

    boolean existsByExternalId(String externalId);

    boolean existsByAccountId(Long accountId);
}
