package dev.ccosta.aisha.infrastructure.persistence.operation;

import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaInvestmentOperationRepository extends JpaRepository<InvestmentOperation, Long>, JpaSpecificationExecutor<InvestmentOperation> {

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
    Page<InvestmentOperation> findAll(
        org.springframework.data.jpa.domain.Specification<InvestmentOperation> specification,
        Pageable pageable
    );

    boolean existsByAssetId(Long assetId);

    boolean existsByExternalId(String externalId);

    boolean existsByAccountId(Long accountId);
}
