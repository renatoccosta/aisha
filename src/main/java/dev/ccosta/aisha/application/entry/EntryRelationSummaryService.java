package dev.ccosta.aisha.application.entry;

import dev.ccosta.aisha.application.entry.transfer.EntryTransferView;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransferRepository;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLinkRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds read-only relationship summaries for financial entries.
 */
@Service
public class EntryRelationSummaryService {

    private final EntryTransferRepository entryTransferRepository;
    private final InvestmentOperationEntryLinkRepository investmentOperationEntryLinkRepository;
    private final BrokerageNoteRepository brokerageNoteRepository;

    public EntryRelationSummaryService(
        EntryTransferRepository entryTransferRepository,
        InvestmentOperationEntryLinkRepository investmentOperationEntryLinkRepository,
        BrokerageNoteRepository brokerageNoteRepository
    ) {
        this.entryTransferRepository = entryTransferRepository;
        this.investmentOperationEntryLinkRepository = investmentOperationEntryLinkRepository;
        this.brokerageNoteRepository = brokerageNoteRepository;
    }

    /**
     * Summarizes relationships for a single entry.
     *
     * @param entry entry whose relationships should be resolved
     * @return relationship summary for the entry
     */
    @Transactional(readOnly = true)
    public EntryRelationSummary summarize(Entry entry) {
        return summarize(List.of(entry)).getOrDefault(entry.getId(), EntryRelationSummary.empty(entry.getId()));
    }

    /**
     * Summarizes relationships for a page or batch of entries using batch queries.
     *
     * @param entries entries whose relationships should be resolved
     * @return summaries keyed by entry id
     */
    @Transactional(readOnly = true)
    public Map<Long, EntryRelationSummary> summarize(Collection<Entry> entries) {
        Map<Long, EntryRelationSummary> summariesByEntryId = initializeSummaries(entries);
        if (summariesByEntryId.isEmpty()) {
            return summariesByEntryId;
        }

        List<Long> entryIds = List.copyOf(summariesByEntryId.keySet());
        fillTransfers(summariesByEntryId, entryTransferRepository.findAllByEntryIds(entryIds));
        fillInvestmentOperations(summariesByEntryId, investmentOperationEntryLinkRepository.findAllByEntryIds(entryIds));
        fillBrokerageNotes(summariesByEntryId, brokerageNoteRepository.findAllByNetEntryIds(entryIds));
        return summariesByEntryId;
    }

    private Map<Long, EntryRelationSummary> initializeSummaries(Collection<Entry> entries) {
        Map<Long, EntryRelationSummary> summariesByEntryId = new LinkedHashMap<>();
        if (entries == null) {
            return summariesByEntryId;
        }
        for (Entry entry : entries) {
            if (entry != null && entry.getId() != null) {
                summariesByEntryId.put(entry.getId(), EntryRelationSummary.empty(entry.getId()));
            }
        }
        return summariesByEntryId;
    }

    private void fillTransfers(Map<Long, EntryRelationSummary> summariesByEntryId, List<EntryTransfer> transfers) {
        for (EntryTransfer transfer : transfers) {
            putTransferSummary(summariesByEntryId, transfer, transfer.getOriginEntry(), transfer.getDestinationEntry(), true);
            putTransferSummary(summariesByEntryId, transfer, transfer.getDestinationEntry(), transfer.getOriginEntry(), false);
        }
    }

    private void putTransferSummary(
        Map<Long, EntryRelationSummary> summariesByEntryId,
        EntryTransfer transfer,
        Entry currentEntry,
        Entry counterpartEntry,
        boolean originSide
    ) {
        if (currentEntry == null || currentEntry.getId() == null || counterpartEntry == null || counterpartEntry.getAccount() == null) {
            return;
        }
        EntryRelationSummary currentSummary = summariesByEntryId.get(currentEntry.getId());
        if (currentSummary == null) {
            return;
        }
        EntryTransferView transferView = new EntryTransferView(
            transfer.getId(),
            counterpartEntry.getId(),
            counterpartEntry.getAccount().getId(),
            counterpartEntry.getAccount().getTitle(),
            originSide
        );
        summariesByEntryId.put(currentEntry.getId(), currentSummary.withTransferView(transferView));
    }

    private void fillInvestmentOperations(Map<Long, EntryRelationSummary> summariesByEntryId, List<InvestmentOperationEntryLink> links) {
        for (InvestmentOperationEntryLink link : links) {
            if (link.getEntry() == null || link.getEntry().getId() == null || link.getOperation() == null) {
                continue;
            }
            EntryRelationSummary currentSummary = summariesByEntryId.get(link.getEntry().getId());
            if (currentSummary != null) {
                EntryRelationSummary updatedSummary = currentSummary.withInvestmentOperationId(link.getOperation().getId());
                if (link.getOperation().getBrokerageNote() != null) {
                    updatedSummary = updatedSummary.withBrokerageNoteId(link.getOperation().getBrokerageNote().getId());
                }
                summariesByEntryId.put(link.getEntry().getId(), updatedSummary);
            }
        }
    }

    private void fillBrokerageNotes(Map<Long, EntryRelationSummary> summariesByEntryId, List<BrokerageNote> brokerageNotes) {
        for (BrokerageNote brokerageNote : brokerageNotes) {
            if (brokerageNote.getNetEntry() == null || brokerageNote.getNetEntry().getId() == null) {
                continue;
            }
            EntryRelationSummary currentSummary = summariesByEntryId.get(brokerageNote.getNetEntry().getId());
            if (currentSummary != null) {
                summariesByEntryId.put(brokerageNote.getNetEntry().getId(), currentSummary.withBrokerageNoteId(brokerageNote.getId()));
            }
        }
    }
}
