package dev.ccosta.aisha.infrastructure.persistence.brokeragenote;

import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for imported brokerage notes.
 */
public interface JpaBrokerageNoteRepository extends JpaRepository<BrokerageNote, Long>, JpaSpecificationExecutor<BrokerageNote> {

    @Override
    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    Optional<BrokerageNote> findById(Long id);

    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    Optional<BrokerageNote> findByNetEntryId(Long entryId);

    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    List<BrokerageNote> findAllByNetEntryIdInOrderByIdAsc(Collection<Long> entryIds);

    /**
     * Searches brokerage notes for the listing screen.
     *
     * @param settlementStartDate inclusive settlement start date
     * @param settlementEndDate inclusive settlement end date
     * @param accountId optional account identifier
     * @param tradeStartDate optional inclusive trade start date
     * @param tradeEndDate optional inclusive trade end date
     * @param pageable pagination request
     * @return matching brokerage notes
     */
    @EntityGraph(attributePaths = {"netEntry", "netEntry.account"})
    Page<BrokerageNote> findAll(
        org.springframework.data.jpa.domain.Specification<BrokerageNote> specification,
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
