package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * Adapts Spring Data brokerage note persistence to the domain repository contract.
 */
@Repository
public class BrokerageNoteRepositoryAdapter implements BrokerageNoteRepository {

    private final JpaBrokerageNoteRepository jpaBrokerageNoteRepository;

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
        Page<BrokerageNote> result = jpaBrokerageNoteRepository.searchByFilters(
            settlementStartDate,
            settlementEndDate,
            accountId,
            tradeStartDate,
            tradeEndDate,
            normalizePrefixFilter(noteNumberPrefix),
            PageRequest.of(page, pageSize)
        );
        return new PagedResult<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public Optional<BrokerageNote> findById(Long id) {
        return jpaBrokerageNoteRepository.findById(id);
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

    private String normalizePrefixFilter(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return escapeLikePattern(value.trim()).toUpperCase(Locale.ROOT);
    }

    private String escapeLikePattern(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
