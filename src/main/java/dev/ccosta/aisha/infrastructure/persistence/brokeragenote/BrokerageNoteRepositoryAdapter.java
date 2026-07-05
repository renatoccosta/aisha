package dev.ccosta.aisha.infrastructure.persistence.brokeragenote;

import dev.ccosta.aisha.application.search.TextSearchQuery;
import dev.ccosta.aisha.application.search.TextSearchQueryParser;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import dev.ccosta.aisha.infrastructure.persistence.search.TextSearchPredicateBuilder;
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

/**
 * Adapts Spring Data brokerage note persistence to the domain repository contract.
 */
@Repository
public class BrokerageNoteRepositoryAdapter implements BrokerageNoteRepository {

    private final JpaBrokerageNoteRepository jpaBrokerageNoteRepository;
    private final TextSearchQueryParser textSearchQueryParser = new TextSearchQueryParser();

    public BrokerageNoteRepositoryAdapter(JpaBrokerageNoteRepository jpaBrokerageNoteRepository) {
        this.jpaBrokerageNoteRepository = jpaBrokerageNoteRepository;
    }

    @Override
    public PagedResult<BrokerageNote> findPageOrdered(
        LocalDate settlementStartDate,
        LocalDate settlementEndDate,
        Long accountId,
        LocalDate tradeStartDate,
        LocalDate tradeEndDate,
        String noteNumberPrefix,
        int page,
        int pageSize
    ) {
        PageRequest pageRequest = PageRequest.of(
            page,
            pageSize,
            Sort.by(Sort.Order.desc("settlementDate"), Sort.Order.desc("tradeDate"), Sort.Order.desc("id"))
        );
        Page<BrokerageNote> result = jpaBrokerageNoteRepository.findAll(
            brokerageNoteSpecification(
                settlementStartDate,
                settlementEndDate,
                accountId,
                tradeStartDate,
                tradeEndDate,
                textSearchQueryParser.parse(noteNumberPrefix)
            ),
            pageRequest
        );
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<BrokerageNote> findById(Long id) {
        return jpaBrokerageNoteRepository.findById(id);
    }

    @Override
    public Optional<BrokerageNote> findByNetEntryId(Long entryId) {
        return jpaBrokerageNoteRepository.findByNetEntryId(entryId);
    }

    @Override
    public List<BrokerageNote> findAllByNetEntryIds(Collection<Long> entryIds) {
        return jpaBrokerageNoteRepository.findAllByNetEntryIdInOrderByIdAsc(entryIds);
    }

    @Override
    public Optional<BrokerageNote> findByBrokerCnpjAndNoteNumberAndTradeDate(
        String brokerCnpj,
        String noteNumber,
        LocalDate tradeDate
    ) {
        return jpaBrokerageNoteRepository.findByBrokerCnpjAndNoteNumberAndTradeDate(brokerCnpj, noteNumber, tradeDate);
    }

    @Override
    public boolean existsByBrokerCnpjAndNoteNumberAndTradeDate(
        String brokerCnpj,
        String noteNumber,
        LocalDate tradeDate
    ) {
        return jpaBrokerageNoteRepository.existsByBrokerCnpjAndNoteNumberAndTradeDate(brokerCnpj, noteNumber, tradeDate);
    }

    @Override
    public BrokerageNote save(BrokerageNote brokerageNote) {
        return jpaBrokerageNoteRepository.save(brokerageNote);
    }

    private Specification<BrokerageNote> brokerageNoteSpecification(
        LocalDate settlementStartDate,
        LocalDate settlementEndDate,
        Long accountId,
        LocalDate tradeStartDate,
        LocalDate tradeEndDate,
        TextSearchQuery noteNumberQuery
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("settlementDate"), settlementStartDate, settlementEndDate));
            if (accountId != null) {
                predicates.add(cb.equal(root.get("netEntry").get("account").get("id"), accountId));
            }
            if (tradeStartDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tradeDate"), tradeStartDate));
            }
            if (tradeEndDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tradeDate"), tradeEndDate));
            }
            Predicate textPredicate = TextSearchPredicateBuilder.build(cb, root.get("noteNumber"), noteNumberQuery);
            if (textPredicate != null) {
                predicates.add(textPredicate);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
