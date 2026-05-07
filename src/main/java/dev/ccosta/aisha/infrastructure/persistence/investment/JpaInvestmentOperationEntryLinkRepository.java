package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvestmentOperationEntryLinkRepository extends JpaRepository<InvestmentOperationEntryLink, Long> {

    @EntityGraph(attributePaths = {"operation", "operation.brokerageNote", "entry"})
    List<InvestmentOperationEntryLink> findAllByOperationIdOrderByIdAsc(Long operationId);

    @EntityGraph(attributePaths = {"operation", "operation.brokerageNote", "entry"})
    List<InvestmentOperationEntryLink> findAllByEntryIdOrderByIdAsc(Long entryId);

    @EntityGraph(attributePaths = {"operation", "operation.brokerageNote", "entry"})
    List<InvestmentOperationEntryLink> findAllByEntryIdInOrderByIdAsc(Collection<Long> entryIds);

    @EntityGraph(attributePaths = {"operation", "operation.brokerageNote", "entry"})
    Optional<InvestmentOperationEntryLink> findByEntryId(Long entryId);

    boolean existsByEntryId(Long entryId);

    void deleteAllByOperationId(Long operationId);

    void deleteAllByOperationIdIn(Collection<Long> operationIds);
}
