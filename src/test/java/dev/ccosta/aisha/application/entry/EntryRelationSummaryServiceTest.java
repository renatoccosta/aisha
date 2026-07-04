package dev.ccosta.aisha.application.entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.ccosta.aisha.domain.account.Account;
import dev.ccosta.aisha.domain.entry.Entry;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransfer;
import dev.ccosta.aisha.domain.entry.transfer.EntryTransferRepository;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNote;
import dev.ccosta.aisha.domain.brokeragenote.BrokerageNoteRepository;
import dev.ccosta.aisha.domain.operation.InvestmentOperation;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLink;
import dev.ccosta.aisha.domain.operation.InvestmentOperationEntryLinkRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EntryRelationSummaryServiceTest {

    @Mock
    private EntryTransferRepository entryTransferRepository;

    @Mock
    private InvestmentOperationEntryLinkRepository investmentOperationEntryLinkRepository;

    @Mock
    private BrokerageNoteRepository brokerageNoteRepository;

    @Test
    void shouldSummarizeEntryRelationshipsInBatch() {
        Entry firstEntry = entryWithId(10L, accountWithId(1L, "Carteira"));
        Entry transferCounterpart = entryWithId(11L, accountWithId(2L, "Reserva"));
        Entry operationEntry = entryWithId(20L, accountWithId(1L, "Carteira"));
        Entry noteEntry = entryWithId(30L, accountWithId(1L, "Carteira"));
        EntryTransfer transfer = transferWithId(100L, firstEntry, transferCounterpart);
        BrokerageNote brokerageNoteFromOperation = brokerageNoteWithId(300L);
        InvestmentOperationEntryLink operationLink = operationLink(operationEntry, operationWithId(200L, brokerageNoteFromOperation));
        BrokerageNote brokerageNote = brokerageNoteWithId(301L);
        brokerageNote.setNetEntry(noteEntry);
        List<Entry> entries = List.of(firstEntry, operationEntry, noteEntry);
        List<Long> entryIds = List.of(10L, 20L, 30L);
        EntryRelationSummaryService service = new EntryRelationSummaryService(
            entryTransferRepository,
            investmentOperationEntryLinkRepository,
            brokerageNoteRepository
        );
        when(entryTransferRepository.findAllByEntryIds(entryIds)).thenReturn(List.of(transfer));
        when(investmentOperationEntryLinkRepository.findAllByEntryIds(entryIds)).thenReturn(List.of(operationLink));
        when(brokerageNoteRepository.findAllByNetEntryIds(entryIds)).thenReturn(List.of(brokerageNote));

        Map<Long, EntryRelationSummary> summaries = service.summarize(entries);

        assertThat(summaries.get(10L).transferView().counterpartEntryId()).isEqualTo(11L);
        assertThat(summaries.get(10L).transferView().counterpartAccountTitle()).isEqualTo("Reserva");
        assertThat(summaries.get(20L).investmentOperationId()).isEqualTo(200L);
        assertThat(summaries.get(20L).brokerageNoteId()).isEqualTo(300L);
        assertThat(summaries.get(30L).brokerageNoteId()).isEqualTo(301L);
    }

    private Entry entryWithId(Long id, Account account) {
        Entry entry = new Entry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setAccount(account);
        return entry;
    }

    private Account accountWithId(Long id, String title) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        account.setTitle(title);
        return account;
    }

    private EntryTransfer transferWithId(Long id, Entry originEntry, Entry destinationEntry) {
        EntryTransfer transfer = new EntryTransfer();
        ReflectionTestUtils.setField(transfer, "id", id);
        transfer.setOriginEntry(originEntry);
        transfer.setDestinationEntry(destinationEntry);
        return transfer;
    }

    private InvestmentOperationEntryLink operationLink(Entry entry, InvestmentOperation operation) {
        InvestmentOperationEntryLink link = new InvestmentOperationEntryLink();
        link.setEntry(entry);
        link.setOperation(operation);
        return link;
    }

    private InvestmentOperation operationWithId(Long id, BrokerageNote brokerageNote) {
        InvestmentOperation operation = new InvestmentOperation();
        ReflectionTestUtils.setField(operation, "id", id);
        operation.setBrokerageNote(brokerageNote);
        return operation;
    }

    private BrokerageNote brokerageNoteWithId(Long id) {
        BrokerageNote brokerageNote = new BrokerageNote();
        ReflectionTestUtils.setField(brokerageNote, "id", id);
        return brokerageNote;
    }
}
