package dev.ccosta.aisha.infrastructure.persistence.asset;

import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAssetRepository extends JpaRepository<Asset, Long> {

    @EntityGraph(attributePaths = {"openingPosition"})
    List<Asset> findAllByOrderByNameAscTickerAscIdAsc();

    @EntityGraph(attributePaths = {"openingPosition"})
    Page<Asset> findAllByOrderByNameAscTickerAscIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"openingPosition"})
    @Query(
        """
        select a
        from Asset a
        where (:type is null or a.type = :type)
        order by a.name asc, a.ticker asc, a.id asc
        """
    )
    Page<Asset> searchByFiltersWithoutDescription(
        @Param("type") AssetType type,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"openingPosition"})
    @Query(
        """
        select a
        from Asset a
        where (:type is null or a.type = :type)
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
        order by a.name asc, a.ticker asc, a.id asc
        """
    )
    Page<Asset> searchByFilters(
        @Param("type") AssetType type,
        @Param("descriptionFilter") String descriptionFilter,
        Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"openingPosition"})
    Optional<Asset> findById(Long id);

    @EntityGraph(attributePaths = {"openingPosition"})
    Optional<Asset> findByIsinIgnoreCase(String isin);

    @EntityGraph(attributePaths = {"openingPosition"})
    Optional<Asset> findByTickerIgnoreCase(String ticker);

    @EntityGraph(attributePaths = {"openingPosition"})
    Optional<Asset> findByNameIgnoreCase(String name);
}
