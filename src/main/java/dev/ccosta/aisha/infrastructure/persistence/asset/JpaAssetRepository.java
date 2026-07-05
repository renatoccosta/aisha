package dev.ccosta.aisha.infrastructure.persistence.asset;

import dev.ccosta.aisha.domain.asset.Asset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaAssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {

    @EntityGraph(attributePaths = {"openingPosition"})
    List<Asset> findAllByOrderByNameAscTickerAscIdAsc();

    @EntityGraph(attributePaths = {"openingPosition"})
    Page<Asset> findAllByOrderByNameAscTickerAscIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"openingPosition"})
    Page<Asset> findAll(org.springframework.data.jpa.domain.Specification<Asset> specification, Pageable pageable);

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
