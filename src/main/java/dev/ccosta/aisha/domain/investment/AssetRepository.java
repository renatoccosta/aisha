package dev.ccosta.aisha.domain.investment;

import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    List<Asset> findAllOrdered();

    PagedResult<Asset> findPageOrdered(int page, int pageSize);

    PagedResult<Asset> findPageOrdered(AssetType type, String descriptionFilter, int page, int pageSize);

    Optional<Asset> findById(Long id);

    Asset save(Asset asset);

    void deleteById(Long id);

    void deleteByIds(Collection<Long> ids);
}
