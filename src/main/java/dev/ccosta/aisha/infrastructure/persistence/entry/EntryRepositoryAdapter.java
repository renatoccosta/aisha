package dev.ccosta.aisha.infrastructure.persistence.entry;

import dev.ccosta.aisha.application.search.TextSearchQuery;
import dev.ccosta.aisha.application.search.TextSearchQueryParser;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategorySuggestionStatus;
import dev.ccosta.aisha.domain.entry.categorization.EntryCategoryTrainingExample;
import dev.ccosta.aisha.domain.entry.EntryRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.persistence.search.TextSearchPredicateBuilder;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class EntryRepositoryAdapter implements EntryRepository {

    private final JpaEntryRepository jpaEntryRepository;
    private final TextSearchQueryParser textSearchQueryParser = new TextSearchQueryParser();

    public EntryRepositoryAdapter(JpaEntryRepository jpaEntryRepository) {
        this.jpaEntryRepository = jpaEntryRepository;
    }

    @Override
    public PagedResult<Entry> listMostRecentBySettlementDateBetweenAndFilters(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        String descriptionFilter,
        boolean onlyWithoutCategory,
        boolean onlyPendingCategorySuggestions,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(Sort.Order.desc("settlementDate"), Sort.Order.desc("id")));
        TextSearchQuery descriptionQuery = textSearchQueryParser.parse(descriptionFilter);
        Page<Entry> result = jpaEntryRepository.findAll(
            entrySpecification(
                startDate,
                endDate,
                accountId,
                categoryId,
                descriptionQuery,
                onlyWithoutCategory,
                onlyPendingCategorySuggestions
            ),
            pageRequest
        );
        return new PagedResult<>(
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Override
    public List<Entry> listAllBySettlementDateLessThanEqual(LocalDate endDate) {
        return jpaEntryRepository.findBySettlementDateLessThanEqualOrderBySettlementDateAscIdAsc(endDate);
    }

    @Override
    public List<EntryCategoryTrainingExample> listCategoryTrainingExamples() {
        return jpaEntryRepository.findCategoryTrainingExamples();
    }

    @Override
    public Optional<Entry> findById(Long id) {
        return jpaEntryRepository.findWithDetailsById(id);
    }

    @Override
    public Entry save(Entry entry) {
        return jpaEntryRepository.save(entry);
    }

    @Override
    public BigDecimal sumAmountByAccountIdAndSettlementDateBetween(Long accountId, LocalDate startDate, LocalDate endDate) {
        return jpaEntryRepository.sumAmountByAccountIdAndSettlementDateBetween(accountId, startDate, endDate);
    }

    @Override
    public Optional<LocalDate> findLatestSettlementDateByAccountId(Long accountId) {
        return Optional.ofNullable(jpaEntryRepository.findLatestSettlementDateByAccountId(accountId));
    }

    @Override
    public boolean existsDuplicate(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        Long categoryId,
        BigDecimal amount,
        String externalId
    ) {
        return jpaEntryRepository.existsByAccountIdAndMovementDateAndSettlementDateAndDescriptionAndCategoryIdAndAmountAndExternalId(
            accountId,
            movementDate,
            settlementDate,
            description,
            categoryId,
            amount,
            externalId
        );
    }

    @Override
    public boolean existsDuplicateIgnoringCategory(
        Long accountId,
        LocalDate movementDate,
        LocalDate settlementDate,
        String description,
        BigDecimal amount,
        String externalId
    ) {
        return jpaEntryRepository.existsDuplicateIgnoringCategory(
            accountId,
            movementDate,
            settlementDate,
            description,
            amount,
            externalId
        );
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return jpaEntryRepository.existsByCategoryId(categoryId);
    }

    @Override
    public boolean existsByAccountId(Long accountId) {
        return jpaEntryRepository.existsByAccountId(accountId);
    }

    @Override
    public void deleteById(Long id) {
        jpaEntryRepository.deleteById(id);
    }

    @Override
    public void deleteByIds(Collection<Long> ids) {
        jpaEntryRepository.deleteAllByIdInBatch(ids);
    }

    private Specification<Entry> entrySpecification(
        LocalDate startDate,
        LocalDate endDate,
        Long accountId,
        Long categoryId,
        TextSearchQuery descriptionQuery,
        boolean onlyWithoutCategory,
        boolean onlyPendingCategorySuggestions
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("settlementDate"), startDate, endDate));
            if (accountId != null) {
                predicates.add(cb.equal(root.get("account").get("id"), accountId));
            }
            if (onlyWithoutCategory) {
                predicates.add(cb.isNull(root.get("category")));
            } else if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (onlyPendingCategorySuggestions) {
                predicates.add(cb.equal(root.get("categorySuggestionStatus"), EntryCategorySuggestionStatus.PENDING));
            }
            Predicate textPredicate = TextSearchPredicateBuilder.build(cb, root.get("description"), descriptionQuery);
            if (textPredicate != null) {
                predicates.add(textPredicate);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
