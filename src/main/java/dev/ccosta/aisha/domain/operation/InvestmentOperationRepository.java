package dev.ccosta.aisha.domain.operation;

import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvestmentOperationRepository {

    List<InvestmentOperation> findAllOrdered();

    PagedResult<InvestmentOperation> findPageOrdered(int page, int pageSize);

    PagedResult<InvestmentOperation> findPageOrdered(
        LocalDate startDate,
        LocalDate endDate,
        String assetFilter,
        Long accountId,
        InvestmentOperationType operationType,
        Long brokerageNoteId,
        int page,
        int pageSize
    );

    Optional<InvestmentOperation> findById(Long id);

    List<InvestmentOperation> findAllByAssetIdOrdered(Long assetId);

    InvestmentOperation save(InvestmentOperation operation);

    boolean existsByExternalId(String externalId);

    boolean existsByAssetId(Long assetId);

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
