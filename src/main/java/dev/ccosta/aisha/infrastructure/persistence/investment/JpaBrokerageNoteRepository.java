package dev.ccosta.aisha.infrastructure.persistence.investment;

import dev.ccosta.aisha.domain.investment.BrokerageNote;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for imported brokerage notes.
 */
public interface JpaBrokerageNoteRepository extends JpaRepository<BrokerageNote, Long> {

    @Override
    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    Optional<BrokerageNote> findById(Long id);

    /**
     * Searches brokerage notes for the listing screen.
     *
     * @param settlementStartDate inclusive settlement start date
     * @param settlementEndDate inclusive settlement end date
     * @param accountId optional account identifier
     * @param tradeStartDate optional inclusive trade start date
     * @param tradeEndDate optional inclusive trade end date
     * @param noteNumberPrefix optional broker note number prefix
     * @param pageable pagination request
     * @return matching brokerage notes
     */
    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    @Query(
        """
        select n
        from BrokerageNote n
        where n.settlementDate between :settlementStartDate and :settlementEndDate
          and (:accountId is null or n.netEntry.account.id = :accountId)
          and (:tradeStartDate is null or n.tradeDate >= :tradeStartDate)
          and (:tradeEndDate is null or n.tradeDate <= :tradeEndDate)
          and (:noteNumberPrefix is null or upper(n.noteNumber) like concat(:noteNumberPrefix, '%') escape '\\')
        order by n.settlementDate desc, n.tradeDate desc, n.id desc
        """
    )
    Page<BrokerageNote> searchByFilters(
        @Param("settlementStartDate") LocalDate settlementStartDate,
        @Param("settlementEndDate") LocalDate settlementEndDate,
        @Param("accountId") Long accountId,
        @Param("tradeStartDate") LocalDate tradeStartDate,
        @Param("tradeEndDate") LocalDate tradeEndDate,
        @Param("noteNumberPrefix") String noteNumberPrefix,
        Pageable pageable
    );

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
