package dev.ccosta.aisha.infrastructure.persistence.asset;

import dev.ccosta.aisha.application.search.TextSearchQuery;
import dev.ccosta.aisha.application.search.TextSearchQueryParser;
import dev.ccosta.aisha.domain.asset.Asset;
import dev.ccosta.aisha.domain.asset.AssetRepository;
import dev.ccosta.aisha.domain.asset.AssetType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.persistence.search.TextSearchPredicateBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepositoryAdapter implements AssetRepository {

    private final JpaAssetRepository jpaAssetRepository;
    private final TextSearchQueryParser textSearchQueryParser = new TextSearchQueryParser();

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
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("ticker"), Sort.Order.asc("id")));
        Page<Asset> result = jpaAssetRepository.findAll(
            assetSpecification(type, textSearchQueryParser.parse(descriptionFilter)),
            pageRequest
        );
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<Asset> findById(Long id) {
        return jpaAssetRepository.findById(id);
    }

    @Override
    public Optional<Asset> findByIsinIgnoreCase(String isin) {
        return jpaAssetRepository.findByIsinIgnoreCase(isin);
    }

    @Override
    public Optional<Asset> findByTickerIgnoreCase(String ticker) {
        return jpaAssetRepository.findByTickerIgnoreCase(ticker);
    }

    @Override
    public Optional<Asset> findByNameIgnoreCase(String name) {
        return jpaAssetRepository.findByNameIgnoreCase(name);
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

    private Specification<Asset> assetSpecification(AssetType type, TextSearchQuery descriptionQuery) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            Expression<String> searchableText = cb.concat(
                cb.concat(cb.concat(cb.coalesce(root.get("name"), ""), " "), cb.coalesce(root.get("ticker"), "")),
                cb.concat(" ", cb.coalesce(root.get("issuer"), ""))
            );
            Predicate textPredicate = TextSearchPredicateBuilder.build(cb, searchableText, descriptionQuery);
            if (textPredicate != null) {
                predicates.add(textPredicate);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
