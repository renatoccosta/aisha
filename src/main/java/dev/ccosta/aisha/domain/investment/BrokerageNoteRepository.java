package dev.ccosta.aisha.domain.investment;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Provides persistence operations for imported brokerage notes and duplicate detection.
 */
public interface BrokerageNoteRepository {

    /**
     * Finds a brokerage note by its internal identifier.
     *
     * @param id brokerage note identifier
     * @return the matching brokerage note, when present
     */
    Optional<BrokerageNote> findById(Long id);

    /**
     * Finds a brokerage note by the broker-issued identity.
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
     * Checks whether a broker note was already imported.
     *
     * @param brokerCnpj broker CNPJ as captured from the note
     * @param noteNumber broker note number
     * @param tradeDate trading session date
     * @return true when an imported note already exists
     */
    boolean existsByBrokerCnpjAndNoteNumberAndTradeDate(
        String brokerCnpj,
        String noteNumber,
        LocalDate tradeDate
    );

    /**
     * Persists an imported brokerage note.
     *
     * @param brokerageNote brokerage note data
     * @return persisted brokerage note
     */
    BrokerageNote save(BrokerageNote brokerageNote);
}
