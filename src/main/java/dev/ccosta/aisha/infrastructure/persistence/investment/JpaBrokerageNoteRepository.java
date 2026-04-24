package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.BrokerageNote;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for imported brokerage notes.
 */
public interface JpaBrokerageNoteRepository extends JpaRepository<BrokerageNote, Long> {

    /**
     * Finds a brokerage note by its broker-issued identity.
     *
     * @param brokerCnpj broker CNPJ as captured from the note
     * @param noteNumber broker note number
     * @param tradeDate trading session date
     * @return the matching imported note, when present
     */
    Optional<BrokerageNote> findByBrokerCnpjAndNoteNumberAndTradeDate(
        String brokerCnpj,
        String noteNumber,
        LocalDate tradeDate
    );

    /**
     * Checks whether a brokerage note with the broker-issued identity exists.
     *
     * @param brokerCnpj broker CNPJ as captured from the note
     * @param noteNumber broker note number
     * @param tradeDate trading session date
     * @return true when the note exists
     */
    boolean existsByBrokerCnpjAndNoteNumberAndTradeDate(
        String brokerCnpj,
        String noteNumber,
        LocalDate tradeDate
    );
}
