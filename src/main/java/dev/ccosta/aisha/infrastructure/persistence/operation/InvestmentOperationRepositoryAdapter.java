package dev.ccosta.aisha.infrastructure.persistence.operation;

import dev.ccosta.aisha.application.search.TextSearchQuery;
import dev.ccosta.aisha.application.search.TextSearchQueryParser;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationType;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.persistence.search.TextSearchPredicateBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
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
public class InvestmentOperationRepositoryAdapter implements InvestmentOperationRepository {

    private final JpaInvestmentOperationRepository jpaInvestmentOperationRepository;
    private final TextSearchQueryParser textSearchQueryParser = new TextSearchQueryParser();

    public InvestmentOperationRepositoryAdapter(JpaInvestmentOperationRepository jpaInvestmentOperationRepository) {
        this.jpaInvestmentOperationRepository = jpaInvestmentOperationRepository;
    }

    @Override
    public List<InvestmentOperation> findAllOrdered() {
        return jpaInvestmentOperationRepository.findAllByOrderByTradeDateAscIdAsc();
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
        Long brokerageNoteId,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("id")));
        Page<InvestmentOperation> result = jpaInvestmentOperationRepository.findAll(
            operationSpecification(
                startDate,
                endDate,
                textSearchQueryParser.parse(assetFilter),
                accountId,
                operationType,
                brokerageNoteId
            ),
            pageRequest
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
    public boolean existsByExternalId(String externalId) {
        return jpaInvestmentOperationRepository.existsByExternalId(externalId);
    }

    @Override
    public boolean existsByAssetId(Long assetId) {
        return jpaInvestmentOperationRepository.existsByAssetId(assetId);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaInvestmentOperationRepository.existsByAccountId(accountId);
    }

    @Override
    public void deleteById(Long id) {
        jpaInvestmentOperationRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaInvestmentOperationRepository.deleteAllByIdInBatch(ids);
    }

    private Specification<InvestmentOperation> operationSpecification(
        LocalDate startDate,
        LocalDate endDate,
        TextSearchQuery assetQuery,
        Long accountId,
        InvestmentOperationType operationType,
        Long brokerageNoteId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (brokerageNoteId == null) {
                predicates.add(cb.between(root.get("settlementDate"), startDate, endDate));
            } else {
                predicates.add(cb.equal(root.get("brokerageNote").get("id"), brokerageNoteId));
            }
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (operationType != null) {
                predicates.add(cb.equal(root.get("operationType"), operationType));
            }
            Expression<String> searchableText = cb.concat(
                cb.concat(cb.coalesce(root.get("asset").get("name"), ""), " "),
                cb.coalesce(root.get("asset").get("ticker"), "")
            );
            Predicate textPredicate = TextSearchPredicateBuilder.build(cb, searchableText, assetQuery);
            if (textPredicate != null) {
                predicates.add(textPredicate);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
