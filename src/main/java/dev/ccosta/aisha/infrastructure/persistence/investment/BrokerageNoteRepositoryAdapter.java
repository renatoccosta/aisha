package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.BrokerageNoteRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;

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
}
