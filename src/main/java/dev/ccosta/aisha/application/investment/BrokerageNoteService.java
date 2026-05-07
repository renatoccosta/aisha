package dev.ccosta.aisha.application.investment;

import dev.ccosta.aisha.domain.investment.BrokerageNote;
import dev.ccosta.aisha.domain.investment.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides read-only brokerage note queries for audit and navigation screens.
 */
@Service
public class BrokerageNoteService {

    private final BrokerageNoteRepository brokerageNoteRepository;

    public BrokerageNoteService(BrokerageNoteRepository brokerageNoteRepository) {
        this.brokerageNoteRepository = brokerageNoteRepository;
    }

    /**
     * Lists brokerage notes using settlement dates from the global filter and optional listing filters.
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
    @Transactional(readOnly = true)
    public PagedResult<BrokerageNote> listPageOrdered(
        LocalDate settlementStartDate,
        LocalDate settlementEndDate,
        Long accountId,
        LocalDate tradeStartDate,
        LocalDate tradeEndDate,
        String noteNumberPrefix,
        int page,
        int pageSize
    ) {
        if (settlementStartDate == null || settlementEndDate == null) {
            throw new IllegalArgumentException("Settlement start and end dates are required");
        }
        if (settlementEndDate.isBefore(settlementStartDate)) {
            throw new IllegalArgumentException("Settlement end date must be greater than or equal to start date");
        }
        if (tradeStartDate != null && tradeEndDate != null && tradeEndDate.isBefore(tradeStartDate)) {
            throw new IllegalArgumentException("Trade end date must be greater than or equal to start date");
        }
        return brokerageNoteRepository.findPageOrdered(
            settlementStartDate,
            settlementEndDate,
            accountId,
            tradeStartDate,
            tradeEndDate,
            noteNumberPrefix,
            page,
            pageSize
        );
    }

    /**
     * Finds a brokerage note by id.
     *
     * @param id brokerage note identifier
     * @return the matching brokerage note
     */
    @Transactional(readOnly = true)
    public BrokerageNote findById(Long id) {
        return brokerageNoteRepository.findById(id)
            .orElseThrow(() -> new BrokerageNoteNotFoundException(id));
    }

    /**
     * Finds the brokerage note that generated the given net financial entry.
     *
     * @param entryId net entry identifier
     * @return the matching brokerage note, when present
     */
    @Transactional(readOnly = true)
    public Optional<BrokerageNote> findByNetEntryId(Long entryId) {
        return brokerageNoteRepository.findByNetEntryId(entryId);
    }
}
