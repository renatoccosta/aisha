package dev.ccosta.aisha.domain.investment;

import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    PagedResult<Asset> findPageOrdered(int page, int pageSize);

    PagedResult<Asset> findPageOrdered(Long accountId, AssetType type, String descriptionFilter, int page, int pageSize);

    List<Asset> findAllByAccountIdOrdered(Long accountId);

    Optional<Asset> findById(Long id);

    Asset save(Asset asset);

    boolean existsByAccountId(Long accountId);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
