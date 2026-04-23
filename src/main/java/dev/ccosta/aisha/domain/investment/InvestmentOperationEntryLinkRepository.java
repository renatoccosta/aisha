package dev.ccosta.aisha.domain.investment;

import java.util.Collection;
import java.util.List;

public interface InvestmentOperationEntryLinkRepository {

    List<InvestmentOperationEntryLink> findAllByOperationId(Long operationId);

    List<InvestmentOperationEntryLink> findAllByEntryId(Long entryId);

    InvestmentOperationEntryLink save(InvestmentOperationEntryLink link);

    void deleteByOperationId(Long operationId);

    void deleteByOperationIds(Collection<Long> operationIds);
}
