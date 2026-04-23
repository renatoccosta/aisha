package dev.ccosta.aisha.domain.investment;

import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface InvestmentOperationRepository {

    PagedResult<InvestmentOperation> findPageOrdered(int page, int pageSize);

    PagedResult<InvestmentOperation> findPageOrdered(
        LocalDate startDate,
        LocalDate endDate,
        String assetFilter,
        Long accountId,
        InvestmentOperationType operationType,
        int page,
        int pageSize
    );

    Optional<InvestmentOperation> findById(Long id);

    InvestmentOperation save(InvestmentOperation operation);

    boolean existsByAssetId(Long assetId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
