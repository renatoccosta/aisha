package dev.ccosta.aisha.domain.investment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvestmentOperationEntryLinkRepository {

    List<InvestmentOperationEntryLink> findAllByOperationId(Long operationId);

    List<InvestmentOperationEntryLink> findAllByEntryId(Long entryId);

    Optional<InvestmentOperationEntryLink> findByEntryId(Long entryId);

    boolean existsByEntryId(Long entryId);

    InvestmentOperationEntryLink save(InvestmentOperationEntryLink link);

    void deleteByOperationId(Long operationId);

    void deleteByOperationIds(Collection<Long> operationIds);
}
