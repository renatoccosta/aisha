package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.InvestmentOperation;
import dev.ccosta.aisha.domain.investment.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.investment.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class InvestmentOperationRepositoryAdapter implements InvestmentOperationRepository {

    private static final String DIACRITICS_PATTERN = "\\p{M}+";

    private final JpaInvestmentOperationRepository jpaInvestmentOperationRepository;

    public InvestmentOperationRepositoryAdapter(JpaInvestmentOperationRepository jpaInvestmentOperationRepository) {
        this.jpaInvestmentOperationRepository = jpaInvestmentOperationRepository;
    }

    @Override
    public PagedResult<InvestmentOperation> findPageOrdered(int page, int pageSize) {
        Page<InvestmentOperation> result = jpaInvestmentOperationRepository.findAllByOrderByTradeDateDescIdDesc(PageRequest.of(page, pageSize));
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public PagedResult<InvestmentOperation> findPageOrdered(
        LocalDate startDate,
        LocalDate endDate,
        String assetFilter,
        Long accountId,
        InvestmentOperationType operationType,
        int page,
        int pageSize
    ) {
        Page<InvestmentOperation> result = jpaInvestmentOperationRepository.searchByFilters(
            startDate,
            endDate,
            normalizeTextFilter(assetFilter),
            accountId,
            operationType,
            PageRequest.of(page, pageSize)
        );
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<InvestmentOperation> findById(Long id) {
        return jpaInvestmentOperationRepository.findById(id);
    }

    @Override
    public List<InvestmentOperation> findAllByAssetIdOrdered(Long assetId) {
        return jpaInvestmentOperationRepository.findAllByAssetIdOrderByTradeDateAscIdAsc(assetId);
    }

    @Override
    public InvestmentOperation save(InvestmentOperation operation) {
        return jpaInvestmentOperationRepository.save(operation);
    }

    @Override
    public boolean existsByAssetId(Long assetId) {
        return jpaInvestmentOperationRepository.existsByAssetId(assetId);
    }

    @Override
    public void deleteById(Long id) {
        jpaInvestmentOperationRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaInvestmentOperationRepository.deleteAllByIdInBatch(ids);
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
