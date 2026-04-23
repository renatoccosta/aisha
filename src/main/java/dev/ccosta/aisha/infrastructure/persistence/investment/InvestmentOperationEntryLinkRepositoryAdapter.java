package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.investment.InvestmentOperationEntryLinkRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InvestmentOperationEntryLinkRepositoryAdapter implements InvestmentOperationEntryLinkRepository {

    private final JpaInvestmentOperationEntryLinkRepository jpaLinkRepository;

    public InvestmentOperationEntryLinkRepositoryAdapter(JpaInvestmentOperationEntryLinkRepository jpaLinkRepository) {
        this.jpaLinkRepository = jpaLinkRepository;
    }

    @Override
    public List<InvestmentOperationEntryLink> findAllByOperationId(Long operationId) {
        return jpaLinkRepository.findAllByOperationIdOrderByIdAsc(operationId);
    }

    @Override
    public List<InvestmentOperationEntryLink> findAllByEntryId(Long entryId) {
        return jpaLinkRepository.findAllByEntryIdOrderByIdAsc(entryId);
    }

    @Override
    public InvestmentOperationEntryLink save(InvestmentOperationEntryLink link) {
        return jpaLinkRepository.save(link);
    }

    @Override
    public void deleteByOperationId(Long operationId) {
        jpaLinkRepository.deleteAllByOperationId(operationId);
    }

    @Override
    public void deleteByOperationIds(Collection<Long> operationIds) {
        jpaLinkRepository.deleteAllByOperationIdIn(operationIds);
    }
}
