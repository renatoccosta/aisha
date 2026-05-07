package dev.ccosta.aisha.domain.investment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import dev.ccosta.aisha.domain.shared.PagedResult;

/**
 * Provides persistence operations for imported brokerage notes and duplicate detection.
 */
public interface BrokerageNoteRepository {

    /**
     * Lists brokerage notes filtered by settlement date, account, trade date, and note number prefix.
     *
     * @param settlementStartDate inclusive settlement start date
     * @param settlementEndDate inclusive settlement end date
     * @param accountId optional account identifier
     * @param tradeStartDate optional inclusive trade start date
     * @param tradeEndDate optional inclusive trade end date
     * @param noteNumberPrefix optional broker note number prefix
     * @param page zero-based page number
     * @param pageSize number of records to return
     * @return a filtered page of brokerage notes
     */
    PagedResult<BrokerageNote> findPageOrdered(
        LocalDate settlementStartDate,
        LocalDate settlementEndDate,
        Long accountId,
        LocalDate tradeStartDate,
        LocalDate tradeEndDate,
        String noteNumberPrefix,
        int page,
        int pageSize
    );

    /**
     * Finds a brokerage note by its internal identifier.
     *
     * @param id brokerage note identifier
     * @return the matching brokerage note, when present
     */
    Optional<BrokerageNote> findById(Long id);

    /**
     * Finds the brokerage note whose net financial entry matches the given entry.
     *
     * @param entryId net entry identifier
     * @return the matching brokerage note, when present
     */
    Optional<BrokerageNote> findByNetEntryId(Long entryId);

    /**
     * Finds brokerage notes whose net entries are included in the given identifiers.
     *
     * @param entryIds net entry identifiers
     * @return matching brokerage notes
     */
    List<BrokerageNote> findAllByNetEntryIds(Collection<Long> entryIds);

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
