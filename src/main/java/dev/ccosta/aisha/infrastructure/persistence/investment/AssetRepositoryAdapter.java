package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.Asset;
import dev.ccosta.aisha.domain.investment.AssetRepository;
import dev.ccosta.aisha.domain.investment.AssetType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.text.Normalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class AssetRepositoryAdapter implements AssetRepository {

    private static final String DIACRITICS_PATTERN = "\\p{M}+";

    private final JpaAssetRepository jpaAssetRepository;

    public AssetRepositoryAdapter(JpaAssetRepository jpaAssetRepository) {
        this.jpaAssetRepository = jpaAssetRepository;
    }

    @Override
    public List<Asset> findAllOrdered() {
        return jpaAssetRepository.findAllByOrderByNameAscTickerAscIdAsc();
    }

    @Override
    public PagedResult<Asset> findPageOrdered(int page, int pageSize) {
        Page<Asset> result = jpaAssetRepository.findAllByOrderByNameAscTickerAscIdAsc(PageRequest.of(page, pageSize));
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public PagedResult<Asset> findPageOrdered(AssetType type, String descriptionFilter, int page, int pageSize) {
        Page<Asset> result = jpaAssetRepository.searchByFilters(
            type,
            normalizeTextFilter(descriptionFilter),
            PageRequest.of(page, pageSize)
        );
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<Asset> findById(Long id) {
        return jpaAssetRepository.findById(id);
    }

    @Override
    public Asset save(Asset asset) {
        return jpaAssetRepository.save(asset);
    }

    @Override
    public void deleteById(Long id) {
        jpaAssetRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaAssetRepository.deleteAllByIdInBatch(ids);
    }

    private String normalizeTextFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replaceAll(DIACRITICS_PATTERN, "");
        return escapeLikePattern(normalized).toUpperCase(Locale.ROOT);
    }

    private String escapeLikePattern(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
