package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInvestmentOperationRepository extends JpaRepository<InvestmentOperation, Long> {

    @EntityGraph(attributePaths = {"asset", "account"})
    Page<InvestmentOperation> findAllByOrderByTradeDateDescIdDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"asset", "account"})
    @Query(
        """
        select o
        from InvestmentOperation o
        where (:accountId is null or o.account.id = :accountId)
          and (:operationType is null or o.operationType = :operationType)
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
        @Param("assetFilter") String assetFilter,
        @Param("accountId") Long accountId,
        @Param("operationType") InvestmentOperationType operationType,
        Pageable pageable
    );

    boolean existsByAccountId(Long accountId);

    boolean existsByAssetId(Long assetId);
}
