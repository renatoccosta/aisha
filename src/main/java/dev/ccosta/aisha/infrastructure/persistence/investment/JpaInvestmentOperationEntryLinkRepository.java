package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInvestmentOperationEntryLinkRepository extends JpaRepository<InvestmentOperationEntryLink, Long> {

    @EntityGraph(attributePaths = {"operation", "entry"})
    List<InvestmentOperationEntryLink> findAllByOperationIdOrderByIdAsc(Long operationId);

    @EntityGraph(attributePaths = {"operation", "entry"})
    List<InvestmentOperationEntryLink> findAllByEntryIdOrderByIdAsc(Long entryId);

    void deleteAllByOperationId(Long operationId);

    void deleteAllByOperationIdIn(Collection<Long> operationIds);
}
