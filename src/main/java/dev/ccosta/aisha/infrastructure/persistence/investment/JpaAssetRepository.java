package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAssetRepository extends JpaRepository<Asset, Long> {

    @EntityGraph(attributePaths = {"account", "openingPosition"})
    List<Asset> findAllByOrderByAccountTitleAscNameAscTickerAscIdAsc();

    @EntityGraph(attributePaths = {"account", "openingPosition"})
    Page<Asset> findAllByOrderByAccountTitleAscNameAscTickerAscIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"account", "openingPosition"})
    @Query(
        """
        select a
        from Asset a
        where (:accountId is null or a.account.id = :accountId)
          and (:type is null or a.type = :type)
          and (
                :descriptionFilter is null
                or upper(
                    function(
                        'translate',
                        concat(coalesce(a.name, ''), ' ', coalesce(a.ticker, ''), ' ', coalesce(a.issuer, '')),
                        'ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑáàâãäéèêëíìîïóòôõöúùûüçñ',
                        'AAAAAEEEEIIIIOOOOOUUUUCNaaaaaeeeeiiiiooooouuuucn'
                    )
                )
                    like concat('%', :descriptionFilter, '%') escape '\\'
          )
        order by a.account.title asc, a.name asc, a.ticker asc, a.id asc
        """
    )
    Page<Asset> searchByFilters(
        @Param("accountId") Long accountId,
        @Param("type") AssetType type,
        @Param("descriptionFilter") String descriptionFilter,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"account", "openingPosition"})
    List<Asset> findAllByAccountIdOrderByNameAscTickerAscIdAsc(Long accountId);

    @Override
    @EntityGraph(attributePaths = {"account", "openingPosition"})
    java.util.Optional<Asset> findById(Long id);

    boolean existsByAccountId(Long accountId);
}
